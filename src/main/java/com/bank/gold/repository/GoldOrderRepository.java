package com.bank.gold.repository;

import com.bank.gold.entity.GoldOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoldOrderRepository extends JpaRepository<GoldOrder, Long> {

    Optional<GoldOrder> findByOrderNo(String orderNo);

    List<GoldOrder> findTop20ByCustomerNoOrderByOrderTimeDesc(String customerNo);

    List<GoldOrder> findByCustomerNoAndOrderTimeBetweenOrderByOrderTimeDesc(
            String customerNo, LocalDateTime begin, LocalDateTime end);

    /**
     * 统计客户当日累计购买金额（用于单日限额校验）。
     */
    @Query("select sum(o.totalAmount) from GoldOrder o "
            + "where o.customerNo = :customerNo and o.status = 'SUCCESS' and o.orderTime >= :begin")
    BigDecimal sumSuccessAmount(@Param("customerNo") String customerNo, @Param("begin") LocalDateTime begin);
}
