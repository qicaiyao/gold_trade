package com.bank.gold.repository;

import com.bank.gold.entity.GoldHolding;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoldHoldingRepository extends JpaRepository<GoldHolding, Long> {

    Optional<GoldHolding> findByCustomerNoAndProductCode(String customerNo, String productCode);

    List<GoldHolding> findByCustomerNoOrderByProductCodeAsc(String customerNo);

    /**
     * 悲观锁读取持仓，保证克重增减的原子性。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from GoldHolding h where h.customerNo = :customerNo and h.productCode = :productCode")
    Optional<GoldHolding> lockOne(@Param("customerNo") String customerNo, @Param("productCode") String productCode);
}
