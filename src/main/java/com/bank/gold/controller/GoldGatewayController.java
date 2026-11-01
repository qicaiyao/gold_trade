package com.bank.gold.controller;

import com.bank.gold.protocol.GoldGatewayService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 实物黄金交易统一网关入口。
 *
 * <p>手机银行只对接一个 URL：{@code POST /gold/gateway}，
 * 通过 header/tranCode 区分具体交易，新增交易码不影响渠道侧改造。</p>
 */
@RestController
@RequestMapping("/gold")
public class GoldGatewayController {

    private static final MediaType XML_UTF8 = MediaType.parseMediaType("application/xml;charset=UTF-8");

    private final GoldGatewayService gatewayService;

    public GoldGatewayController(GoldGatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @PostMapping(value = "/gateway", produces = "application/xml;charset=UTF-8")
    public ResponseEntity<String> gateway(@RequestBody(required = false) String xml) {
        return ResponseEntity.ok().contentType(XML_UTF8).body(gatewayService.handle(xml));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("OK");
    }
}
