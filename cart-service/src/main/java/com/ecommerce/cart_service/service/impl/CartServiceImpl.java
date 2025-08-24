package com.ecommerce.cart_service.service.impl;

import com.ecommerce.cart_service.entity.Cart;
import com.ecommerce.cart_service.entity.CartItem;
import com.ecommerce.cart_service.repository.CartRepository;
import com.ecommerce.cart_service.service.CartService;
import com.ecommerce.cart_service.client.ProductClient;
import com.ecommerce.cart_service.client.ProductDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final ProductClient productClient;
    private final com.ecommerce.cart_service.client.OrderClient orderClient;

    @Override
    public Cart createCart(Long userId) {
        Cart cart = Cart.builder()
                .userId(userId)
                .items(new ArrayList<>())
                .total(0)
                .build();
        return cartRepository.save(cart);
    }

    @Override
    public Optional<Cart> getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId);
    }

    @Override
    public Cart addItem(Long userId, CartItem item) {
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> createCart(userId));
        // Consultar datos reales del producto
        ProductDTO product = productClient.getProductById(item.getProductId());
        if (product == null) {
            throw new RuntimeException("Producto no encontrado en product-service");
        }
        item.setProductName(product.getName());
        item.setUnitPrice(product.getPrice());
    item.setProductCategory(product.getCategory());
    // Check stock availability before merging/adding
    int availableStock = product.getStock();
        int requestedQty = item.getQuantity();
        int existingQty = 0;
        if (cart.getItems() != null) {
            for (CartItem existing : cart.getItems()) {
                if (existing.getProductId() != null && existing.getProductId().equals(item.getProductId())) {
                    existingQty = existing.getQuantity();
                    break;
                }
            }
        }
        if (requestedQty + existingQty > availableStock) {
            throw new RuntimeException("Requested quantity exceeds available stock. Available=" + availableStock + " requested=" + (requestedQty + existingQty));
        }

        // If the cart already contains the same productId, merge quantities instead of adding a duplicate entry
        boolean merged = false;
        if (cart.getItems() != null) {
            for (CartItem existing : cart.getItems()) {
                if (existing.getProductId() != null && existing.getProductId().equals(item.getProductId())) {
                    int newQty = existing.getQuantity() + item.getQuantity();
                    existing.setQuantity(newQty);
                    existing.setUnitPrice(product.getPrice());
                    existing.setSubtotal(product.getPrice() * newQty);
                    merged = true;
                    break;
                }
            }
        }
        if (!merged) {
            item.setSubtotal(product.getPrice() * item.getQuantity());
            cart.getItems().add(item);
        }
        cart.setTotal(cart.getItems().stream().mapToDouble(CartItem::getSubtotal).sum());
        return cartRepository.save(cart);
    }

    @Override
    public Cart removeItem(Long userId, Long itemId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow();
        cart.getItems().removeIf(i -> i.getId().equals(itemId));
        cart.setTotal(cart.getItems().stream().mapToDouble(CartItem::getSubtotal).sum());
        return cartRepository.save(cart);
    }

    @Override
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow();
        cart.getItems().clear();
        cart.setTotal(0);
        cartRepository.save(cart);
    }

    @Override
    public String checkout(Long userId, String username, String jwt) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Cart not found"));
        // don't allow checkout when cart is empty
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot checkout an empty cart");
        }
        // Build order request
        com.ecommerce.cart_service.client.OrderClient.CreateOrderRequest req = new com.ecommerce.cart_service.client.OrderClient.CreateOrderRequest();
        java.util.List<com.ecommerce.cart_service.client.OrderClient.OrderItem> items = new java.util.ArrayList<>();
        for (CartItem ci : cart.getItems()) {
            com.ecommerce.cart_service.client.OrderClient.OrderItem oi = new com.ecommerce.cart_service.client.OrderClient.OrderItem();
            oi.setProductId(ci.getProductId());
            oi.setProductName(ci.getProductName());
            oi.setQuantity(ci.getQuantity());
            oi.setPrice(java.math.BigDecimal.valueOf(ci.getUnitPrice()));
            items.add(oi);
        }
        req.setItems(items);
        req.setNotes("Checkout from cart-service user:" + userId);
    req.setUserId(userId);
    // set userName from provided username when available
    req.setUserName(username != null && !username.isBlank() ? username : (cart.getUserId() != null ? String.valueOf(cart.getUserId()) : null));

        // Call order-service to create a PENDING order and obtain order id
        // The order-service will create the order with default status PENDING
        var resp = orderClient.createOrder(req, jwt);
        Object body = resp.getBody();
        Long createdOrderId = null;
        try {
            if (body instanceof java.util.Map) {
                Object idv = ((java.util.Map) body).get("id");
                if (idv instanceof Number) createdOrderId = ((Number) idv).longValue();
                else if (idv instanceof String) createdOrderId = Long.parseLong((String) idv);
            }
        } catch (Exception e) {
            // ignore parsing errors
        }

        // Build payment URL (simulate external gateway) - payment-service will notify order-service via webhook
        String paymentServiceUrl = System.getProperty("payment.service.url", "http://localhost:8082");

        // Option A: create a Payment record first by calling payment-service /api/payments
        String paymentId = null;
        try {
            org.springframework.web.client.RestTemplate rest = new org.springframework.web.client.RestTemplate();
            java.util.Map<String, Object> payReq = new java.util.HashMap<>();
            // avoid sending null which causes SQL not-null violations in payment-service
            payReq.put("payerAccount", "");
            payReq.put("payeeAccount", "");
            payReq.put("amount", cart.getTotal());
            payReq.put("currency", "PEN");
            payReq.put("description", "Order " + (createdOrderId != null ? createdOrderId : ""));
            org.springframework.http.ResponseEntity<java.util.Map> payResp = rest.postForEntity(paymentServiceUrl + "/api/payments", payReq, java.util.Map.class);
            if (payResp != null && payResp.getStatusCode().is2xxSuccessful() && payResp.getBody() != null) {
                Object idv = payResp.getBody().get("id");
                if (idv != null) paymentId = String.valueOf(idv);
            }
        } catch (Exception ex) {
            // log and continue — we still redirect to simulator even if payment record creation fails
            org.slf4j.LoggerFactory.getLogger(CartServiceImpl.class).warn("Failed to create payment record before redirect: {}", ex.getMessage());
        }

        // include paymentId in the simulator URL so the simulator notifies order-service with the same id
        String paymentUrl = paymentServiceUrl + "/api/payments/sim/confirm?orderId=" + (createdOrderId != null ? createdOrderId : "");
        if (paymentId != null) paymentUrl += "&paymentId=" + java.net.URLEncoder.encode(paymentId, java.nio.charset.StandardCharsets.UTF_8);

    // Don't clear the cart here; cart should be cleared after payment confirmation
    return paymentUrl;
    }

    @Override
    public org.springframework.data.domain.Page<CartItem> getItems(Long userId, org.springframework.data.domain.Pageable pageable) {
        List<CartItem> items = cartRepository.findByUserId(userId)
                .map(Cart::getItems)
                .orElse(new ArrayList<>());
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), items.size());
        List<CartItem> pagedItems = items.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pagedItems, pageable, items.size());
    }

    @Override
    public int getCount(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(c -> c.getItems() != null ? c.getItems().size() : 0)
                .orElse(0);
    }
}
