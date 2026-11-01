package com.bank.gold.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报价单：价格锁定凭证。下单时必须携带未过期的报价单号，避免价格争议。
 */
@Data
@Entity
@Table(name = "gold_quote")
public class GoldQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quote_no", nullable = false, length = 40)
    private String quoteNo;

    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;

    @Column(name = "price_per_gram", precision = 12, scale = 2)
    private BigDecimal pricePerGram;

    @Column(name = "fee_per_gram", precision = 12, scale = 2)
    private BigDecimal feePerGram;

    @Column(name = "quote_time")
    private LocalDateTime quoteTime;

    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    /** UNUSED / USED / EXPIRED */
    @Column(name = "status", length = 16)
    private String status;
}
