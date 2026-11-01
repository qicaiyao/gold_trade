package com.bank.gold.common;

/**
 * 业务异常：携带响应码，由网关统一转换为 XML 错误报文。
 */
public class GoldException extends RuntimeException {

    private final RespCode respCode;

    public GoldException(RespCode respCode) {
        super(respCode.getMessage());
        this.respCode = respCode;
    }

    public GoldException(RespCode respCode, String message) {
        super(message);
        this.respCode = respCode;
    }

    public RespCode getRespCode() {
        return respCode;
    }
}
