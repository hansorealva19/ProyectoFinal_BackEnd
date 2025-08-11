package com.paymentservice.payment_service.service.impl;

import com.paymentservice.payment_service.dto.CardPaymentRequest;
import com.paymentservice.payment_service.dto.CardPaymentResponse;
import com.paymentservice.payment_service.service.CardPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Service
@RequiredArgsConstructor
public class CardPaymentServiceImpl implements CardPaymentService {

    @Value("${bank.service.url:http://localhost:8081}")
    private String bankServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public CardPaymentResponse processCardPayment(CardPaymentRequest request) {
        String url = bankServiceUrl + "/api/cards/charge";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CardPaymentRequest> entity = new HttpEntity<>(request, headers);
        try {
            ResponseEntity<CardPaymentResponse> response = restTemplate.postForEntity(url, entity, CardPaymentResponse.class);
            return response.getBody();
        } catch (Exception e) {
            CardPaymentResponse error = new CardPaymentResponse();
            error.setSuccess(false);
            error.setMessage("Error al conectar con el banco: " + e.getMessage());
            return error;
        }
    }
}
