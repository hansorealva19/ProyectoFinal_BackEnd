package com.bankservice.bank_service.service.impl;

import com.bankservice.bank_service.entity.User;
import com.bankservice.bank_service.repository.UserRepository;
import com.bankservice.bank_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}

