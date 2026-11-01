package com.bank.gold.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * 黄金交易接口配置（application.yml 中 gold.* 节点）。
 */
@Data
@ConfigurationProperties(prefix = "gold")
public class GoldTradeProperties {

    private Sign sign = new Sign();
    private Quote quote = new Quote();
    private Limit limit = new Limit();

    /** 报文时间戳允许偏差（秒），超出视为重放攻击 */
    private int timeToleranceSeconds = 300;

    @Data
    public static class Sign {
        /** 是否校验渠道方请求签名，生产必须为 true */
        private boolean enabled = true;
        /** 签名算法，当前仅支持 RSA2（SHA256withRSA） */
        private String signType = "RSA2";
        /** 渠道方（手机银行）公钥，Base64 X.509 */
        private String publicKey;
        /** 本行私钥，Base64 PKCS#8，用于对响应报文签名 */
        private String privateKey;
    }

    @Data
    public static class Quote {
        /** 报价锁定有效期（秒） */
        private int ttlSeconds = 60;
    }

    @Data
    public static class Limit {
        /** 单笔购买金额上限 */
        private BigDecimal singleAmountLimit = new BigDecimal("200000.00");
        /** 单客户单日累计购买金额上限 */
        private BigDecimal dailyAmountLimit = new BigDecimal("500000.00");
    }
}
