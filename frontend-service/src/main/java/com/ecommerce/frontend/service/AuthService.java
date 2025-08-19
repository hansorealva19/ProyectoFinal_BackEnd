// src/main/java/com/ecommerce/frontend/service/AuthService.java
package com.ecommerce.frontend.service;

import com.ecommerce.frontend.model.LoginRequest;
import com.ecommerce.frontend.model.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthService {

    @Value("${microservices.user-service.url}")
    private String userServiceUrl;

    private final RestTemplate restTemplate;

    public AuthService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public LoginResponse login(String username, String password) {
        String url = userServiceUrl + "/api/users/login";
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        return restTemplate.postForObject(url, req, LoginResponse.class);
    }
}