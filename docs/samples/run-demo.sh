#!/bin/bash
# 一键演示：启动服务 → 依次调用 7 个交易码 → 打印真实报文 → 关闭服务
set -u
cd "$(dirname "$0")/../.." || exit 1

# 默认使用 PATH 中的 mvn，也可通过 MVN=/path/to/mvn 指定
MVN="${MVN:-mvn}"
PORT=18090

$MVN -o spring-boot:run \
  -Dspring-boot.run.arguments="--gold.sign.enabled=false --server.port=${PORT}" > /tmp/gold-run.log 2>&1 &
MVN_PID=$!

for i in $(seq 1 60); do
  if [ "$(curl -s -o /dev/null -w '%{http_code}' -m 2 "http://127.0.0.1:${PORT}/gold/health")" = "200" ]; then
    echo "server up after ${i}s"; break
  fi
  sleep 1
done

post() {
  local code="$1" serial="$2" body="$3"
  local now
  now=$(date "+%Y-%m-%d %H:%M:%S")
  cat > /tmp/req.xml <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<request>
  <header>
    <tranCode>${code}</tranCode>
    <version>1.0.0</version>
    <channel>MOBILE_BANK</channel>
    <bankCode>ICBC</bankCode>
    <reqSerialNo>${serial}</reqSerialNo>
    <reqTime>${now}</reqTime>
    <signType>RSA2</signType>
    <sign>DEMO</sign>
  </header>
  <body>${body}</body>
</request>
EOF
  curl -s -m 10 -X POST "http://127.0.0.1:${PORT}/gold/gateway" \
    -H "Content-Type: application/xml;charset=UTF-8" --data-binary @/tmp/req.xml
}

post GOLD1001 MB20261101000101 "<productCode>AU9999-10G</productCode>" > /tmp/resp1001.xml
post GOLD1002 MB20261101000102 "<customerNo>C1000001</customerNo><productCode>AU9999-10G</productCode><weight>10</weight>" > /tmp/resp1002.xml
QUOTE=$(sed -n 's:.*<quoteNo>\([^<]*\)</quoteNo>.*:\1:p' /tmp/resp1002.xml)
echo "quoteNo=$QUOTE"
post GOLD1003 MB20261101000103 "<customerNo>C1000001</customerNo><accountNo>6222021234567890</accountNo><productCode>AU9999-10G</productCode><quoteNo>${QUOTE}</quoteNo><weight>10</weight><deliveryMode>DEPOSIT</deliveryMode><remark>手机银行购买</remark>" > /tmp/resp1003.xml
ORDER=$(sed -n 's:.*<orderNo>\([^<]*\)</orderNo>.*:\1:p' /tmp/resp1003.xml)
echo "orderNo=$ORDER"
post GOLD1004 MB20261101000104 "<customerNo>C1000001</customerNo><orderNo>${ORDER}</orderNo>" > /tmp/resp1004.xml
post GOLD1007 MB20261101000105 "<customerNo>C1000001</customerNo>" > /tmp/resp1007.xml
post GOLD1006 MB20261101000106 "<customerNo>C1000001</customerNo><orderNo>${ORDER}</orderNo><deliveryMode>PICKUP</deliveryMode><branchNo>01001</branchNo><branchName>北京西单支行</branchName>" > /tmp/resp1006.xml
post GOLD1005 MB20261101000107 "<customerNo>C1000001</customerNo><orderNo>${ORDER}</orderNo><reason>客户取消</reason>" > /tmp/resp1005.xml
post GOLD1003 MB20261101000108 "<customerNo>C1000001</customerNo><accountNo>6222021234567890</accountNo><productCode>AU9999-10G</productCode><quoteNo>Q-NOT-EXIST</quoteNo><weight>10</weight><deliveryMode>DEPOSIT</deliveryMode>" > /tmp/resp_err.xml

echo "===== 1001 ====="; cat /tmp/resp1001.xml
echo "===== 1002 ====="; cat /tmp/resp1002.xml
echo "===== 1003 ====="; cat /tmp/resp1003.xml
echo "===== 1004 ====="; cat /tmp/resp1004.xml
echo "===== 1007 ====="; cat /tmp/resp1007.xml
echo "===== 1006 ====="; cat /tmp/resp1006.xml
echo "===== 1005 ====="; cat /tmp/resp1005.xml
echo "===== ERR  ====="; cat /tmp/resp_err.xml

kill $MVN_PID 2>/dev/null
sleep 1
pkill -f GoldTradeApplication 2>/dev/null
exit 0
