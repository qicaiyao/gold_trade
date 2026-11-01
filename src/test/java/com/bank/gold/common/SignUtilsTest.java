package com.bank.gold.common;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.security.KeyPair;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 签名与待签名串规则单元测试。
 */
class SignUtilsTest {

    private static final String XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <request>
              <header>
                <tranCode>GOLD1003</tranCode>
                <reqSerialNo>MB20261101000001</reqSerialNo>
                <sign>IGNORED</sign>
              </header>
              <body>
                <customerNo>C1000001</customerNo>
                <weight>10.0000</weight>
              </body>
            </request>
            """;

    @Test
    void canonicalNodesShouldFlattenLeafNodesAndSkipSign() {
        Document document = XmlUtils.parse(XML);
        TreeMap<String, String> nodes = SignUtils.canonicalNodes(document.getDocumentElement());

        assertEquals(4, nodes.size());
        assertFalse(nodes.containsKey("header.sign"), "sign 节点不应参与签名");
        assertEquals("GOLD1003", nodes.get("header.tranCode"));
        assertEquals("10.0000", nodes.get("body.weight"));
        assertEquals("body.customerNo=C1000001&body.weight=10.0000"
                        + "&header.reqSerialNo=MB20261101000001&header.tranCode=GOLD1003",
                SignUtils.canonicalString(nodes));
    }

    @Test
    void signatureShouldBeVerifiableAndTamperProof() {
        KeyPair keyPair = SignUtils.generateKeyPair();
        Document document = XmlUtils.parse(XML);
        String canonical = SignUtils.canonicalString(SignUtils.canonicalNodes(document.getDocumentElement()));

        String sign = SignUtils.sign(canonical, keyPair.getPrivate());
        assertTrue(SignUtils.verify(canonical, sign, keyPair.getPublic()));

        // 篡改任意字段后验签必须失败
        String tampered = canonical.replace("10.0000", "100.0000");
        assertFalse(SignUtils.verify(tampered, sign, keyPair.getPublic()));
    }

    @Test
    void repeatedSiblingNodesShouldBeIndexed() {
        String xml = """
                <body>
                  <products>
                    <product><code>A</code></product>
                    <product><code>B</code></product>
                  </products>
                </body>
                """;
        TreeMap<String, String> nodes = SignUtils.canonicalNodes(XmlUtils.parse(xml).getDocumentElement());

        assertEquals("A", nodes.get("products.product[1].code"));
        assertEquals("B", nodes.get("products.product[2].code"));
    }
}
