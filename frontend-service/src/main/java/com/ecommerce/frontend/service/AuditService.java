package com.ecommerce.frontend.service;

import com.ecommerce.frontend.model.AuditViewModel;
import com.ecommerce.frontend.model.AuditRemoteDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditService {
    private final RestTemplate restTemplate;
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    @Value("${microservices.order-service.url}")
    private String orderServiceUrl;

    @Value("${frontend.audit.fetchLimit:1000}")
    private int auditFetchLimit;

    public AuditService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<AuditViewModel> getAudits() {
        try {
            String url = orderServiceUrl + "/internal/audit?limit=" + auditFetchLimit;
            // Use a local DTO to avoid depending on remote package classes which are not on the classpath
            AuditRemoteDto[] arr = restTemplate.getForObject(url, AuditRemoteDto[].class);
            if (arr == null) return List.of();
            return Arrays.stream(arr).map(a -> {
                AuditViewModel v = new AuditViewModel();
                v.setDate(a.getWhenRecorded() != null ? a.getWhenRecorded().replace('T',' ').substring(0,16) : "");
                v.setUser(a.getUsername());
                v.setAction(a.getAction());
                v.setDetail(a.getDetail());
                return v;
            }).collect(Collectors.toList());
        } catch (Exception ex) {
            // log and rethrow so controller can show a friendly message
            log.error("Failed to load audits from order-service: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to load audits from order-service", ex);
        }
    }

    /**
     * Fetch audits and apply simple filters: username (contains), action (equals),
     * date range (from/to inclusive). Dates expected as yyyy-MM-dd (ISO date).
     */
    public List<AuditViewModel> getAuditsFiltered(String username, String action, String fromDate, String toDate) {
        List<AuditViewModel> all = getAudits();
        final java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        // Parse filter bounds: accept full datetime-local (ISO) or plain date. Convert to LocalDateTime bounds.
        java.time.LocalDateTime fromLdt = null;
        java.time.LocalDateTime toLdt = null;
        if (fromDate != null && !fromDate.isBlank()) {
            try {
                // Try ISO datetime first (e.g. 2025-08-24T12:03)
                fromLdt = java.time.LocalDateTime.parse(fromDate);
            } catch (Exception ex) {
                try {
                    // Fallback to date-only -> start of day
                    java.time.LocalDate ld = java.time.LocalDate.parse(fromDate);
                    fromLdt = ld.atStartOfDay();
                } catch (Exception e) {
                    // ignore parse error
                }
            }
        }
        if (toDate != null && !toDate.isBlank()) {
            try {
                toLdt = java.time.LocalDateTime.parse(toDate);
            } catch (Exception ex) {
                try {
                    java.time.LocalDate ld = java.time.LocalDate.parse(toDate);
                    // include entire day for a date-only 'to'
                    toLdt = ld.atTime(23, 59, 59);
                } catch (Exception e) {
                    // ignore parse error
                }
            }
        }
        final java.time.LocalDateTime fromLocal = fromLdt;
        final java.time.LocalDateTime toLocal = toLdt;

        return all.stream().filter(a -> {
            if (username != null && !username.isBlank()) {
                if (a.getUser() == null || !a.getUser().toLowerCase().contains(username.toLowerCase())) return false;
            }
            if (action != null && !action.isBlank()) {
                if (a.getAction() == null || !a.getAction().equalsIgnoreCase(action)) return false;
            }
            if (fromLocal != null || toLocal != null) {
                if (a.getDate() == null || a.getDate().isBlank()) return false;
                try {
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(a.getDate(), fmt);
                    if (fromLocal != null && ldt.isBefore(fromLocal)) return false;
                    if (toLocal != null && ldt.isAfter(toLocal)) return false;
                } catch (Exception e) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());
    }
}
