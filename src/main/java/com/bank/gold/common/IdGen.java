package com.bank.gold.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 单号生成器。生产环境建议改为「发号器服务 / 雪花算法」，保证全局唯一且可追溯。
 */
public final class IdGen {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private IdGen() {
    }

    /** 订单号：AU + 时间戳 + 4 位随机数 */
    public static String orderNo() {
        return gen("AU");
    }

    /** 报价单号 */
    public static String quoteNo() {
        return gen("Q");
    }

    /** 提货单号 */
    public static String deliveryNo() {
        return gen("D");
    }

    /** 提货凭证号（客户凭此到网点提金） */
    public static String voucherNo() {
        return gen("V");
    }

    /** 响应流水号 */
    public static String respSerialNo() {
        return gen("R");
    }

    private static String gen(String prefix) {
        return prefix + LocalDateTime.now().format(TS) + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
