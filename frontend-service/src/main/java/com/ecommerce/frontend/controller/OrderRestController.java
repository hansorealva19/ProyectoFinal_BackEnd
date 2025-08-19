package com.ecommerce.frontend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/orders")
public class OrderRestController {
    @Value("${order.service.url:http://localhost:8084}")
    private String orderServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping
    public ResponseEntity<?> getOrders(jakarta.servlet.http.HttpSession session) {
        // Llama al microservicio order-service y reenvia JWT si existe
        String url = orderServiceUrl + "/api/orders";
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        Object token = session != null ? session.getAttribute("JWT") : null;
        if (token instanceof String) headers.setBearerAuth((String) token);
        org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
        return restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, Object.class);
    }
}
