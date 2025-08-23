package com.bankservice.bank_service.service;

import com.bankservice.bank_service.dto.TransactionDTO;
import com.bankservice.bank_service.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionService {
    void transfer(TransactionDTO transactionDTO);

    List<Transaction> getAll();

    Transaction create(Transaction transaction);

    Transaction getById(Long id);
    
    List<TransactionDTO> getTransactionsByAccountId(Long accountId);

    Page<TransactionDTO> getTransactionsByAccountId(Long accountId, Pageable pageable);

    // Deposit money into an account (external deposit -> credit to account)
    void deposit(Long toAccountId, BigDecimal amount, String description);
}
