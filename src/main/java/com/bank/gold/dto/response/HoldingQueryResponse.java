package com.bank.gold.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * GOLD1007 黄金持仓查询 - 响应体。
 */
@Data
@JacksonXmlRootElement(localName = "body")
@JsonPropertyOrder({"customerNo", "totalWeight", "totalCostAmount", "totalMarketValue", "totalProfit", "holdings"})
public class HoldingQueryResponse {

    private String customerNo;

    /** 持仓总克重 */
    private BigDecimal totalWeight;

    /** 持仓总成本 */
    private BigDecimal totalCostAmount;

    /** 按当前金价计算的市值 */
    private BigDecimal totalMarketValue;

    /** 浮动盈亏 */
    private BigDecimal totalProfit;

    @JacksonXmlElementWrapper(localName = "holdings")
    @JacksonXmlProperty(localName = "holding")
    private List<HoldingItem> holdings;

    @Data
    @JsonPropertyOrder({"productCode", "productName", "totalWeight", "availableWeight", "frozenWeight",
            "costAmount", "avgPrice", "marketValue", "profit"})
    public static class HoldingItem {
        private String productCode;
        private String productName;
        private BigDecimal totalWeight;
        /** 可提货克重 */
        private BigDecimal availableWeight;
        /** 已申请提货冻结克重 */
        private BigDecimal frozenWeight;
        private BigDecimal costAmount;
        /** 持仓均价（元/克） */
        private BigDecimal avgPrice;
        private BigDecimal marketValue;
        private BigDecimal profit;
    }
}
