// src/main/java/com/ecommerce/frontend/service/CartService.java
package com.ecommerce.frontend.service;

import com.ecommerce.frontend.model.CartItemViewModel;
import com.ecommerce.frontend.model.ProductViewModel;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import com.ecommerce.frontend.service.CartRestService;

@Service
public class CartService {
    private final CartRestService cartRestService;

    public CartService(CartRestService cartRestService) {
        this.cartRestService = cartRestService;
    }

    public List<CartItemViewModel> getCartItems(String username, int page, int size, String jwt) {
        return cartRestService.getCartItems(username, page, size, jwt);
    }

    public void addToCart(String username, Long productId, int quantity, String jwt) {
        cartRestService.addToCart(username, productId, quantity, jwt);
    }

    public String checkout(String username, String jwt) {
        return cartRestService.checkout(username, jwt);
    }

    public int getCartCount(String username, String jwt) {
        return cartRestService.getCartCount(username, jwt);
    }

    public void clearCart(String username, String jwt) {
        cartRestService.clearCart(username, jwt);
    }

    public void updateCartItemQuantity(String username, Long productId, int quantity, String jwt) {
        cartRestService.updateCartItemQuantity(username, productId, quantity, jwt);
    }

    public void removeItemByProductId(String username, Long productId, String jwt) {
        cartRestService.removeItemByProductId(username, productId, jwt);
    }

    // Merge a session-stored cart into the persistent cart for the given username.
    // This iterates session items and forwards them to the cart-service via CartRestService.addToCart.
    public void mergeSessionCart(String username, java.util.List<CartItemViewModel> items, String jwt) {
        if (username == null || items == null || items.isEmpty()) return;
        for (CartItemViewModel it : items) {
            if (it == null) continue;
            Long pid = it.getProductId();
            int qty = it.getQuantity();
            if (pid == null || qty <= 0) continue;
            try {
                cartRestService.addToCart(username, pid, qty, jwt);
            } catch (Exception e) {
                // best-effort merge: continue on errors
            }
        }
    }
}