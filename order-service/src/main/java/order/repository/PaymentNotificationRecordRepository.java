package order.repository;

import order.domain.PaymentNotificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentNotificationRecordRepository extends JpaRepository<PaymentNotificationRecord, Long> {
    Optional<PaymentNotificationRecord> findByPaymentId(String paymentId);
}
