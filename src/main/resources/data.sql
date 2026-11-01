-- 演示数据
INSERT INTO gold_customer (customer_no, customer_name, phone, risk_level, status) VALUES
('C1000001', '张三', '13800138001', 'R4', 'ACTIVE'),
('C1000002', '李四', '13800138002', 'R2', 'ACTIVE');

INSERT INTO gold_account (account_no, customer_no, account_name, currency, available_balance, frozen_balance) VALUES
('6222021234567890', 'C1000001', '张三', 'CNY', 200000.00, 0.00),
('6222021234567891', 'C1000002', '李四', 'CNY', 5000.00, 0.00);

INSERT INTO gold_product (product_code, product_name, brand, spec, unit, price_per_gram, fee_per_gram,
                          min_weight, max_weight_per_order, step_weight, risk_level, status) VALUES
('AU9999-10G', '如意金条 10克', '工银金', '10克/根', '克', 618.50, 12.00, 10.0000, 100.0000, 10.0000, 'R2', 'ON_SALE'),
('AU9999-50G', '如意金条 50克', '工银金', '50克/根', '克', 616.80, 10.00, 50.0000, 500.0000, 50.0000, 'R2', 'ON_SALE'),
('AU9999-100G', '如意金条 100克', '工银金', '100克/根', '克', 615.20, 8.00, 100.0000, 1000.0000, 100.0000, 'R3', 'ON_SALE'),
('AU-JC-001', '积存金（可按克购买）', '工银金', '1克起', '克', 619.90, 0.00, 1.0000, 500.0000, 0.0100, 'R2', 'ON_SALE'),
('AU9999-OLD', '旧版金条（已下架）', '工银金', '20克/根', '克', 610.00, 10.00, 20.0000, 200.0000, 20.0000, 'R2', 'OFF_SALE');
