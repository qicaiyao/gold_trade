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
 * 黄金产品（金条 / 积存金等）。
 */
@Data
@Entity
@Table(name = "gold_product")
public class GoldProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 128)
    private String productName;

    @Column(name = "brand", length = 64)
    private String brand;

    @Column(name = "spec", length = 64)
    private String spec;

    @Column(name = "unit", length = 8)
    private String unit;

    /** 当前金价（元/克），由行情系统推送更新 */
    @Column(name = "price_per_gram", precision = 12, scale = 2)
    private BigDecimal pricePerGram;

    /** 加工费（元/克） */
    @Column(name = "fee_per_gram", precision = 12, scale = 2)
    private BigDecimal feePerGram;

    /** 最小购买克重 */
    @Column(name = "min_weight", precision = 12, scale = 4)
    private BigDecimal minWeight;

    /** 单笔最大克重 */
    @Column(name = "max_weight_per_order", precision = 12, scale = 4)
    private BigDecimal maxWeightPerOrder;

    /** 克重步长（如金条 10 克起、10 克递增；积存金 0.01 克递增） */
    @Column(name = "step_weight", precision = 12, scale = 4)
    private BigDecimal stepWeight;

    /** 产品风险等级 R1~R5 */
    @Column(name = "risk_level", length = 8)
    private String riskLevel;

    @Column(name = "status", length = 16)
    private String status;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
