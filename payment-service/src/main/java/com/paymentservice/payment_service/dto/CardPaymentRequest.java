package com.paymentservice.payment_service.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CardPaymentRequest {
    private String cardNumber;
    private String cardHolder;
    private String cvv;
    private String expirationDate; // yyyy-MM-dd
    private BigDecimal amount;
    private String description; // descripción opcional del pago
}
