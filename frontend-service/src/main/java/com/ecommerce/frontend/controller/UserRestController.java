package com.ecommerce.frontend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/users")
public class UserRestController {
    @Value("${user.service.url:http://localhost:8085}")
    private String userServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Object user) {
        String url = userServiceUrl + "/api/users/register";
        return restTemplate.postForEntity(url, user, Object.class);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password) {
        String url = userServiceUrl + "/api/users/login?username=" + username + "&password=" + password;
        return restTemplate.postForEntity(url, null, Object.class);
    }
}
