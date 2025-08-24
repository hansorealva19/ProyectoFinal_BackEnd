package order.service;

import order.dto.CreateOrderRequest;
import order.dto.OrderDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
	Page<OrderDTO> getOrdersByUser(String username, Pageable pageable);

	OrderDTO createOrder(CreateOrderRequest req);

	// retrieve order by id
	OrderDTO getOrderById(Long id);

	// Handle async payment notification (webhook) from payment-service
	// signature: optional HMAC signature provided in X-Signature header
	void handlePaymentNotification(order.dto.PaymentNotification note, String signature);

	// alternative: receive the raw JSON body and signature to validate HMAC over exact bytes
	void handlePaymentNotificationRaw(String rawJson, String signature);

	// Cancel pending orders for a user (used when user empties cart)
	void cancelPendingOrdersForUser(Long userId, String username);
}
