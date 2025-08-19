package com.bankservice.bank_service.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchants", indexes = {@Index(columnList = "merchantCode", name = "idx_merchant_code")})
public class Merchant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String merchantCode;

    @Column(nullable = false)
    private Long accountId;

    private String displayName;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Merchant() {}

    public Merchant(String merchantCode, Long accountId, String displayName) {
        this.merchantCode = merchantCode;
        this.accountId = accountId;
        this.displayName = displayName;
    }

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMerchantCode() { return merchantCode; }
    public void setMerchantCode(String merchantCode) { this.merchantCode = merchantCode; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
