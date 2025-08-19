package com.paymentservice.payment_service.repository;

import com.paymentservice.payment_service.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface PaymentRepository extends JpaRepository<Payment, Long> {
	// find most recent payment whose description mentions the order (e.g. "Order 27")
	java.util.Optional<Payment> findTopByDescriptionContainingOrderByCreatedAtDesc(String descriptionFragment);
}
