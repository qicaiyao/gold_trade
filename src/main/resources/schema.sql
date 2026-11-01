-- ============================================================
-- 实物黄金交易 H2 演示库表结构（MySQL 版本见 docs/schema-mysql.sql）
-- ============================================================
DROP TABLE IF EXISTS gold_tran_log;
DROP TABLE IF EXISTS gold_delivery;
DROP TABLE IF EXISTS gold_holding;
DROP TABLE IF EXISTS gold_order;
DROP TABLE IF EXISTS gold_quote;
DROP TABLE IF EXISTS gold_account;
DROP TABLE IF EXISTS gold_product;
DROP TABLE IF EXISTS gold_customer;

-- 客户（银行核心同步而来，接口侧只读）
CREATE TABLE gold_customer (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_no   VARCHAR(32)  NOT NULL UNIQUE,
    customer_name VARCHAR(64)  NOT NULL,
    phone         VARCHAR(20),
    risk_level    VARCHAR(8)   NOT NULL DEFAULT 'R1',
    status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 资金账户（付款账户，演示用）
CREATE TABLE gold_account (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_no        VARCHAR(32)    NOT NULL UNIQUE,
    customer_no       VARCHAR(32)    NOT NULL,
    account_name      VARCHAR(64),
    currency          VARCHAR(8)     DEFAULT 'CNY',
    available_balance DECIMAL(18,2)  NOT NULL DEFAULT 0,
    frozen_balance    DECIMAL(18,2)  NOT NULL DEFAULT 0,
    create_time       TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);

-- 黄金产品
CREATE TABLE gold_product (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code         VARCHAR(32)   NOT NULL UNIQUE,
    product_name         VARCHAR(128)  NOT NULL,
    brand                VARCHAR(64),
    spec                 VARCHAR(64),
    unit                 VARCHAR(8)    DEFAULT '克',
    price_per_gram       DECIMAL(12,2) NOT NULL,
    fee_per_gram         DECIMAL(12,2) DEFAULT 0,
    min_weight           DECIMAL(12,4) DEFAULT 1,
    max_weight_per_order DECIMAL(12,4) DEFAULT 1000,
    step_weight          DECIMAL(12,4) DEFAULT 1,
    risk_level           VARCHAR(8)    DEFAULT 'R2',
    status               VARCHAR(16)   DEFAULT 'ON_SALE',
    create_time          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- 报价单（价格锁定凭证）
CREATE TABLE gold_quote (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    quote_no       VARCHAR(40)   NOT NULL UNIQUE,
    product_code   VARCHAR(32)   NOT NULL,
    price_per_gram DECIMAL(12,2) NOT NULL,
    fee_per_gram   DECIMAL(12,2) DEFAULT 0,
    quote_time     TIMESTAMP     NOT NULL,
    expire_time    TIMESTAMP     NOT NULL,
    status         VARCHAR(16)   DEFAULT 'UNUSED'
);

-- 购买订单
CREATE TABLE gold_order (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no      VARCHAR(40)   NOT NULL UNIQUE,
    req_serial_no VARCHAR(40)   NOT NULL,
    quote_no      VARCHAR(40),
    customer_no   VARCHAR(32)   NOT NULL,
    account_no    VARCHAR(32)   NOT NULL,
    product_code  VARCHAR(32)   NOT NULL,
    product_name  VARCHAR(128),
    weight        DECIMAL(12,4) NOT NULL,
    price_per_gram DECIMAL(12,2) NOT NULL,
    gold_amount   DECIMAL(18,2) NOT NULL,
    fee_amount    DECIMAL(18,2) DEFAULT 0,
    total_amount  DECIMAL(18,2) NOT NULL,
    delivery_mode VARCHAR(16)   NOT NULL,
    status        VARCHAR(16)   NOT NULL,
    voucher_no    VARCHAR(40),
    order_time    TIMESTAMP     NOT NULL,
    cancel_time   TIMESTAMP,
    remark        VARCHAR(256),
    create_time   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_order_customer ON gold_order (customer_no, order_time);

-- 黄金持仓（存金账户）
CREATE TABLE gold_holding (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_no      VARCHAR(32)   NOT NULL,
    product_code     VARCHAR(32)   NOT NULL,
    product_name     VARCHAR(128),
    total_weight     DECIMAL(12,4) DEFAULT 0,
    available_weight DECIMAL(12,4) DEFAULT 0,
    frozen_weight    DECIMAL(12,4) DEFAULT 0,
    cost_amount      DECIMAL(18,2) DEFAULT 0,
    create_time      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_holding UNIQUE (customer_no, product_code)
);

-- 提货单
CREATE TABLE gold_delivery (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    delivery_no      VARCHAR(40)  NOT NULL UNIQUE,
    order_no         VARCHAR(40)  NOT NULL,
    customer_no      VARCHAR(32)  NOT NULL,
    product_code     VARCHAR(32)  NOT NULL,
    weight           DECIMAL(12,4) NOT NULL,
    delivery_mode    VARCHAR(16)  NOT NULL,
    branch_no        VARCHAR(32),
    branch_name      VARCHAR(128),
    receiver_name    VARCHAR(64),
    receiver_phone   VARCHAR(20),
    receiver_address VARCHAR(256),
    status           VARCHAR(16)  NOT NULL,
    apply_time       TIMESTAMP    NOT NULL,
    create_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 交易流水（幂等 / 审计）
CREATE TABLE gold_tran_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    req_serial_no VARCHAR(40) NOT NULL UNIQUE,
    tran_code     VARCHAR(16) NOT NULL,
    status        VARCHAR(16) NOT NULL,
    resp_code     VARCHAR(16),
    resp_msg      VARCHAR(256),
    resp_xml      CLOB,
    cost_ms       BIGINT,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
