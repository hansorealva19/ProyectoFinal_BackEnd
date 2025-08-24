package com.ecommerce.frontend.controller;

import com.ecommerce.frontend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;

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
                        if (username == null) {
                            // show session-stored cart for anonymous users
                            java.util.List<com.ecommerce.frontend.model.CartItemViewModel> items = (java.util.List<com.ecommerce.frontend.model.CartItemViewModel>) session.getAttribute("SESSION_CART");
                            if (items == null) items = java.util.Collections.emptyList();
                            model.addAttribute("cartItems", items);
                            double cartTotal = items.stream().mapToDouble(item -> item.getTotal()).sum();
                            model.addAttribute("cartTotal", cartTotal);
                            return "cart";
                        }
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
            if (username == null) {
                // store in session cart for anonymous users
                java.util.List<com.ecommerce.frontend.model.CartItemViewModel> items = (java.util.List<com.ecommerce.frontend.model.CartItemViewModel>) session.getAttribute("SESSION_CART");
                if (items == null) items = new java.util.ArrayList<>();
                boolean merged = false;
                for (com.ecommerce.frontend.model.CartItemViewModel it : items) {
                    if (it.getProductId() != null && it.getProductId().equals(productId)) {
                        it.setQuantity(it.getQuantity() + quantity);
                        // update total if unit price present
                        if (it.getProduct() != null && it.getProduct().getPrice() != null)
                            it.setTotal(it.getQuantity() * it.getProduct().getPrice());
                        merged = true; break;
                    }
                }
                if (!merged) {
                    com.ecommerce.frontend.model.CartItemViewModel civ = new com.ecommerce.frontend.model.CartItemViewModel(productId, quantity, null);
                    // total and product details will be resolved when viewing the cart
                    items.add(civ);
                }
                session.setAttribute("SESSION_CART", items);
                model.addAttribute("success", "Producto agregado al carrito.");
            } else {
                cartService.addToCart(username, productId, quantity, jwt);
                model.addAttribute("success", "Producto agregado al carrito.");
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "No se pudo agregar el producto. Intente más tarde.");
        }
        return "redirect:/cart";
    }

    // AJAX handler: respond 200/500 and avoid redirect so fetch() callers work well
    @PostMapping(value = "/cart/add", headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public ResponseEntity<?> addToCartAjax(@RequestParam Long productId,
                                                @RequestParam(defaultValue = "1") int quantity,
                                                HttpServletRequest request,
                                                HttpSession session,
                                                @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
        String username = (String) session.getAttribute("USERNAME");
        String jwt = (String) session.getAttribute("JWT");
        try {
            if (username == null) {
                java.util.List<com.ecommerce.frontend.model.CartItemViewModel> items = (java.util.List<com.ecommerce.frontend.model.CartItemViewModel>) session.getAttribute("SESSION_CART");
                if (items == null) items = new java.util.ArrayList<>();
                boolean merged = false;
                for (com.ecommerce.frontend.model.CartItemViewModel it : items) {
                    if (it.getProductId() != null && it.getProductId().equals(productId)) {
                        it.setQuantity(it.getQuantity() + quantity);
                        if (it.getProduct() != null && it.getProduct().getPrice() != null)
                            it.setTotal(it.getQuantity() * it.getProduct().getPrice());
                        merged = true; break;
                    }
                }
                if (!merged) items.add(new com.ecommerce.frontend.model.CartItemViewModel(productId, quantity, null));
                session.setAttribute("SESSION_CART", items);
                // compute count from session
                int count = 0; for (com.ecommerce.frontend.model.CartItemViewModel it : items) count += it != null ? it.getQuantity() : 0;
                return ResponseEntity.ok(java.util.Map.of("count", count));
            }
            try {
                cartService.addToCart(username, productId, quantity, jwt);
                int count = cartService.getCartCount(username, jwt);
                return ResponseEntity.ok(java.util.Map.of("count", count));
            } catch (RuntimeException rex) {
                String m = rex.getMessage() != null ? rex.getMessage() : "";
                String lower = m.toLowerCase();
                // Detect stock-related errors and return a friendly Spanish message only
                if (lower.contains("stock") || lower.contains("sin stock") || lower.contains("available") || lower.contains("exceeds") || lower.contains("excede") || lower.contains("supera") || lower.contains("requested")) {
                    return ResponseEntity.status(400).body(java.util.Map.of("message", "La cantidad seleccionada supera el stock"));
                }
                // For other client errors return a generic Spanish message (avoid exposing raw internals)
                return ResponseEntity.status(400).body(java.util.Map.of("message", "No se pudo agregar el producto al carrito"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("count", 0));
        }
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

    // Update quantity (AJAX)
    @PostMapping(value = "/cart/update", headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public ResponseEntity<?> updateCartAjax(@RequestParam Long productId,
                                            @RequestParam int quantity,
                                            HttpSession session) {
        try {
            String username = (String) session.getAttribute("USERNAME");
            if (username == null) {
                java.util.List<com.ecommerce.frontend.model.CartItemViewModel> items = (java.util.List<com.ecommerce.frontend.model.CartItemViewModel>) session.getAttribute("SESSION_CART");
                if (items == null) items = new java.util.ArrayList<>();
                double cartTotal = 0.0;
                int count = 0;
                for (int i = 0; i < items.size(); i++) {
                    com.ecommerce.frontend.model.CartItemViewModel it = items.get(i);
                    if (it.getProductId() != null && it.getProductId().equals(productId)) {
                        if (quantity <= 0) {
                            items.remove(i);
                        } else {
                            it.setQuantity(quantity);
                            if (it.getProduct() != null && it.getProduct().getPrice() != null) {
                                it.setTotal(it.getProduct().getPrice() * quantity);
                            }
                        }
                        break;
                    }
                }
                // recalc totals
                for (com.ecommerce.frontend.model.CartItemViewModel it2 : items) {
                    double t = (it2.getTotal() != 0.0) ? it2.getTotal() : (it2.getProduct() != null && it2.getProduct().getPrice() != null ? it2.getProduct().getPrice() * it2.getQuantity() : 0.0);
                    cartTotal += t; count += it2.getQuantity();
                }
                session.setAttribute("SESSION_CART", items);
                return ResponseEntity.ok(java.util.Map.of("cartTotal", cartTotal, "count", count));
            }
            // For logged-in users delegate to cartService which will update remote cart
            String jwt = (String) session.getAttribute("JWT");
            cartService.updateCartItemQuantity(username, productId, quantity, jwt);
            // fetch current items to compute total and sum of quantities (count)
            java.util.List<com.ecommerce.frontend.model.CartItemViewModel> items = cartService.getCartItems(username, 0, 100, jwt);
            double cartTotal = 0.0;
            int count = 0;
            if (items != null && !items.isEmpty()) {
                cartTotal = items.stream().mapToDouble(i -> i.getTotal()).sum();
                count = items.stream().mapToInt(i -> i.getQuantity()).sum();
            } else {
                count = cartService.getCartCount(username, jwt);
            }
            return ResponseEntity.ok(java.util.Map.of("cartTotal", cartTotal, "count", count));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("message", "error"));
        }
    }

    // Remove item (AJAX)
    @PostMapping(value = "/cart/remove", headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public ResponseEntity<?> removeItemAjax(@RequestParam Long productId, HttpSession session) {
        try {
            String username = (String) session.getAttribute("USERNAME");
            if (username == null) {
                java.util.List<com.ecommerce.frontend.model.CartItemViewModel> items = (java.util.List<com.ecommerce.frontend.model.CartItemViewModel>) session.getAttribute("SESSION_CART");
                if (items == null) items = new java.util.ArrayList<>();
                items.removeIf(i -> i.getProductId() != null && i.getProductId().equals(productId));
                double cartTotal = 0.0; int count = 0;
                for (com.ecommerce.frontend.model.CartItemViewModel it2 : items) {
                    double t = (it2.getTotal() != 0.0) ? it2.getTotal() : (it2.getProduct() != null && it2.getProduct().getPrice() != null ? it2.getProduct().getPrice() * it2.getQuantity() : 0.0);
                    cartTotal += t; count += it2.getQuantity();
                }
                session.setAttribute("SESSION_CART", items);
                return ResponseEntity.ok(java.util.Map.of("cartTotal", cartTotal, "count", count));
            }
            // for authenticated users, remove by delegating to cartService and return updated totals
            String jwt = (String) session.getAttribute("JWT");
            cartService.removeItemByProductId(username, productId, jwt);
            java.util.List<com.ecommerce.frontend.model.CartItemViewModel> items = cartService.getCartItems(username, 0, 100, jwt);
            double cartTotal = items.stream().mapToDouble(i -> i.getTotal()).sum();
            int count = items.stream().mapToInt(i -> i.getQuantity()).sum();
            return ResponseEntity.ok(java.util.Map.of("cartTotal", cartTotal, "count", count));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("message", "error"));
        }
    }

    // Clear cart (AJAX)
    @PostMapping(value = "/cart/clear", headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public ResponseEntity<?> clearCartAjax(HttpSession session) {
        try {
            String username = (String) session.getAttribute("USERNAME");
            String jwt = (String) session.getAttribute("JWT");
            if (username == null) {
                session.removeAttribute("SESSION_CART");
                return ResponseEntity.ok(java.util.Map.of("cartTotal", 0.0, "count", 0));
            }
            // for authenticated users, clear remote cart via cartService
            cartService.clearCart(username, jwt);
            return ResponseEntity.ok(java.util.Map.of("cartTotal", 0.0, "count", 0));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("message", "error"));
        }
    }

    // Non-AJAX handler so the form submit works if JS is disabled or blocked
    @PostMapping("/cart/clear")
    public String clearCartForm(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            String username = (String) session.getAttribute("USERNAME");
            String jwt = (String) session.getAttribute("JWT");
            if (username == null) {
                session.removeAttribute("SESSION_CART");
            } else {
                cartService.clearCart(username, jwt);
            }
            redirectAttributes.addFlashAttribute("success", "Carrito vaciado");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se pudo vaciar el carrito");
        }
        return "redirect:/cart";
    }
}