package com.bank.gold;

import com.bank.gold.common.XmlUtils;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 网关集成测试：走完整 HTTP + XML 链路（H2 内存库）。
 * <p>演示环境关闭验签，签名逻辑由 {@link com.bank.gold.common.SignUtilsTest} 覆盖。</p>
 */
@SpringBootTest(properties = "gold.sign.enabled=false")
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GoldGatewayIntegrationTest {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final AtomicInteger SERIAL = new AtomicInteger(1000);

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    void fullPurchaseFlowShouldSucceed() throws Exception {
        // 1. 产品与金价查询
        Document products = call("GOLD1001", "<productCode>AU9999-10G</productCode>");
        assertSuccess(products);
        assertEquals("AU9999-10G", text(products, "products", "product", "productCode"));
        assertDecimal("618.50", text(products, "products", "product", "pricePerGram"));

        // 2. 报价锁定（含试算）
        String quoteNo = lockQuote("AU9999-10G", "10");
        assertNotNull(quoteNo);

        // 3. 购买下单（账户存金）：10 克 × 618.50 + 10 克 × 12.00 加工费 = 6305.00
        String orderNo = purchase(quoteNo, "AU9999-10G", "10", "DEPOSIT", "6305.00");

        // 4. 订单查询
        Document orderQuery = call("GOLD1004", "<customerNo>C1000001</customerNo><orderNo>" + orderNo + "</orderNo>");
        assertSuccess(orderQuery);
        assertEquals("1", text(orderQuery, "totalCount"));
        assertEquals(orderNo, text(orderQuery, "orders", "order", "orderNo"));

        // 5. 持仓查询：10 克可提，市值按当前金价计算
        Document holding = call("GOLD1007", "<customerNo>C1000001</customerNo>");
        assertSuccess(holding);
        assertDecimal("10.0000", text(holding, "totalWeight"));
        assertDecimal("10.0000", text(holding, "holdings", "holding", "availableWeight"));
        assertDecimal("6185.00", text(holding, "totalMarketValue"));

        // 6. 提货申请（存金转网点自提），可用克重转为冻结克重
        Document delivery = call("GOLD1006", "<customerNo>C1000001</customerNo><orderNo>" + orderNo
                + "</orderNo><deliveryMode>PICKUP</deliveryMode><branchNo>01001</branchNo>"
                + "<branchName>北京西单支行</branchName>");
        assertSuccess(delivery);
        assertEquals("APPLIED", text(delivery, "status"));
        assertNotNull(text(delivery, "voucherNo"));

        Document holdingAfterDelivery = call("GOLD1007", "<customerNo>C1000001</customerNo>");
        assertDecimal("0.0000", text(holdingAfterDelivery, "holdings", "holding", "availableWeight"));
        assertDecimal("10.0000", text(holdingAfterDelivery, "holdings", "holding", "frozenWeight"));

        // 7. 撤销订单：资金原路退回，持仓归零
        Document cancel = call("GOLD1005", "<customerNo>C1000001</customerNo><orderNo>" + orderNo
                + "</orderNo><reason>客户反悔</reason>");
        assertSuccess(cancel);
        assertEquals("CANCELLED", text(cancel, "status"));
        assertDecimal("6305.00", text(cancel, "refundAmount"));
        assertDecimal("0.0000", text(cancel, "holdingWeight"));
    }

    @Test
    @Order(2)
    void repeatedSerialNoShouldReturnFirstResultWithoutDoubleCharge() throws Exception {
        String serial = nextSerial();
        String quoteNo = lockQuote("AU9999-10G", "10");
        String body = "<customerNo>C1000001</customerNo><accountNo>6222021234567890</accountNo>"
                + "<productCode>AU9999-10G</productCode><quoteNo>" + quoteNo + "</quoteNo>"
                + "<weight>10</weight><deliveryMode>DEPOSIT</deliveryMode>";

        Document first = call("GOLD1003", body, serial);
        assertSuccess(first);
        String firstOrderNo = text(first, "orderNo");
        String firstBalance = text(first, "balanceAfter");

        // 同一流水号重复提交，必须返回首笔结果
        Document second = call("GOLD1003", body, serial);
        assertSuccess(second);
        assertEquals(firstOrderNo, text(second, "orderNo"));
        assertEquals(firstBalance, text(second, "balanceAfter"));
    }

    @Test
    @Order(3)
    void insufficientBalanceShouldBeRejected() throws Exception {
        String quoteNo = lockQuote("AU9999-10G", "10");
        Document response = call("GOLD1003", "<customerNo>C1000002</customerNo>"
                + "<accountNo>6222021234567891</accountNo><productCode>AU9999-10G</productCode>"
                + "<quoteNo>" + quoteNo + "</quoteNo><weight>10</weight><deliveryMode>DEPOSIT</deliveryMode>");
        assertEquals("200006", respCode(response));
    }

    @Test
    @Order(4)
    void invalidQuoteShouldBeRejected() throws Exception {
        Document response = call("GOLD1003", "<customerNo>C1000001</customerNo>"
                + "<accountNo>6222021234567890</accountNo><productCode>AU9999-10G</productCode>"
                + "<quoteNo>Q-NOT-EXIST</quoteNo><weight>10</weight><deliveryMode>DEPOSIT</deliveryMode>");
        assertEquals("200003", respCode(response));
    }

    @Test
    @Order(5)
    void offSaleProductShouldBeRejected() throws Exception {
        Document response = call("GOLD1001", "<productCode>AU9999-OLD</productCode>");
        assertSuccess(response);
        assertEquals("0", text(response, "totalCount"), "已下架产品不应出现在查询结果中");
    }

    @Test
    @Order(6)
    void missingRequiredFieldShouldReturnParamMissing() throws Exception {
        Document response = call("GOLD1007", "<phone>13800138000</phone>");
        assertEquals("100002", respCode(response));
    }

    @Test
    @Order(7)
    void unsupportedTranCodeShouldBeRejected() throws Exception {
        Document response = call("GOLD9999", "<customerNo>C1000001</customerNo>");
        assertEquals("100006", respCode(response));
    }

    // ------------------------------------------------------------------

    private String lockQuote(String productCode, String weight) throws Exception {
        Document response = call("GOLD1002", "<customerNo>C1000001</customerNo><productCode>" + productCode
                + "</productCode><weight>" + weight + "</weight>");
        assertSuccess(response);
        return text(response, "quoteNo");
    }

    private String purchase(String quoteNo, String productCode, String weight, String mode, String expectAmount)
            throws Exception {
        Document response = call("GOLD1003", "<customerNo>C1000001</customerNo>"
                + "<accountNo>6222021234567890</accountNo><productCode>" + productCode + "</productCode>"
                + "<quoteNo>" + quoteNo + "</quoteNo><weight>" + weight + "</weight>"
                + "<deliveryMode>" + mode + "</deliveryMode><branchNo>01001</branchNo>");
        assertSuccess(response);
        assertDecimal(expectAmount, text(response, "totalAmount"));
        return text(response, "orderNo");
    }

    private Document call(String tranCode, String bodyInner) throws Exception {
        return call(tranCode, bodyInner, nextSerial());
    }

    private Document call(String tranCode, String bodyInner, String serial) throws Exception {
        String request = """
                <?xml version="1.0" encoding="UTF-8"?>
                <request>
                  <header>
                    <tranCode>%s</tranCode>
                    <version>1.0.0</version>
                    <channel>MOBILE_BANK</channel>
                    <bankCode>ICBC</bankCode>
                    <reqSerialNo>%s</reqSerialNo>
                    <reqTime>%s</reqTime>
                    <signType>RSA2</signType>
                    <sign>DEMO</sign>
                  </header>
                  <body>%s</body>
                </request>
                """.formatted(tranCode, serial, LocalDateTime.now().format(TIME_FORMAT), bodyInner);

        MvcResult result = mockMvc.perform(post("/gold/gateway")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(request.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andReturn();

        String responseXml = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Document document = XmlUtils.parse(responseXml);
        // 响应报文必须带签名，渠道侧可验签
        assertFalse(XmlUtils.childText(header(document), "sign").isEmpty(), "响应报文缺少签名");
        return document;
    }

    private void assertSuccess(Document document) {
        assertEquals("000000", respCode(document), "交易应成功，实际: " + respMsg(document));
    }

    private String respCode(Document document) {
        return XmlUtils.childText(header(document), "respCode");
    }

    private String respMsg(Document document) {
        return XmlUtils.childText(header(document), "respMsg");
    }

    private Element header(Document document) {
        Element header = XmlUtils.firstChild(document.getDocumentElement(), "header");
        assertNotNull(header, "响应报文缺少 header");
        return header;
    }

    private String text(Document document, String... path) {
        Element current = XmlUtils.firstChild(document.getDocumentElement(), "body");
        assertNotNull(current, "响应报文缺少 body");
        String value = null;
        for (String node : path) {
            Element next = XmlUtils.firstChild(current, node);
            assertNotNull(next, "响应报文缺少节点: " + String.join(".", path));
            current = next;
        }
        value = current.getTextContent();
        return value == null ? null : value.trim();
    }

    private void assertDecimal(String expected, String actual) {
        assertNotNull(actual, "金额/克重为空");
        assertTrue(new BigDecimal(expected).compareTo(new BigDecimal(actual)) == 0,
                "期望 " + expected + "，实际 " + actual);
    }

    private String nextSerial() {
        return "MB" + System.currentTimeMillis() + SERIAL.incrementAndGet();
    }
}
