package com.bank.gold.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.math.BigDecimal;

/**
 * GOLD1006 提货申请 - 响应体。
 */
@Data
@JacksonXmlRootElement(localName = "body")
@JsonPropertyOrder({"deliveryNo", "orderNo", "voucherNo", "deliveryMode", "weight", "status",
        "branchNo", "branchName", "expectTime"})
public class DeliveryResponse {

    private String deliveryNo;

    private String orderNo;

    /** 提货凭证号，客户凭此到网点提金 */
    private String voucherNo;

    private String deliveryMode;

    private BigDecimal weight;

    /** APPLIED 已受理 */
    private String status;

    private String branchNo;

    private String branchName;

    /** 预计可提货时间 */
    private String expectTime;
}
