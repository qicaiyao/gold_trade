package com.bank.gold.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.math.BigDecimal;

/**
 * GOLD1002 报价锁定 - 响应体。
 */
@Data
@JacksonXmlRootElement(localName = "body")
@JsonPropertyOrder({"quoteNo", "customerNo", "productCode", "productName", "pricePerGram", "feePerGram",
        "weight", "goldAmount", "feeAmount", "totalAmount", "quoteTime", "expireTime", "expireSeconds"})
public class QuoteLockResponse {

    /** 报价单号，下单时回传 */
    private String quoteNo;

    private String customerNo;

    private String productCode;

    private String productName;

    /** 锁定金价（元/克） */
    private BigDecimal pricePerGram;

    /** 加工费（元/克） */
    private BigDecimal feePerGram;

    /** 试算克重（请求未传时为空） */
    private BigDecimal weight;

    /** 金价金额 */
    private BigDecimal goldAmount;

    /** 加工费金额 */
    private BigDecimal feeAmount;

    /** 合计应付 */
    private BigDecimal totalAmount;

    private String quoteTime;

    /** 报价失效时间 */
    private String expireTime;

    /** 剩余有效秒数 */
    private Integer expireSeconds;
}
