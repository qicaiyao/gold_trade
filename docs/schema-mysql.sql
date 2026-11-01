-- ============================================================
-- 实物黄金交易 MySQL 8.0 建表脚本
-- 库：gold_trade   字符集：utf8mb4   引擎：InnoDB
-- ============================================================
CREATE DATABASE IF NOT EXISTS `gold_trade` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `gold_trade`;

-- 客户（银行核心同步，接口侧只读）
CREATE TABLE IF NOT EXISTS `gold_customer` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `customer_no`   VARCHAR(32)  NOT NULL COMMENT '客户号',
    `customer_name` VARCHAR(64)  NOT NULL COMMENT '客户姓名',
    `phone`         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `risk_level`    VARCHAR(8)   NOT NULL DEFAULT 'R1' COMMENT '风险等级 R1~R5',
    `status`        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE/FROZEN',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_customer_no` (`customer_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黄金业务客户表';

-- 资金账户（演示用；生产对接核心账务）
CREATE TABLE IF NOT EXISTS `gold_account` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT,
    `account_no`        VARCHAR(32)   NOT NULL COMMENT '账号',
    `customer_no`       VARCHAR(32)   NOT NULL COMMENT '客户号',
    `account_name`      VARCHAR(64)   DEFAULT NULL COMMENT '户名',
    `currency`          VARCHAR(8)    DEFAULT 'CNY' COMMENT '币种',
    `available_balance` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '可用余额',
    `frozen_balance`    DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '冻结余额',
    `create_time`       DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account_no` (`account_no`),
    KEY `idx_customer_no` (`customer_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金账户表';

-- 黄金产品
CREATE TABLE IF NOT EXISTS `gold_product` (
    `id`                   BIGINT        NOT NULL AUTO_INCREMENT,
    `product_code`         VARCHAR(32)   NOT NULL COMMENT '产品代码',
    `product_name`         VARCHAR(128)  NOT NULL COMMENT '产品名称',
    `brand`                VARCHAR(64)   DEFAULT NULL COMMENT '品牌',
    `spec`                 VARCHAR(64)   DEFAULT NULL COMMENT '规格',
    `unit`                 VARCHAR(8)    DEFAULT '克' COMMENT '计价单位',
    `price_per_gram`       DECIMAL(12,2) NOT NULL COMMENT '当前金价（元/克）',
    `fee_per_gram`         DECIMAL(12,2) DEFAULT 0 COMMENT '加工费（元/克）',
    `min_weight`           DECIMAL(12,4) DEFAULT 1 COMMENT '最小购买克重',
    `max_weight_per_order` DECIMAL(12,4) DEFAULT 1000 COMMENT '单笔最大克重',
    `step_weight`          DECIMAL(12,4) DEFAULT 1 COMMENT '克重步长',
    `risk_level`           VARCHAR(8)    DEFAULT 'R2' COMMENT '产品风险等级',
    `status`               VARCHAR(16)   DEFAULT 'ON_SALE' COMMENT '状态 ON_SALE/OFF_SALE',
    `create_time`          DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `update_time`          DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_code` (`product_code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黄金产品表';

-- 报价单（价格锁定凭证）
CREATE TABLE IF NOT EXISTS `gold_quote` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `quote_no`       VARCHAR(40)   NOT NULL COMMENT '报价单号',
    `product_code`   VARCHAR(32)   NOT NULL COMMENT '产品代码',
    `price_per_gram` DECIMAL(12,2) NOT NULL COMMENT '锁定金价（元/克）',
    `fee_per_gram`   DECIMAL(12,2) DEFAULT 0 COMMENT '加工费（元/克）',
    `quote_time`     DATETIME      NOT NULL COMMENT '锁价时间',
    `expire_time`    DATETIME      NOT NULL COMMENT '失效时间',
    `status`         VARCHAR(16)   DEFAULT 'UNUSED' COMMENT '状态 UNUSED/USED/EXPIRED',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quote_no` (`quote_no`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报价单表';

-- 购买订单
CREATE TABLE IF NOT EXISTS `gold_order` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `order_no`       VARCHAR(40)   NOT NULL COMMENT '订单号',
    `req_serial_no`  VARCHAR(40)   NOT NULL COMMENT '渠道请求流水号',
    `quote_no`       VARCHAR(40)   DEFAULT NULL COMMENT '报价单号',
    `customer_no`    VARCHAR(32)   NOT NULL COMMENT '客户号',
    `account_no`     VARCHAR(32)   NOT NULL COMMENT '付款账号',
    `product_code`   VARCHAR(32)   NOT NULL COMMENT '产品代码',
    `product_name`   VARCHAR(128)  DEFAULT NULL COMMENT '产品名称',
    `weight`         DECIMAL(12,4) NOT NULL COMMENT '成交克重',
    `price_per_gram` DECIMAL(12,2) NOT NULL COMMENT '成交金价',
    `gold_amount`    DECIMAL(18,2) NOT NULL COMMENT '金价金额',
    `fee_amount`     DECIMAL(18,2) DEFAULT 0 COMMENT '加工费',
    `total_amount`   DECIMAL(18,2) NOT NULL COMMENT '实付金额',
    `delivery_mode`  VARCHAR(16)   NOT NULL COMMENT 'DEPOSIT/PICKUP/EXPRESS',
    `status`         VARCHAR(16)   NOT NULL COMMENT 'SUCCESS/CANCELLED',
    `voucher_no`     VARCHAR(40)   DEFAULT NULL COMMENT '提货凭证号',
    `order_time`     DATETIME      NOT NULL COMMENT '下单时间',
    `cancel_time`    DATETIME      DEFAULT NULL COMMENT '撤销时间',
    `remark`         VARCHAR(256)  DEFAULT NULL COMMENT '备注',
    `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_customer_time` (`customer_no`, `order_time`),
    KEY `idx_req_serial_no` (`req_serial_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实物黄金购买订单表';

-- 持仓（存金账户）
CREATE TABLE IF NOT EXISTS `gold_holding` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `customer_no`      VARCHAR(32)   NOT NULL COMMENT '客户号',
    `product_code`     VARCHAR(32)   NOT NULL COMMENT '产品代码',
    `product_name`     VARCHAR(128)  DEFAULT NULL COMMENT '产品名称',
    `total_weight`     DECIMAL(12,4) DEFAULT 0 COMMENT '总克重',
    `available_weight` DECIMAL(12,4) DEFAULT 0 COMMENT '可提货克重',
    `frozen_weight`    DECIMAL(12,4) DEFAULT 0 COMMENT '已申请提货冻结克重',
    `cost_amount`      DECIMAL(18,2) DEFAULT 0 COMMENT '持仓成本',
    `create_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_customer_product` (`customer_no`, `product_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黄金持仓表';

-- 提货单
CREATE TABLE IF NOT EXISTS `gold_delivery` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `delivery_no`      VARCHAR(40)   NOT NULL COMMENT '提货单号',
    `order_no`         VARCHAR(40)   NOT NULL COMMENT '订单号',
    `customer_no`      VARCHAR(32)   NOT NULL COMMENT '客户号',
    `product_code`     VARCHAR(32)   NOT NULL COMMENT '产品代码',
    `weight`           DECIMAL(12,4) NOT NULL COMMENT '提货克重',
    `delivery_mode`    VARCHAR(16)   NOT NULL COMMENT 'PICKUP/EXPRESS',
    `branch_no`        VARCHAR(32)   DEFAULT NULL COMMENT '提货网点号',
    `branch_name`      VARCHAR(128)  DEFAULT NULL COMMENT '提货网点名称',
    `receiver_name`    VARCHAR(64)   DEFAULT NULL COMMENT '收件人',
    `receiver_phone`   VARCHAR(20)   DEFAULT NULL COMMENT '收件人手机号',
    `receiver_address` VARCHAR(256)  DEFAULT NULL COMMENT '收件地址',
    `status`           VARCHAR(16)   NOT NULL COMMENT 'APPLIED/FINISHED/CANCELLED',
    `apply_time`       DATETIME      NOT NULL COMMENT '申请时间',
    `create_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_delivery_no` (`delivery_no`),
    KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提货单表';

-- 交易流水（幂等 + 审计）
CREATE TABLE IF NOT EXISTS `gold_tran_log` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `req_serial_no` VARCHAR(40)  NOT NULL COMMENT '渠道请求流水号（幂等键）',
    `tran_code`     VARCHAR(16)  NOT NULL COMMENT '交易码',
    `status`        VARCHAR(16)  NOT NULL COMMENT 'PROCESSING/SUCCESS/FAIL',
    `resp_code`     VARCHAR(16)  DEFAULT NULL COMMENT '响应码',
    `resp_msg`      VARCHAR(256) DEFAULT NULL COMMENT '响应描述',
    `resp_xml`      LONGTEXT     DEFAULT NULL COMMENT '首次响应报文',
    `cost_ms`       BIGINT       DEFAULT NULL COMMENT '处理耗时(ms)',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_req_serial_no` (`req_serial_no`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易流水表';
