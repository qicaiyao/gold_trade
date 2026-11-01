package com.bank.gold.dto.request;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

/**
 * GOLD1005 购买订单撤销 - 请求体。
 * <p>仅支持当日、未提货的订单撤销，资金原路退回。</p>
 */
@Data
@JacksonXmlRootElement(localName = "body")
@JsonPropertyOrder({"customerNo", "orderNo", "reason"})
public class CancelRequest {

    private String customerNo;

    private String orderNo;

    /** 撤销原因 */
    private String reason;
}
