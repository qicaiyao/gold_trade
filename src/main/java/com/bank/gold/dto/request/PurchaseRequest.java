package com.bank.gold.dto.request;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.math.BigDecimal;

/**
 * GOLD1003 实物黄金购买下单 - 请求体。
 */
@Data
@JacksonXmlRootElement(localName = "body")
@JsonPropertyOrder({"customerNo", "accountNo", "productCode", "quoteNo", "weight",
        "deliveryMode", "branchNo", "receiverName", "receiverPhone", "receiverAddress", "remark"})
public class PurchaseRequest {

    /** 客户号 */
    private String customerNo;

    /** 付款账号（借记卡） */
    private String accountNo;

    /** 产品代码 */
    private String productCode;

    /** 报价单号（GOLD1002 返回） */
    private String quoteNo;

    /** 购买克重，最多 4 位小数 */
    private BigDecimal weight;

    /** 提货方式：DEPOSIT 账户存金 / PICKUP 网点自提 / EXPRESS 快递配送 */
    private String deliveryMode;

    /** 提货网点号，deliveryMode=PICKUP 时必填 */
    private String branchNo;

    /** 收件人，deliveryMode=EXPRESS 时必填 */
    private String receiverName;

    /** 收件人手机号，deliveryMode=EXPRESS 时必填 */
    private String receiverPhone;

    /** 收件地址，deliveryMode=EXPRESS 时必填 */
    private String receiverAddress;

    /** 备注 */
    private String remark;
}
