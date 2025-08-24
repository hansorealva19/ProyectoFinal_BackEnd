package com.ecommerce.user.controller;

import com.ecommerce.user.entity.User;
import com.ecommerce.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Optional;
import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.security.JwtUtil;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    @GetMapping("/by-username/{username}")
    public ResponseEntity<Long> getUserIdByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
            .map(u -> ResponseEntity.ok(u.getId()))
            .orElse(ResponseEntity.notFound().build());
    }
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        logger.info("Registro solicitado para username={}, email={}, fullName={}",
                user.getUsername(), user.getEmail(), user.getFullName());
        User created = userService.createUser(user);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/login")
        public ResponseEntity<com.ecommerce.user.dto.LoginResponse> login(
                @RequestBody(required = false) LoginRequest loginRequest,
                @RequestParam(required = false) String username,
                @RequestParam(required = false) String password) {
            // Permitir login por JSON o por parámetros
            String user = (loginRequest != null) ? loginRequest.getUsername() : username;
            String pass = (loginRequest != null) ? loginRequest.getPassword() : password;
            logger.info("[LOGIN] Intento de login: username={}", user);

            if (user == null || pass == null) {
                return ResponseEntity.status(400).build();
            }

            Optional<User> userOpt = userService.getUserByUsername(user);
            if (userOpt.isPresent()) {
                User u = userOpt.get();
                boolean match = userService.getPasswordEncoder().matches(pass, u.getPassword());
                logger.info("[LOGIN] Usuario encontrado. ¿Contraseña válida?: {}", match);

                if (match) {
                    String token = jwtUtil.generateToken(user);
                    com.ecommerce.user.dto.LoginResponse response = new com.ecommerce.user.dto.LoginResponse();
                    response.setToken(token);
                    response.setUsername(user);
                    response.setRole(u.getRole());
                    return ResponseEntity.ok(response);
                } else {
                    return ResponseEntity.status(401).build();
                }
            } else {
                return ResponseEntity.status(401).build();
            }
        }

    @GetMapping("/{username}")
    public ResponseEntity<User> getByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{username}")
    public ResponseEntity<?> patchUser(@PathVariable String username, @RequestBody Map<String, Object> body) {
        String email = body.containsKey("email") ? (String) body.get("email") : null;
        String fullName = body.containsKey("fullName") ? (String) body.get("fullName") : null;
        User updated = userService.updateUserByUsername(username, email, fullName);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{username}/password")
    public ResponseEntity<?> changePassword(@PathVariable String username, @RequestBody Map<String, String> body) {
        String current = body.get("currentPassword");
        String nw = body.get("newPassword");
        if (current == null || nw == null) return ResponseEntity.status(400).body(Map.of("message","missing fields"));
        boolean ok = userService.changePassword(username, current, nw);
        if (!ok) return ResponseEntity.status(403).body(Map.of("message","invalid current password"));
        return ResponseEntity.ok(Map.of("message","password changed"));
    }
}
