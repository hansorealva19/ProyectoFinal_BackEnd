package com.ecommerce.cart_service.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.math.BigDecimal;

@Component
public class OrderClient {
    @Value("${microservices.order-service.url:http://localhost:8084}")
    private String orderServiceUrl;

    private final RestTemplate rest = new RestTemplate();

    public org.springframework.http.ResponseEntity<Object> createOrder(CreateOrderRequest req, String jwt) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        if (jwt != null && !jwt.isBlank()) headers.setBearerAuth(jwt);
        org.springframework.http.HttpEntity<CreateOrderRequest> entity = new org.springframework.http.HttpEntity<>(req, headers);
        return rest.postForEntity(orderServiceUrl + "/api/orders", entity, Object.class);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrderRequest {
        private List<OrderItem> items;
        private String notes;
        private Long userId;
        private String userName;
    // optional initial status (e.g. PENDING)
    private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal price;
    }
}
