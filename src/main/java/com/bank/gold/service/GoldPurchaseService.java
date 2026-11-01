package com.bank.gold.service;

import com.bank.gold.common.GoldConstants;
import com.bank.gold.common.GoldException;
import com.bank.gold.common.IdGen;
import com.bank.gold.common.ParamCheck;
import com.bank.gold.common.RespCode;
import com.bank.gold.config.GoldTradeProperties;
import com.bank.gold.dto.request.CancelRequest;
import com.bank.gold.dto.request.PurchaseRequest;
import com.bank.gold.dto.response.CancelResponse;
import com.bank.gold.dto.response.PurchaseResponse;
import com.bank.gold.entity.GoldAccount;
import com.bank.gold.entity.GoldCustomer;
import com.bank.gold.entity.GoldDelivery;
import com.bank.gold.entity.GoldHolding;
import com.bank.gold.entity.GoldOrder;
import com.bank.gold.entity.GoldProduct;
import com.bank.gold.entity.GoldQuote;
import com.bank.gold.repository.GoldAccountRepository;
import com.bank.gold.repository.GoldCustomerRepository;
import com.bank.gold.repository.GoldDeliveryRepository;
import com.bank.gold.repository.GoldHoldingRepository;
import com.bank.gold.repository.GoldOrderRepository;
import com.bank.gold.repository.GoldQuoteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 实物黄金购买与撤销服务（核心交易）。
 *
 * <p>下单校验顺序：客户状态 → 产品在售 → 克重规格 → 风险等级 → 报价有效性 → 提货要素
 * → 限额 → 账户余额。全部通过后才落单扣款，任一步失败整体回滚。</p>
 */
@Slf4j
@Service
public class GoldPurchaseService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GoldCustomerRepository customerRepository;
    private final GoldAccountRepository accountRepository;
    private final GoldHoldingRepository holdingRepository;
    private final GoldOrderRepository orderRepository;
    private final GoldQuoteRepository quoteRepository;
    private final GoldDeliveryRepository deliveryRepository;
    private final GoldProductService productService;
    private final GoldDeliveryService deliveryService;
    private final GoldTradeProperties properties;

    public GoldPurchaseService(GoldCustomerRepository customerRepository,
                               GoldAccountRepository accountRepository,
                               GoldHoldingRepository holdingRepository,
                               GoldOrderRepository orderRepository,
                               GoldQuoteRepository quoteRepository,
                               GoldDeliveryRepository deliveryRepository,
                               GoldProductService productService,
                               GoldDeliveryService deliveryService,
                               GoldTradeProperties properties) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.holdingRepository = holdingRepository;
        this.orderRepository = orderRepository;
        this.quoteRepository = quoteRepository;
        this.deliveryRepository = deliveryRepository;
        this.productService = productService;
        this.deliveryService = deliveryService;
        this.properties = properties;
    }

    /**
     * GOLD1003 实物黄金购买下单。
     */
    @Transactional(rollbackFor = Exception.class)
    public PurchaseResponse purchase(PurchaseRequest request, String reqSerialNo) {
        String customerNo = ParamCheck.notBlank(request.getCustomerNo(), "customerNo", "客户号");
        String accountNo = ParamCheck.notBlank(request.getAccountNo(), "accountNo", "付款账号");
        String productCode = ParamCheck.notBlank(request.getProductCode(), "productCode", "产品代码");
        String quoteNo = ParamCheck.notBlank(request.getQuoteNo(), "quoteNo", "报价单号");
        String mode = ParamCheck.notBlank(request.getDeliveryMode(), "deliveryMode", "提货方式");
        BigDecimal weight = ParamCheck.positive(request.getWeight(), "weight", "购买克重");
        ParamCheck.maxLength(request.getRemark(), 200, "remark");

        GoldCustomer customer = customerRepository.findByCustomerNo(customerNo)
                .orElseThrow(() -> new GoldException(RespCode.CUSTOMER_INVALID, "客户不存在: " + customerNo));
        if (!GoldConstants.CUSTOMER_ACTIVE.equals(customer.getStatus())) {
            throw new GoldException(RespCode.CUSTOMER_INVALID, "客户状态异常: " + customer.getStatus());
        }

        GoldProduct product = productService.requireOnSale(productCode);
        productService.checkWeight(weight, product);
        checkRiskLevel(customer, product);
        GoldQuote quote = productService.requireValidQuote(quoteNo, productCode);
        deliveryService.checkDeliveryInfo(mode, request.getBranchNo(), request.getReceiverName(),
                request.getReceiverPhone(), request.getReceiverAddress());

        BigDecimal price = quote.getPricePerGram();
        BigDecimal feePerGram = quote.getFeePerGram() == null ? BigDecimal.ZERO : quote.getFeePerGram();
        BigDecimal goldAmount = productService.calcAmount(weight, price);
        BigDecimal feeAmount = productService.calcAmount(weight, feePerGram);
        BigDecimal totalAmount = goldAmount.add(feeAmount);

        checkAmountLimit(customerNo, totalAmount);

        // 1) 扣款（悲观锁，防并发超扣）
        GoldAccount account = accountRepository.lockByAccountNo(accountNo)
                .orElseThrow(() -> new GoldException(RespCode.BALANCE_NOT_ENOUGH, "付款账户不存在: " + accountNo));
        if (!account.getCustomerNo().equals(customerNo)) {
            throw new GoldException(RespCode.CUSTOMER_INVALID, "付款账户与客户不匹配");
        }
        BigDecimal balance = account.getAvailableBalance() == null ? BigDecimal.ZERO : account.getAvailableBalance();
        if (balance.compareTo(totalAmount) < 0) {
            throw new GoldException(RespCode.BALANCE_NOT_ENOUGH,
                    "账户可用余额不足，应付 " + totalAmount.toPlainString() + " 元，可用 " + balance.toPlainString() + " 元");
        }
        account.setAvailableBalance(balance.subtract(totalAmount));
        account.setUpdateTime(LocalDateTime.now());
        accountRepository.save(account);

        // 2) 报价核销（一单一价，防止重复使用）
        quote.setStatus(GoldConstants.QUOTE_USED);
        quoteRepository.save(quote);

        // 3) 落订单
        LocalDateTime now = LocalDateTime.now();
        GoldOrder order = new GoldOrder();
        order.setOrderNo(IdGen.orderNo());
        order.setReqSerialNo(reqSerialNo);
        order.setQuoteNo(quoteNo);
        order.setCustomerNo(customerNo);
        order.setAccountNo(accountNo);
        order.setProductCode(product.getProductCode());
        order.setProductName(product.getProductName());
        order.setWeight(weight);
        order.setPricePerGram(price);
        order.setGoldAmount(goldAmount);
        order.setFeeAmount(feeAmount);
        order.setTotalAmount(totalAmount);
        order.setDeliveryMode(mode);
        order.setStatus(GoldConstants.ORDER_SUCCESS);
        order.setOrderTime(now);
        order.setRemark(request.getRemark());
        order.setCreateTime(now);
        order.setUpdateTime(now);

        // 4) 需要实物提取时生成提货单与凭证号
        String deliveryNo = null;
        if (!GoldConstants.MODE_DEPOSIT.equals(mode)) {
            String voucherNo = IdGen.voucherNo();
            order.setVoucherNo(voucherNo);
            orderRepository.save(order);
            GoldDelivery delivery = deliveryService.create(order, mode, request.getBranchNo(), null,
                    request.getReceiverName(), request.getReceiverPhone(), request.getReceiverAddress());
            deliveryNo = delivery.getDeliveryNo();
        } else {
            orderRepository.save(order);
        }

        // 5) 持仓入账
        GoldHolding holding = applyHolding(customerNo, product, weight, totalAmount, mode);

        PurchaseResponse response = new PurchaseResponse();
        response.setOrderNo(order.getOrderNo());
        response.setStatus(order.getStatus());
        response.setCustomerNo(customerNo);
        response.setProductCode(product.getProductCode());
        response.setProductName(product.getProductName());
        response.setWeight(weight);
        response.setPricePerGram(price);
        response.setGoldAmount(goldAmount);
        response.setFeeAmount(feeAmount);
        response.setTotalAmount(totalAmount);
        response.setDeliveryMode(mode);
        response.setVoucherNo(order.getVoucherNo());
        response.setDeliveryNo(deliveryNo);
        response.setAccountNo(accountNo);
        response.setBalanceAfter(account.getAvailableBalance());
        response.setHoldingWeight(holding.getTotalWeight());
        response.setOrderTime(now.format(TIME_FORMAT));

        log.info("黄金购买成功，订单号={}, 客户={}, 产品={}, 克重={}, 实付={}",
                order.getOrderNo(), customerNo, product.getProductCode(), weight, totalAmount);
        return response;
    }

    /**
     * GOLD1005 购买订单撤销（当日、未提货）。
     */
    @Transactional(rollbackFor = Exception.class)
    public CancelResponse cancel(CancelRequest request) {
        String customerNo = ParamCheck.notBlank(request.getCustomerNo(), "customerNo", "客户号");
        String orderNo = ParamCheck.notBlank(request.getOrderNo(), "orderNo", "订单号");
        ParamCheck.maxLength(request.getReason(), 200, "reason");

        GoldOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new GoldException(RespCode.ORDER_NOT_FOUND, "订单不存在: " + orderNo));
        if (!order.getCustomerNo().equals(customerNo)) {
            throw new GoldException(RespCode.ORDER_NOT_FOUND, "订单与客户不匹配: " + orderNo);
        }
        if (!GoldConstants.ORDER_SUCCESS.equals(order.getStatus())) {
            throw new GoldException(RespCode.ORDER_STATUS_INVALID, "当前订单状态不允许撤销: " + order.getStatus());
        }
        if (!order.getOrderTime().toLocalDate().equals(LocalDate.now())) {
            throw new GoldException(RespCode.ORDER_STATUS_INVALID, "仅支持撤销当日订单");
        }
        GoldDelivery delivery = deliveryRepository.findByOrderNo(orderNo).orElse(null);
        if (delivery != null && !GoldConstants.DELIVERY_APPLIED.equals(delivery.getStatus())) {
            throw new GoldException(RespCode.ORDER_STATUS_INVALID, "订单已提货或已取消，不可撤销");
        }

        LocalDateTime now = LocalDateTime.now();

        // 资金原路退回
        GoldAccount account = accountRepository.lockByAccountNo(order.getAccountNo())
                .orElseThrow(() -> new GoldException(RespCode.BALANCE_NOT_ENOUGH, "付款账户不存在"));
        BigDecimal balance = account.getAvailableBalance() == null ? BigDecimal.ZERO : account.getAvailableBalance();
        account.setAvailableBalance(balance.add(order.getTotalAmount()));
        account.setUpdateTime(now);
        accountRepository.save(account);

        // 持仓回退
        GoldHolding holding = holdingRepository.lockOne(customerNo, order.getProductCode()).orElse(null);
        BigDecimal holdingWeight = BigDecimal.ZERO;
        if (holding != null) {
            holding.setTotalWeight(nz(holding.getTotalWeight()).subtract(order.getWeight()));
            if (GoldConstants.MODE_DEPOSIT.equals(order.getDeliveryMode())) {
                holding.setAvailableWeight(nz(holding.getAvailableWeight()).subtract(order.getWeight()));
            } else {
                holding.setFrozenWeight(nz(holding.getFrozenWeight()).subtract(order.getWeight()));
            }
            holding.setCostAmount(nz(holding.getCostAmount()).subtract(order.getTotalAmount()));
            holding.setUpdateTime(now);
            holdingRepository.save(holding);
            holdingWeight = holding.getTotalWeight();
        }

        if (delivery != null) {
            delivery.setStatus(GoldConstants.DELIVERY_CANCELLED);
            delivery.setUpdateTime(now);
            deliveryRepository.save(delivery);
        }

        order.setStatus(GoldConstants.ORDER_CANCELLED);
        order.setCancelTime(now);
        order.setRemark(request.getReason() == null ? order.getRemark() : request.getReason());
        order.setUpdateTime(now);
        orderRepository.save(order);

        CancelResponse response = new CancelResponse();
        response.setOrderNo(orderNo);
        response.setStatus(order.getStatus());
        response.setRefundAmount(order.getTotalAmount());
        response.setAccountNo(order.getAccountNo());
        response.setBalanceAfter(account.getAvailableBalance());
        response.setHoldingWeight(holdingWeight);
        response.setCancelTime(now.format(TIME_FORMAT));

        log.info("订单撤销成功，订单号={}, 退款={}", orderNo, order.getTotalAmount());
        return response;
    }

    // ------------------------------------------------------------------

    private void checkRiskLevel(GoldCustomer customer, GoldProduct product) {
        int customerRank = riskRank(customer.getRiskLevel());
        int productRank = riskRank(product.getRiskLevel());
        if (customerRank < productRank) {
            throw new GoldException(RespCode.RISK_LEVEL_NOT_ENOUGH,
                    "客户风险等级 " + customer.getRiskLevel() + " 低于产品要求 " + product.getRiskLevel());
        }
    }

    private int riskRank(String level) {
        if (level == null || level.length() < 2) {
            return 1;
        }
        char c = level.charAt(1);
        return Character.isDigit(c) ? Character.getNumericValue(c) : 1;
    }

    private void checkAmountLimit(String customerNo, BigDecimal totalAmount) {
        BigDecimal singleLimit = properties.getLimit().getSingleAmountLimit();
        if (singleLimit != null && totalAmount.compareTo(singleLimit) > 0) {
            throw new GoldException(RespCode.SINGLE_LIMIT_EXCEEDED,
                    "单笔购买金额不能超过 " + singleLimit.toPlainString() + " 元");
        }
        BigDecimal dailyLimit = properties.getLimit().getDailyAmountLimit();
        if (dailyLimit == null) {
            return;
        }
        BigDecimal todayAmount = orderRepository.sumSuccessAmount(customerNo, LocalDate.now().atStartOfDay());
        BigDecimal used = todayAmount == null ? BigDecimal.ZERO : todayAmount;
        if (used.add(totalAmount).compareTo(dailyLimit) > 0) {
            throw new GoldException(RespCode.DAILY_LIMIT_EXCEEDED,
                    "超过单日累计购买限额 " + dailyLimit.toPlainString() + " 元，今日已购 " + used.toPlainString() + " 元");
        }
    }

    private GoldHolding applyHolding(String customerNo, GoldProduct product, BigDecimal weight,
                                     BigDecimal totalAmount, String mode) {
        LocalDateTime now = LocalDateTime.now();
        GoldHolding holding = holdingRepository.lockOne(customerNo, product.getProductCode()).orElse(null);
        if (holding == null) {
            holding = new GoldHolding();
            holding.setCustomerNo(customerNo);
            holding.setProductCode(product.getProductCode());
            holding.setProductName(product.getProductName());
            holding.setTotalWeight(BigDecimal.ZERO);
            holding.setAvailableWeight(BigDecimal.ZERO);
            holding.setFrozenWeight(BigDecimal.ZERO);
            holding.setCostAmount(BigDecimal.ZERO);
            holding.setCreateTime(now);
        }
        holding.setTotalWeight(nz(holding.getTotalWeight()).add(weight));
        if (GoldConstants.MODE_DEPOSIT.equals(mode)) {
            holding.setAvailableWeight(nz(holding.getAvailableWeight()).add(weight));
        } else {
            holding.setFrozenWeight(nz(holding.getFrozenWeight()).add(weight));
        }
        holding.setCostAmount(nz(holding.getCostAmount()).add(totalAmount));
        holding.setUpdateTime(now);
        return holdingRepository.save(holding);
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
