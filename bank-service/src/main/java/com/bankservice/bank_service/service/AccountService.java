package com.bankservice.bank_service.service;

import com.bankservice.bank_service.dto.AccountDTO;

import java.util.List;

public interface AccountService {
    AccountDTO createAccount(AccountDTO accountDTO);
    List<AccountDTO> getAccountsByUserId(Long userId);
    List<AccountDTO> getAllAccounts();
    AccountDTO getById(Long id);
}
