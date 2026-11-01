package com.bank.gold.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * GOLD1004 购买订单查询 - 响应体。
 */
@Data
@JacksonXmlRootElement(localName = "body")
@JsonPropertyOrder({"totalCount", "orders"})
public class OrderQueryResponse {

    private Integer totalCount;

    @JacksonXmlElementWrapper(localName = "orders")
    @JacksonXmlProperty(localName = "order")
    private List<OrderItem> orders;

    @Data
    @JsonPropertyOrder({"orderNo", "productCode", "productName", "weight", "pricePerGram", "goldAmount",
            "feeAmount", "totalAmount", "deliveryMode", "status", "voucherNo", "orderTime"})
    public static class OrderItem {
        private String orderNo;
        private String productCode;
        private String productName;
        private BigDecimal weight;
        private BigDecimal pricePerGram;
        private BigDecimal goldAmount;
        private BigDecimal feeAmount;
        private BigDecimal totalAmount;
        private String deliveryMode;
        private String status;
        private String voucherNo;
        private String orderTime;
    }
}
