package com.bank.gold.repository;

import com.bank.gold.entity.GoldProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoldProductRepository extends JpaRepository<GoldProduct, Long> {

    Optional<GoldProduct> findByProductCode(String productCode);

    List<GoldProduct> findByStatusOrderByPricePerGramAsc(String status);

    List<GoldProduct> findByStatusAndProductCodeOrderByPricePerGramAsc(String status, String productCode);
}
