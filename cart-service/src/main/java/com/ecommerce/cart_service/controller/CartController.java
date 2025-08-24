package com.ecommerce.cart_service.controller;

import com.ecommerce.cart_service.entity.Cart;
import com.ecommerce.cart_service.entity.CartItem;
import com.ecommerce.cart_service.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @PostMapping("/{userId}")
    public ResponseEntity<Cart> createCart(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(cartService.createCart(userId));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Cart> getCart(@PathVariable("userId") Long userId) {
        return cartService.getCartByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<?> addItem(@PathVariable("userId") Long userId, @RequestBody CartItem item) {
        try {
            Cart c = cartService.addItem(userId, item);
            return ResponseEntity.ok(c);
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Error adding item";
            if (msg.toLowerCase().contains("producto no encontrado") || msg.toLowerCase().contains("not found")) {
                return ResponseEntity.status(404).body(java.util.Map.of("message", "Producto no encontrado", "detail", msg));
            }
            return ResponseEntity.status(500).body(java.util.Map.of("message", "Error adding item", "detail", msg));
        }
    }

    @DeleteMapping("/{userId}/items/{itemId}")
    public ResponseEntity<Cart> removeItem(@PathVariable("userId") Long userId, @PathVariable("itemId") Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(userId, itemId));
    }

    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<Void> clearCart(@PathVariable("userId") Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/items")
    public ResponseEntity<org.springframework.data.domain.Page<CartItem>> getItems(@PathVariable("userId") Long userId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ResponseEntity.ok(cartService.getItems(userId, pageable));
    }

    @GetMapping("/{userId}/count")
    public ResponseEntity<java.util.Map<String, Integer>> getCount(@PathVariable("userId") Long userId) {
        int count = 0;
        try {
            java.util.Optional<Cart> oc = cartService.getCartByUserId(userId);
            if (oc.isPresent() && oc.get().getItems() != null) {
                count = oc.get().getItems().stream().mapToInt(i -> i.getQuantity()).sum();
            }
        } catch (Exception e) {
            // ignore and return 0
        }
        return ResponseEntity.ok(java.util.Map.of("count", count));
    }

    @PostMapping("/checkout/{userId}")
    public ResponseEntity<?> checkout(@PathVariable("userId") Long userId,
                                      @RequestHeader(name = "Authorization", required = false) String authorization,
                                      @RequestParam(name = "username", required = false) String username) {
        try {
            String jwt = null;
            if (authorization != null && authorization.startsWith("Bearer ")) jwt = authorization.substring(7);
            // returns a payment URL where the user should complete the payment
            String paymentUrl = cartService.checkout(userId, username, jwt);
            return ResponseEntity.ok(java.util.Map.of("paymentUrl", paymentUrl));
        } catch (Exception e) {
            // If the service threw an error indicating the cart is empty, return 400 so callers can show a user-friendly message
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("empty") || msg.contains("vacío") || msg.contains("vacio")) {
                return ResponseEntity.badRequest().body(java.util.Map.of("message", "Cart is empty", "detail", e.getMessage()));
            }
            return ResponseEntity.status(500).body(java.util.Map.of("message", "Error processing checkout", "detail", e.getMessage()));
        }
    }
}
