package com.ecommerce.frontend.controller;

import com.ecommerce.frontend.service.CartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartBadgeController {

    private final CartService cartService;

    @GetMapping("/refresh-badge")
    public ResponseEntity<java.util.Map<String, Integer>> refreshBadge(HttpSession session) {
        String username = (String) session.getAttribute("USERNAME");
        String jwt = (String) session.getAttribute("JWT");
        // If anonymous, compute count from SESSION_CART stored in session
        if (username == null) {
            int count = 0;
            try {
                java.util.List<com.ecommerce.frontend.model.CartItemViewModel> items = (java.util.List<com.ecommerce.frontend.model.CartItemViewModel>) session.getAttribute("SESSION_CART");
                if (items != null) {
                    for (com.ecommerce.frontend.model.CartItemViewModel it : items) {
                        count += it != null ? it.getQuantity() : 0;
                    }
                }
            } catch (Exception e) {
                // ignore and return 0
            }
            return ResponseEntity.ok(java.util.Map.of("count", count));
        }
        int count = 0;
        try { count = cartService.getCartCount(username, jwt); } catch (Exception e) { /* ignore */ }
        return ResponseEntity.ok(java.util.Map.of("count", count));
    }
}
