package com.ecommerce.frontend.controller;

import com.ecommerce.frontend.model.UserProfileViewModel;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.RestTemplate;

@Controller
public class ProfileController {
    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final RestTemplate restTemplate;

    @Value("${microservices.user-service.url}")
    private String userServiceUrl;

    public ProfileController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        Object usernameObj = session.getAttribute("USERNAME");
        if (usernameObj == null) {
            log.info("Acceso a /profile sin sesión - redirigiendo a /login");
            return "redirect:/login";
        }
        String username = usernameObj.toString();
        UserProfileViewModel vm = new UserProfileViewModel();
        vm.setUsername(username);

        // Try to fetch authoritative user fields from user-service. RestTemplate is
        // configured to propagate the JWT from the session via an interceptor.
        try {
            String url = userServiceUrl + "/api/users/" + username;
            ResponseEntity<java.util.Map> resp = restTemplate.getForEntity(url, java.util.Map.class);
            if (resp != null && resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                java.util.Map body = resp.getBody();
                if (body.containsKey("email")) vm.setEmail((String) body.get("email"));
                if (body.containsKey("fullName")) vm.setFullName((String) body.get("fullName"));
                if (body.containsKey("role")) vm.setRoles((String) body.get("role"));
            } else {
                log.debug("user-service returned non-2xx for {}: {}", username, resp == null ? "null resp" : resp.getStatusCode());
            }
        } catch (Exception ex) {
            log.warn("No se pudo obtener datos desde user-service para {}: {}", username, ex.getMessage());
        }

    // no createdAt/lastLogin in current DB schema

    // If user updated profile but user-service didn't persist it, prefer session-stored values
    Object sessEmail = session.getAttribute("EMAIL");
    Object sessFull = session.getAttribute("FULL_NAME");
    if (sessEmail != null) vm.setEmail(sessEmail.toString());
    if (sessFull != null) vm.setFullName(sessFull.toString());

        model.addAttribute("user", vm);
        return "profile";
    }

    @PostMapping("/profile")
    public Object updateProfile(UserProfileViewModel user, HttpSession session, HttpServletRequest request) {
            // Allow editing of fields that exist in the DB: email and fullName.
            String username = (String) session.getAttribute("USERNAME");
            if (username == null) return "redirect:/login";
            // Try to call user-service PATCH /api/users/{username} if available.
            String xhr = request.getHeader("X-Requested-With");
            Map<String, Object> responseBody = new HashMap<>();
            try {
                String url = userServiceUrl + "/api/users/" + username;
                Map<String, Object> payload = new HashMap<>();
                if (user.getEmail() != null) payload.put("email", user.getEmail());
                if (user.getFullName() != null) payload.put("fullName", user.getFullName());
                if (!payload.isEmpty()) {
                    // Attempt PATCH - use exchange with method PATCH; if user-service doesn't support it,
                    // we'll catch and fallback to session storage.
                    org.springframework.http.HttpEntity<Map<String,Object>> entity = new org.springframework.http.HttpEntity<>(payload);
                    restTemplate.exchange(url, org.springframework.http.HttpMethod.PATCH, entity, java.util.Map.class);
                    responseBody.put("status", "ok");
                    responseBody.put("message", "Perfil actualizado");
                } else {
                    responseBody.put("status", "noop");
                    responseBody.put("message", "No hay cambios");
                }
            } catch (Exception ex) {
                // Fallback: store in session so user sees changes in UI, but warn it's not persisted
                if (user.getEmail() != null) session.setAttribute("EMAIL", user.getEmail());
                if (user.getFullName() != null) session.setAttribute("FULL_NAME", user.getFullName());
                responseBody.put("status", "ok");
                responseBody.put("message", "Cambios aplicados localmente (persistencia no soportada en user-service)");
                log.warn("No se pudo persistir en user-service: {}", ex.getMessage());
            }
            if (xhr != null && "XMLHttpRequest".equals(xhr)) {
                return ResponseEntity.ok(responseBody);
            }
            return "redirect:/profile?updated";
    }

    @PostMapping("/profile/password")
    public Object changePassword(String currentPassword, String newPassword, HttpSession session, HttpServletRequest request) {
        String username = (String) session.getAttribute("USERNAME");
        if (username == null) return "redirect:/login";
        // Forward the change-password request to user-service
        log.info("Cambio de contraseña solicitado para {}", username);
        String xhr = request.getHeader("X-Requested-With");
        Map<String, String> payload = new HashMap<>();
        payload.put("currentPassword", currentPassword);
        payload.put("newPassword", newPassword);
        try {
            String url = userServiceUrl + "/api/users/" + username + "/password";
            org.springframework.http.HttpEntity<Map<String,String>> entity = new org.springframework.http.HttpEntity<>(payload);
            ResponseEntity<java.util.Map> resp = restTemplate.postForEntity(url, entity, java.util.Map.class);
            if (xhr != null && "XMLHttpRequest".equals(xhr)) {
                if (resp != null && resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    return ResponseEntity.ok(Map.of("status","ok","message", resp.getBody().getOrDefault("message","Contraseña actualizada")));
                } else if (resp != null) {
                    return ResponseEntity.status(resp.getStatusCode()).body(Map.of("status","error","message","No se pudo cambiar la contraseña"));
                }
            }
        } catch (Exception ex) {
            log.warn("Error al cambiar contraseña en user-service: {}", ex.getMessage());
            if (xhr != null && "XMLHttpRequest".equals(xhr)) {
                return ResponseEntity.status(500).body(Map.of("status","error","message","Error interno al cambiar contraseña"));
            }
        }
        return "redirect:/profile?pwdchanged";
    }

    @GetMapping("/profile/edit")
    public String editProfile(Model model, HttpSession session) {
        Object usernameObj = session.getAttribute("USERNAME");
        if (usernameObj == null) return "redirect:/login";
        // reuse profile data prepared in profile()
        profile(model, session);
        return "profile-edit";
    }
}
