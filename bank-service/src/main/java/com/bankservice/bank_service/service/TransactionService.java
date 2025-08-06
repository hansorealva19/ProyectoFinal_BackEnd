package com.bankservice.bank_service.service;

import com.bankservice.bank_service.dto.TransactionDTO;
import com.bankservice.bank_service.entity.Transaction;

import java.util.List;

public interface TransactionService {
    void transfer(TransactionDTO transactionDTO);

    List<Transaction> getAll();

    Transaction create(Transaction transaction);

    Transaction getById(Long id);
    
    List<TransactionDTO> getTransactionsByAccountId(Long accountId);
}
