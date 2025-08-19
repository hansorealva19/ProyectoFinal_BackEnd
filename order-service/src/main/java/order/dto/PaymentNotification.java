package order.dto;

import lombok.Data;

@Data
public class PaymentNotification {
    private Long orderId;
    private String paymentId;
    private String status; // e.g. SUCCESS, FAILED
    private String message;
}
