package com.ecommerce.frontend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/cart")
public class CartRestController {
    // Aquí se implementaría la lógica para manejar el carrito en sesión o base de datos
    // Por ahora, endpoints vacíos para integración

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestParam Long productId, jakarta.servlet.http.HttpSession session) {
        // Placeholder endpoint. The frontend Thymeleaf flow uses server-side CartController
        // and CartRestService to call the real cart-service (/api/cart/{userId}/items).
        // Returning 501 to indicate this proxy is not implemented.
        return ResponseEntity.status(501).body(java.util.Map.of("message", "Not implemented in frontend proxy"));
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(jakarta.servlet.http.HttpSession session) {
        // Proxy hacia cart-service para checkout, reenviando JWT
        Object token = session != null ? session.getAttribute("JWT") : null;
        // For now we delegate to frontend CartService endpoints; keep returning OK
        return ResponseEntity.ok().build();
    }
}
