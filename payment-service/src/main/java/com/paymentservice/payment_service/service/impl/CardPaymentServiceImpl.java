package com.paymentservice.payment_service.service.impl;

import com.paymentservice.payment_service.dto.CardPaymentRequest;
import com.paymentservice.payment_service.dto.CardPaymentResponse;
import com.paymentservice.payment_service.dto.BankCardChargeRequest;
import com.paymentservice.payment_service.service.CardPaymentService;
import com.paymentservice.payment_service.entity.Payment;
import com.paymentservice.payment_service.repository.PaymentRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardPaymentServiceImpl implements CardPaymentService {
    private final PaymentRepository paymentRepository;

    @Value("${bank.service.url:http://localhost:8080}")
    private String bankServiceUrl;

    @Value("${payment.merchant.accountId:}")
    private String merchantAccountId;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public CardPaymentResponse processCardPayment(CardPaymentRequest request) {
        String url = bankServiceUrl + "/api/cards/charge";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    // build internal bankEntity below
        try {
            // Build internal request for bank and inject merchant account id implicitly
            BankCardChargeRequest bankReq = new BankCardChargeRequest();
            bankReq.setCardNumber(request.getCardNumber());
            bankReq.setCardHolder(request.getCardHolder());
            bankReq.setCvv(request.getCvv());
            bankReq.setExpirationDate(request.getExpirationDate());
            bankReq.setAmount(request.getAmount());
            bankReq.setDescription(request.getDescription());
            if (merchantAccountId != null && !merchantAccountId.isBlank()) {
                try {
                    bankReq.setToAccountId(Long.parseLong(merchantAccountId));
                } catch (Exception ex) {
                    log.debug("merchantAccountId present but could not parse to long: {}", merchantAccountId);
                }
            }
            HttpEntity<BankCardChargeRequest> bankEntity = new HttpEntity<>(bankReq, headers);
            ResponseEntity<CardPaymentResponse> response = null;
            CardPaymentResponse result = null;
            try {
                response = restTemplate.postForEntity(url, bankEntity, CardPaymentResponse.class);
                result = response != null ? response.getBody() : null;
            } catch (org.springframework.web.client.RestClientResponseException r) {
                // Bank returned non-2xx; try to parse message for logging
                String err = r.getResponseBodyAsString();
                log.warn("Bank returned error for card charge: status={} body={}", r.getRawStatusCode(), err);
                CardPaymentResponse errResp = new CardPaymentResponse();
                errResp.setSuccess(false);
                errResp.setMessage(err != null && !err.isBlank() ? err : r.getMessage());
                result = errResp;
            }
            log.debug("Bank response for card charge: {}", result);
            // Only persist a payment record when the bank explicitly reports success
            if (result != null && result.isSuccess()) {
        // Prefer the account number returned by the bank when available (it represents the actual debited account)
        String payerAccount = result.getFromAccountNumber() != null && !result.getFromAccountNumber().isBlank()
                ? result.getFromAccountNumber()
                : request.getCardNumber();

        Payment payment = Payment.builder()
            .payerAccount(payerAccount)
            .payeeAccount(bankReq.getToAccountId() != null ? String.valueOf(bankReq.getToAccountId()) : "")
            .amount(request.getAmount() != null ? request.getAmount().doubleValue() : 0.0)
                        .currency("PEN")
                        .status("COMPLETED")
                        .createdAt(LocalDateTime.now())
                        .description(request.getDescription())
                        .build();
                log.debug("Saving payment record: {}", payment);
                paymentRepository.save(payment);
                log.debug("Payment saved with id: {}", payment.getId());
            } else {
                log.debug("Payment not successful or null response, skipping save. response={}", result);
            }
            return result;
        } catch (Exception e) {
            log.error("Error processing card payment", e);
            CardPaymentResponse error = new CardPaymentResponse();
            error.setSuccess(false);
            error.setMessage("Error al conectar con el banco: " + e.getMessage());
            return error;
        }
    }
}
