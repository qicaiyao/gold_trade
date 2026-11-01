package com.bank.gold.common;

import java.math.BigDecimal;

/**
 * 字段级校验工具。银行接口需要把「哪个字段不合法」明确回给渠道，便于手机银行定位问题。
 */
public final class ParamCheck {

    private ParamCheck() {
    }

    public static String notBlank(String value, String field, String desc) {
        if (value == null || value.trim().isEmpty()) {
            throw new GoldException(RespCode.PARAM_MISSING, "缺少必填字段: " + field + "(" + desc + ")");
        }
        return value.trim();
    }

    public static String optional(String value) {
        return value == null ? null : value.trim();
    }

    public static BigDecimal positive(BigDecimal value, String field, String desc) {
        if (value == null) {
            throw new GoldException(RespCode.PARAM_MISSING, "缺少必填字段: " + field + "(" + desc + ")");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new GoldException(RespCode.PARAM_FORMAT_ERROR, field + "(" + desc + ")必须大于 0");
        }
        return value;
    }

    public static void maxLength(String value, int max, String field) {
        if (value != null && value.length() > max) {
            throw new GoldException(RespCode.PARAM_FORMAT_ERROR, field + " 长度不能超过 " + max + " 位");
        }
    }

    public static void assertTrue(boolean expression, RespCode code, String message) {
        if (!expression) {
            throw new GoldException(code, message);
        }
    }
}
