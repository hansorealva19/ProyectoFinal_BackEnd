package com.bankservice.bank_service.controller;

import com.bankservice.bank_service.dto.AuthRequest;
import com.bankservice.bank_service.dto.AuthResponse;
import com.bankservice.bank_service.dto.RegisterRequest;
import com.bankservice.bank_service.entity.User;
import com.bankservice.bank_service.security.JwtUtil;
import com.bankservice.bank_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid AuthRequest request) {
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        String token = jwtUtil.generateToken(authentication.getName());
        return new AuthResponse(token);
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterRequest request) {
        User createdUser = authService.register(request);
        return ResponseEntity.ok(createdUser);
    }

}

