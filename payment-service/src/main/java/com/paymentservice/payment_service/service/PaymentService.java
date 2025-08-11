package com.paymentservice.payment_service.service;

import com.paymentservice.payment_service.dto.PaymentRequestDTO;
import com.paymentservice.payment_service.dto.PaymentResponseDTO;
import com.paymentservice.payment_service.entity.Payment;
import java.util.List;
import java.util.Optional;

public interface PaymentService {
    PaymentResponseDTO processPayment(PaymentRequestDTO requestDTO);
    List<Payment> findAll();
    Optional<Payment> findById(Long id);
}
