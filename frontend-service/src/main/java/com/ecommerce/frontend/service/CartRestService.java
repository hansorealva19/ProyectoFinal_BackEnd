package com.ecommerce.frontend.service;

import com.ecommerce.frontend.model.CartItemViewModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.util.List;

@Service
public class CartRestService {
    private static final Logger log = LoggerFactory.getLogger(CartRestService.class);
    @Value("${microservices.cart-service.url}")
    private String cartServiceUrl;
    @Value("${microservices.user-service.url}")
    private String userServiceUrl;
    @Value("${microservices.product-service.url}")
    private String productServiceUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    // Ahora aceptamos un JWT opcional para reenviarlo a los microservicios protegidos
    public List<CartItemViewModel> getCartItems(String username, int page, int size, String jwt) {
        String userIdUrl = userServiceUrl + "/api/users/by-username/" + username;
        // Petición al user-service, incluir JWT si está disponible
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (jwt != null) headers.setBearerAuth(jwt);
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.http.ResponseEntity<Long> userResp = null;
            try {
                userResp = restTemplate.exchange(userIdUrl, org.springframework.http.HttpMethod.GET, entity, Long.class);
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound nf) {
                log.debug("User not found when resolving username {} -> {}", username, userIdUrl);
                return java.util.Collections.emptyList();
            }
            Long userId = userResp.getBody();
            if (userId == null) return java.util.Collections.emptyList();
            String url = cartServiceUrl + "/api/cart/" + userId + "/items?page=" + page + "&size=" + size;
            log.debug("Requesting cart items from: {}", url);
            org.springframework.http.ResponseEntity<java.util.Map> resp = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, java.util.Map.class);
            java.util.Map response = resp.getBody();
            if (response != null && response.containsKey("content")) {
                Object content = response.get("content");
                // content is a List of maps representing CartItem from cart-service
                java.util.List<?> rawList = (java.util.List<?>) content;
                java.util.List<CartItemViewModel> items = new java.util.ArrayList<>();
                for (Object o : rawList) {
                    if (!(o instanceof java.util.Map)) continue;
                    java.util.Map map = (java.util.Map) o;
                    CartItemViewModel civ = new CartItemViewModel();
                    // quantity
                    Object q = map.get("quantity");
                    civ.setQuantity(q instanceof Number ? ((Number) q).intValue() : 0);
                    // subtotal -> total
                    Object sub = map.get("subtotal");
                    double subtotal = sub instanceof Number ? ((Number) sub).doubleValue() : 0.0;
                    civ.setTotal(subtotal);
                    // unitPrice -> product.price
                    Object up = map.get("unitPrice");
                    double unitPrice = up instanceof Number ? ((Number) up).doubleValue() : 0.0;
                    civ.setProductId(map.get("productId") instanceof Number ? ((Number) map.get("productId")).longValue() : null);
                    // build minimal ProductViewModel
                    com.ecommerce.frontend.model.ProductViewModel pv = new com.ecommerce.frontend.model.ProductViewModel();
                    pv.setId(civ.getProductId());
                    Object pname = map.get("productName");
                    pv.setName(pname != null ? pname.toString() : null);
                    pv.setPrice(unitPrice);
                    // If category is not provided in cart payload, try to fetch full product from product-service
                    try {
                        if ((pv.getCategory() == null || pv.getCategory().isEmpty()) && pv.getId() != null && productServiceUrl != null && !productServiceUrl.isBlank()) {
                            String prodUrl = productServiceUrl + "/api/products/" + pv.getId();
                            org.springframework.http.ResponseEntity<com.ecommerce.frontend.model.ProductViewModel> prodResp = restTemplate.getForEntity(prodUrl, com.ecommerce.frontend.model.ProductViewModel.class);
                            if (prodResp.getStatusCode().is2xxSuccessful() && prodResp.getBody() != null) {
                                com.ecommerce.frontend.model.ProductViewModel full = prodResp.getBody();
                                pv.setCategory(full.getCategory());
                                pv.setImageUrl(full.getImageUrl());
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Could not fetch product {} from product-service: {}", pv.getId(), e.toString());
                    }
                    civ.setProduct(pv);
                    items.add(civ);
                }
                return items;
            }
        } catch (org.springframework.web.client.HttpClientErrorException.Forbidden ex) {
            throw ex; // propagar 403 para que el controlador lo maneje
        } catch (Exception ex) {
            log.error("Error al obtener items del carrito: {}", ex.toString());
            return java.util.Collections.emptyList();
        }
        return java.util.Collections.emptyList();
    }

    public void addToCart(String username, Long productId, int quantity, String jwt) {
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (jwt != null) headers.setBearerAuth(jwt);
            // Resolve userId from user-service first (same approach as getCartItems)
            String userIdUrl = userServiceUrl + "/api/users/by-username/" + username;
            org.springframework.http.HttpEntity<Void> idEntity = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.http.ResponseEntity<Long> userResp;
            try {
                userResp = restTemplate.exchange(userIdUrl, org.springframework.http.HttpMethod.GET, idEntity, Long.class);
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound nf) {
                throw new RuntimeException("Usuario no encontrado");
            }
            Long userId = userResp.getBody();
            if (userId == null) throw new RuntimeException("Usuario no encontrado");

            // Call cart-service endpoint to add item to user's cart
            org.springframework.http.HttpEntity<CartItemViewModel> entity = new org.springframework.http.HttpEntity<>(new CartItemViewModel(productId, quantity, username), headers);
            String url = cartServiceUrl + "/api/cart/" + userId + "/items";
            restTemplate.exchange(url, org.springframework.http.HttpMethod.POST, entity, Void.class);
        } catch (Exception ex) {
            throw new RuntimeException("Error al agregar al carrito", ex);
        }
    }

    public String checkout(String username, String jwt) {
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (jwt != null) headers.setBearerAuth(jwt);
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
            // Resolve userId first (like other cart operations)
            String userIdUrl = userServiceUrl + "/api/users/by-username/" + username;
            org.springframework.http.ResponseEntity<Long> userResp = restTemplate.exchange(userIdUrl, org.springframework.http.HttpMethod.GET, entity, Long.class);
            Long userId = userResp.getBody();
            // Forward JWT to cart-service
            org.springframework.http.HttpHeaders headers2 = new org.springframework.http.HttpHeaders();
            if (jwt != null) headers2.setBearerAuth(jwt);
            org.springframework.http.HttpEntity<Void> entity2 = new org.springframework.http.HttpEntity<>(headers2);
            // include username as query param so cart-service can attach it to the order
            String checkoutUrl = cartServiceUrl + "/api/cart/checkout/" + userId + "?username=" + java.net.URLEncoder.encode(username == null ? "" : username, java.nio.charset.StandardCharsets.UTF_8);
            try {
                org.springframework.http.ResponseEntity<java.util.Map> resp = restTemplate.exchange(checkoutUrl, org.springframework.http.HttpMethod.POST, entity2, java.util.Map.class);
                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    Object pu = resp.getBody().get("paymentUrl");
                    return pu != null ? pu.toString() : null;
                }
                return null;
            } catch (org.springframework.web.client.HttpClientErrorException he) {
                // cart-service returned 4xx; if it's 400 interpret as empty cart
                if (he.getStatusCode() == org.springframework.http.HttpStatus.BAD_REQUEST) {
                    String respBody = he.getResponseBodyAsString();
                    String message = "Cart is empty";
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                        java.util.Map m = om.readValue(respBody, java.util.Map.class);
                        if (m.get("message") != null) message = m.get("message").toString();
                    } catch (Exception ex) {
                        // ignore parsing error
                    }
                    throw new com.ecommerce.frontend.exception.EmptyCartException(message);
                }
                throw he;
            }
        } catch (Exception ex) {
            // propagate specific empty-cart exception
            if (ex instanceof com.ecommerce.frontend.exception.EmptyCartException) throw (com.ecommerce.frontend.exception.EmptyCartException) ex;
            throw new RuntimeException("Error al procesar checkout", ex);
        }
    }
}
