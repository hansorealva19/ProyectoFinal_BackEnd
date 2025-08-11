package com.bankservice.bank_service.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CardDTO {
    private Long id;
    private String cardNumber;
    private String cardHolder;
    private String cvv;
    private LocalDate expirationDate;
    private boolean active;
}
