package com.bankservice.bank_service.service.impl;

import com.bankservice.bank_service.dto.AccountDTO;
import com.bankservice.bank_service.entity.Account;
import com.bankservice.bank_service.entity.User;
import com.bankservice.bank_service.repository.AccountRepository;
import com.bankservice.bank_service.repository.UserRepository;
import com.bankservice.bank_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Override
    public AccountDTO createAccount(AccountDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Generar número de cuenta automáticamente
        String accountNumber = generateAccountNumber();
        
        // Verificar que el número de cuenta no exista
        while (accountRepository.findByAccountNumber(accountNumber).isPresent()) {
            accountNumber = generateAccountNumber();
        }

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .balance(dto.getBalance() != null ? dto.getBalance() : BigDecimal.ZERO)
                .user(user)
                .build();

        Account saved = accountRepository.save(account);
        return convertToDTO(saved);
    }

    @Override
    public List<AccountDTO> getAccountsByUserId(Long userId) {
        return accountRepository.findAll().stream()
                .filter(acc -> acc.getUser().getId().equals(userId))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountDTO> getAllAccounts() {
        return accountRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AccountDTO getById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));
        return convertToDTO(account);
    }

    private AccountDTO convertToDTO(Account account) {
        AccountDTO dto = new AccountDTO();
        dto.setId(account.getId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setBalance(account.getBalance());
        dto.setUserId(account.getUser().getId());
        return dto;
    }

    private String generateAccountNumber() {
        // Generar número de cuenta de 10 dígitos: formato XXXXXXXXXX
        Random random = new Random();
        StringBuilder accountNumber = new StringBuilder();
        
        // Primer dígito entre 1-9 (no puede empezar con 0)
        accountNumber.append(random.nextInt(9) + 1);
        
        // Siguientes 9 dígitos entre 0-9
        for (int i = 0; i < 9; i++) {
            accountNumber.append(random.nextInt(10));
        }
        
        return accountNumber.toString();
    }
}
