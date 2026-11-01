package com.bank.gold.common;

/**
 * 枚举值常量（数据库中以字符串存储，便于对账与人工排查）。
 */
public final class GoldConstants {

    private GoldConstants() {
    }

    /** 客户状态 */
    public static final String CUSTOMER_ACTIVE = "ACTIVE";
    public static final String CUSTOMER_FROZEN = "FROZEN";

    /** 产品状态 */
    public static final String PRODUCT_ON_SALE = "ON_SALE";
    public static final String PRODUCT_OFF_SALE = "OFF_SALE";

    /** 报价单状态 */
    public static final String QUOTE_UNUSED = "UNUSED";
    public static final String QUOTE_USED = "USED";
    public static final String QUOTE_EXPIRED = "EXPIRED";

    /** 订单状态 */
    public static final String ORDER_SUCCESS = "SUCCESS";
    public static final String ORDER_CANCELLED = "CANCELLED";

    /** 提货单状态 */
    public static final String DELIVERY_APPLIED = "APPLIED";
    public static final String DELIVERY_FINISHED = "FINISHED";
    public static final String DELIVERY_CANCELLED = "CANCELLED";

    /** 提货方式 */
    public static final String MODE_DEPOSIT = "DEPOSIT";
    public static final String MODE_PICKUP = "PICKUP";
    public static final String MODE_EXPRESS = "EXPRESS";

    /** 交易流水状态 */
    public static final String TRAN_PROCESSING = "PROCESSING";
    public static final String TRAN_SUCCESS = "SUCCESS";
    public static final String TRAN_FAIL = "FAIL";
}
