# gold-trade · 实物黄金购买接口（HTTP + XML）

面向手机银行渠道的实物黄金交易接口服务：**HTTP + XML 报文、单入口按交易码路由、RSA2 签名、请求幂等、悲观锁扣款**，基于 Spring Boot 3.3 / Java 17。

完整接口协议见 **[docs/实物黄金购买接口文档.md](docs/实物黄金购买接口文档.md)**。

---

## 1. 能力清单

| 交易码 | 接口 | 说明 |
| --- | --- | --- |
| GOLD1001 | 产品与实时金价查询 | 在售产品、金价、加工费、克重规则 |
| GOLD1002 | 报价锁定 | 生成 quoteNo，锁价 60 秒，可试算金额 |
| GOLD1003 | 实物黄金购买下单 | 校验 + 扣款 + 落单 + 持仓入账 |
| GOLD1004 | 购买订单查询 | 单笔精确查询 / 客户日期区间查询 |
| GOLD1005 | 购买订单撤销 | 当日未提货订单可撤，资金原路退回 |
| GOLD1006 | 提货申请 | 账户存金转网点自提 / 快递配送 |
| GOLD1007 | 黄金持仓查询 | 克重、可用/冻结、市值、浮盈 |

## 2. 技术栈

- Spring Boot 3.3.5 / Java 17
- Spring MVC（`@RequestBody String` 接收原始 XML，DOM 解析 + 验签 + 分发）
- Jackson XML（body ↔ DTO 序列化）
- Spring Data JPA（H2 演示 / MySQL 生产）
- 签名：`SHA256withRSA`（RSA2），待签名串规则见文档第 4.1 节

## 3. 目录结构

```
gold-trade
├── docs
│   ├── 实物黄金购买接口文档.md      # 接口协议、字段表、报文示例、错误码
│   ├── schema-mysql.sql            # 生产环境 MySQL 建表脚本
│   └── samples/run-demo.sh         # 一键启动并抓取各接口真实报文
└── src/main/java/com/bank/gold
    ├── common/         # 响应码、交易码、异常、签名工具、XML 工具、单号生成
    ├── config/         # 配置属性、XmlMapper、全局异常
    ├── protocol/       # 报文信封、验签服务、响应组装、交易分发、网关编排
    ├── controller/     # 统一网关入口 POST /gold/gateway
    ├── dto/request     # 各交易请求体
    ├── dto/response    # 各交易响应体
    ├── entity/         # 客户、账户、产品、报价、订单、持仓、提货单、交易流水
    ├── repository/     # JPA 仓储（含悲观锁查询）
    └── service/        # 产品报价、购买撤销、提货、查询、幂等流水
```

## 4. 快速开始

```bash
cd gold-trade

# 编译 + 跑测试（H2 内存库，无需外部依赖）
mvn test

# 启动服务（默认 8090 端口，内置 H2 + 演示数据）
mvn spring-boot:run
```

联调时若不签名，可临时关闭验签：

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--gold.sign.enabled=false"
```

调用示例（`sign` 关闭验签时可随意填）：

```bash
curl -X POST http://localhost:8090/gold/gateway \
  -H "Content-Type: application/xml;charset=UTF-8" \
  --data-binary @docs/samples/purchase-request.xml
```

一键跑完整链路（产品查询 → 锁价 → 下单 → 查询 → 持仓 → 提货 → 撤销）并打印真实报文：

```bash
bash docs/samples/run-demo.sh
```

## 5. 切换 MySQL

1. 执行 `docs/schema-mysql.sql` 建库建表；
2. 修改 `application.yml`：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/gold_trade?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: your_user
    password: your_password
  sql:
    init:
      mode: never        # 生产由 DBA 执行脚本，禁止应用自动建表
```

## 6. 密钥配置

```yaml
gold:
  sign:
    enabled: true
    sign-type: RSA2
    private-key: ${GOLD_SIGN_PRIVATE_KEY}   # 本行私钥，Base64 PKCS#8
    public-key: ${GOLD_SIGN_PUBLIC_KEY}     # 渠道公钥，Base64 X.509
```

未配置时服务启动会自动生成临时密钥对，并在日志打印本行响应公钥（仅用于演示，重启即失效）。

## 7. 关键设计点

- **单入口路由**：手机银行只对接 `POST /gold/gateway`，新增交易码不动 URL 与防火墙策略。
- **验签在业务之前**：`GoldGatewayService` 顺序为 解析 → 时间戳 → 验签 → 幂等 → 业务，任何一步失败都不触碰账务。
- **幂等独立事务**：`gold_tran_log.req_serial_no` 唯一索引占位，重复请求原样返回首次响应，不会重复扣款。
- **价格锁定**：下单必须携带未过期的 `quoteNo`，一单一价，避免金价波动引发的价格争议。
- **资金与持仓一致**：账户 `SELECT ... FOR UPDATE` 扣款，持仓同事务增减，任一步失败整体回滚。
- **XML 安全**：解析器关闭 DTD 与外部实体，防 XXE。
- **错误必返 XML**：`@RestControllerAdvice` 兜底，任何异常都返回可解析的 `<response>`，不返回 Spring 默认 JSON。

## 8. 测试覆盖

- `SignUtilsTest`：待签名串规则（含同名节点序号、sign 节点排除）、签名验签与篡改校验。
- `GoldGatewayIntegrationTest`：完整 HTTP + XML 链路 7 个交易码、金额计算、幂等重复提交、余额不足、报价失效、下架产品、缺字段、非法交易码。
