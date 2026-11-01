package com.bank.gold.service;

import com.bank.gold.common.GoldException;
import com.bank.gold.common.ParamCheck;
import com.bank.gold.common.RespCode;
import com.bank.gold.dto.request.HoldingQueryRequest;
import com.bank.gold.dto.request.OrderQueryRequest;
import com.bank.gold.dto.response.HoldingQueryResponse;
import com.bank.gold.dto.response.OrderQueryResponse;
import com.bank.gold.entity.GoldHolding;
import com.bank.gold.entity.GoldOrder;
import com.bank.gold.entity.GoldProduct;
import com.bank.gold.repository.GoldHoldingRepository;
import com.bank.gold.repository.GoldOrderRepository;
import com.bank.gold.repository.GoldProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询服务：订单查询、持仓查询。
 */
@Service
@Transactional(readOnly = true)
public class GoldQueryService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final GoldOrderRepository orderRepository;
    private final GoldHoldingRepository holdingRepository;
    private final GoldProductRepository productRepository;

    public GoldQueryService(GoldOrderRepository orderRepository,
                            GoldHoldingRepository holdingRepository,
                            GoldProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.holdingRepository = holdingRepository;
        this.productRepository = productRepository;
    }

    /**
     * GOLD1004 购买订单查询：传 orderNo 精确查单笔，否则按客户 + 日期区间查询（默认最近 20 笔）。
     */
    public OrderQueryResponse queryOrders(OrderQueryRequest request) {
        String customerNo = ParamCheck.notBlank(request.getCustomerNo(), "customerNo", "客户号");
        String orderNo = ParamCheck.optional(request.getOrderNo());
        String beginDate = ParamCheck.optional(request.getBeginDate());
        String endDate = ParamCheck.optional(request.getEndDate());

        List<GoldOrder> orders = new ArrayList<>();
        if (orderNo != null) {
            GoldOrder order = orderRepository.findByOrderNo(orderNo)
                    .orElseThrow(() -> new GoldException(RespCode.ORDER_NOT_FOUND, "订单不存在: " + orderNo));
            if (!order.getCustomerNo().equals(customerNo)) {
                throw new GoldException(RespCode.ORDER_NOT_FOUND, "订单与客户不匹配: " + orderNo);
            }
            orders.add(order);
        } else if (beginDate != null && endDate != null) {
            LocalDate begin = parseDate(beginDate, "beginDate");
            LocalDate end = parseDate(endDate, "endDate");
            if (begin.isAfter(end)) {
                throw new GoldException(RespCode.PARAM_FORMAT_ERROR, "beginDate 不能晚于 endDate");
            }
            orders.addAll(orderRepository.findByCustomerNoAndOrderTimeBetweenOrderByOrderTimeDesc(
                    customerNo, begin.atStartOfDay(), end.plusDays(1).atStartOfDay()));
        } else {
            orders.addAll(orderRepository.findTop20ByCustomerNoOrderByOrderTimeDesc(customerNo));
        }

        OrderQueryResponse response = new OrderQueryResponse();
        response.setTotalCount(orders.size());
        response.setOrders(orders.stream().map(this::toItem).toList());
        return response;
    }

    /**
     * GOLD1007 黄金持仓查询：按当前金价计算市值与浮动盈亏。
     */
    public HoldingQueryResponse queryHoldings(HoldingQueryRequest request) {
        String customerNo = ParamCheck.notBlank(request.getCustomerNo(), "customerNo", "客户号");
        List<GoldHolding> holdings = holdingRepository.findByCustomerNoOrderByProductCodeAsc(customerNo);

        HoldingQueryResponse response = new HoldingQueryResponse();
        response.setCustomerNo(customerNo);
        List<HoldingQueryResponse.HoldingItem> items = new ArrayList<>();
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalMarket = BigDecimal.ZERO;

        for (GoldHolding holding : holdings) {
            BigDecimal weight = nz(holding.getTotalWeight());
            BigDecimal cost = nz(holding.getCostAmount());
            BigDecimal price = productRepository.findByProductCode(holding.getProductCode())
                    .map(GoldProduct::getPricePerGram)
                    .orElse(BigDecimal.ZERO);
            BigDecimal marketValue = weight.multiply(price).setScale(2, RoundingMode.HALF_UP);
            BigDecimal avgPrice = weight.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : cost.divide(weight, 2, RoundingMode.HALF_UP);

            HoldingQueryResponse.HoldingItem item = new HoldingQueryResponse.HoldingItem();
            item.setProductCode(holding.getProductCode());
            item.setProductName(holding.getProductName());
            item.setTotalWeight(weight);
            item.setAvailableWeight(nz(holding.getAvailableWeight()));
            item.setFrozenWeight(nz(holding.getFrozenWeight()));
            item.setCostAmount(cost);
            item.setAvgPrice(avgPrice);
            item.setMarketValue(marketValue);
            item.setProfit(marketValue.subtract(cost));
            items.add(item);

            totalWeight = totalWeight.add(weight);
            totalCost = totalCost.add(cost);
            totalMarket = totalMarket.add(marketValue);
        }

        response.setHoldings(items);
        response.setTotalWeight(totalWeight);
        response.setTotalCostAmount(totalCost);
        response.setTotalMarketValue(totalMarket);
        response.setTotalProfit(totalMarket.subtract(totalCost));
        return response;
    }

    private OrderQueryResponse.OrderItem toItem(GoldOrder order) {
        OrderQueryResponse.OrderItem item = new OrderQueryResponse.OrderItem();
        item.setOrderNo(order.getOrderNo());
        item.setProductCode(order.getProductCode());
        item.setProductName(order.getProductName());
        item.setWeight(order.getWeight());
        item.setPricePerGram(order.getPricePerGram());
        item.setGoldAmount(order.getGoldAmount());
        item.setFeeAmount(order.getFeeAmount());
        item.setTotalAmount(order.getTotalAmount());
        item.setDeliveryMode(order.getDeliveryMode());
        item.setStatus(order.getStatus());
        item.setVoucherNo(order.getVoucherNo());
        item.setOrderTime(order.getOrderTime() == null ? null : order.getOrderTime().format(TIME_FORMAT));
        return item;
    }

    private LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value, DATE_FORMAT);
        } catch (Exception e) {
            throw new GoldException(RespCode.PARAM_FORMAT_ERROR, field + " 格式应为 yyyy-MM-dd");
        }
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
