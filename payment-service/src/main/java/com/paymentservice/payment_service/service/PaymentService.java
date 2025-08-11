package com.paymentservice.payment_service.service;

import com.paymentservice.payment_service.dto.PaymentRequestDTO;
import com.paymentservice.payment_service.dto.PaymentResponseDTO;

public interface PaymentService {
    PaymentResponseDTO processPayment(PaymentRequestDTO requestDTO);
}
