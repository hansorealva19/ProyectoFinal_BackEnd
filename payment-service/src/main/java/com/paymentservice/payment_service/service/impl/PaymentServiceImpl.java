
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
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    @org.springframework.beans.factory.annotation.Value("${payment.merchant.accountId:}")
    private String merchantAccountId;

    @Override
    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackProcessPayment")
    public PaymentResponseDTO processPayment(PaymentRequestDTO requestDTO) {
        logger.info("Processing payment: {}", requestDTO);
        String payer = requestDTO.getPayerAccount() != null ? requestDTO.getPayerAccount() : "";
        String payee = requestDTO.getPayeeAccount();
        if ((payee == null || payee.isBlank()) && merchantAccountId != null && !merchantAccountId.isBlank()) {
            payee = merchantAccountId;
        }
        Payment payment = Payment.builder()
                .payerAccount(payer)
                .payeeAccount(payee != null ? payee : "")
                .amount(requestDTO.getAmount())
                .currency(requestDTO.getCurrency())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .description(requestDTO.getDescription())
                .build();
    payment = paymentRepository.save(payment);
    final Payment publishedPayment = payment;
        // Publish event only after the surrounding transaction commits to avoid inconsistency
        try {
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                    try {
                                        paymentEventPublisher.publishPaymentEvent(publishedPayment);
                                    } catch (Exception e) {
                                        logger.warn("Failed to publish payment event after commit: {}", e.getMessage());
                                    }
                            }
                        }
                );
            } else {
                // no transaction active, publish immediately
                paymentEventPublisher.publishPaymentEvent(payment);
            }
        } catch (Exception ex) {
            logger.warn("Error registering transaction synchronization for payment event: {}", ex.getMessage());
            // fallback: try to publish now
            try { paymentEventPublisher.publishPaymentEvent(payment); } catch (Exception e) { logger.warn("Fallback publish failed: {}", e.getMessage()); }
        }
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
