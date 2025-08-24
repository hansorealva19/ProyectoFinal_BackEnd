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
    public String audit(Model model,
                        @RequestParam(name = "page", defaultValue = "0") int page,
                        @RequestParam(name = "size", defaultValue = "15") int size,
                        @RequestParam(name = "username", required = false) String username,
                        @RequestParam(name = "action", required = false) String action,
                        @RequestParam(name = "from", required = false) String fromDate,
                        @RequestParam(name = "to", required = false) String toDate) {
        try {
            java.util.List<com.ecommerce.frontend.model.AuditViewModel> audits = auditService.getAuditsFiltered(username, action, fromDate, toDate);
            // server-side pagination over the fetched audit list
            int startIndex = Math.max(0, page * size);
            int endIndex = Math.min(audits.size(), startIndex + size);
            java.util.List<com.ecommerce.frontend.model.AuditViewModel> pageContent = audits.subList(startIndex, endIndex);
            model.addAttribute("audits", pageContent);
            model.addAttribute("filterUsername", username);
            model.addAttribute("filterAction", action);
            model.addAttribute("filterFrom", fromDate);
            model.addAttribute("filterTo", toDate);
            model.addAttribute("page", page);
            model.addAttribute("size", size);
            model.addAttribute("totalPages", (int) Math.ceil((double) audits.size() / size));
            model.addAttribute("totalElements", audits.size());
            // pass filters back to template so form keeps values
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

    @GetMapping(path = "/audit/download/csv")
    public ResponseEntity<byte[]> downloadCsv(@RequestParam(name = "username", required = false) String username,
                                              @RequestParam(name = "action", required = false) String action,
                                              @RequestParam(name = "from", required = false) String fromDate,
                                              @RequestParam(name = "to", required = false) String toDate) {
        try {
                java.util.List<com.ecommerce.frontend.model.AuditViewModel> audits = auditService.getAuditsFiltered(username, action, fromDate, toDate);
            java.io.StringWriter sw = new java.io.StringWriter();
            try (java.io.PrintWriter pw = new java.io.PrintWriter(sw)) {
                pw.println("date,username,action,detail");
                for (com.ecommerce.frontend.model.AuditViewModel a : audits) {
                    String line = String.format("\"%s\",\"%s\",\"%s\",\"%s\"",
                        a.getDate(), a.getUser(), a.getAction(), a.getDetail().replace("\"","'"));
                    pw.println(line);
                }
            }
            byte[] bytes = sw.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType("text/csv; charset=utf-8"));
            headers.setContentDispositionFormData("attachment", "audit.csv");
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(("Error generating CSV: " + ex.getMessage()).getBytes());
        }
    }

    @GetMapping(path = "/audit/print")
    public String auditPrint(Model model,
                             @RequestParam(name = "page", defaultValue = "0") int page,
                             @RequestParam(name = "size", defaultValue = "15") int size,
                             @RequestParam(name = "all", defaultValue = "false") boolean all,
                             @RequestParam(name = "username", required = false) String username,
                             @RequestParam(name = "action", required = false) String action,
                             @RequestParam(name = "from", required = false) String fromDate,
                             @RequestParam(name = "to", required = false) String toDate) {
        try {
                java.util.List<com.ecommerce.frontend.model.AuditViewModel> audits = auditService.getAuditsFiltered(username, action, fromDate, toDate);
            if (!all) {
                int startIndex = Math.max(0, page * size);
                int endIndex = Math.min(audits.size(), startIndex + size);
                model.addAttribute("audits", audits.subList(startIndex, endIndex));
                model.addAttribute("page", page);
                model.addAttribute("size", size);
                model.addAttribute("totalPages", (int) Math.ceil((double) audits.size() / size));
            } else {
                model.addAttribute("audits", audits);
            }
        } catch (Exception ex) {
            model.addAttribute("audits", java.util.List.of());
            model.addAttribute("auditError", ex.getMessage());
        }
        return "audit-print";
    }
}
