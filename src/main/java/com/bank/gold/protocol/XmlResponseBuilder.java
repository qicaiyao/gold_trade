package com.bank.gold.protocol;

import com.bank.gold.common.IdGen;
import com.bank.gold.common.RespCode;
import com.bank.gold.common.TranCode;
import com.bank.gold.common.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 响应报文组装：&lt;response&gt; + &lt;header&gt; + &lt;body&gt;，并对整包签名。
 */
public final class XmlResponseBuilder {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private XmlResponseBuilder() {
    }

    public static String success(SignService signService, TranCode tranCode, String reqSerialNo, String bodyXml) {
        return build(signService, tranCode, reqSerialNo, RespCode.SUCCESS, RespCode.SUCCESS.getMessage(), bodyXml);
    }

    public static String error(SignService signService, TranCode tranCode, String reqSerialNo,
                               RespCode respCode, String respMsg) {
        return build(signService, tranCode, reqSerialNo, respCode, respMsg, null);
    }

    /**
     * 报文级异常（如 Content-Type 不支持、XML 无法解析）的兜底响应，此时无法签名。
     */
    public static String unsignedError(RespCode respCode, String respMsg) {
        return build(null, null, null, respCode, respMsg, null);
    }

    private static String build(SignService signService, TranCode tranCode, String reqSerialNo,
                                RespCode respCode, String respMsg, String bodyXml) {
        Document document = XmlUtils.newDocument();
        Element root = document.createElement("response");
        document.appendChild(root);

        Element header = document.createElement("header");
        root.appendChild(header);
        XmlUtils.appendText(document, header, "tranCode", tranCode == null ? "UNKNOWN" : tranCode.getCode());
        XmlUtils.appendText(document, header, "respCode", respCode.getCode());
        XmlUtils.appendText(document, header, "respMsg", respMsg == null ? respCode.getMessage() : respMsg);
        XmlUtils.appendText(document, header, "reqSerialNo", reqSerialNo);
        XmlUtils.appendText(document, header, "respSerialNo", IdGen.respSerialNo());
        XmlUtils.appendText(document, header, "respTime", LocalDateTime.now().format(TIME_FORMAT));

        if (bodyXml != null && !bodyXml.isBlank()) {
            root.appendChild(XmlUtils.parseAndImport(document, bodyXml));
        }

        // 签名必须覆盖 header 与 body 的全部业务字段，sign 节点最后追加
        if (signService != null) {
            XmlUtils.appendText(document, header, "sign", signService.sign(root));
        }
        return XmlUtils.toXmlString(document, true);
    }
}
