package order.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_notifications", indexes = {@Index(name = "idx_payment_id", columnList = "paymentId")})
public class PaymentNotificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = true)
    private String paymentId;

    @Column(nullable = false)
    private String payloadHash;

    @Column(nullable = false)
    private LocalDateTime receivedAt = LocalDateTime.now();

    public PaymentNotificationRecord() {}

    public PaymentNotificationRecord(Long orderId, String paymentId, String payloadHash) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.payloadHash = payloadHash;
        this.receivedAt = LocalDateTime.now();
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
}
