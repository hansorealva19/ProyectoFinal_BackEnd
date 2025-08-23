package com.bankservice.bank_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@lombok.Data
@lombok.AllArgsConstructor
public class CardChargeResponse {
    private boolean success;
    private String message;
    // Optional: id/number of the account that was debited by the card charge
    private Long fromAccountId;
    private String fromAccountNumber;
    
    public CardChargeResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
