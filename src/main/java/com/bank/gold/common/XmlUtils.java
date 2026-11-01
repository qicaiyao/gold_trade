package com.bank.gold.common;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * XML 报文解析 / 组装工具。
 * <p>解析时显式关闭 DTD 与外部实体，防止 XXE 攻击（银行外联接口的必检项）。</p>
 */
public final class XmlUtils {

    private XmlUtils() {
    }

    public static Document newDocument() {
        try {
            return factory().newDocumentBuilder().newDocument();
        } catch (Exception e) {
            throw new IllegalStateException("创建 XML 文档失败", e);
        }
    }

    /**
     * 解析报文，任何格式问题统一抛 100001。
     */
    public static Document parse(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new GoldException(RespCode.MSG_FORMAT_ERROR, "请求报文为空");
        }
        try {
            DocumentBuilder builder = factory().newDocumentBuilder();
            builder.setErrorHandler(null);
            Document document = builder.parse(new InputSource(new StringReader(xml.trim())));
            if (document.getDocumentElement() == null) {
                throw new GoldException(RespCode.MSG_FORMAT_ERROR, "请求报文缺少根节点");
            }
            return document;
        } catch (GoldException e) {
            throw e;
        } catch (Exception e) {
            throw new GoldException(RespCode.MSG_FORMAT_ERROR, "请求报文解析失败: " + e.getMessage());
        }
    }

    /**
     * 将一段 XML（如 body 片段）解析后挂载到目标文档。
     */
    public static Element parseAndImport(Document target, String xml) {
        Document fragment = parse(xml);
        return (Element) target.importNode(fragment.getDocumentElement(), true);
    }

    public static Element firstChild(Element parent, String name) {
        if (parent == null) {
            return null;
        }
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && name.equals(node.getNodeName())) {
                return (Element) node;
            }
        }
        return null;
    }

    public static String childText(Element parent, String name) {
        Element child = firstChild(parent, name);
        if (child == null) {
            return null;
        }
        String text = child.getTextContent();
        return text == null ? null : text.trim();
    }

    public static Element appendText(Document doc, Element parent, String name, String value) {
        Element element = doc.createElement(name);
        if (value != null) {
            element.setTextContent(value);
        }
        parent.appendChild(element);
        return element;
    }

    /**
     * 输出报文文本。缩进仅影响可读性，不影响验签结果（验签基于叶子节点文本）。
     */
    public static String toXmlString(Document document, boolean indent) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, indent ? "yes" : "no");
            if (indent) {
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            }
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new IllegalStateException("XML 序列化失败", e);
        }
    }

    /**
     * 输出单个节点（含自身标签）的报文文本，用于把 body 片段还原成 XML 再反序列化。
     */
    public static String toXmlString(Element element) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(element), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new IllegalStateException("XML 节点序列化失败", e);
        }
    }

    private static DocumentBuilderFactory factory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        trySetFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        trySetFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        trySetFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        trySetFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return factory;
    }

    private static void trySetFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception ignored) {
            // 部分解析器不支持该特性，忽略
        }
    }
}
