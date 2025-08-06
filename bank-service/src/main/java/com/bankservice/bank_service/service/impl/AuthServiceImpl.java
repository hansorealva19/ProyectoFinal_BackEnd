package com.bankservice.bank_service.service.impl;

import com.bankservice.bank_service.dto.RegisterRequest;
import com.bankservice.bank_service.entity.User;
import com.bankservice.bank_service.repository.UserRepository;
import com.bankservice.bank_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("El usuario ya existe");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");

        return userRepository.save(user);
    }
}
