package com.bank.gold.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.math.BigDecimal;

/**
 * GOLD1003 实物黄金购买下单 - 响应体。
 */
@Data
@JacksonXmlRootElement(localName = "body")
@JsonPropertyOrder({"orderNo", "status", "customerNo", "productCode", "productName", "weight", "pricePerGram",
        "goldAmount", "feeAmount", "totalAmount", "deliveryMode", "voucherNo", "deliveryNo",
        "accountNo", "balanceAfter", "holdingWeight", "orderTime"})
public class PurchaseResponse {

    /** 订单号 */
    private String orderNo;

    /** 订单状态 SUCCESS */
    private String status;

    private String customerNo;

    private String productCode;

    private String productName;

    /** 成交克重 */
    private BigDecimal weight;

    /** 成交金价（元/克） */
    private BigDecimal pricePerGram;

    private BigDecimal goldAmount;

    private BigDecimal feeAmount;

    private BigDecimal totalAmount;

    private String deliveryMode;

    /** 提货凭证号，DEPOSIT 方式为空 */
    private String voucherNo;

    /** 提货单号，DEPOSIT 方式为空 */
    private String deliveryNo;

    private String accountNo;

    /** 扣款后账户可用余额 */
    private BigDecimal balanceAfter;

    /** 本次买入后该产品累计持仓克重 */
    private BigDecimal holdingWeight;

    private String orderTime;
}
