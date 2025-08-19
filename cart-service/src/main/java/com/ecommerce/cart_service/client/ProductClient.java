package com.ecommerce.cart_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ProductClient {
    @Value("${microservices.product-service.url:http://localhost:8083}")
    private String productServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public ProductDTO getProductById(Long productId) {
        String url = productServiceUrl + "/api/products/" + productId;
        return restTemplate.getForObject(url, ProductDTO.class);
    }
}
