package com.paymentservice.payment_service.controller;

import com.paymentservice.payment_service.dto.CardPaymentRequest;
import com.paymentservice.payment_service.dto.CardPaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class PaymentTestController {

    private final com.paymentservice.payment_service.service.CardPaymentService cardPaymentService;

    /**
     * Endpoint de prueba para simular una compra con tarjeta.
     * Puedes probarlo desde Postman enviando un JSON con los datos de la tarjeta.
     */
    @PostMapping("/card-purchase")
    public ResponseEntity<CardPaymentResponse> testCardPurchase(@RequestBody CardPaymentRequest request) {
        CardPaymentResponse response = cardPaymentService.processCardPayment(request);
        return ResponseEntity.ok(response);
    }
}
