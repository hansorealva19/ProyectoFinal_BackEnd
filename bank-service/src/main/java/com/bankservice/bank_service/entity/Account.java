package com.bankservice.bank_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;
    
    @Column(nullable = false)
    @Builder.Default
    private String bankCode = "MIBANK"; // Código del banco por defecto
    
    @Column(nullable = false)
    @Builder.Default
    private String bankName = "Mi Banco"; // Nombre del banco por defecto

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
