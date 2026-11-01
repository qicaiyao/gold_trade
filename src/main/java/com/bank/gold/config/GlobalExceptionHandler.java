package com.bank.gold.config;

import com.bank.gold.common.RespCode;
import com.bank.gold.protocol.XmlResponseBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * 兜底异常处理：保证任何情况下都返回 XML（而非 Spring 默认的 JSON 错误结构），
 * 避免手机银行侧因报文格式不符而解析失败。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final MediaType XML_UTF8 = MediaType.parseMediaType("application/xml;charset=UTF-8");

    @ExceptionHandler({HttpMessageNotReadableException.class,
            HttpMediaTypeNotSupportedException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<String> handleBadRequest(Exception e) {
        log.warn("请求报文不合法: {}", e.getMessage());
        return build(RespCode.MSG_FORMAT_ERROR, "请求报文格式错误或 Content-Type 不支持");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        log.error("未捕获异常", e);
        return build(RespCode.SYSTEM_ERROR, RespCode.SYSTEM_ERROR.getMessage());
    }

    private ResponseEntity<String> build(RespCode respCode, String message) {
        String xml = XmlResponseBuilder.unsignedError(respCode, message);
        return ResponseEntity.ok().contentType(XML_UTF8).body(xml);
    }
}
