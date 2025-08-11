package com.bankservice.bank_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CardChargeResponse {
    private boolean success;
    private String message;
}
