package order.service.impl;

import order.domain.Order;
import order.domain.OrderItem;
import order.dto.CreateOrderRequest;
import order.dto.OrderDTO;
import order.dto.OrderItemDTO;
import order.repository.OrderRepository;
import order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;
import order.domain.OrderStatus;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import order.repository.StockUpdateFailureRepository;
import order.domain.StockUpdateFailure;
import order.repository.PaymentNotificationRecordRepository;
import order.domain.PaymentNotificationRecord;
import java.security.MessageDigest;

@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	private OrderRepository orderRepository;

	@Value("${microservices.cart-service.url:http://localhost:8091}")
	private String cartServiceUrl;

	@Value("${microservices.product-service.url:http://localhost:8083}")
	private String productServiceUrl;

	private final RestTemplate rest;

	// initialize RestTemplate with Apache HttpClient to support PATCH
	public OrderServiceImpl() {
		CloseableHttpClient httpClient = HttpClients.custom().build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		this.rest = new RestTemplate(requestFactory);
	}

	@Override
	public Page<OrderDTO> getOrdersByUser(String username, Pageable pageable) {
		Page<Order> page = orderRepository.findByUserName(username, pageable);
		List<OrderDTO> dtos = page.stream().map(this::toDTO).collect(Collectors.toList());
		return new PageImpl<>(dtos, pageable, page.getTotalElements());
	}

	@Override
	public OrderDTO createOrder(CreateOrderRequest req) {
	Order order = Order.builder()
		.userId(req.getUserId())
		.userName(req.getUserName())
		.notes(req.getNotes())
		.build();

		if (req.getItems() != null) {
			for (OrderItemDTO it : req.getItems()) {
		OrderItem oi = OrderItem.builder()
			.productId(it.getProductId())
			.productName(it.getProductName())
			.quantity(it.getQuantity())
			.unitPrice(it.getPrice() != null ? it.getPrice() : BigDecimal.ZERO)
			.build();
				order.addItem(oi);
			}
		}

		order.recalculateTotal();
		Order saved = orderRepository.save(order);
		return toDTO(saved);
	}

	@Override
	public OrderDTO getOrderById(Long id) {
		Optional<Order> o = orderRepository.findById(id);
		return o.map(this::toDTO).orElse(null);
	}


	@Override
	@Transactional
	public void handlePaymentNotification(order.dto.PaymentNotification note, String signature) {
		if (note == null || note.getOrderId() == null) return;

	// basic payload hash to help deduplication when paymentId is missing
	String payload = canonicalJson(note);
		String payloadHash = sha256Hex(payload);

		// check for duplicate by paymentId first
		try {
			if (note.getPaymentId() != null) {
				var existing = paymentNotificationRecordRepository.findByPaymentId(note.getPaymentId());
				if (existing.isPresent()) {
					org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).info("Duplicate payment notification ignored for paymentId={}", note.getPaymentId());
					return;
				}
			}
		} catch (Exception e) {
			org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).warn("Could not check existing notifications: {}", e.getMessage());
		}

		// validate signature if present
		if (signature != null && !signature.isBlank()) {
			try {
				// compute expected signature here to help debugging
				javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
				mac.init(new javax.crypto.spec.SecretKeySpec(webhookSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
				byte[] raw = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
				String expected = java.util.Base64.getEncoder().encodeToString(raw);
				org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).debug("[Signature DEBUG] payload='{}'", payload);
				org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).debug("[Signature DEBUG] expectedSignature='{}'", expected);
				org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).debug("[Signature DEBUG] receivedSignature='{}'", signature);
				if (!MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8), signature.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
					throw new SecurityException("Invalid signature");
				}
			} catch (Exception se) {
				org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).error("Signature validation failed: {}", se.getMessage());
				throw new SecurityException("Signature validation failed");
			}
		} else {
			org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).warn("No signature provided for payment notification orderId={}", note.getOrderId());
		}

		Optional<Order> found = orderRepository.findById(note.getOrderId());
		if (found.isEmpty()) {
			throw new IllegalArgumentException("Order not found: " + note.getOrderId());
		}
		Order order = found.get();
		// if already final, skip
		if (order.getStatus() != null && order.getStatus().isFinal()) return;
		// Only mark order as CONFIRMED when payment notification explicitly indicates success.
		String nstatus = note.getStatus();
		if (nstatus == null) {
			org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).warn("Payment notification for order {} contains no status; skipping confirmation", note.getOrderId());
		} else if ("SUCCESS".equalsIgnoreCase(nstatus)) {
			order.setStatus(OrderStatus.CONFIRMED);
			orderRepository.save(order);
		} else if ("FAILED".equalsIgnoreCase(nstatus)) {
			// mark as cancelled for failed payments to avoid leaving orders in limbo
			order.setStatus(OrderStatus.CANCELLED);
			orderRepository.save(order);
		} else {
			org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).info("Payment notification for order {} has non-terminal status='{}' - leaving order in current state", note.getOrderId(), nstatus);
		}

		// persist notification record to avoid reprocessing
		try {
			PaymentNotificationRecord rec = new PaymentNotificationRecord(note.getOrderId(), note.getPaymentId(), payloadHash);
			paymentNotificationRecordRepository.save(rec);
		} catch (Exception ex) {
			org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).warn("Could not persist payment notification record: {}", ex.getMessage());
		}

		// try clearing cart for this user (best-effort) — add logging, single retry, and fallback to parse notes
		try {
			Long uid = order.getUserId();
			if (uid == null) {
				// try to parse user id from notes left by cart-service: "Checkout from cart-service user:{userId}"
				String notes = order.getNotes();
				if (notes != null) {
					try {
						java.util.regex.Matcher m = java.util.regex.Pattern.compile("user:?\\s*(\\d+)").matcher(notes);
						if (m.find()) {
							uid = Long.parseLong(m.group(1));
							org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).info("Parsed userId {} from order.notes for order {}", uid, order.getId());
						}
					} catch (Exception px) {
						org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).warn("Failed to parse userId from notes for order {}: {}", order.getId(), px.getMessage());
					}
				}
			}
			if (uid != null) {
				String clearUrl = cartServiceUrl + "/api/cart/" + uid + "/clear";
				org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).info("Attempting to clear cart for user {} via {}", uid, clearUrl);
				try {
					rest.delete(clearUrl);
				} catch (Exception e1) {
					org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).warn("First attempt to clear cart failed for user {}: {} - retrying once", uid, e1.getMessage());
					try {
						Thread.sleep(200);
						rest.delete(clearUrl);
					} catch (Exception e2) {
						org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).warn("Retry to clear cart failed for user {}: {}", uid, e2.getMessage());
					}
				}
			} else {
				org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).warn("Order {} has no userId and notes did not contain user info; skipping cart clear", order.getId());
			}
		} catch (Exception ex) {
			org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).warn("Could not clear cart for order {} (userId maybe null): {}", order.getId(), ex.getMessage());
		}
		// schedule delivery simulation after 20 seconds
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.schedule(() -> {
			try {
				Optional<Order> o2 = orderRepository.findById(order.getId());
				if (o2.isPresent()) {
					Order od = o2.get();
					od.setStatus(OrderStatus.DELIVERED);
					orderRepository.save(od);
				}
			} catch (Exception e) {
				// log and ignore
				org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).error("Failed to mark delivered: {}", e.getMessage(), e);
			}
		}, 20, TimeUnit.SECONDS);

		// If the order was confirmed, decrement stock in product-service for each item (best-effort)
		if (order.getStatus() == OrderStatus.CONFIRMED) {
			try {
				for (OrderItem it : order.getItems()) {
					int qty = it.getQuantity();
					// product-service expects positive quantities to increase stock; send negative to decrement
					int delta = -Math.abs(qty);
					String patchUrl = productServiceUrl + "/api/products/" + it.getProductId() + "/stock?quantity=" + delta;
					boolean success = false;
					int attempts = 0;
					int[] delays = new int[] {500, 1000, 2000}; // ms
					while (attempts < delays.length && !success) {
						try {
							rest.patchForObject(patchUrl, null, Void.class);
							success = true;
							org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).info("Requested stock update for product {} by {} (delta={})", it.getProductId(), qty, delta);
						} catch (Exception ex) {
							org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).warn("Attempt {} to update stock for product {} failed: {}", attempts + 1, it.getProductId(), ex.getMessage());
							try {
								Thread.sleep(delays[attempts]);
							} catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
						}
						attempts++;
					}
					if (!success) {
						// persist failure for later reconciliation
						try {
							StockUpdateFailure failure = new StockUpdateFailure(order.getId(), it.getProductId(), qty, "Failed to update product-service after retries");
							stockUpdateFailureRepository.save(failure);
							org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).warn("Recorded stock update failure for order {} product {} qty {}", order.getId(), it.getProductId(), qty);
						} catch (Exception rex) {
							org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).error("Failed to persist stock update failure: {}", rex.getMessage());
						}
					}
				}
			} catch (Exception ex) {
				org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).warn("Error while updating product stock after order confirmation: {}", ex.getMessage());
			}
		}
	}

	@Override
	@Transactional
	public void handlePaymentNotificationRaw(String rawJson, String signature) {
		if (rawJson == null || rawJson.isBlank()) return;
		// validate signature strictly over exact bytes received
		if (signature != null && !signature.isBlank()) {
			try {
				javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
				mac.init(new javax.crypto.spec.SecretKeySpec(webhookSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
				byte[] raw = mac.doFinal(rawJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
				String expected = java.util.Base64.getEncoder().encodeToString(raw);
				org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).debug("[Signature RAW] expected='{}' received='{}'", expected, signature);
				if (!MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8), signature.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
					throw new SecurityException("Invalid signature");
				}
			} catch (Exception se) {
				org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).error("Signature validation failed: {}", se.getMessage());
				throw new SecurityException("Signature validation failed");
			}
		} else {
			org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).warn("No signature provided for raw payment notification");
		}
		// parse the JSON into the DTO and reuse logic
		try {
			com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
			order.dto.PaymentNotification note = om.readValue(rawJson, order.dto.PaymentNotification.class);
			// delegate to existing method which performs idempotency and state update
			this.handlePaymentNotification(note, signature);
		} catch (Exception e) {
			org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class).error("Failed to parse raw payment notification: {}", e.getMessage(), e);
			throw new IllegalArgumentException("Invalid payment notification payload");
		}
	}

	private String sha256Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] d = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : d) sb.append(String.format("%02x", b));
			return sb.toString();
		} catch (Exception e) {
			return Integer.toHexString(input.hashCode());
		}
	}

	@Autowired
	private PaymentNotificationRecordRepository paymentNotificationRecordRepository;

	@Autowired
	private StockUpdateFailureRepository stockUpdateFailureRepository;

	@Value("${order.webhook.secret:change-me-to-a-strong-secret}")
	private String webhookSecret;
    


	// validateSignature removed (logic inlined where needed)

	// produce a stable JSON representation for signing
	@SuppressWarnings("unchecked")
	private String canonicalJson(order.dto.PaymentNotification note) {
		try {
			com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
			// ensure maps are ordered by key and POJO properties are sorted consistently
			om.configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
			om.configure(com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
			om.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
			// convert to a Map first so representation matches the payment simulator which serializes maps
			java.util.Map<String,Object> m = om.convertValue(note, java.util.Map.class);
			return om.writeValueAsString(m);
		} catch (Exception e) {
			return note.toString();
		}
	}

	private OrderDTO toDTO(Order o) {
		OrderDTO dto = OrderDTO.builder()
				.id(o.getId())
				.userId(o.getUserId())
				.userName(o.getUserName())
				.status(o.getStatus())
				.totalAmount(o.getTotalAmount() != null ? o.getTotalAmount().toString() : "0")
				.itemsCount(o.getItemsCount())
				.notes(o.getNotes())
				.createdAt(o.getCreatedAt() != null ? o.getCreatedAt().toString() : null)
				.updatedAt(o.getUpdatedAt() != null ? o.getUpdatedAt().toString() : null)
				.build();
		return dto;
	}
}
