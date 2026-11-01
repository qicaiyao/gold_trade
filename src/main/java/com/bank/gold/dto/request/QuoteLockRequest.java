package com.bank.gold.dto.request;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.math.BigDecimal;

/**
 * GOLD1002 报价锁定 - 请求体。
 * <p>锁定成功后返回 quoteNo，在有效期内价格不受行情波动影响。</p>
 */
@Data
@JacksonXmlRootElement(localName = "body")
@JsonPropertyOrder({"customerNo", "productCode", "weight"})
public class QuoteLockRequest {

    private String customerNo;

    private String productCode;

    /** 拟购买克重，可选；填写时返回试算金额 */
    private BigDecimal weight;
}
