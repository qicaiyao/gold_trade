package com.bank.gold.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户信息（由银行核心系统同步，接口侧只读）。
 */
@Data
@Entity
@Table(name = "gold_customer")
public class GoldCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_no", nullable = false, length = 32)
    private String customerNo;

    @Column(name = "customer_name", nullable = false, length = 64)
    private String customerName;

    @Column(name = "phone", length = 20)
    private String phone;

    /** 风险等级 R1~R5 */
    @Column(name = "risk_level", length = 8)
    private String riskLevel;

    /** ACTIVE / FROZEN */
    @Column(name = "status", length = 16)
    private String status;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
