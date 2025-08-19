package com.ecommerce.frontend.controller;

import com.ecommerce.frontend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

@Controller
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping("/")
    public String home(Model model) {
        return "home";
    }

    @GetMapping("/products")
    public String products(Model model,
                          @RequestParam(name = "page", defaultValue = "1") int page,
                          @RequestParam(name = "size", defaultValue = "12") int size,
                          @RequestParam(name = "name", required = false) String name,
                          @RequestParam(name = "category", required = false) String category,
                          HttpSession session) {
        try {
            // read JWT from HttpSession and forward it to the ProductService so downstream services receive the token
            String jwt = null;
            if (session != null) {
                Object token = session.getAttribute("JWT");
                if (token instanceof String) jwt = (String) token;
            }

            // page param coming from the UI is 1-based; convert to 0-based for pageable
            int pageIndex = Math.max(0, page - 1);
            var productPage = productService.getProducts(name, category, pageIndex, size, jwt);
            model.addAttribute("products", productPage.getContent());
            model.addAttribute("page", productPage.getPage() + 1); // convert to 1-based for the template helper
            model.addAttribute("totalPages", productPage.getTotalPages());
            // keep the form values and category list in the model for the template
            model.addAttribute("name", name);
            model.addAttribute("category", category);
            model.addAttribute("categories", productService.getCategories(jwt));
            model.addAttribute("size", size);
            org.slf4j.LoggerFactory.getLogger(ProductController.class).info("[ProductController] Productos cargados: {}", productPage.getContent().size());
            return "products";
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(ProductController.class).error("[ProductController] Error cargando productos: {}", e.getMessage());
            model.addAttribute("errorMessage", "No se pudo cargar los productos. Intente más tarde.");
            return "error";
        }
    }
}
