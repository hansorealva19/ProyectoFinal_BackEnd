package com.ecommerce.frontend.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/home")
    public String home(Model model, HttpSession session) {
        // Server-side guard: ensure the user has a session or authentication before serving the home page
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object username = session.getAttribute("USERNAME");
        if (auth == null || !auth.isAuthenticated() || username == null) {
            return "redirect:/login";
        }
        return "home";
    }
}
