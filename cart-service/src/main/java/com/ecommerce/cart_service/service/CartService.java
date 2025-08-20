package com.ecommerce.cart_service.service;

import com.ecommerce.cart_service.entity.Cart;
import com.ecommerce.cart_service.entity.CartItem;
import java.util.List;
import java.util.Optional;

public interface CartService {
    Cart createCart(Long userId);
    Optional<Cart> getCartByUserId(Long userId);
    Cart addItem(Long userId, CartItem item);
    Cart removeItem(Long userId, Long itemId);
    void clearCart(Long userId);
    org.springframework.data.domain.Page<CartItem> getItems(Long userId, org.springframework.data.domain.Pageable pageable);
    // start checkout and return a payment URL where the user should complete payment
    String checkout(Long userId, String username, String jwt);
    // lightweight count of items in cart
    int getCount(Long userId);
}
