package com.ecommerce.frontend.service;

import com.ecommerce.frontend.model.OrderViewModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Arrays;
import java.util.List;

@Service
public class OrderRestService {
    @Value("${microservices.order-service.url}")
    private String orderServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public java.util.Map<String,Object> getOrdersByUser(String username, int page, int size, String jwt) {
        int currentPage = Math.max(0, page);
        int pageSize = size > 0 ? size : 10;
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (jwt != null) headers.setBearerAuth(jwt);
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
            String url = orderServiceUrl + "/api/orders/user/" + username + "?page=" + currentPage + "&size=" + pageSize;
            org.springframework.http.ResponseEntity<java.util.Map> resp = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, java.util.Map.class);
            java.util.Map response = resp.getBody();
            // normalize: map backend 'content' -> list of OrderViewModel
            java.util.List<OrderViewModel> orders = new java.util.ArrayList<>();
            if (response != null && response.containsKey("content")) {
                Object content = response.get("content");
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.List<java.util.Map<String,Object>> raw = mapper.convertValue(content, mapper.getTypeFactory().constructCollectionType(java.util.List.class, java.util.Map.class));
                for (java.util.Map<String,Object> item : raw) {
                    try {
                        OrderViewModel ov = new OrderViewModel();
                        Object idObj = item.get("id");
                        if (idObj instanceof Number) ov.setId(((Number) idObj).longValue());
                        else if (idObj instanceof String) { try { ov.setId(Long.parseLong((String) idObj)); } catch (Exception e) { } }
                        Object dateObj = item.getOrDefault("createdAt", item.getOrDefault("created_at", item.get("date")));
                        if (dateObj != null) ov.setDate(dateObj.toString());
                        Object totalObj = item.getOrDefault("totalAmount", item.get("total"));
                        try { if (totalObj instanceof Number) ov.setTotal(((Number) totalObj).doubleValue()); else if (totalObj instanceof String) ov.setTotal(Double.parseDouble((String) totalObj)); } catch (Exception ex) { }
                        Object statusObj = item.get("status"); if (statusObj != null) ov.setStatus(statusObj.toString());
                        Object notesObj = item.get("notes"); if (notesObj != null) ov.setAudit(notesObj.toString());
                        ov.setItems(java.util.Collections.emptyList());
                        orders.add(ov);
                    } catch (Exception e) {
                        org.slf4j.LoggerFactory.getLogger(OrderRestService.class).warn("Skipping invalid order element: {}", e.getMessage());
                    }
                }
            }
            java.util.Map<String,Object> result = new java.util.HashMap<>();
            result.put("orders", orders);
            // copy pagination metadata if present
            if (response != null) {
                result.put("page", response.getOrDefault("number", currentPage));
                result.put("size", response.getOrDefault("size", pageSize));
                result.put("totalPages", response.getOrDefault("totalPages", 1));
                result.put("totalElements", response.getOrDefault("totalElements", orders.size()));
            } else {
                result.put("page", currentPage);
                result.put("size", pageSize);
                result.put("totalPages", 1);
                result.put("totalElements", orders.size());
            }
            return result;
        } catch (Exception ex) {
            throw new RuntimeException("Error al obtener pedidos", ex);
        }
    }

    public OrderViewModel getOrderById(Long id, String jwt) {
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (jwt != null) headers.setBearerAuth(jwt);
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.http.ResponseEntity<OrderViewModel> resp = restTemplate.exchange(orderServiceUrl + "/api/orders/" + id, org.springframework.http.HttpMethod.GET, entity, OrderViewModel.class);
            return resp.getBody();
        } catch (Exception ex) {
            throw new RuntimeException("Error al obtener pedido por id", ex);
        }
    }
}
