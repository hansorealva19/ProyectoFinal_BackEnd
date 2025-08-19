package com.paymentservice.payment_service.service;

import com.paymentservice.payment_service.entity.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(PaymentEventPublisher.class);

    @Autowired(required = false)
    private KafkaTemplate<String, Payment> kafkaTemplate;

    public void publishPaymentEvent(Payment payment) {
        logger.info("Publishing payment event to Kafka: {}", payment);
        if (kafkaTemplate == null) {
            logger.debug("KafkaTemplate bean not present, skipping publish (Kafka not configured)");
            return;
        }
        try {
            kafkaTemplate.send("payments", payment);
        } catch (Exception e) {
            logger.warn("Failed to publish payment event to Kafka (broker may be down): {}", e.getMessage());
        }
    }
}
