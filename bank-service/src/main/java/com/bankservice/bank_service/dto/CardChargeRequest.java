package com.bankservice.bank_service.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CardChargeRequest {
    private String cardNumber;
    private String cardHolder;
    private String cvv;
    private String expirationDate; // formato: yyyy-MM-dd
    private BigDecimal amount;
}
