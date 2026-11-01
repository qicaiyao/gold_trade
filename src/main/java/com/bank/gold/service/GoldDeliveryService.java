package com.bank.gold.service;

import com.bank.gold.common.GoldConstants;
import com.bank.gold.common.GoldException;
import com.bank.gold.common.IdGen;
import com.bank.gold.common.ParamCheck;
import com.bank.gold.common.RespCode;
import com.bank.gold.dto.request.DeliveryRequest;
import com.bank.gold.dto.response.DeliveryResponse;
import com.bank.gold.entity.GoldDelivery;
import com.bank.gold.entity.GoldHolding;
import com.bank.gold.entity.GoldOrder;
import com.bank.gold.repository.GoldDeliveryRepository;
import com.bank.gold.repository.GoldHoldingRepository;
import com.bank.gold.repository.GoldOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 提货服务：账户存金转实物提取（网点自提 / 快递配送）。
 */
@Slf4j
@Service
public class GoldDeliveryService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GoldDeliveryRepository deliveryRepository;
    private final GoldOrderRepository orderRepository;
    private final GoldHoldingRepository holdingRepository;

    public GoldDeliveryService(GoldDeliveryRepository deliveryRepository,
                               GoldOrderRepository orderRepository,
                               GoldHoldingRepository holdingRepository) {
        this.deliveryRepository = deliveryRepository;
        this.orderRepository = orderRepository;
        this.holdingRepository = holdingRepository;
    }

    /**
     * GOLD1006 提货申请。
     */
    @Transactional(rollbackFor = Exception.class)
    public DeliveryResponse apply(DeliveryRequest request) {
        String customerNo = ParamCheck.notBlank(request.getCustomerNo(), "customerNo", "客户号");
        String orderNo = ParamCheck.notBlank(request.getOrderNo(), "orderNo", "订单号");
        String mode = ParamCheck.notBlank(request.getDeliveryMode(), "deliveryMode", "提货方式");

        GoldOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new GoldException(RespCode.ORDER_NOT_FOUND, "订单不存在: " + orderNo));
        if (!order.getCustomerNo().equals(customerNo)) {
            throw new GoldException(RespCode.ORDER_NOT_FOUND, "订单与客户不匹配: " + orderNo);
        }
        if (!GoldConstants.ORDER_SUCCESS.equals(order.getStatus())) {
            throw new GoldException(RespCode.ORDER_STATUS_INVALID, "当前订单状态不允许提货");
        }
        if (deliveryRepository.findByOrderNo(orderNo).isPresent()) {
            throw new GoldException(RespCode.DELIVERY_EXISTS, "该订单已申请提货，请勿重复提交");
        }
        if (!GoldConstants.MODE_PICKUP.equals(mode) && !GoldConstants.MODE_EXPRESS.equals(mode)) {
            throw new GoldException(RespCode.DELIVERY_INFO_INVALID, "提货方式仅支持 PICKUP 或 EXPRESS");
        }
        checkDeliveryInfo(mode, request.getBranchNo(), request.getReceiverName(),
                request.getReceiverPhone(), request.getReceiverAddress());

        // 可用克重转为待提货冻结克重
        GoldHolding holding = holdingRepository.lockOne(customerNo, order.getProductCode())
                .orElseThrow(() -> new GoldException(RespCode.HOLDING_NOT_ENOUGH, "无可用持仓"));
        if (holding.getAvailableWeight().compareTo(order.getWeight()) < 0) {
            throw new GoldException(RespCode.HOLDING_NOT_ENOUGH,
                    "可提货克重不足，当前可用 " + holding.getAvailableWeight().toPlainString() + " 克");
        }
        holding.setAvailableWeight(holding.getAvailableWeight().subtract(order.getWeight()));
        holding.setFrozenWeight(nz(holding.getFrozenWeight()).add(order.getWeight()));
        holding.setUpdateTime(LocalDateTime.now());
        holdingRepository.save(holding);

        String voucherNo = order.getVoucherNo() == null ? IdGen.voucherNo() : order.getVoucherNo();
        GoldDelivery delivery = create(order, mode, request.getBranchNo(), request.getBranchName(),
                request.getReceiverName(), request.getReceiverPhone(), request.getReceiverAddress());

        order.setDeliveryMode(mode);
        order.setVoucherNo(voucherNo);
        order.setUpdateTime(LocalDateTime.now());
        orderRepository.save(order);

        DeliveryResponse response = new DeliveryResponse();
        response.setDeliveryNo(delivery.getDeliveryNo());
        response.setOrderNo(orderNo);
        response.setVoucherNo(voucherNo);
        response.setDeliveryMode(mode);
        response.setWeight(order.getWeight());
        response.setStatus(delivery.getStatus());
        response.setBranchNo(delivery.getBranchNo());
        response.setBranchName(delivery.getBranchName());
        // 网点自提 T+1 起可提，快递配送 T+3 内寄出
        response.setExpectTime(LocalDateTime.now().plusDays(GoldConstants.MODE_PICKUP.equals(mode) ? 1 : 3)
                .format(TIME_FORMAT));
        log.info("提货申请受理成功，订单号={}, 提货单号={}, 方式={}", orderNo, delivery.getDeliveryNo(), mode);
        return response;
    }

    /**
     * 创建提货单（下单直提与存金转提货共用）。
     */
    public GoldDelivery create(GoldOrder order, String mode, String branchNo, String branchName,
                               String receiverName, String receiverPhone, String receiverAddress) {
        LocalDateTime now = LocalDateTime.now();
        GoldDelivery delivery = new GoldDelivery();
        delivery.setDeliveryNo(IdGen.deliveryNo());
        delivery.setOrderNo(order.getOrderNo());
        delivery.setCustomerNo(order.getCustomerNo());
        delivery.setProductCode(order.getProductCode());
        delivery.setWeight(order.getWeight());
        delivery.setDeliveryMode(mode);
        delivery.setBranchNo(branchNo);
        delivery.setBranchName(branchName);
        delivery.setReceiverName(receiverName);
        delivery.setReceiverPhone(receiverPhone);
        delivery.setReceiverAddress(receiverAddress);
        delivery.setStatus(GoldConstants.DELIVERY_APPLIED);
        delivery.setApplyTime(now);
        delivery.setCreateTime(now);
        delivery.setUpdateTime(now);
        return deliveryRepository.save(delivery);
    }

    /**
     * 提货要素校验：网点自提必填网点，快递配送必填收件信息。
     */
    public void checkDeliveryInfo(String mode, String branchNo, String receiverName,
                                  String receiverPhone, String receiverAddress) {
        if (GoldConstants.MODE_PICKUP.equals(mode)) {
            if (isBlank(branchNo)) {
                throw new GoldException(RespCode.DELIVERY_INFO_INVALID, "网点自提时 branchNo(提货网点号) 必填");
            }
        } else if (GoldConstants.MODE_EXPRESS.equals(mode)) {
            if (isBlank(receiverName) || isBlank(receiverPhone) || isBlank(receiverAddress)) {
                throw new GoldException(RespCode.DELIVERY_INFO_INVALID,
                        "快递配送时 receiverName / receiverPhone / receiverAddress 必填");
            }
            ParamCheck.maxLength(receiverAddress, 200, "receiverAddress");
        } else if (!GoldConstants.MODE_DEPOSIT.equals(mode)) {
            throw new GoldException(RespCode.DELIVERY_INFO_INVALID, "提货方式不合法: " + mode);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
