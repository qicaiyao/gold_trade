package com.bank.gold.protocol;

import com.bank.gold.common.GoldException;
import com.bank.gold.common.RespCode;
import com.bank.gold.common.TranCode;
import com.bank.gold.config.GoldTradeProperties;
import com.bank.gold.service.TranLogService;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 网关服务：解析报文 → 时间戳校验 → 验签 → 幂等 → 业务分发 → 组装并签名响应。
 */
@Slf4j
@Service
public class GoldGatewayService {

    private static final List<DateTimeFormatter> TIME_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    );

    private final SignService signService;
    private final TranLogService tranLogService;
    private final TranDispatcher dispatcher;
    private final XmlMapper xmlMapper;
    private final GoldTradeProperties properties;

    public GoldGatewayService(SignService signService,
                              TranLogService tranLogService,
                              TranDispatcher dispatcher,
                              XmlMapper xmlMapper,
                              GoldTradeProperties properties) {
        this.signService = signService;
        this.tranLogService = tranLogService;
        this.dispatcher = dispatcher;
        this.xmlMapper = xmlMapper;
        this.properties = properties;
    }

    /**
     * 处理一次接口调用，始终返回可被手机银行解析的 XML 报文。
     */
    public String handle(String rawXml) {
        long start = System.currentTimeMillis();
        String reqSerialNo = null;
        TranCode tranCode = null;
        boolean owned = false;

        try {
            XmlEnvelope envelope = XmlEnvelope.parse(rawXml);
            reqSerialNo = envelope.getHeader().reqSerialNo();
            tranCode = TranCode.of(envelope.getHeader().tranCode());
            checkRequestTime(envelope.getHeader().reqTime());
            signService.verify(envelope);

            TranLogService.Begin begin = tranLogService.begin(reqSerialNo, tranCode);
            owned = true;
            if (begin.cachedResponse() != null) {
                log.info("幂等命中，返回首次响应。流水号={}, 交易码={}", reqSerialNo, tranCode.getCode());
                return begin.cachedResponse();
            }

            Object body = dispatcher.dispatch(tranCode, envelope.bodyXml(), reqSerialNo);
            String bodyXml = body == null ? null : xmlMapper.writeValueAsString(body);
            String responseXml = XmlResponseBuilder.success(signService, tranCode, reqSerialNo, bodyXml);
            tranLogService.finishSuccess(reqSerialNo, responseXml, System.currentTimeMillis() - start);
            log.info("交易成功。流水号={}, 交易码={}, 耗时={}ms",
                    reqSerialNo, tranCode.getCode(), System.currentTimeMillis() - start);
            return responseXml;

        } catch (GoldException e) {
            log.warn("交易失败。流水号={}, 交易码={}, 响应码={}, 原因={}",
                    reqSerialNo, tranCode == null ? "-" : tranCode.getCode(), e.getRespCode().getCode(), e.getMessage());
            String responseXml = buildError(tranCode, reqSerialNo, e.getRespCode(), e.getMessage());
            if (owned) {
                tranLogService.finishFail(reqSerialNo, e.getRespCode(), e.getMessage(), responseXml,
                        System.currentTimeMillis() - start);
            }
            return responseXml;

        } catch (Exception e) {
            log.error("交易异常。流水号={}, 交易码={}", reqSerialNo, tranCode == null ? "-" : tranCode.getCode(), e);
            String responseXml = buildError(tranCode, reqSerialNo, RespCode.SYSTEM_ERROR, RespCode.SYSTEM_ERROR.getMessage());
            if (owned) {
                tranLogService.finishFail(reqSerialNo, RespCode.SYSTEM_ERROR, e.getMessage(), responseXml,
                        System.currentTimeMillis() - start);
            }
            return responseXml;
        }
    }

    /**
     * 防重放：请求时间与服务器时间偏差超过阈值直接拒绝。
     */
    private void checkRequestTime(String reqTime) {
        LocalDateTime time = null;
        for (DateTimeFormatter formatter : TIME_FORMATS) {
            try {
                time = LocalDateTime.parse(reqTime, formatter);
                break;
            } catch (Exception ignored) {
                // 尝试下一种格式
            }
        }
        if (time == null) {
            throw new GoldException(RespCode.PARAM_FORMAT_ERROR,
                    "header.reqTime 格式应为 yyyy-MM-dd HH:mm:ss");
        }
        long diff = Math.abs(Duration.between(time, LocalDateTime.now()).getSeconds());
        int tolerance = properties.getTimeToleranceSeconds();
        if (diff > tolerance) {
            throw new GoldException(RespCode.MSG_EXPIRED,
                    "请求时间与服务器时间偏差 " + diff + " 秒，超过允许的 " + tolerance + " 秒");
        }
    }

    private String buildError(TranCode tranCode, String reqSerialNo, RespCode respCode, String respMsg) {
        try {
            return XmlResponseBuilder.error(signService, tranCode, reqSerialNo, respCode, respMsg);
        } catch (Exception e) {
            log.error("组装错误响应失败", e);
            return XmlResponseBuilder.unsignedError(respCode, respMsg);
        }
    }
}
