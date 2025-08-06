package com.bankservice.bank_service.controller;

import com.bankservice.bank_service.entity.Transaction;
import com.bankservice.bank_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public List<Transaction> getAll() {
        return transactionService.getAll();
    }

    @PostMapping
    public Transaction create(@RequestBody Transaction transaction) {
        return transactionService.create(transaction);
    }

    @GetMapping("/{id}")
    public Transaction getById(@PathVariable Long id) {
        return transactionService.getById(id);
    }
}
