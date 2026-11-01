package com.bank.gold.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * GOLD1001 黄金产品与实时金价查询 - 响应体。
 */
@Data
@JacksonXmlRootElement(localName = "body")
@JsonPropertyOrder({"queryTime", "totalCount", "products"})
public class ProductQueryResponse {

    private String queryTime;

    private Integer totalCount;

    @JacksonXmlElementWrapper(localName = "products")
    @JacksonXmlProperty(localName = "product")
    private List<ProductItem> products;

    @Data
    @JsonPropertyOrder({"productCode", "productName", "brand", "spec", "unit", "pricePerGram", "feePerGram",
            "minWeight", "maxWeightPerOrder", "stepWeight", "riskLevel", "status"})
    public static class ProductItem {
        private String productCode;
        private String productName;
        private String brand;
        private String spec;
        private String unit;
        /** 当前金价（元/克） */
        private BigDecimal pricePerGram;
        /** 加工费（元/克） */
        private BigDecimal feePerGram;
        private BigDecimal minWeight;
        private BigDecimal maxWeightPerOrder;
        private BigDecimal stepWeight;
        private String riskLevel;
        private String status;
    }
}
