package com.bank.gold.protocol;

import com.bank.gold.common.GoldException;
import com.bank.gold.common.RespCode;
import com.bank.gold.common.SignUtils;
import com.bank.gold.config.GoldTradeProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

/**
 * 报文签名服务。
 * <ul>
 *   <li>请求：使用渠道方公钥验签，防止报文被篡改；</li>
 *   <li>响应：使用本行私钥签名，手机银行侧用本行公钥验签。</li>
 * </ul>
 * <p>未配置密钥时自动生成临时密钥对并打印公钥，仅用于本地演示。</p>
 */
@Slf4j
@Component
public class SignService {

    private final GoldTradeProperties properties;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final String publicKeyBase64;

    public SignService(GoldTradeProperties properties) {
        this.properties = properties;
        GoldTradeProperties.Sign sign = properties.getSign();
        if (StringUtils.hasText(sign.getPrivateKey()) && StringUtils.hasText(sign.getPublicKey())) {
            this.privateKey = SignUtils.parsePrivateKey(sign.getPrivateKey());
            this.publicKey = SignUtils.parsePublicKey(sign.getPublicKey());
            this.publicKeyBase64 = sign.getPublicKey();
            log.info("签名密钥加载完成，签名算法: {}", sign.getSignType());
        } else {
            KeyPair keyPair = SignUtils.generateKeyPair();
            this.privateKey = keyPair.getPrivate();
            this.publicKey = keyPair.getPublic();
            this.publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            log.warn("未配置签名密钥，已生成临时 RSA 密钥对（仅用于演示）。本行响应公钥(Base64): {}", publicKeyBase64);
        }
    }

    /**
     * 请求报文验签。
     */
    public void verify(XmlEnvelope envelope) {
        if (!properties.getSign().isEnabled()) {
            log.debug("签名校验已关闭，跳过验签。流水号: {}", envelope.getHeader().reqSerialNo());
            return;
        }
        String signType = envelope.getHeader().signType();
        if (!SignUtils.SIGN_TYPE_RSA2.equalsIgnoreCase(signType)) {
            throw new GoldException(RespCode.SIGN_ERROR, "不支持的签名类型: " + signType);
        }
        String sign = envelope.getHeader().sign();
        if (!StringUtils.hasText(sign)) {
            throw new GoldException(RespCode.SIGN_ERROR, "报文签名 sign 不能为空");
        }
        String canonical = SignUtils.canonicalString(SignUtils.canonicalNodes(envelope.getRoot()));
        if (!SignUtils.verify(canonical, sign, publicKey)) {
            log.warn("验签失败，流水号: {}", envelope.getHeader().reqSerialNo());
            throw new GoldException(RespCode.SIGN_ERROR, "签名验证失败");
        }
    }

    /**
     * 对响应报文签名（在追加 sign 节点之前调用）。
     */
    public String sign(Element root) {
        String canonical = SignUtils.canonicalString(SignUtils.canonicalNodes(root));
        return SignUtils.sign(canonical, privateKey);
    }

    public String getPublicKeyBase64() {
        return publicKeyBase64;
    }
}
