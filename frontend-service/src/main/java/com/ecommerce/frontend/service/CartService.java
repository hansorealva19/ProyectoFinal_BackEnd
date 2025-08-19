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
}