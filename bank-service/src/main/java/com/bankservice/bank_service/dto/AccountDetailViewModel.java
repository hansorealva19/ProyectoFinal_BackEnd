package com.bankservice.bank_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class AccountDetailViewModel {
    private AccountDTO account;
    private List<CardDTO> cards;
    private String userFullName;
    private String userEmail;
    private List<?> transactions; // Use TransactionDTO if needed
}
