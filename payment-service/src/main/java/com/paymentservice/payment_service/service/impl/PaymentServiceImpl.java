package com.paymentservice.payment_service.service.impl;

import com.paymentservice.payment_service.dto.PaymentRequestDTO;
import com.paymentservice.payment_service.dto.PaymentResponseDTO;
import com.paymentservice.payment_service.entity.Payment;
import com.paymentservice.payment_service.repository.PaymentRepository;
import com.paymentservice.payment_service.service.PaymentService;
import com.paymentservice.payment_service.service.PaymentEventPublisher;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    @Override
    @Transactional
    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackProcessPayment")
    public PaymentResponseDTO processPayment(PaymentRequestDTO requestDTO) {
        logger.info("Processing payment: {}", requestDTO);
        Payment payment = Payment.builder()
                .payerAccount(requestDTO.getPayerAccount())
                .payeeAccount(requestDTO.getPayeeAccount())
                .amount(requestDTO.getAmount())
                .currency(requestDTO.getCurrency())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .description(requestDTO.getDescription())
                .build();
        payment = paymentRepository.save(payment);
        paymentEventPublisher.publishPaymentEvent(payment);
        logger.info("Payment saved and event published: {}", payment);
        return PaymentResponseDTO.builder()
                .id(payment.getId())
                .status(payment.getStatus())
                .message("Payment created successfully")
                .build();
    }

    public PaymentResponseDTO fallbackProcessPayment(PaymentRequestDTO requestDTO, Throwable t) {
        logger.error("Payment service fallback triggered: {}", t.getMessage());
        return PaymentResponseDTO.builder()
                .id(null)
                .status("FAILED")
                .message("Payment service is temporarily unavailable. Please try again later.")
                .build();
    }
}
