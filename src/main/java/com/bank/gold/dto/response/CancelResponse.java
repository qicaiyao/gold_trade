package com.bank.gold.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.math.BigDecimal;

/**
 * GOLD1005 购买订单撤销 - 响应体。
 */
@Data
@JacksonXmlRootElement(localName = "body")
@JsonPropertyOrder({"orderNo", "status", "refundAmount", "accountNo", "balanceAfter", "holdingWeight", "cancelTime"})
public class CancelResponse {

    private String orderNo;

    /** 撤销后状态 CANCELLED */
    private String status;

    /** 退款金额（原路退回） */
    private BigDecimal refundAmount;

    private String accountNo;

    private BigDecimal balanceAfter;

    /** 撤销后该产品持仓克重 */
    private BigDecimal holdingWeight;

    private String cancelTime;
}
