package com.bankservice.bank_service.dto;

import lombok.Data;

@Data
public class CardRegisterRequest {
    private String cardNumber;
    private String cardHolder;
    private String cvv;
    private String expirationDate; // yyyy-MM-dd
    private String accountNumber;
}
