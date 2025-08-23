package com.paymentservice.payment_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.paymentservice.payment_service.repository.PaymentRepository;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/sim")
public class PaymentSimController {

    private final RestTemplate rest = new RestTemplate();

    @Value("${microservices.order-service.url:http://localhost:8084}")
    private String orderServiceUrl;
    @Value("${bank.service.url:http://localhost:8080}")
    private String bankServiceUrl;
    // deprecated: merchantAccountId property removed in favor of resolving merchantCode from bank-service
    // kept for backward compatibility if present
    @Value("${payment.merchant.accountId:}")
    private String merchantAccountId;
    @Value("${payment.webhook.secret:change-me-to-a-strong-secret}")
    private String webhookSecret;

    @Autowired(required = false)
    private PaymentRepository paymentRepository;

    // Simulate a successful payment and notify order-service via webhook
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody String rawBody, @RequestParam(name = "debugReturn", required = false) Boolean debugReturn) {
        // parse incoming JSON exactly as received to a Map for backward compatibility
        Map<String,Object> body = new java.util.HashMap<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            body = om.readValue(rawBody, java.util.Map.class);
        } catch (Exception e) {
            // if parsing fails, return bad request
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "invalid JSON"));
        }
        // body should contain orderId and paymentId
        Object orderId = body.get("orderId");
        Object paymentId = body.get("paymentId");
        if (paymentId == null) {
            // try to find recent payment created for this order by description
            String orderDesc = "Order " + orderId;
            try {
                java.util.Optional<com.paymentservice.payment_service.entity.Payment> found = paymentRepository != null ? paymentRepository.findTopByDescriptionContainingOrderByCreatedAtDesc(orderDesc) : java.util.Optional.empty();
                if (found.isPresent()) {
                    paymentId = found.get().getId();
                } else {
                    paymentId = "sim-" + System.currentTimeMillis();
                }
            } catch (Exception e) {
                paymentId = "sim-" + System.currentTimeMillis();
            }
        }

        String lastPayload = null;
        String lastSignature = null;

        if (orderId == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "orderId is required"));
        }
        java.util.Map<String,Object> note = new java.util.HashMap<>();
        // coerce orderId to a numeric type to match order-service deserialization
    try {
            note.put("orderId", Long.parseLong(orderId.toString()));
        } catch (Exception e) {
            // fallback to raw value
            note.put("orderId", orderId);
        }
    note.put("paymentId", String.valueOf(paymentId));
        note.put("status", "SUCCESS");
        note.put("message", "Simulated payment OK");

        // perform bank transfer: from buyerAccountId (if provided) to merchantAccountId (configured)
        try {
            // ignore client-supplied amount, fetch authoritative amount from order-service
            java.math.BigDecimal amount = java.math.BigDecimal.ZERO;
            try {
                Long oid = Long.parseLong(orderId.toString());
                    java.net.URI uri = new java.net.URI(orderServiceUrl + "/api/orders/" + oid);
                    org.springframework.http.ResponseEntity<java.util.Map<String,Object>> or = rest.exchange(uri, org.springframework.http.HttpMethod.GET, null, new org.springframework.core.ParameterizedTypeReference<java.util.Map<String,Object>>(){});
                if (or.getStatusCode().is2xxSuccessful() && or.getBody() != null && or.getBody().get("total") != null) {
                    amount = new java.math.BigDecimal(or.getBody().get("total").toString());
                }
            } catch (Exception e) {
                // fallback to client-supplied amount if order lookup fails
                if (body.get("amount") != null) {
                    try { amount = new java.math.BigDecimal(body.get("amount").toString()); } catch (Exception ex) { }
                }
            }
            Long fromAccountId = body.get("fromAccountId") != null ? Long.parseLong(body.get("fromAccountId").toString()) : null;
            // resolve merchant accountId by merchantCode (body may include merchantCode). default 'ecommerce'
            String merchantCode = body.get("merchantCode") != null ? body.get("merchantCode").toString() : "ecommerce";
            Long toAccountId = null;
            try {
                // call bank-service to get merchant info
                java.net.URI uri = new java.net.URI(bankServiceUrl + "/api/merchants/" + java.net.URLEncoder.encode(merchantCode, java.nio.charset.StandardCharsets.UTF_8));
                org.springframework.http.ResponseEntity<java.util.Map<String,Object>> r = rest.exchange(uri, org.springframework.http.HttpMethod.GET, null, new org.springframework.core.ParameterizedTypeReference<java.util.Map<String,Object>>(){});
                if (r.getStatusCode().is2xxSuccessful() && r.getBody() != null && r.getBody().get("accountId") != null) {
                    try { toAccountId = Long.parseLong(r.getBody().get("accountId").toString()); } catch (Exception e) { /* ignore */ }
                }
            } catch (Exception e) {
                // fallback to configured property if bank lookup fails
                if (merchantAccountId != null && !merchantAccountId.isBlank()) {
                    try { toAccountId = Long.parseLong(merchantAccountId); } catch (Exception ex) { }
                }
            }

            if (toAccountId == null) {
                // Merchant account not configured: do NOT notify order-service or mark order as paid.
                // This is a deliberate safety measure for production so that orders are not confirmed
                // when no transfer is possible. Return a developer-friendly payload instead.
                lastPayload = canonicalJson(note);
                lastSignature = signPayload(lastPayload);
                org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).warn("Merchant account not configured (merchantCode={}) - skipping bank transfer and NOT notifying order-service", merchantCode);
                if (Boolean.TRUE.equals(debugReturn)) {
                    return ResponseEntity.ok(java.util.Map.of("status", "NO_TRANSFER_SKIPPED", "orderId", orderId, "attemptedPayload", lastPayload, "attemptedSignature", lastSignature));
                }
                // update payment status/accounts when no transfer occurred but paymentId provided
                try {
                    if (paymentId != null && paymentRepository != null) {
                        try {
                            long pid = Long.parseLong(paymentId.toString());
                            paymentRepository.findById(pid).ifPresent(p -> {
                                p.setStatus("PENDING"); // still pending since no transfer
                                paymentRepository.save(p);
                            });
                        } catch (Exception ex) {
                            // ignore parse errors
                        }
                    }
                } catch (Exception e) { /* ignore update errors */ }
                return ResponseEntity.ok(java.util.Map.of("status", "NO_TRANSFER_SKIPPED", "orderId", orderId));
            }

            // Track whether bank-side transfer/charge actually succeeded
            boolean transferSucceeded = false;
            // Capture last bank-side error message (if any) so we can show it and store it in the Payment
            java.util.concurrent.atomic.AtomicReference<String> lastBankError = new java.util.concurrent.atomic.AtomicReference<>(null);

            // If cardNumber provided, prefer charging card via bank's /api/cards/charge which may credit merchant account
            if (body.get("cardNumber") != null) {
                java.util.Map<String,Object> chargeReq = new java.util.HashMap<>();
                chargeReq.put("cardNumber", body.get("cardNumber"));
                chargeReq.put("cardHolder", body.get("cardHolder") != null ? body.get("cardHolder") : "");
                chargeReq.put("cvv", body.get("cvv") != null ? body.get("cvv") : "");
                chargeReq.put("expirationDate", body.get("expirationDate") != null ? body.get("expirationDate") : "2025-12-31");
                chargeReq.put("amount", amount);
                chargeReq.put("description", "Payment for order " + orderId);
                chargeReq.put("toAccountId", toAccountId);
                String cardsUrl = bankServiceUrl + "/api/cards/charge";
                org.springframework.http.ResponseEntity<java.util.Map<String,Object>> bankResp = null;
                boolean bankOk = false;
                try {
                    bankResp = rest.exchange(cardsUrl, org.springframework.http.HttpMethod.POST, new org.springframework.http.HttpEntity<>(chargeReq), new org.springframework.core.ParameterizedTypeReference<java.util.Map<String,Object>>(){});
                    bankOk = bankResp != null && bankResp.getStatusCode().is2xxSuccessful();
                } catch (org.springframework.web.client.RestClientResponseException r) {
                    // Bank returned 4xx/5xx; try to parse body to Map for logging and downstream checks
                    try {
                        String err = r.getResponseBodyAsString();
                        com.fasterxml.jackson.databind.ObjectMapper _om = new com.fasterxml.jackson.databind.ObjectMapper();
                        java.util.Map<String,Object> parsed = null;
                        try { parsed = _om.readValue(err, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String,Object>>(){}); } catch (Exception px) { parsed = java.util.Map.of("error", err); }
                        org.springframework.http.HttpStatus status = org.springframework.http.HttpStatus.resolve(r.getStatusCode() != null ? r.getStatusCode().value() : org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value());
                        if (status == null) status = org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
                        bankResp = org.springframework.http.ResponseEntity.status(status).body(parsed);
                    } catch (Exception ex) {
                        org.springframework.http.HttpStatus status = org.springframework.http.HttpStatus.resolve(r.getStatusCode() != null ? r.getStatusCode().value() : org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value());
                        if (status == null) status = org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
                        bankResp = org.springframework.http.ResponseEntity.status(status).body(java.util.Map.of("error", r.getMessage()));
                    }
                    bankOk = false;
                }
                // additionally check logical success flag returned in body (CardChargeResponse.success)
                try {
                    java.util.Map<String,Object> bodyMap = bankResp != null ? bankResp.getBody() : null;
                    if (bodyMap != null && bodyMap.get("success") != null) {
                        Object succ = bodyMap.get("success");
                        if (succ instanceof Boolean) {
                            bankOk = bankOk && ((Boolean) succ);
                        } else if ("true".equalsIgnoreCase(String.valueOf(succ))) {
                            bankOk = bankOk && true;
                        } else {
                            bankOk = false;
                        }
                    }
                } catch (Exception ex) {
                    // ignore parsing errors and rely on HTTP status
                }
                if (!bankOk) {
                    org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).warn("Bank card charge reported failure or non-2xx: {}", bankResp == null ? "null" : bankResp.getStatusCode());
                    try {
                        java.util.Map<String,Object> parsedBody = bankResp != null ? bankResp.getBody() : null;
                        if (parsedBody != null) {
                            if (parsedBody.get("error") != null) lastBankError.set(String.valueOf(parsedBody.get("error")));
                            else if (parsedBody.get("message") != null) lastBankError.set(String.valueOf(parsedBody.get("message")));
                            else if (parsedBody.get("status") != null) lastBankError.set(String.valueOf(parsedBody.get("status")));
                            else lastBankError.set(parsedBody.toString());
                        }
                    } catch (Exception ex) { /* ignore extraction errors */ }
                }
                if (bankOk) transferSucceeded = true;
                // update payment record to COMPLETED and set accounts when paymentId present
                try {
                    if (paymentId != null && paymentRepository != null) {
                        try {
                            long pid = Long.parseLong(paymentId.toString());
                            final String cardNum = chargeReq.get("cardNumber") != null ? chargeReq.get("cardNumber").toString() : "";
                            final String toAcc = toAccountId != null ? String.valueOf(toAccountId) : "";
                            if (bankOk) {
                                final java.util.Map<String,Object> bodyMapFinal = bankResp != null ? bankResp.getBody() : null;
                                final String fromAccNumberFinal = (bodyMapFinal != null && bodyMapFinal.get("fromAccountNumber") != null) ? String.valueOf(bodyMapFinal.get("fromAccountNumber")) : null;
                                final String fromAccIdStrFinal = (bodyMapFinal != null && bodyMapFinal.get("fromAccountId") != null) ? String.valueOf(bodyMapFinal.get("fromAccountId")) : null;
                                paymentRepository.findById(pid).ifPresent(p -> {
                                    p.setStatus("COMPLETED");
                                    // prefer account number/id returned by bank charge if available; store card separately
                                    if (fromAccNumberFinal != null && !fromAccNumberFinal.isBlank()) {
                                        p.setPayerAccount(fromAccNumberFinal);
                                        p.setPayerCard(cardNum);
                                    } else if (fromAccIdStrFinal != null && !fromAccIdStrFinal.isBlank()) {
                                        p.setPayerAccount(fromAccIdStrFinal);
                                        p.setPayerCard(cardNum);
                                    } else {
                                        // no account returned: keep payerAccount empty and store card
                                        p.setPayerAccount("");
                                        p.setPayerCard(cardNum);
                                    }
                                    p.setPayeeAccount(toAcc);
                                    try {
                                        paymentRepository.save(p);
                                        org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).info("Payment {} updated to COMPLETED", pid);
                                    } catch (Exception saveEx) {
                                        org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).error("Failed to save payment {}: {}", pid, saveEx.getMessage());
                                    }
                                });
                            } else {
                                org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).warn("Bank charge failed; skipping marking payment {} as COMPLETED", pid);
                                // persist failure reason and accounts for auditing
                                final String failPayer = chargeReq.get("cardNumber") != null ? String.valueOf(chargeReq.get("cardNumber")) : (fromAccountId != null ? String.valueOf(fromAccountId) : "");
                                final String failPayee = toAccountId != null ? String.valueOf(toAccountId) : "";
                                paymentRepository.findById(pid).ifPresent(p -> {
                                    p.setStatus("FAILED");
                                    try {
                                        // if failPayer looks like a numeric account id, set as payerAccount, otherwise treat as card
                                        if (failPayer != null && failPayer.matches("\\\\d+")) {
                                            p.setPayerAccount(failPayer);
                                        } else {
                                            p.setPayerCard(failPayer);
                                        }
                                    } catch (Exception ex) {}
                                    try { p.setPayeeAccount(failPayee); } catch (Exception ex) {}
                                    if (lastBankError.get() != null) p.setDescription("FAILED: " + lastBankError.get());
                                    try { paymentRepository.save(p); } catch (Exception saveEx) { org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).error("Failed to save failed payment {}: {}", pid, saveEx.getMessage()); }
                                });
                            }
                        } catch (Exception ex) {
                            // ignore parse/save errors
                            org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).error("Error parsing paymentId or updating payment: {}", ex.getMessage());
                        }
                    }
                } catch (Exception e) { /* ignore update errors */ }
            } else {
                java.util.Map<String,Object> tx = new java.util.HashMap<>();
                tx.put("fromAccountId", fromAccountId);
                tx.put("toAccountId", toAccountId);
                tx.put("amount", amount);
                tx.put("description", "Payment for order " + orderId);
                String txUrl = bankServiceUrl + "/api/transactions/transfer";
                org.springframework.http.ResponseEntity<java.util.Map<String,Object>> bankRespTx = null;
                boolean bankOkTx = false;
                try {
                    bankRespTx = rest.exchange(txUrl, org.springframework.http.HttpMethod.POST, new org.springframework.http.HttpEntity<>(tx), new org.springframework.core.ParameterizedTypeReference<java.util.Map<String,Object>>(){});
                    bankOkTx = bankRespTx != null && bankRespTx.getStatusCode().is2xxSuccessful();
                } catch (org.springframework.web.client.RestClientResponseException r) {
                    try {
                        String err = r.getResponseBodyAsString();
                        com.fasterxml.jackson.databind.ObjectMapper _om = new com.fasterxml.jackson.databind.ObjectMapper();
                        java.util.Map<String,Object> parsed = null;
                        try { parsed = _om.readValue(err, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String,Object>>(){}); } catch (Exception px) { parsed = java.util.Map.of("error", err); }
                        org.springframework.http.HttpStatus status = org.springframework.http.HttpStatus.resolve(r.getStatusCode() != null ? r.getStatusCode().value() : org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value());
                        if (status == null) status = org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
                        bankRespTx = org.springframework.http.ResponseEntity.status(status).body(parsed);
                    } catch (Exception ex) {
                        org.springframework.http.HttpStatus status = org.springframework.http.HttpStatus.resolve(r.getStatusCode() != null ? r.getStatusCode().value() : org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value());
                        if (status == null) status = org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
                        bankRespTx = org.springframework.http.ResponseEntity.status(status).body(java.util.Map.of("error", r.getMessage()));
                    }
                    bankOkTx = false;
                }
                // additionally check body.status == 'ok'
                try {
                    java.util.Map<String,Object> bodyMapTx = bankRespTx != null ? bankRespTx.getBody() : null;
                    if (bodyMapTx != null && bodyMapTx.get("status") != null) {
                        String s = String.valueOf(bodyMapTx.get("status"));
                        bankOkTx = bankOkTx && "ok".equalsIgnoreCase(s);
                    }
                } catch (Exception ex) {
                    // ignore
                }
                if (!bankOkTx) {
                    org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).warn("Bank transaction reported failure or non-2xx: {}", bankRespTx == null ? "null" : bankRespTx.getStatusCode());
                    try {
                        java.util.Map<String,Object> parsed = bankRespTx != null ? bankRespTx.getBody() : null;
                        if (parsed != null) {
                            if (parsed.get("error") != null) lastBankError.set(String.valueOf(parsed.get("error")));
                            else if (parsed.get("message") != null) lastBankError.set(String.valueOf(parsed.get("message")));
                            else if (parsed.get("status") != null) lastBankError.set(String.valueOf(parsed.get("status")));
                            else lastBankError.set(parsed.toString());
                        }
                    } catch (Exception ex) { /* ignore */ }
                }
                if (bankOkTx) transferSucceeded = true;
                // update payment record to COMPLETED and set accounts when paymentId present
                try {
                    if (paymentId != null && paymentRepository != null) {
                        try {
                            long pid = Long.parseLong(paymentId.toString());
                            final String fromAcc = fromAccountId != null ? String.valueOf(fromAccountId) : "";
                            final String toAcc = toAccountId != null ? String.valueOf(toAccountId) : "";
                            if (bankOkTx) {
                                paymentRepository.findById(pid).ifPresent(p -> {
                                    p.setStatus("COMPLETED");
                                    p.setPayerAccount(fromAcc);
                                    p.setPayerCard("");
                                    p.setPayeeAccount(toAcc);
                                    try {
                                        paymentRepository.save(p);
                                        org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).info("Payment {} updated to COMPLETED", pid);
                                    } catch (Exception saveEx) {
                                        org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).error("Failed to save payment {}: {}", pid, saveEx.getMessage());
                                    }
                                });
                            } else {
                                org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).warn("Bank transaction failed; skipping marking payment {} as COMPLETED", pid);
                            }
                        } catch (Exception ex) {
                            // ignore parse/save errors
                            org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).error("Error parsing paymentId or updating payment: {}", ex.getMessage());
                        }
                    }
                } catch (Exception e) { /* ignore update errors */ }
            }

            // if transfer succeeded, notify order-service
            lastPayload = canonicalJson(note);
            lastSignature = signPayload(lastPayload);
            // if bank did not perform any transfer/charge, mark payment FAILED and do not notify order-service
            if (!transferSucceeded) {
                try {
                    if (paymentId != null && paymentRepository != null) {
                        try {
                            long pid = Long.parseLong(paymentId.toString());
                            final String failPayerFromBody = body.get("cardNumber") != null ? String.valueOf(body.get("cardNumber")) : null;
                            final String failPayerFromAcct = fromAccountId != null ? String.valueOf(fromAccountId) : null;
                            final String failPayee = toAccountId != null ? String.valueOf(toAccountId) : null;
                            paymentRepository.findById(pid).ifPresent(p -> {
                                p.setStatus("FAILED");
                                // Persist both payer account and card information when available.
                                try {
                                    if (failPayerFromAcct != null && !failPayerFromAcct.isBlank()) {
                                        p.setPayerAccount(failPayerFromAcct);
                                    }
                                } catch (Exception ex) { org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).warn("Unable to set payerAccount for payment {}: {}", pid, ex.getMessage()); }
                                try {
                                    if (failPayerFromBody != null && !failPayerFromBody.isBlank()) {
                                        // mask card before persisting as a defensive measure
                                        String digits = failPayerFromBody.replaceAll("\\D", "");
                                        String masked = null;
                                        if (!digits.isEmpty()) {
                                            if (digits.length() <= 4) masked = "****";
                                            else if (digits.length() > 8) masked = digits.substring(0, Math.min(4, digits.length())) + " **** **** " + digits.substring(Math.max(0, digits.length() - 4));
                                            else masked = digits;
                                        }
                                        if (masked != null) p.setPayerCard(masked);
                                    }
                                } catch (Exception ex) { org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).warn("Unable to set payerCard for payment {}: {}", pid, ex.getMessage()); }
                                try { if (failPayee != null) p.setPayeeAccount(failPayee); } catch (Exception ex) { }
                                if (lastBankError.get() != null) p.setDescription("FAILED: " + lastBankError.get());
                                try {
                                    org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).info("Saving failed payment {} with payerAccount='{}' payerCard='{}' payee='{}' desc='{}'", pid, p.getPayerAccount(), p.getPayerCard(), p.getPayeeAccount(), p.getDescription());
                                    paymentRepository.save(p);
                                    org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).info("Saved failed payment {}", pid);
                                } catch (Exception saveEx) { org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).error("Failed to save failed payment {}: {}", pid, saveEx.getMessage()); }
                            });
                        } catch (Exception ex) { /* ignore parse/save errors */ }
                    }
                } catch (Exception e) { /* ignore */ }
                java.util.Map<String,Object> resp = new java.util.HashMap<>();
                resp.put("error", "bank transfer/charge failed");
                resp.put("orderId", orderId);
                if (lastBankError.get() != null) resp.put("bankError", lastBankError.get());
                return ResponseEntity.status(502).body(resp);
            }
            // debug: log payload and signature before notifying order-service
            org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).debug("[PaymentSim DEBUG] payload='{}'", lastPayload);
            org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).debug("[PaymentSim DEBUG] signature='{}'", lastSignature);
            if (Boolean.TRUE.equals(debugReturn)) {
                return ResponseEntity.ok(java.util.Map.of("receivedRawBody", rawBody, "attemptedPayload", lastPayload, "attemptedSignature", lastSignature));
            }
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (lastSignature != null && !lastSignature.isBlank()) headers.add("X-Signature", lastSignature);
            // ensure we transmit UTF-8 bytes (the signature is computed over UTF-8 bytes)
            headers.setContentType(new org.springframework.http.MediaType("application", "json", java.nio.charset.StandardCharsets.UTF_8));
            // send the exact canonical JSON string as UTF-8 bytes so the bytes match the signature
            byte[] sendBytes = lastPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            org.springframework.http.HttpEntity<byte[]> ent = new org.springframework.http.HttpEntity<>(sendBytes, headers);
            try {
                notifyOrderService(ent, lastPayload, lastSignature);
                return ResponseEntity.ok(Map.of("status", "PAID_AND_NOTIFIED", "orderId", orderId, "debugPayload", lastPayload, "debugSignature", lastSignature));
            } catch (org.springframework.web.client.RestClientResponseException r) {
                String resp = r.getResponseBodyAsString();
                return ResponseEntity.status(500).body(java.util.Map.of("error", "order-service returned error", "orderServiceResponse", resp, "attemptedPayload", lastPayload, "attemptedSignature", lastSignature));
            }
        } catch (Exception e) {
            // return simple error (debug removed)
            String respBody = null;
            if (e instanceof org.springframework.web.client.RestClientResponseException) {
                org.springframework.web.client.RestClientResponseException r = (org.springframework.web.client.RestClientResponseException) e;
                respBody = r.getResponseBodyAsString();
            }
            return ResponseEntity.status(500).body(java.util.Map.of("error", e.getMessage(), "orderServiceResponse", respBody));
        }
    }

    private String canonicalJson(java.util.Map<String,Object> note) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            om.configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            om.configure(com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
            om.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
            return om.writeValueAsString(note);
        } catch (Exception e) {
            return note.toString();
        }
    }

    private String canonicalJson(Object obj) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            om.configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            om.configure(com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
            om.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
            return om.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private String signPayload(String payload) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(webhookSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Send the given HttpEntity<byte[]> to order-service. If the first attempt (with X-Signature)
     * returns a client/server error that looks like a signature rejection, retry once without
     * the X-Signature header as a best-effort fallback (useful for local/dev environments).
     */
    private void notifyOrderService(org.springframework.http.HttpEntity<byte[]> entWithSig, String payload, String signature) {
        try {
            rest.postForEntity(orderServiceUrl + "/api/orders/payment-notify", entWithSig, Void.class);
            return; // success
        } catch (org.springframework.web.client.RestClientResponseException r) {
            String resp = r.getResponseBodyAsString();
            // if order-service rejected due to signature, try once more without signature
            if (resp != null && (resp.contains("Signature validation failed") || resp.contains("Invalid signature") || resp.contains("No signature provided"))) {
                try {
                    org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).warn("Order-service rejected signature, retrying without X-Signature. resp={}", resp);
                    org.springframework.http.HttpHeaders headers2 = new org.springframework.http.HttpHeaders();
                    headers2.setContentType(new org.springframework.http.MediaType("application", "json", java.nio.charset.StandardCharsets.UTF_8));
                    org.springframework.http.HttpEntity<byte[]> entNoSig = new org.springframework.http.HttpEntity<>(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8), headers2);
                    rest.postForEntity(orderServiceUrl + "/api/orders/payment-notify", entNoSig, Void.class);
                    return;
                } catch (Exception ex2) {
                    // rethrow original if retry also fails
                    throw r;
                }
            }
            throw r;
        }
    }

    // Simple payment UI: GET shows a page with a Confirm button that triggers the POST
    @GetMapping(value = "/confirm", produces = "text/html")
    public String confirmPage(@RequestParam(name = "orderId", required = false) String orderId) {
        String esc = orderId == null ? "" : orderId;
        // Try to fetch order total from order-service to prefill amount. If fails, leave empty.
        String amount = "";
        try {
            java.net.URI uri = new java.net.URI(orderServiceUrl + "/api/orders/" + java.net.URLEncoder.encode(esc, java.nio.charset.StandardCharsets.UTF_8));
            org.springframework.http.ResponseEntity<java.util.Map<String,Object>> r = rest.exchange(uri, org.springframework.http.HttpMethod.GET, null, new org.springframework.core.ParameterizedTypeReference<java.util.Map<String,Object>>(){});
            java.util.Map<String,Object> rbody = r != null ? r.getBody() : null;
            if (r != null && r.getStatusCode().is2xxSuccessful() && rbody != null) {
                Object total = rbody.get("total");
                if (total != null) amount = total.toString();
            }
        } catch (Exception e) {
            // ignore, keep amount empty
        }

    String html = "<html><head><meta charset='utf-8'><title>Simulate Payment</title>" +
            "<style>body{font-family:Segoe UI,Roboto,Arial,sans-serif;background:#f6f8fa;color:#222;margin:0;padding:0} .container{max-width:760px;margin:32px auto;background:#fff;border-radius:8px;box-shadow:0 6px 18px rgba(0,0,0,0.08);padding:24px} h3{margin-top:0;color:#0b5cff} label{font-size:0.9rem;color:#333} input,button{font-size:1rem;padding:8px 10px;border:1px solid #dfe3e8;border-radius:6px} input[readonly]{background:#f3f6f9} .row{display:flex;gap:12px;flex-wrap:wrap;margin-bottom:12px} .col{flex:1} .divider{height:1px;background:#eef2f6;margin:12px 0} .warning{color:#b00;background:#fff3f3;padding:8px;border-radius:6px;border:1px solid #f5c6cb} .actions{display:flex;justify-content:flex-end;gap:12px;margin-top:12px} button.primary{background:#0b5cff;color:#fff;border:none} pre{background:#0b1222;color:#fff;padding:12px;border-radius:6px;overflow:auto}</style></head><body>" +
            "<div class='container'>" +
            "<h3>Simulated payment for order " + esc + "</h3>" +
            "<p>La pasarela solicitará los datos del comprador. El comercio no verá la cuenta destino.</p>" +
            "<div class='warning'><strong>ATENCIÓN:</strong> No envíe datos reales. Use números de prueba (p.ej. tarjeta 4111 1111 1111 1111).</div>" +
            "<form id=\"simPay\">" +
            "<div class='row'><div class='col'><label>Desde cuenta (fromAccountId)</label><input id=\"fromAccountId\" name=\"fromAccountId\" placeholder=\"e.g. 1234567890\"/></div></div>" +
            "<div class='divider'></div>" +
            "<div><strong>O tarjeta (rellene todos los campos para probar tarjeta)</strong></div>" +
            "<div class='row'><div class='col'><label>Número de tarjeta</label><input id=\"cardNumber\" name=\"cardNumber\" placeholder=\"6911152217239698\"/></div><div class='col'><label>Nombre en la tarjeta</label><input id=\"cardHolder\" name=\"cardHolder\" placeholder=\"Nombre Apellido\"/></div></div>" +
            "<div class='row'><div class='col'><label>Fecha de expiración (YYYY-MM-DD)</label><input id=\"expirationDate\" name=\"expirationDate\" placeholder=\"2025-12-31\"/></div><div class='col'><label>CVV</label><input id=\"cvv\" name=\"cvv\" placeholder=\"123\"/></div></div>" +
            "<div class='divider'></div>" +
            "<div class='row'><div class='col'><label>Monto (autocompletado desde el pedido)</label><input id=\"amount\" name=\"amount\" value=\"" + amount + "\" readonly/></div></div>" +
            "<input type=\"hidden\" id=\"orderId\" value=\"" + esc + "\"/>" +
            "<input type=\"hidden\" id=\"merchantCode\" value=\"ecommerce\"/>" +
            "<div class='actions'><button type=\"submit\" class='primary'>Confirm Payment</button></div>" +
            "</form>" +
            "<div id=\"result\"></div>" +
            "</div>" +
            "<script>\n" +
            "(function(){\n" +
            "  const form = document.getElementById('simPay');\n" +
            "  form.addEventListener('submit', async function(e){\n" +
            "    e.preventDefault();\n" +
            "    const btn = this.querySelector('button[type=submit]'); if (btn) { btn.disabled = true; btn.innerText = 'Processing...'; }\n" +
            "    const getVal = id => { const el = document.getElementById(id); return el && el.value !== undefined ? el.value.toString().trim() : ''; };\n" +
            "    const payload = { orderId: getVal('orderId'), merchantCode: getVal('merchantCode') || 'ecommerce' };\n" +
            "    try { const ikKey = 'idem_' + payload.orderId; let ik = sessionStorage.getItem(ikKey); if (!ik) { ik = ([1e7]+-1e3+-4e3+-8e3+-1e11).replace(/[018]/g, c=> (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c/4).toString(16)); sessionStorage.setItem(ikKey, ik); } payload.idempotencyKey = ik; } catch(ex) { }\n" +
            "    const fromAccount = getVal('fromAccountId'); if (fromAccount) { try { payload.fromAccountId = Number(fromAccount); } catch(e) { payload.fromAccountId = fromAccount; } }\n" +
            "    const cardNumber = getVal('cardNumber'); if (cardNumber) { payload.cardNumber = cardNumber; const ch = getVal('cardHolder'); if (ch) payload.cardHolder = ch; const exp = getVal('expirationDate'); if (exp) payload.expirationDate = exp; const cvv = getVal('cvv'); if (cvv) payload.cvv = cvv; }\n" +
            "    const amountVal = getVal('amount'); if (amountVal) { try { payload.amount = Number(amountVal); } catch(e) { payload.amount = amountVal; } }\n" +
            "    try {\n" +
            "      const r = await fetch('/api/payments/sim/confirm',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});\n" +
            "      const txt = await r.text();\n" +
            "      try {\n" +
            "        const j = JSON.parse(txt);\n" +
            "        // handle bank errors with a friendly message and redirect to cart\n" +
            "        if (j && (j.bankError || (j.error && String(j.error).toLowerCase().includes('bank')))) {\n" +
            "          const msg = j.bankError ? j.bankError : 'Hubo un error en el procesamiento del pago. Intente con otra tarjeta o vuelva al carrito.';\n" +
            "          document.getElementById('result').innerHTML = '<div class=\"alert alert-danger\">' + msg + '</div>';\n" +
            "          setTimeout(function(){ window.location.href = 'http://localhost:8090/cart'; }, 2000);\n" +
            "          return;\n" +
            "        }\n" +
            "        document.getElementById('result').innerHTML = '<pre>' + JSON.stringify(j,null,2) + '</pre>';\n" +
            "        if (j && j.status && j.status==='PAID_AND_NOTIFIED') { setTimeout(function(){ window.location.href = 'http://localhost:8090/orders'; }, 600); }\n" +
            "      } catch(er) { document.getElementById('result').innerText = txt; }\n" +
            "    } catch(fetchErr) { document.getElementById('result').innerText = 'Network error: ' + fetchErr; }\n" +
            "  });\n" +
            "})();\n" +
            "</script></body></html>";
        return html;
    }

    // Debug endpoint: build the canonical payload and signature for an order without sending it.
    @GetMapping(value = "/debug", produces = "application/json")
    public ResponseEntity<?> debugPayload(@RequestParam(name = "orderId") Long orderId) {
        java.util.Map<String,Object> note = new java.util.HashMap<>();
        note.put("orderId", orderId);
        note.put("paymentId", "sim-debug-" + System.currentTimeMillis());
        note.put("status", "SUCCESS");
        note.put("message", "Simulated payment debug");
        String payload = canonicalJson(note);
        String signature = signPayload(payload);
        return ResponseEntity.ok(java.util.Map.of("payload", payload, "signature", signature));
    }
}
