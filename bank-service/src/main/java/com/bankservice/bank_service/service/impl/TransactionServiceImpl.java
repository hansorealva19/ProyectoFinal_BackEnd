package com.bankservice.bank_service.service.impl;

import com.bankservice.bank_service.dto.TransactionDTO;
import com.bankservice.bank_service.entity.Account;
import com.bankservice.bank_service.entity.Transaction;
import com.bankservice.bank_service.repository.AccountRepository;
import com.bankservice.bank_service.repository.TransactionRepository;
import com.bankservice.bank_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public void transfer(TransactionDTO dto) {
        Account from = accountRepository.findById(dto.getFromAccountId())
                .orElseThrow(() -> new RuntimeException("Cuenta origen no encontrada"));
        Account to = accountRepository.findById(dto.getToAccountId())
                .orElseThrow(() -> new RuntimeException("Cuenta destino no encontrada"));

        if (from.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new RuntimeException("Saldo insuficiente");
        }

        from.setBalance(from.getBalance().subtract(dto.getAmount()));
        to.setBalance(to.getBalance().add(dto.getAmount()));

        accountRepository.save(from);
        accountRepository.save(to);

        Transaction transaction = Transaction.builder()
                .fromAccount(from)
                .toAccount(to)
                .amount(dto.getAmount())
                .description(dto.getDescription())
                .timestamp(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
    }

    @Override
    public List<Transaction> getAll() {
        return transactionRepository.findAll();
    }

    @Override
    public Transaction create(Transaction transaction) {
        transaction.setTimestamp(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    @Override
    public Transaction getById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transacción no encontrada"));
    }

    @Override
    public List<TransactionDTO> getTransactionsByAccountId(Long accountId) {
        List<Transaction> transactions = transactionRepository.findByAccountId(accountId);
        return transactions.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private TransactionDTO convertToDTO(Transaction transaction) {
        return TransactionDTO.builder()
                .id(transaction.getId())
                .fromAccountId(transaction.getFromAccount().getId())
                .toAccountId(transaction.getToAccount().getId())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .date(transaction.getTimestamp())
                .build();
    }
}
