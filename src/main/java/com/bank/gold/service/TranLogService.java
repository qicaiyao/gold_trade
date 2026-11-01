package com.bank.gold.service;

import com.bank.gold.common.GoldConstants;
import com.bank.gold.common.GoldException;
import com.bank.gold.common.RespCode;
import com.bank.gold.common.TranCode;
import com.bank.gold.entity.GoldTranLog;
import com.bank.gold.repository.GoldTranLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 幂等服务。
 *
 * <p>规则：同一 reqSerialNo 只允许产生一笔业务结果。</p>
 * <ul>
 *   <li>首次请求：插入流水（PROCESSING）后正常处理；</li>
 *   <li>重复请求且首次已成功：原样返回首次响应报文，不重复扣款；</li>
 *   <li>重复请求且首次仍在处理：返回 100007，渠道侧稍后查询；</li>
 *   <li>重复请求且首次失败：允许重新处理（避免网络抖动导致交易永久卡死）。</li>
 * </ul>
 */
@Slf4j
@Service
public class TranLogService {

    private final GoldTranLogRepository repository;
    private final TranLogWriter writer;

    public TranLogService(GoldTranLogRepository repository, TranLogWriter writer) {
        this.repository = repository;
        this.writer = writer;
    }

    public Begin begin(String reqSerialNo, TranCode tranCode) {
        try {
            writer.insertNew(reqSerialNo, tranCode.getCode());
            return new Begin(null);
        } catch (DataIntegrityViolationException e) {
            log.info("检测到重复请求流水号: {}", reqSerialNo);
            return handleDuplicate(reqSerialNo);
        }
    }

    private Begin handleDuplicate(String reqSerialNo) {
        GoldTranLog exist = repository.findByReqSerialNo(reqSerialNo)
                // 并发场景下另一事务尚未提交，按处理中返回
                .orElseThrow(() -> new GoldException(RespCode.DUPLICATE_REQUEST));
        if (GoldConstants.TRAN_SUCCESS.equals(exist.getStatus())) {
            return new Begin(exist.getRespXml());
        }
        if (GoldConstants.TRAN_PROCESSING.equals(exist.getStatus())) {
            throw new GoldException(RespCode.DUPLICATE_REQUEST);
        }
        // 上次失败：重置为处理中，允许重试
        writer.markProcessing(exist.getId());
        return new Begin(null);
    }

    public void finishSuccess(String reqSerialNo, String respXml, long costMs) {
        writer.finishSuccess(reqSerialNo, respXml, costMs);
    }

    public void finishFail(String reqSerialNo, RespCode respCode, String respMsg, String respXml, long costMs) {
        writer.finishFail(reqSerialNo, respCode.getCode(), respMsg, respXml, costMs);
    }

    /**
     * @param cachedResponse 非空表示命中幂等，直接返回首次响应报文
     */
    public record Begin(String cachedResponse) {
    }
}
