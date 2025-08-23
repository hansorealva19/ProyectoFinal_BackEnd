package com.paymentservice.payment_service.dto;

import lombok.Data;

@Data
public class CardPaymentResponse {
    private boolean success;
    private String message;
    // Optional: the bank can return which account was debited when charging a card
    private Long fromAccountId;
    private String fromAccountNumber;
}
