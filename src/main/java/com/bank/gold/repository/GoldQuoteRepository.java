package com.bank.gold.repository;

import com.bank.gold.entity.GoldQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoldQuoteRepository extends JpaRepository<GoldQuote, Long> {

    Optional<GoldQuote> findByQuoteNo(String quoteNo);
}
