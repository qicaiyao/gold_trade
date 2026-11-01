package com.bank.gold.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交易流水：幂等控制 + 报文留痕，供对账与问题排查使用。
 */
@Data
@Entity
@Table(name = "gold_tran_log")
public class GoldTranLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 渠道请求流水号，唯一索引保证幂等 */
    @Column(name = "req_serial_no", nullable = false, length = 40)
    private String reqSerialNo;

    @Column(name = "tran_code", nullable = false, length = 16)
    private String tranCode;

    /** PROCESSING / SUCCESS / FAIL */
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "resp_code", length = 16)
    private String respCode;

    @Column(name = "resp_msg", length = 256)
    private String respMsg;

    /** 首次响应报文，重复请求时原样返回 */
    @Lob
    @Column(name = "resp_xml")
    private String respXml;

    @Column(name = "cost_ms")
    private Long costMs;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
