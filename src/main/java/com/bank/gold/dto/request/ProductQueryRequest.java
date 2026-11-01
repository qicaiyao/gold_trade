package com.bank.gold.dto.request;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

/**
 * GOLD1001 黄金产品与实时金价查询 - 请求体。
 */
@Data
@JacksonXmlRootElement(localName = "body")
@JsonPropertyOrder({"productCode", "brand"})
public class ProductQueryRequest {

    /** 产品代码，可选；为空时返回全部在售产品 */
    private String productCode;

    /** 品牌，可选 */
    private String brand;
}
