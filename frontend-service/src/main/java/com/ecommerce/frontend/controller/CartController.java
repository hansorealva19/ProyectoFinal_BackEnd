package com.ecommerce.frontend.controller;

import com.ecommerce.frontend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

@Controller
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping("/cart")
    public String cart(Model model, HttpSession session,
                      @RequestParam(name = "page", defaultValue = "0") int page,
                      @RequestParam(name = "size", defaultValue = "10") int size) {
        String username = (String) session.getAttribute("USERNAME");
        String jwt = (String) session.getAttribute("JWT");
        try {
            java.util.List<com.ecommerce.frontend.model.CartItemViewModel> items = cartService.getCartItems(username, page, size, jwt);
            model.addAttribute("cartItems", items);
            double cartTotal = items.stream().mapToDouble(item -> item.getTotal()).sum();
            model.addAttribute("cartTotal", cartTotal);
            return "cart";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "No se pudo cargar el carrito. Intente más tarde.");
            return "error";
        }
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") int quantity,
                            Model model,
                            HttpSession session) {
        String username = (String) session.getAttribute("USERNAME");
        String jwt = (String) session.getAttribute("JWT");
        try {
            cartService.addToCart(username, productId, quantity, jwt);
            model.addAttribute("success", "Producto agregado al carrito.");
        } catch (Exception e) {
            model.addAttribute("errorMessage", "No se pudo agregar el producto. Intente más tarde.");
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/checkout")
    public String checkout(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("USERNAME");
        String jwt = (String) session.getAttribute("JWT");
        try {
            String paymentUrl = cartService.checkout(username, jwt);
            if (paymentUrl != null && !paymentUrl.isBlank()) {
                // redirect the browser to the payment UI (simulated)
                return "redirect:" + paymentUrl;
            }
            model.addAttribute("success", "Compra iniciada. Por favor completa el pago.");
        } catch (Exception e) {
            // handle empty cart specifically
            if (e instanceof com.ecommerce.frontend.exception.EmptyCartException) {
                // use flash so message survives the redirect
                redirectAttributes.addFlashAttribute("errorMessage", "No puedes comprar: el carrito está vacío.");
                return "redirect:/cart";
            }
            model.addAttribute("errorMessage", "No se pudo procesar la compra. Intente más tarde.");
        }
        return "redirect:/orders";
    }
}