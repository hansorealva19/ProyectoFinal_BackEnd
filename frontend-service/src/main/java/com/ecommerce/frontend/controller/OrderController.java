package com.ecommerce.frontend.controller;

import com.ecommerce.frontend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping("/orders")
    public String orders(Model model, HttpSession session,
                        @RequestParam(name = "page", defaultValue = "0") int page,
                        @RequestParam(name = "size", defaultValue = "-1") int size) {
        String username = (String) session.getAttribute("USERNAME");
        org.slf4j.LoggerFactory.getLogger(OrderController.class).info("[OrderController] USERNAME en sesión: {}", username);
        String jwt = (String) session.getAttribute("JWT");
        try {
            int requestSize = size == -1 ? 0 : size;
            java.util.Map<String,Object> pageMap = orderService.getOrders(username, page, requestSize, jwt);
            java.util.List<?> orders = (java.util.List<?>) pageMap.getOrDefault("orders", java.util.Collections.emptyList());
            model.addAttribute("orders", orders);
            // normalize pagination metadata to numbers for Thymeleaf
            int currentPageInt = page;
            int pageSizeInt = requestSize > 0 ? requestSize : 10;
            int totalPagesInt = 1;
            long totalElementsLong = orders.size();
            try {
                Object p = pageMap.get("page"); if (p != null) currentPageInt = Integer.parseInt(p.toString());
            } catch (Exception ex) { }
            try {
                Object s = pageMap.get("size"); if (s != null) pageSizeInt = Integer.parseInt(s.toString());
            } catch (Exception ex) { }
            try {
                Object tp = pageMap.get("totalPages"); if (tp != null) totalPagesInt = Integer.parseInt(tp.toString());
            } catch (Exception ex) { }
            try {
                Object te = pageMap.get("totalElements"); if (te != null) totalElementsLong = Long.parseLong(te.toString());
            } catch (Exception ex) { }
            model.addAttribute("currentPage", currentPageInt);
            model.addAttribute("pageSize", pageSizeInt);
            model.addAttribute("totalPages", totalPagesInt);
            model.addAttribute("totalElements", totalElementsLong);
            org.slf4j.LoggerFactory.getLogger(OrderController.class).info("[OrderController] Pedidos cargados: {}", orders.size());
            return "orders";
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(OrderController.class).error("[OrderController] Error cargando pedidos: {}", e.getMessage());
            model.addAttribute("errorMessage", "No se pudo cargar los pedidos. Intente más tarde.");
            return "error";
        }
                        // Removed duplicate catch block
    }
}