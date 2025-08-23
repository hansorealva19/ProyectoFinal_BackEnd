package com.paymentservice.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payments_idempotency_key", columnList = "idempotency_key")
})
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
    
    @Column(nullable = true)
    private String payerCard;

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

    @Column(name = "idempotency_key", nullable = true, unique = false)
    private String idempotencyKey;

    // For Thymeleaf compatibility: expose 'date' as alias for createdAt
    public LocalDateTime getDate() {
        return createdAt;
    }

    // For Thymeleaf compatibility: expose 'sourceAccount' and 'destinationAccount'
    public String getSourceAccount() {
        return payerAccount;
    }

    // expose card for templates
    public String getCard() {
        return payerCard;
    }

    public String getDestinationAccount() {
        return payeeAccount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    // Ensure payerCard never stores a full PAN: mask it as a last-resort guard before persisting
    @jakarta.persistence.PrePersist
    @jakarta.persistence.PreUpdate
    private void ensurePayerCardMasked() {
        if (this.payerCard == null) return;
        String pc = this.payerCard.trim();
        if (pc.isEmpty()) { this.payerCard = null; return; }
        // If it already looks masked (contains '*'), assume safe
        if (pc.indexOf('*') >= 0) return;
        // Keep only digits then mask
        String digits = pc.replaceAll("\\D", "");
        if (digits.isEmpty()) { this.payerCard = null; return; }
        if (digits.length() <= 4) {
            // too short to meaningfully mask, hide entirely
            this.payerCard = "****";
            return;
        }
        String start = digits.substring(0, Math.min(4, digits.length()));
        String end = digits.substring(Math.max(0, digits.length() - 4));
        this.payerCard = start + " **** **** " + end;
    }
}
