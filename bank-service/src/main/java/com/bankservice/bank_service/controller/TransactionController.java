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

    // New: accept a transfer DTO (fromAccountId/toAccountId) and perform balance updates
    @PostMapping("/transfer")
    public org.springframework.http.ResponseEntity<?> transfer(@RequestBody com.bankservice.bank_service.dto.TransactionDTO dto) {
        try {
            transactionService.transfer(dto);
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of("status","ok"));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(400).body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public Transaction getById(@PathVariable Long id) {
        return transactionService.getById(id);
    }
}
