package com.paymentservice.payment_service.dto;

import lombok.Data;

@Data
public class CardPaymentResponse {
    private boolean success;
    private String message;
}
