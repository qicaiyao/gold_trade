package com.bank.gold.common;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 报文签名工具（RSA2 = SHA256withRSA）。
 *
 * <p><b>待签名串规则</b>：取报文全部叶子节点，按「节点路径=节点文本」逐行拼接，
 * 节点路径按 ASCII 升序排列，字段之间以 &amp; 连接；同名兄弟节点追加 [n] 序号；
 * 签名节点 sign 本身不参与签名；节点文本两端空白会被去除。</p>
 *
 * <p>示例：{@code header.channel=MOBILE_BANK&header.reqSerialNo=MB2026...&body.productCode=AU9999-10G}</p>
 *
 * <p>该规则与报文缩进、属性顺序无关，双方各自按同一规则重组待签名串即可，实现简单且不易出错。</p>
 */
public final class SignUtils {

    /** 签名类型：RSA2 */
    public static final String SIGN_TYPE_RSA2 = "RSA2";

    private static final String ALGORITHM = "SHA256withRSA";
    private static final String SIGN_NODE_NAME = "sign";
    private static final String SEPARATOR = "&";
    private static final String KV = "=";

    private SignUtils() {
    }

    /**
     * 展平报文为「路径 -> 文本」的有序集合。
     */
    public static TreeMap<String, String> canonicalNodes(Element root) {
        TreeMap<String, String> nodes = new TreeMap<>();
        collect(root, "", nodes);
        return nodes;
    }

    /**
     * 生成待签名串。
     */
    public static String canonicalString(Map<String, String> nodes) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : nodes.entrySet()) {
            if (!first) {
                sb.append(SEPARATOR);
            }
            sb.append(entry.getKey()).append(KV).append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }

    /**
     * 使用私钥签名，返回 Base64 字符串。
     */
    public static String sign(String canonical, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(privateKey);
            signature.update(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new GoldException(RespCode.SIGN_ERROR, "报文签名失败: " + e.getMessage());
        }
    }

    /**
     * 使用公钥验签。
     */
    public static boolean verify(String canonical, String signBase64, PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(canonical.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signBase64));
        } catch (Exception e) {
            return false;
        }
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("生成 RSA 密钥对失败", e);
        }
    }

    public static PublicKey parsePublicKey(String base64) {
        try {
            byte[] key = Base64.getDecoder().decode(removePemHeader(base64));
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key));
        } catch (Exception e) {
            throw new IllegalStateException("公钥解析失败，请确认是 Base64 编码的 X.509 公钥", e);
        }
    }

    public static PrivateKey parsePrivateKey(String base64) {
        try {
            byte[] key = Base64.getDecoder().decode(removePemHeader(base64));
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(key));
        } catch (Exception e) {
            throw new IllegalStateException("私钥解析失败，请确认是 Base64 编码的 PKCS#8 私钥", e);
        }
    }

    // ------------------------------------------------------------------

    private static void collect(Element parent, String path, Map<String, String> out) {
        List<Element> children = childElements(parent);
        Map<String, Integer> nameCount = new HashMap<>();
        for (Element child : children) {
            nameCount.merge(child.getTagName(), 1, Integer::sum);
        }

        Map<String, Integer> index = new HashMap<>();
        for (Element child : children) {
            String name = child.getTagName();
            // sign 节点本身不参与签名
            if (SIGN_NODE_NAME.equalsIgnoreCase(name)) {
                continue;
            }
            String currentPath = path.isEmpty() ? name : path + "." + name;
            if (nameCount.get(name) > 1) {
                currentPath = currentPath + "[" + index.merge(name, 1, Integer::sum) + "]";
            }
            List<Element> grandChildren = childElements(child);
            if (grandChildren.isEmpty()) {
                out.put(currentPath, textOf(child));
            } else {
                collect(child, currentPath, out);
            }
        }
    }

    private static List<Element> childElements(Element parent) {
        List<Element> list = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                list.add((Element) node);
            }
        }
        return list;
    }

    private static String textOf(Element element) {
        String text = element.getTextContent();
        return text == null ? "" : text.trim();
    }

    private static String removePemHeader(String key) {
        if (key == null) {
            return "";
        }
        return key.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
    }
}
