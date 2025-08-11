package com.paymentservice.payment_service.service;

import com.paymentservice.payment_service.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(PaymentEventPublisher.class);
    private final KafkaTemplate<String, Payment> kafkaTemplate;

    public void publishPaymentEvent(Payment payment) {
        logger.info("Publishing payment event to Kafka: {}", payment);
        kafkaTemplate.send("payments", payment);
    }
}
