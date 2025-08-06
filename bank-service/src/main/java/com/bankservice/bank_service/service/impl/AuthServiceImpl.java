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
        // Validaciones básicas
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new RuntimeException("El nombre de usuario es requerido");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("La contraseña es requerida");
        }
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new RuntimeException("El nombre es requerido");
        }
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new RuntimeException("Los apellidos son requeridos");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("El email es requerido");
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new RuntimeException("El teléfono es requerido");
        }
        if (request.getDocumentNumber() == null || request.getDocumentNumber().trim().isEmpty()) {
            throw new RuntimeException("El número de documento es requerido");
        }
        if (request.getDocumentType() == null || request.getDocumentType().trim().isEmpty()) {
            throw new RuntimeException("El tipo de documento es requerido");
        }
        if (request.getBirthDate() == null) {
            throw new RuntimeException("La fecha de nacimiento es requerida");
        }
        if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
            throw new RuntimeException("La dirección es requerida");
        }
        if (request.getCity() == null || request.getCity().trim().isEmpty()) {
            throw new RuntimeException("La ciudad es requerida");
        }
        if (request.getState() == null || request.getState().trim().isEmpty()) {
            throw new RuntimeException("El estado/provincia es requerido");
        }
        if (request.getPostalCode() == null || request.getPostalCode().trim().isEmpty()) {
            throw new RuntimeException("El código postal es requerido");
        }
        if (request.getCountry() == null || request.getCountry().trim().isEmpty()) {
            throw new RuntimeException("El país es requerido");
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("El usuario ya existe");
        }

        // Verificar que el email no esté en uso
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado");
        }

        // Verificar que el documento no esté en uso
        if (userRepository.findByDocumentNumber(request.getDocumentNumber()).isPresent()) {
            throw new RuntimeException("El documento de identidad ya está registrado");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .documentNumber(request.getDocumentNumber())
                .documentType(request.getDocumentType())
                .birthDate(request.getBirthDate())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .occupation(request.getOccupation())
                .monthlyIncome(request.getMonthlyIncome())
                .build();

        return userRepository.save(user);
    }
}
