package com.bank.gold.repository;

import com.bank.gold.entity.GoldAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoldAccountRepository extends JpaRepository<GoldAccount, Long> {

    Optional<GoldAccount> findByAccountNo(String accountNo);

    /**
     * 悲观锁读取账户，保证扣款/退款期间余额不被并发覆盖。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from GoldAccount a where a.accountNo = :accountNo")
    Optional<GoldAccount> lockByAccountNo(@Param("accountNo") String accountNo);
}
