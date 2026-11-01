package com.bank.gold.dto.request;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

/**
 * GOLD1006 提货申请 - 请求体。
 * <p>针对「账户存金」的订单发起实物提取，网点自提或快递配送。</p>
 */
@Data
@JacksonXmlRootElement(localName = "body")
@JsonPropertyOrder({"customerNo", "orderNo", "deliveryMode", "branchNo", "branchName",
        "receiverName", "receiverPhone", "receiverAddress"})
public class DeliveryRequest {

    private String customerNo;

    private String orderNo;

    /** PICKUP 网点自提 / EXPRESS 快递配送 */
    private String deliveryMode;

    /** 提货网点号，PICKUP 必填 */
    private String branchNo;

    /** 提货网点名称，PICKUP 必填 */
    private String branchName;

    /** 收件人，EXPRESS 必填 */
    private String receiverName;

    /** 收件人手机号，EXPRESS 必填 */
    private String receiverPhone;

    /** 收件地址，EXPRESS 必填 */
    private String receiverAddress;
}
