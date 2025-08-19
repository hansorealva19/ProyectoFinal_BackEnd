package com.ecommerce.frontend.controller;

import com.ecommerce.frontend.model.UserProfileViewModel;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ProfileController {
    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        Object username = session.getAttribute("USERNAME");
        if (username == null) {
            log.info("Acceso a /profile sin sesión - redirigiendo a /login");
            return "redirect:/login";
        }
        UserProfileViewModel vm = new UserProfileViewModel();
        vm.setUsername(username.toString());
        // other fields can be loaded from user-service if needed
        model.addAttribute("user", vm);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(UserProfileViewModel user, HttpSession session) {
        // here you would call user-service to update the profile using JWT from session
        log.info("Perfil actualizado (simulado) para {}", user.getUsername());
        return "redirect:/profile?updated";
    }
}
