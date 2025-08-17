package com.paymentservice.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String payerAccount;

    @Column(nullable = false)
    private String payeeAccount;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private String description;

    // For Thymeleaf compatibility: expose 'date' as alias for createdAt
    public LocalDateTime getDate() {
        return createdAt;
    }

    // For Thymeleaf compatibility: expose 'sourceAccount' and 'destinationAccount'
    public String getSourceAccount() {
        return payerAccount;
    }

    public String getDestinationAccount() {
        return payeeAccount;
    }
}
