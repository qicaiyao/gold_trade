package com.bank.gold.dto.request;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

/**
 * GOLD1004 购买订单查询 - 请求体。
 */
@Data
@JacksonXmlRootElement(localName = "body")
@JsonPropertyOrder({"customerNo", "orderNo", "beginDate", "endDate"})
public class OrderQueryRequest {

    private String customerNo;

    /** 订单号，可选；填写时精确查询单笔 */
    private String orderNo;

    /** 起始日期 yyyy-MM-dd，可选 */
    private String beginDate;

    /** 截止日期 yyyy-MM-dd，可选 */
    private String endDate;
}
