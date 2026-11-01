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
 * 黄金持仓（存金账户）。可用克重可提货，冻结克重为已申请提货待出库部分。
 */
@Data
@Entity
@Table(name = "gold_holding")
public class GoldHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_no", nullable = false, length = 32)
    private String customerNo;

    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;

    @Column(name = "product_name", length = 128)
    private String productName;

    @Column(name = "total_weight", precision = 12, scale = 4)
    private BigDecimal totalWeight;

    @Column(name = "available_weight", precision = 12, scale = 4)
    private BigDecimal availableWeight;

    @Column(name = "frozen_weight", precision = 12, scale = 4)
    private BigDecimal frozenWeight;

    /** 持仓成本（含加工费） */
    @Column(name = "cost_amount", precision = 18, scale = 2)
    private BigDecimal costAmount;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
