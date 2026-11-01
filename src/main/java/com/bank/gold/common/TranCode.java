package com.bank.gold.common;

import java.util.Arrays;

/**
 * 交易码。手机银行通过 header/tranCode 指定具体业务。
 */
public enum TranCode {

    GOLD1001("GOLD1001", "黄金产品与实时金价查询"),
    GOLD1002("GOLD1002", "报价锁定"),
    GOLD1003("GOLD1003", "实物黄金购买下单"),
    GOLD1004("GOLD1004", "购买订单查询"),
    GOLD1005("GOLD1005", "购买订单撤销"),
    GOLD1006("GOLD1006", "提货申请"),
    GOLD1007("GOLD1007", "黄金持仓查询");

    private final String code;
    private final String desc;

    TranCode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static TranCode of(String code) {
        return Arrays.stream(values())
                .filter(t -> t.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new GoldException(RespCode.TRAN_NOT_SUPPORTED, "不支持的交易码: " + code));
    }
}
