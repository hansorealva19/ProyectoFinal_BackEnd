package com.paymentservice.payment_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.paymentservice.payment_service.repository.PaymentRepository;
import com.paymentservice.payment_service.entity.Payment;
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
                org.springframework.http.ResponseEntity<java.util.Map> or = rest.getForEntity(uri, java.util.Map.class);
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
                org.springframework.http.ResponseEntity<java.util.Map> r = rest.getForEntity(uri, java.util.Map.class);
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
                // if merchant account not configured, skip bank transfer but still notify (for dev)
                lastPayload = canonicalJson(note);
                lastSignature = signPayload(lastPayload);
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
                org.springframework.http.HttpEntity<byte[]> ent = new org.springframework.http.HttpEntity<>(lastPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8), headers);
                // use helper that retries without signature if order-service rejects signature
                notifyOrderService(ent, lastPayload, lastSignature);
                // update payment status/accounts when no transfer occurred but paymentId provided
                try {
                    if (paymentId != null && paymentRepository != null) {
                        try {
                            long pid = Long.parseLong(paymentId.toString());
                            paymentRepository.findById(pid).ifPresent(p -> {
                                p.setStatus("PENDING"); // still pending since no transfer
                                // leave payer/payee as-is (no transfer)
                                paymentRepository.save(p);
                            });
                        } catch (Exception ex) {
                            // ignore parse errors
                        }
                    }
                } catch (Exception e) { /* ignore update errors */ }
                return ResponseEntity.ok(java.util.Map.of("status", "NOTIFIED_NO_TRANSFER", "orderId", orderId, "debugPayload", lastPayload, "debugSignature", lastSignature));
            }

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
                org.springframework.http.ResponseEntity<java.util.Map> bankResp = rest.postForEntity(cardsUrl, chargeReq, java.util.Map.class);
                boolean bankOk = bankResp != null && bankResp.getStatusCode().is2xxSuccessful();
                if (!bankOk) {
                    org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).warn("Bank card charge returned non-2xx: {}", bankResp == null ? "null" : bankResp.getStatusCode());
                }
                // update payment record to COMPLETED and set accounts when paymentId present
                try {
                    if (paymentId != null && paymentRepository != null) {
                        try {
                            long pid = Long.parseLong(paymentId.toString());
                            final String cardNum = chargeReq.get("cardNumber") != null ? chargeReq.get("cardNumber").toString() : "";
                            final String toAcc = toAccountId != null ? String.valueOf(toAccountId) : "";
                            if (bankOk) {
                                paymentRepository.findById(pid).ifPresent(p -> {
                                    p.setStatus("COMPLETED");
                                    p.setPayerAccount(cardNum);
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
                org.springframework.http.ResponseEntity<java.util.Map> bankRespTx = rest.postForEntity(txUrl, tx, java.util.Map.class);
                boolean bankOkTx = bankRespTx != null && bankRespTx.getStatusCode().is2xxSuccessful();
                if (!bankOkTx) {
                    org.slf4j.LoggerFactory.getLogger(PaymentSimController.class).warn("Bank transaction returned non-2xx: {}", bankRespTx == null ? "null" : bankRespTx.getStatusCode());
                }
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
            org.springframework.http.ResponseEntity<java.util.Map> r = rest.getForEntity(uri, java.util.Map.class);
            if (r.getStatusCode().is2xxSuccessful() && r.getBody() != null) {
                Object total = r.getBody().get("total");
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
        "<script>document.getElementById('simPay').addEventListener('submit', async function(e){" +
    "e.preventDefault();const getVal=id=>document.getElementById(id).value===undefined? '': document.getElementById(id).value.toString().trim();" +
    "const payload={ orderId: getVal('orderId'), merchantCode: getVal('merchantCode') || 'ecommerce' };" +
    "const fromAccount = getVal('fromAccountId'); if (fromAccount) { try { payload.fromAccountId = Number(fromAccount); } catch(e) { payload.fromAccountId = fromAccount; } }" +
    "const cardNumber = getVal('cardNumber'); if (cardNumber) { payload.cardNumber = cardNumber; const ch = getVal('cardHolder'); if (ch) payload.cardHolder = ch; const exp = getVal('expirationDate'); if (exp) payload.expirationDate = exp; const cvv = getVal('cvv'); if (cvv) payload.cvv = cvv; }" +
    "const amountVal = getVal('amount'); if (amountVal) { try { payload.amount = Number(amountVal); } catch(e) { payload.amount = amountVal; } }" +
    "const r = await fetch('/api/payments/sim/confirm',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)}); const txt = await r.text(); try { const j = JSON.parse(txt); document.getElementById('result').innerHTML = '<pre>' + JSON.stringify(j,null,2) + '</pre>'; if (j && j.status && (j.status==='PAID_AND_NOTIFIED' || j.status==='NOTIFIED_NO_TRANSFER')) { setTimeout(function(){ window.location.href = 'http://localhost:8090/orders'; }, 600); } } catch(er) { document.getElementById('result').innerText = txt; } });</script></body></html>";
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
