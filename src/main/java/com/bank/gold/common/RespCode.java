package com.bank.gold.common;

/**
 * 接口响应码。
 * <p>约定：000000 成功；1xxxxx 报文/安全类错误；2xxxxx 业务类错误；999999 系统级错误。</p>
 */
public enum RespCode {

    SUCCESS("000000", "交易成功"),

    // ---------- 报文与安全 ----------
    MSG_FORMAT_ERROR("100001", "报文格式错误"),
    PARAM_MISSING("100002", "必填字段缺失"),
    PARAM_FORMAT_ERROR("100003", "字段格式错误"),
    SIGN_ERROR("100004", "签名验证失败"),
    MSG_EXPIRED("100005", "报文时间戳超出允许范围"),
    TRAN_NOT_SUPPORTED("100006", "不支持的交易码"),
    DUPLICATE_REQUEST("100007", "交易正在处理中，请勿重复提交"),

    // ---------- 业务 ----------
    CUSTOMER_INVALID("200001", "客户不存在或状态异常"),
    PRODUCT_INVALID("200002", "产品不存在或已下架"),
    QUOTE_INVALID("200003", "报价已失效，请重新获取"),
    WEIGHT_INVALID("200004", "购买克重不符合产品规格"),
    RISK_LEVEL_NOT_ENOUGH("200005", "客户风险等级不满足产品要求"),
    BALANCE_NOT_ENOUGH("200006", "账户可用余额不足"),
    SINGLE_LIMIT_EXCEEDED("200007", "超过单笔购买限额"),
    DAILY_LIMIT_EXCEEDED("200008", "超过单日累计购买限额"),
    ORDER_NOT_FOUND("200009", "订单不存在"),
    ORDER_STATUS_INVALID("200010", "订单状态不允许该操作"),
    DELIVERY_INFO_INVALID("200011", "提货信息不完整或不合法"),
    DELIVERY_EXISTS("200012", "该订单已申请提货"),
    HOLDING_NOT_ENOUGH("200013", "可提货克重不足"),

    // ---------- 系统 ----------
    SYSTEM_ERROR("999999", "系统繁忙，请稍后重试");

    private final String code;
    private final String message;

    RespCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
