package com.bank.gold.protocol;

import com.bank.gold.common.GoldException;
import com.bank.gold.common.ParamCheck;
import com.bank.gold.common.RespCode;
import com.bank.gold.common.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * 请求报文信封：&lt;request&gt; + &lt;header&gt; + &lt;body&gt;。
 * <p>header 为公共字段（渠道、流水、签名等），body 为各交易码的业务字段。</p>
 */
public class XmlEnvelope {

    private static final String ROOT_NAME = "request";

    private final Document document;
    private final Element root;
    private final MsgHeader header;
    private final Element body;

    private XmlEnvelope(Document document, Element root, MsgHeader header, Element body) {
        this.document = document;
        this.root = root;
        this.header = header;
        this.body = body;
    }

    public static XmlEnvelope parse(String xml) {
        Document document = XmlUtils.parse(xml);
        Element root = document.getDocumentElement();
        if (!ROOT_NAME.equalsIgnoreCase(root.getTagName())) {
            throw new GoldException(RespCode.MSG_FORMAT_ERROR, "根节点必须为 <request>，实际为 <" + root.getTagName() + ">");
        }

        Element headerElement = XmlUtils.firstChild(root, "header");
        if (headerElement == null) {
            throw new GoldException(RespCode.PARAM_MISSING, "缺少 header 节点");
        }

        MsgHeader header = new MsgHeader(
                ParamCheck.notBlank(XmlUtils.childText(headerElement, "tranCode"), "header.tranCode", "交易码"),
                ParamCheck.notBlank(XmlUtils.childText(headerElement, "version"), "header.version", "接口版本"),
                ParamCheck.notBlank(XmlUtils.childText(headerElement, "channel"), "header.channel", "渠道号"),
                ParamCheck.notBlank(XmlUtils.childText(headerElement, "bankCode"), "header.bankCode", "机构号"),
                ParamCheck.notBlank(XmlUtils.childText(headerElement, "reqSerialNo"), "header.reqSerialNo", "请求流水号"),
                ParamCheck.notBlank(XmlUtils.childText(headerElement, "reqTime"), "header.reqTime", "请求时间"),
                ParamCheck.notBlank(XmlUtils.childText(headerElement, "signType"), "header.signType", "签名类型"),
                ParamCheck.optional(XmlUtils.childText(headerElement, "sign"))
        );
        ParamCheck.maxLength(header.reqSerialNo(), 32, "header.reqSerialNo");

        Element bodyElement = XmlUtils.firstChild(root, "body");
        return new XmlEnvelope(document, root, header, bodyElement);
    }

    /**
     * body 节点的 XML 文本（含 &lt;body&gt; 标签），用于反序列化为业务 DTO。
     */
    public String bodyXml() {
        if (body == null) {
            throw new GoldException(RespCode.PARAM_MISSING, "缺少 body 节点");
        }
        return XmlUtils.toXmlString(body);
    }

    public Document getDocument() {
        return document;
    }

    public Element getRoot() {
        return root;
    }

    public MsgHeader getHeader() {
        return header;
    }

    /**
     * 报文公共头字段。
     */
    public record MsgHeader(String tranCode,
                            String version,
                            String channel,
                            String bankCode,
                            String reqSerialNo,
                            String reqTime,
                            String signType,
                            String sign) {
    }
}
