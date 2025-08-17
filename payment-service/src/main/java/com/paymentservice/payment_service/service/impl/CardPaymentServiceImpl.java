package com.paymentservice.payment_service.service.impl;

import com.paymentservice.payment_service.dto.CardPaymentRequest;
import com.paymentservice.payment_service.dto.CardPaymentResponse;
import com.paymentservice.payment_service.service.CardPaymentService;
import com.paymentservice.payment_service.entity.Payment;
import com.paymentservice.payment_service.repository.PaymentRepository;
import java.time.LocalDateTime;
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
    private final PaymentRepository paymentRepository;

    @Value("${bank.service.url:http://localhost:8080}")
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
            CardPaymentResponse result = response.getBody();
            // Si el pago fue exitoso, registrar en la tabla payments
            if (result != null && result.isSuccess()) {
                Payment payment = Payment.builder()
                        .payerAccount(request.getCardNumber())
                        .payeeAccount("") // Puedes poner el comercio o dejar vacío
                        .amount(request.getAmount().doubleValue())
                        .currency("PEN") // O usa el valor real si lo tienes
                        .status("COMPLETED")
                        .createdAt(LocalDateTime.now())
                        .description(request.getDescription())
                        .build();
                paymentRepository.save(payment);
            }
            return result;
        } catch (Exception e) {
            CardPaymentResponse error = new CardPaymentResponse();
            error.setSuccess(false);
            error.setMessage("Error al conectar con el banco: " + e.getMessage());
            return error;
        }
    }
}
