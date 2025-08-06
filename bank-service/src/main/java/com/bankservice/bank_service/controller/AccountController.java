package com.bankservice.bank_service.controller;

import com.bankservice.bank_service.dto.AccountDTO;
import com.bankservice.bank_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public List<AccountDTO> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    @PostMapping
    public AccountDTO createAccount(@RequestBody AccountDTO accountDTO) {
        return accountService.createAccount(accountDTO);
    }

    @GetMapping("/{id}")
    public AccountDTO getById(@PathVariable Long id) {
        return accountService.getById(id);
    }

    @GetMapping("/user/{userId}")
    public List<AccountDTO> getAccountsByUserId(@PathVariable Long userId) {
        return accountService.getAccountsByUserId(userId);
    }
}
