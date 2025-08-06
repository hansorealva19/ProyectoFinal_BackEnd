package com.bankservice.bank_service.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AccountDTO {
    private Long id;
    private String accountNumber;
    private BigDecimal balance;
    private Long userId;
    private String bankCode;
    private String bankName;
    
    // Información del titular de la cuenta
    private String ownerName; // Nombre completo del propietario
    private String ownerEmail; // Email del titular
    private String ownerPhone; // Teléfono del titular
    private String ownerDocumentNumber; // Documento de identidad del titular
    private String ownerDocumentType; // Tipo de documento del titular
}
