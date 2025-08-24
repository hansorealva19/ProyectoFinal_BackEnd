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

    public AuditService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<AuditViewModel> getAudits() {
        try {
            String url = orderServiceUrl + "/internal/audit?limit=50";
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
}
