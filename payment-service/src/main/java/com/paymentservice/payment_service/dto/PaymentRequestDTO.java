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
    private String description;
}
