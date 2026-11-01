package com.bank.gold.protocol;

import com.bank.gold.common.GoldException;
import com.bank.gold.common.RespCode;
import com.bank.gold.common.TranCode;
import com.bank.gold.dto.request.CancelRequest;
import com.bank.gold.dto.request.DeliveryRequest;
import com.bank.gold.dto.request.HoldingQueryRequest;
import com.bank.gold.dto.request.OrderQueryRequest;
import com.bank.gold.dto.request.ProductQueryRequest;
import com.bank.gold.dto.request.PurchaseRequest;
import com.bank.gold.dto.request.QuoteLockRequest;
import com.bank.gold.service.GoldDeliveryService;
import com.bank.gold.service.GoldProductService;
import com.bank.gold.service.GoldPurchaseService;
import com.bank.gold.service.GoldQueryService;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.stereotype.Component;

/**
 * 交易码分发器：把 body 报文反序列化为对应 DTO，并路由到业务服务。
 */
@Component
public class TranDispatcher {

    private final XmlMapper xmlMapper;
    private final GoldProductService productService;
    private final GoldPurchaseService purchaseService;
    private final GoldQueryService queryService;
    private final GoldDeliveryService deliveryService;

    public TranDispatcher(XmlMapper xmlMapper,
                          GoldProductService productService,
                          GoldPurchaseService purchaseService,
                          GoldQueryService queryService,
                          GoldDeliveryService deliveryService) {
        this.xmlMapper = xmlMapper;
        this.productService = productService;
        this.purchaseService = purchaseService;
        this.queryService = queryService;
        this.deliveryService = deliveryService;
    }

    /**
     * @return 响应 body 对象，由网关统一序列化为 XML
     */
    public Object dispatch(TranCode tranCode, String bodyXml, String reqSerialNo) {
        return switch (tranCode) {
            case GOLD1001 -> productService.queryProducts(read(bodyXml, ProductQueryRequest.class));
            case GOLD1002 -> productService.lockQuote(read(bodyXml, QuoteLockRequest.class));
            case GOLD1003 -> purchaseService.purchase(read(bodyXml, PurchaseRequest.class), reqSerialNo);
            case GOLD1004 -> queryService.queryOrders(read(bodyXml, OrderQueryRequest.class));
            case GOLD1005 -> purchaseService.cancel(read(bodyXml, CancelRequest.class));
            case GOLD1006 -> deliveryService.apply(read(bodyXml, DeliveryRequest.class));
            case GOLD1007 -> queryService.queryHoldings(read(bodyXml, HoldingQueryRequest.class));
        };
    }

    private <T> T read(String bodyXml, Class<T> type) {
        try {
            return xmlMapper.readValue(bodyXml, type);
        } catch (Exception e) {
            throw new GoldException(RespCode.MSG_FORMAT_ERROR, "body 报文解析失败: " + e.getMessage());
        }
    }
}
