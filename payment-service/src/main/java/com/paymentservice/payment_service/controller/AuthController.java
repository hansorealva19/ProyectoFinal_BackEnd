package com.paymentservice.payment_service.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.paymentservice.payment_service.entity.User;
import com.paymentservice.payment_service.entity.Role;
import com.paymentservice.payment_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import java.util.Collections;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;

    @RequestMapping("/")
    public String rootRedirect() {
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") @Valid User user,
                              BindingResult result,
                              @RequestParam("role") String role,
                              Model model) {
        logger.info("Intentando registrar usuario: {} con rol {}", user.getUsername(), role);
        if (userService.existsByUsername(user.getUsername())) {
            logger.warn("El usuario ya existe: {}", user.getUsername());
            result.rejectValue("username", null, "El usuario ya existe");
        }
        if (result.hasErrors()) {
            logger.warn("Errores de validación al registrar usuario: {}", user.getUsername());
            return "register";
        }
        Role userRole = userService.getOrCreateRole(role);
        user.setRoles(Collections.singleton(userRole));
        User saved = userService.saveUser(user);
        logger.info("Usuario guardado: {} con contraseña cifrada: {}", saved.getUsername(), saved.getPassword());
        model.addAttribute("success", "Usuario registrado correctamente");
        return "login";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @GetMapping("/logout-success")
    public String logoutPage() {
        return "logout";
    }
}
