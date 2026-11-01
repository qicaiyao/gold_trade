package com.bank.gold;

import com.bank.gold.config.GoldTradeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 实物黄金购买接口服务（HTTP + XML，面向手机银行渠道）。
 */
@SpringBootApplication
@EnableConfigurationProperties(GoldTradeProperties.class)
public class GoldTradeApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoldTradeApplication.class, args);
    }
}
