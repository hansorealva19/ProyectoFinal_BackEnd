package com.paymentservice.payment_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDTO {
    private String payerAccount;
    private String payeeAccount;
    private Double amount;
    private String currency;
    // Optional key supplied by the client to make the request idempotent
    private String idempotencyKey;
    private String description;
    // optional card number when paying by card
    private String cardNumber;
}
