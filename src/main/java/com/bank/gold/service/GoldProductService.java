package com.bank.gold.service;

import com.bank.gold.common.GoldConstants;
import com.bank.gold.common.GoldException;
import com.bank.gold.common.IdGen;
import com.bank.gold.common.ParamCheck;
import com.bank.gold.common.RespCode;
import com.bank.gold.config.GoldTradeProperties;
import com.bank.gold.dto.request.ProductQueryRequest;
import com.bank.gold.dto.request.QuoteLockRequest;
import com.bank.gold.dto.response.ProductQueryResponse;
import com.bank.gold.dto.response.QuoteLockResponse;
import com.bank.gold.entity.GoldProduct;
import com.bank.gold.entity.GoldQuote;
import com.bank.gold.repository.GoldProductRepository;
import com.bank.gold.repository.GoldQuoteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 产品与报价服务：产品查询、报价锁定、产品/报价校验。
 */
@Slf4j
@Service
public class GoldProductService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GoldProductRepository productRepository;
    private final GoldQuoteRepository quoteRepository;
    private final GoldTradeProperties properties;

    public GoldProductService(GoldProductRepository productRepository,
                              GoldQuoteRepository quoteRepository,
                              GoldTradeProperties properties) {
        this.productRepository = productRepository;
        this.quoteRepository = quoteRepository;
        this.properties = properties;
    }

    /**
     * GOLD1001 产品与实时金价查询。
     */
    public ProductQueryResponse queryProducts(ProductQueryRequest request) {
        String productCode = ParamCheck.optional(request.getProductCode());
        String brand = ParamCheck.optional(request.getBrand());

        List<GoldProduct> products = (productCode == null
                ? productRepository.findByStatusOrderByPricePerGramAsc(GoldConstants.PRODUCT_ON_SALE)
                : productRepository.findByStatusAndProductCodeOrderByPricePerGramAsc(
                        GoldConstants.PRODUCT_ON_SALE, productCode))
                .stream()
                .filter(p -> brand == null || brand.equals(p.getBrand()))
                .toList();

        ProductQueryResponse response = new ProductQueryResponse();
        response.setQueryTime(LocalDateTime.now().format(TIME_FORMAT));
        response.setTotalCount(products.size());
        response.setProducts(products.stream().map(this::toItem).toList());
        return response;
    }

    /**
     * GOLD1002 报价锁定：返回 quoteNo，有效期内按该价格成交。
     */
    @Transactional(rollbackFor = Exception.class)
    public QuoteLockResponse lockQuote(QuoteLockRequest request) {
        ParamCheck.notBlank(request.getCustomerNo(), "customerNo", "客户号");
        String productCode = ParamCheck.notBlank(request.getProductCode(), "productCode", "产品代码");
        GoldProduct product = requireOnSale(productCode);

        LocalDateTime now = LocalDateTime.now();
        int ttl = Math.max(properties.getQuote().getTtlSeconds(), 10);
        LocalDateTime expireTime = now.plusSeconds(ttl);

        GoldQuote quote = new GoldQuote();
        quote.setQuoteNo(IdGen.quoteNo());
        quote.setProductCode(product.getProductCode());
        quote.setPricePerGram(product.getPricePerGram());
        quote.setFeePerGram(feeOf(product));
        quote.setQuoteTime(now);
        quote.setExpireTime(expireTime);
        quote.setStatus(GoldConstants.QUOTE_UNUSED);
        quoteRepository.save(quote);

        QuoteLockResponse response = new QuoteLockResponse();
        response.setQuoteNo(quote.getQuoteNo());
        response.setCustomerNo(request.getCustomerNo());
        response.setProductCode(product.getProductCode());
        response.setProductName(product.getProductName());
        response.setPricePerGram(quote.getPricePerGram());
        response.setFeePerGram(quote.getFeePerGram());
        response.setQuoteTime(now.format(TIME_FORMAT));
        response.setExpireTime(expireTime.format(TIME_FORMAT));
        response.setExpireSeconds(ttl);

        // 传入克重时顺带试算金额，减少手机银行一次交互
        if (request.getWeight() != null) {
            checkWeight(request.getWeight(), product);
            BigDecimal goldAmount = calcAmount(request.getWeight(), quote.getPricePerGram());
            BigDecimal feeAmount = calcAmount(request.getWeight(), quote.getFeePerGram());
            response.setWeight(request.getWeight());
            response.setGoldAmount(goldAmount);
            response.setFeeAmount(feeAmount);
            response.setTotalAmount(goldAmount.add(feeAmount));
        }
        return response;
    }

    /**
     * 校验产品在售。
     */
    public GoldProduct requireOnSale(String productCode) {
        GoldProduct product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new GoldException(RespCode.PRODUCT_INVALID, "产品不存在: " + productCode));
        if (!GoldConstants.PRODUCT_ON_SALE.equals(product.getStatus())) {
            throw new GoldException(RespCode.PRODUCT_INVALID, "产品已下架: " + productCode);
        }
        return product;
    }

    /**
     * 校验报价单可用（存在、未使用、未过期、产品匹配）。
     */
    public GoldQuote requireValidQuote(String quoteNo, String productCode) {
        GoldQuote quote = quoteRepository.findByQuoteNo(quoteNo)
                .orElseThrow(() -> new GoldException(RespCode.QUOTE_INVALID, "报价单不存在: " + quoteNo));
        if (!GoldConstants.QUOTE_UNUSED.equals(quote.getStatus())) {
            throw new GoldException(RespCode.QUOTE_INVALID, "报价单已使用或已失效: " + quoteNo);
        }
        if (quote.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new GoldException(RespCode.QUOTE_INVALID, "报价已过期，请重新获取实时金价");
        }
        if (!quote.getProductCode().equals(productCode)) {
            throw new GoldException(RespCode.QUOTE_INVALID, "报价单与产品不匹配");
        }
        return quote;
    }

    /**
     * 克重规格校验：精度、上下限、步长。
     */
    public void checkWeight(BigDecimal weight, GoldProduct product) {
        if (weight.stripTrailingZeros().scale() > 4) {
            throw new GoldException(RespCode.WEIGHT_INVALID, "购买克重最多支持 4 位小数");
        }
        if (product.getMinWeight() != null && weight.compareTo(product.getMinWeight()) < 0) {
            throw new GoldException(RespCode.WEIGHT_INVALID,
                    "购买克重不能低于 " + plain(product.getMinWeight()) + " 克");
        }
        if (product.getMaxWeightPerOrder() != null && weight.compareTo(product.getMaxWeightPerOrder()) > 0) {
            throw new GoldException(RespCode.WEIGHT_INVALID,
                    "单笔购买克重不能超过 " + plain(product.getMaxWeightPerOrder()) + " 克");
        }
        BigDecimal step = product.getStepWeight();
        if (step != null && step.compareTo(BigDecimal.ZERO) > 0
                && weight.remainder(step).compareTo(BigDecimal.ZERO) != 0) {
            throw new GoldException(RespCode.WEIGHT_INVALID,
                    "购买克重需为 " + plain(step) + " 克的整数倍");
        }
    }

    /**
     * 金额计算：克重 × 单价，保留 2 位小数（四舍五入）。
     */
    public BigDecimal calcAmount(BigDecimal weight, BigDecimal perGram) {
        if (perGram == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return weight.multiply(perGram).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal feeOf(GoldProduct product) {
        return product.getFeePerGram() == null ? BigDecimal.ZERO : product.getFeePerGram();
    }

    private ProductQueryResponse.ProductItem toItem(GoldProduct product) {
        ProductQueryResponse.ProductItem item = new ProductQueryResponse.ProductItem();
        item.setProductCode(product.getProductCode());
        item.setProductName(product.getProductName());
        item.setBrand(product.getBrand());
        item.setSpec(product.getSpec());
        item.setUnit(product.getUnit());
        item.setPricePerGram(product.getPricePerGram());
        item.setFeePerGram(feeOf(product));
        item.setMinWeight(product.getMinWeight());
        item.setMaxWeightPerOrder(product.getMaxWeightPerOrder());
        item.setStepWeight(product.getStepWeight());
        item.setRiskLevel(product.getRiskLevel());
        item.setStatus(product.getStatus());
        return item;
    }

    private String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
