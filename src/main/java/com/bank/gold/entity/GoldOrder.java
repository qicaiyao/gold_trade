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
 * 实物黄金购买订单。
 */
@Data
@Entity
@Table(name = "gold_order")
public class GoldOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 40)
    private String orderNo;

    /** 渠道请求流水号，用于幂等与对账 */
    @Column(name = "req_serial_no", nullable = false, length = 40)
    private String reqSerialNo;

    @Column(name = "quote_no", length = 40)
    private String quoteNo;

    @Column(name = "customer_no", nullable = false, length = 32)
    private String customerNo;

    @Column(name = "account_no", nullable = false, length = 32)
    private String accountNo;

    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;

    @Column(name = "product_name", length = 128)
    private String productName;

    /** 购买克重 */
    @Column(name = "weight", precision = 12, scale = 4)
    private BigDecimal weight;

    @Column(name = "price_per_gram", precision = 12, scale = 2)
    private BigDecimal pricePerGram;

    /** 金价金额 = 克重 × 金价 */
    @Column(name = "gold_amount", precision = 18, scale = 2)
    private BigDecimal goldAmount;

    /** 加工费 */
    @Column(name = "fee_amount", precision = 18, scale = 2)
    private BigDecimal feeAmount;

    /** 实付金额 = 金价金额 + 加工费 */
    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    /** DEPOSIT 账户存金 / PICKUP 网点自提 / EXPRESS 快递配送 */
    @Column(name = "delivery_mode", length = 16)
    private String deliveryMode;

    /** SUCCESS / CANCELLED */
    @Column(name = "status", length = 16)
    private String status;

    /** 提货凭证号 */
    @Column(name = "voucher_no", length = 40)
    private String voucherNo;

    @Column(name = "order_time")
    private LocalDateTime orderTime;

    @Column(name = "cancel_time")
    private LocalDateTime cancelTime;

    @Column(name = "remark", length = 256)
    private String remark;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
