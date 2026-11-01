package com.bank.gold.repository;

import com.bank.gold.entity.GoldDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoldDeliveryRepository extends JpaRepository<GoldDelivery, Long> {

    Optional<GoldDelivery> findByDeliveryNo(String deliveryNo);

    Optional<GoldDelivery> findByOrderNo(String orderNo);
}
