package com.ecommerce.frontend.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthStatusController {

    @GetMapping("/api/auth/check")
    public ResponseEntity<Void> check(HttpSession session) {
        Object jwt = session != null ? session.getAttribute("JWT") : null;
        if (jwt != null) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(401).build();
    }
}
