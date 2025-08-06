package com.bankservice.bank_service.repository;

import com.bankservice.bank_service.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    @Query(value = "SELECT * FROM transaction WHERE from_account_id = :accountId OR to_account_id = :accountId ORDER BY timestamp DESC", nativeQuery = true)
    List<Transaction> findByAccountId(@Param("accountId") Long accountId);
}
