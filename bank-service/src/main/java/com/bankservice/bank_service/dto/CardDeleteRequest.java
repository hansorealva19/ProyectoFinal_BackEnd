package com.bankservice.bank_service.dto;

import lombok.Data;

@Data
public class CardDeleteRequest {
    private String username;
    private String password;
    private String dni;
}
