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
 * 提货单（网点自提 / 快递配送）。
 */
@Data
@Entity
@Table(name = "gold_delivery")
public class GoldDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_no", nullable = false, length = 40)
    private String deliveryNo;

    @Column(name = "order_no", nullable = false, length = 40)
    private String orderNo;

    @Column(name = "customer_no", nullable = false, length = 32)
    private String customerNo;

    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;

    @Column(name = "weight", precision = 12, scale = 4)
    private BigDecimal weight;

    /** PICKUP / EXPRESS */
    @Column(name = "delivery_mode", length = 16)
    private String deliveryMode;

    @Column(name = "branch_no", length = 32)
    private String branchNo;

    @Column(name = "branch_name", length = 128)
    private String branchName;

    @Column(name = "receiver_name", length = 64)
    private String receiverName;

    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    @Column(name = "receiver_address", length = 256)
    private String receiverAddress;

    /** APPLIED / FINISHED / CANCELLED */
    @Column(name = "status", length = 16)
    private String status;

    @Column(name = "apply_time")
    private LocalDateTime applyTime;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
