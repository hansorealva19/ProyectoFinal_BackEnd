package com.paymentservice.payment_service.controller;

import com.paymentservice.payment_service.dto.CardPaymentRequest;
import com.paymentservice.payment_service.dto.CardPaymentResponse;
import com.paymentservice.payment_service.service.CardPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test-payment")
@RequiredArgsConstructor
public class TestCardPaymentRestController {
    private final CardPaymentService cardPaymentService;

    @PostMapping
    public ResponseEntity<CardPaymentResponse> testCardPayment(@RequestBody CardPaymentRequest request) {
        CardPaymentResponse response = cardPaymentService.processCardPayment(request);
        return ResponseEntity.ok(response);
    }
}
