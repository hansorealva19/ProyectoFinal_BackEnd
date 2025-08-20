package com.ecommerce.frontend.config;

import com.ecommerce.frontend.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class GlobalModelAttributes {

    private final CartService cartService;

    public GlobalModelAttributes(CartService cartService) {
        this.cartService = cartService;
    }

    @ModelAttribute("cartCount")
    public Integer cartCount(HttpSession session) {
        try {
            Object username = session != null ? session.getAttribute("USERNAME") : null;
            String user = username instanceof String ? (String) username : null;
            if (user == null || user.isBlank()) return 0;
            String jwt = session.getAttribute("JWT") instanceof String ? (String) session.getAttribute("JWT") : null;
            int count = cartService.getCartCount(user, jwt);
            return count;
        } catch (Exception e) {
            return 0;
        }
    }
}
