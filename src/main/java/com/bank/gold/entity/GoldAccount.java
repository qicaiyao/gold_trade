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
 * 资金账户（付款账户）。金额统一使用 BigDecimal，禁止 double。
 */
@Data
@Entity
@Table(name = "gold_account")
public class GoldAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_no", nullable = false, length = 32)
    private String accountNo;

    @Column(name = "customer_no", nullable = false, length = 32)
    private String customerNo;

    @Column(name = "account_name", length = 64)
    private String accountName;

    @Column(name = "currency", length = 8)
    private String currency;

    @Column(name = "available_balance", precision = 18, scale = 2)
    private BigDecimal availableBalance;

    @Column(name = "frozen_balance", precision = 18, scale = 2)
    private BigDecimal frozenBalance;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
