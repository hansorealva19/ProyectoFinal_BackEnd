package com.ecommerce.frontend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/products")
public class ProductRestController {
    @Value("${product.service.url:http://localhost:8083}")
    private String productServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping
    public ResponseEntity<?> getProducts(@RequestParam(required = false) String name,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(defaultValue = "1") int page,
                                         jakarta.servlet.http.HttpSession session) {
        // Llama al microservicio product-service y reenvia el JWT de la sesión si existe
        String url = productServiceUrl + "/api/products?name=" + (name != null ? name : "") + "&category=" + (category != null ? category : "") + "&page=" + page;
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        Object token = session != null ? session.getAttribute("JWT") : null;
        if (token instanceof String) headers.setBearerAuth((String) token);
        org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
        return restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, Object.class);
    }
}
