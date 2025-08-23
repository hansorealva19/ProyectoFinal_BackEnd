package com.bankservice.bank_service.repository;

import com.bankservice.bank_service.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    @Query(value = "SELECT * FROM transaction WHERE from_account_id = :accountId OR to_account_id = :accountId ORDER BY timestamp DESC", nativeQuery = true)
    List<Transaction> findByAccountId(@Param("accountId") Long accountId);

    @Query(value = "SELECT * FROM transaction WHERE from_account_id = :accountId OR to_account_id = :accountId ORDER BY timestamp DESC",
        countQuery = "SELECT count(*) FROM transaction WHERE from_account_id = :accountId OR to_account_id = :accountId",
        nativeQuery = true)
    Page<Transaction> findByAccountId(@Param("accountId") Long accountId, Pageable pageable);
}
