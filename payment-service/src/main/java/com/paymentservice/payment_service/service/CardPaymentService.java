package com.paymentservice.payment_service.service;

import com.paymentservice.payment_service.dto.CardPaymentRequest;
import com.paymentservice.payment_service.dto.CardPaymentResponse;

public interface CardPaymentService {
    CardPaymentResponse processCardPayment(CardPaymentRequest request);
}
