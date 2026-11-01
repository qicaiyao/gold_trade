package com.bank.gold.repository;

import com.bank.gold.entity.GoldCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoldCustomerRepository extends JpaRepository<GoldCustomer, Long> {

    Optional<GoldCustomer> findByCustomerNo(String customerNo);
}
