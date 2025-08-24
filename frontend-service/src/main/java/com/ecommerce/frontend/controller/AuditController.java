package com.ecommerce.frontend.controller;

import com.ecommerce.frontend.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

@Controller
@RequiredArgsConstructor
public class AuditController {
    private final AuditService auditService;
    private final RestTemplate restTemplate;

    @Value("${microservices.order-service.url}")
    private String orderServiceUrl;

    @GetMapping("/audit")
    public String audit(Model model) {
        try {
            model.addAttribute("audits", auditService.getAudits());
        } catch (Exception ex) {
            model.addAttribute("audits", java.util.List.of());
            model.addAttribute("auditError", ex.getMessage());
        }
        return "audit";
    }

    // Debug proxy to fetch raw JSON from order-service without CORS issues
    @GetMapping(path = "/internal/debug-audit")
    @ResponseBody
    public ResponseEntity<String> debugAudit(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        String url = orderServiceUrl + "/internal/audit?limit=" + limit;
        try {
            String body = restTemplate.getForObject(url, String.class);
            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Failed to contact order-service: " + ex.getMessage());
        }
    }
}
