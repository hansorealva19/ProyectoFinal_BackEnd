package com.ecommerce.frontend.service;

import com.ecommerce.frontend.model.LoginRequest;
import com.ecommerce.frontend.model.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class UserGateway {

    private final RestTemplate restTemplate;

    @Value("${microservices.user-service.url}")
    private String userServiceUrl;

    public LoginResponse login(LoginRequest req) {
        String url = userServiceUrl + "/api/users/login"; // tu user-service ya lo tiene

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(req, headers);

        ResponseEntity<LoginResponse> resp =
                restTemplate.exchange(url, HttpMethod.POST, entity, LoginResponse.class);

        return resp.getBody();
    }

    public ResponseEntity<String> register(Object userPayload) {
        String url = userServiceUrl + "/api/users/register";
        return restTemplate.postForEntity(url, userPayload, String.class);
    }
}
