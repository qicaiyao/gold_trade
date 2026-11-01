package com.bank.gold.repository;

import com.bank.gold.entity.GoldTranLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoldTranLogRepository extends JpaRepository<GoldTranLog, Long> {

    Optional<GoldTranLog> findByReqSerialNo(String reqSerialNo);
}
