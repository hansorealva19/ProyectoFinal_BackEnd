package com.ecommerce.frontend.controller;

import com.ecommerce.frontend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

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
                           HttpSession session,
                           HttpServletResponse response) {
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
            // expose login state to templates so we can hide/show admin actions
            model.addAttribute("isLogged", jwt != null && !jwt.isEmpty());
            // Prevent browser caching listing page so Back after logout will not show protected UI
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);

            LoggerFactory.getLogger(ProductController.class).info("[ProductController] Productos cargados: {}", productPage.getContent().size());
            return "products";
        } catch (Exception e) {
            LoggerFactory.getLogger(ProductController.class).error("[ProductController] Error cargando productos: {}", e.getMessage());
            model.addAttribute("errorMessage", "No se pudo cargar los productos. Intente más tarde.");
            return "error";
        }
    }

    @GetMapping("/products/{id}/edit")
    public String editProductForm(@PathVariable("id") Long id,
                                  Model model,
                                  HttpSession session,
                                  HttpServletResponse response) {
        try {
            String jwt = null;
            if (session != null) {
                Object token = session.getAttribute("JWT");
                if (token instanceof String) jwt = (String) token;
            }

            // Prevent browser caching this sensitive page
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);

            var product = productService.getProduct(id, jwt);
            model.addAttribute("product", product);
            model.addAttribute("categories", productService.getCategories(jwt));
            // Esta vista ya está protegida por Spring Security; marcamos isLogged=true para el template
            model.addAttribute("isLogged", true);
            return "edit-product";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "No se pudo cargar el producto para editar.");
            return "error";
        }
    }

    @PostMapping("/products/{id}/edit")
    public String submitEditProduct(@PathVariable("id") Long id,
                                    @ModelAttribute com.ecommerce.frontend.model.ProductViewModel form,
                                    @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                    HttpSession session,
                                    HttpServletResponse response,
                                    Model model) {
        try {
            String jwt = null;
            if (session != null) {
                Object token = session.getAttribute("JWT");
                if (token instanceof String) jwt = (String) token;
            }

            // submit edit - handled by Spring Security

            // La ruta ya está protegida por Spring Security, no redirigimos manualmente

            // prevent caching of the response
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);

            // update main fields
            productService.updateProduct(id, form, jwt);

            // if image uploaded, call image endpoint
            if (imageFile != null && !imageFile.isEmpty()) {
                productService.updateProductImage(id, imageFile, jwt);
            }

            return "redirect:/products";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error al actualizar el producto: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/products/create")
    public String submitCreateProduct(@ModelAttribute com.ecommerce.frontend.model.ProductViewModel form,
                                      @RequestParam(value = "image", required = false) MultipartFile imageFile,
                                      HttpSession session,
                                      Model model) {
        try {
            String jwt = null;
            if (session != null) {
                Object token = session.getAttribute("JWT");
                if (token instanceof String) jwt = (String) token;
            }

            java.util.Map<String,Object> fields = new java.util.HashMap<>();
            fields.put("name", form.getName());
            fields.put("description", form.getDescription());
            fields.put("price", form.getPrice());
            fields.put("stock", form.getStock());
            fields.put("category", form.getCategory());

            productService.createProduct(fields, imageFile, jwt);
            return "redirect:/products";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error al crear el producto: " + e.getMessage());
            return "error";
        }
    }
}
