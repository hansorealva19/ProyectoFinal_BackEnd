package com.ecommerce.frontend.controller;

import com.ecommerce.frontend.model.LoginRequest;
import com.ecommerce.frontend.model.LoginResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Controller
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Value("${microservices.user-service.url}")
    private String userServiceUrl;

    private final RestTemplate restTemplate;

    public AuthController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String username,
                               @RequestParam String password,
                               Model model,
                               HttpSession session) {
        log.info("Intentando login para usuario: {}", username);
        String url = userServiceUrl + "/api/users/login";
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);
        log.debug("Payload enviado al backend: {}", loginRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);
        try {
            ResponseEntity<LoginResponse> response = restTemplate.postForEntity(url, request, LoginResponse.class);
            log.info("Respuesta backend: status={}, body={}", response.getStatusCode(), response.getBody());
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                LoginResponse lr = response.getBody();
                session.setAttribute("JWT", lr.getToken());
                session.setAttribute("USERNAME", lr.getUsername());
                session.setAttribute("ROLE", lr.getRole());
                log.info("Login OK para {} con rol {}", lr.getUsername(), lr.getRole());
                return "redirect:/home";
            } else {
                model.addAttribute("error", "Usuario o contraseña incorrectos o backend no responde.");
                log.error("Login fallido para usuario: {}. Status: {} Body: {}", username, response.getStatusCode(), response.getBody());
                return "login";
            }
        } catch (Exception e) {
            model.addAttribute("error", "No se pudo conectar con el backend o la respuesta fue vacía. Intente más tarde.");
            log.error("Excepción en login de usuario: {} - {}", username, e.getMessage());
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(@RequestParam String username,
                                  @RequestParam String fullName,
                                  @RequestParam String email,
                                  @RequestParam String password,
                                  Model model) {
        String url = userServiceUrl + "/api/users/register";
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("fullName", fullName);
        payload.put("email", email);
        payload.put("password", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        try {
            ResponseEntity<?> response = restTemplate.postForEntity(url, request, Object.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                // registration ok -> redirect to login with a flag
                return "redirect:/login?registered";
            } else {
                model.addAttribute("error", "No se pudo registrar: backend devolvió " + response.getStatusCode());
                return "register";
            }
        } catch (Exception e) {
            model.addAttribute("error", "No se pudo conectar con el backend: " + e.getMessage());
            return "register";
        }
    }

    @PostMapping("/logout")
    public String doLogout(HttpSession session) {
        session.invalidate();
        // redirect to the frontend login page
        return "redirect:/login?logout";
    }
}
