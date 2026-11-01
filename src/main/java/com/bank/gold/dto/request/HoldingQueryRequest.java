package com.bank.gold.dto.request;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

/**
 * GOLD1007 黄金持仓查询 - 请求体。
 */
@Data
@JacksonXmlRootElement(localName = "body")
@JsonPropertyOrder({"customerNo"})
public class HoldingQueryRequest {

    private String customerNo;
}
