package order.controller;

import order.dto.OrderDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableAsync;
import order.service.OrderService;
import order.dto.CreateOrderRequest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/orders")
@EnableAsync
public class OrderController {

	@Autowired
	private OrderService orderService;

	// Endpoint paginado para obtener órdenes de un usuario
	@GetMapping(value = "/user/{username}")
	public ResponseEntity<java.util.Map<String, Object>> getOrdersByUser(
			@PathVariable String username,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page, size);
		// Defensive checks: if username is missing, return empty content instead of throwing
		if (username == null || username.trim().isEmpty() || "null".equalsIgnoreCase(username.trim())) {
			org.slf4j.LoggerFactory.getLogger(OrderController.class).warn("[OrderController] getOrdersByUser called with empty username, returning empty content");
			java.util.Map<String, Object> empty = java.util.Map.of(
					"content", java.util.Collections.emptyList(),
					"totalElements", 0,
					"number", page,
					"size", size
			);
			return ResponseEntity.ok(empty);
		}
		try {
			org.slf4j.LoggerFactory.getLogger(OrderController.class).info("[OrderController] Fetching orders for user={} page={} size={}", username, page, size);
			var pageResult = orderService.getOrdersByUser(username, pageable);
			java.util.Map<String, Object> body = new java.util.HashMap<>();
			body.put("content", pageResult.getContent());
			body.put("totalElements", pageResult.getTotalElements());
			body.put("number", pageResult.getNumber());
			body.put("size", pageResult.getSize());
			body.put("totalPages", pageResult.getTotalPages());
			// optional: include pageable metadata expected by some clients
			body.put("pageable", java.util.Map.of("pageNumber", pageResult.getNumber(), "pageSize", pageResult.getSize()));
			org.slf4j.LoggerFactory.getLogger(OrderController.class).info("[OrderController] Returning {} orders for user={}", pageResult.getNumberOfElements(), username);
			return ResponseEntity.ok(body);
		} catch (Exception ex) {
			org.slf4j.LoggerFactory.getLogger(OrderController.class).error("[OrderController] Error fetching orders for {}: {}", username, ex.getMessage(), ex);
			java.util.Map<String, Object> empty = java.util.Map.of(
					"content", java.util.Collections.emptyList(),
					"totalElements", 0,
					"number", page,
					"size", size
			);
			return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(empty);
		}
	}

	// Endpoint para crear un pedido
	@PostMapping
	public ResponseEntity<OrderDTO> createOrder(@RequestBody CreateOrderRequest req) {
		OrderDTO created = orderService.createOrder(req);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	// Endpoint to fetch an order by id (used by payment-service to read total)
	@GetMapping(value = "/{id}")
	public ResponseEntity<?> getOrderById(@PathVariable("id") Long id) {
		try {
			OrderDTO dto = orderService.getOrderById(id);
			if (dto == null) return ResponseEntity.notFound().build();
			java.util.Map<String,Object> body = new java.util.HashMap<>();
			body.put("id", dto.getId());
			body.put("total", dto.getTotalAmount());
			body.put("status", dto.getStatus() != null ? dto.getStatus().name() : null);
			// include createdAt so payment UI can compute remaining time before auto-cancel
			body.put("createdAt", dto.getCreatedAt());
			return ResponseEntity.ok(body);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of("error", e.getMessage()));
		}
	}

	// Webhook endpoint used by payment-service to notify payment result
	@PostMapping(value = "/payment-notify", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> paymentNotify(@RequestHeader(value = "X-Signature", required = false) String signature,
										   @RequestBody String rawBody) {
		try {
			// debug writes removed
			// log basic summary after parsing minimal fields for convenience
			com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
			java.util.Map<String,Object> m = om.readValue(rawBody, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String,Object>>(){});
			Object orderId = m.get("orderId");
			Object status = m.get("status");
			org.slf4j.LoggerFactory.getLogger(OrderController.class).info("[OrderController] Payment notification received for orderId={} status={} signature={} ", orderId, status, signature);
			orderService.handlePaymentNotificationRaw(rawBody, signature);
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			org.slf4j.LoggerFactory.getLogger(OrderController.class).error("Error handling payment notification: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of("error", e.getMessage()));
		}
	}

	// Cancel pending orders for a user (e.g., when user empties cart)
	@PostMapping(value = "/user/{userId}/cancel-pending")
	public ResponseEntity<?> cancelPendingForUser(@PathVariable("userId") Long userId, @RequestParam(value = "username", required = false) String username) {
		try {
			orderService.cancelPendingOrdersForUser(userId, username);
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			org.slf4j.LoggerFactory.getLogger(OrderController.class).error("Failed to cancel pending orders for user {}: {}", userId, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of("error", e.getMessage()));
		}
	}
}
