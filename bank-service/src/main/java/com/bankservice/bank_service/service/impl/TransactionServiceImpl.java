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

        // Si toAccountId es null, es una transferencia interbancaria
        if (dto.getToAccountId() == null) {
            // Transferencia interbancaria - solo débito
            if (from.getBalance().compareTo(dto.getAmount()) < 0) {
                throw new RuntimeException("Saldo insuficiente");
            }

            from.setBalance(from.getBalance().subtract(dto.getAmount()));
            accountRepository.save(from);

            Transaction transaction = Transaction.builder()
                    .fromAccount(from)
                    .toAccount(null) // No hay cuenta destino para transferencia interbancaria
                    .amount(dto.getAmount())
                    .description(dto.getDescription())
                    .timestamp(LocalDateTime.now())
                    .build();

            transactionRepository.save(transaction);
        } else {
            // Transferencia normal entre cuentas del mismo banco
            Account to = null;
            try {
                to = accountRepository.findById(dto.getToAccountId())
                        .orElse(null);
            } catch (Exception ex) {
                to = null;
            }
            // Fallback: if not found by id, try find by accountNumber (caller might have sent accountNumber digits)
            if (to == null) {
                try {
                    String possibleNumber = String.valueOf(dto.getToAccountId());
                    to = accountRepository.findByAccountNumber(possibleNumber).orElse(null);
                } catch (Exception ex) {
                    to = null;
                }
            }
            if (to == null) {
                throw new RuntimeException("Cuenta destino no encontrada");
            }

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
        System.out.println("DEBUG: Found " + transactions.size() + " transactions for account " + accountId);
        return transactions.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private TransactionDTO convertToDTO(Transaction transaction) {
        System.out.println("DEBUG: Converting transaction " + transaction.getId() + 
                          " - fromAccount: " + (transaction.getFromAccount() != null ? transaction.getFromAccount().getId() : "null") +
                          " - toAccount: " + (transaction.getToAccount() != null ? transaction.getToAccount().getId() : "null"));
        
        return TransactionDTO.builder()
                .id(transaction.getId())
                .fromAccountId(transaction.getFromAccount().getId())
                .toAccountId(transaction.getToAccount() != null ? transaction.getToAccount().getId() : null)
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .date(transaction.getTimestamp())
                .build();
    }
}
