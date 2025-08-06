package com.bankservice.bank_service.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class RegisterRequest {
    // Credenciales de acceso
    private String username;
    private String password;
    
    // Información personal
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    
    // Documentos de identidad
    private String documentNumber;
    private String documentType;
    private LocalDate birthDate;
    
    // Dirección
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    
    // Información laboral
    private String occupation;
    private String monthlyIncome;
}
