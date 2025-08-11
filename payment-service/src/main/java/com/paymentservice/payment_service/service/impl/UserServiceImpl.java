package com.paymentservice.payment_service.service.impl;

import com.paymentservice.payment_service.entity.User;
import com.paymentservice.payment_service.entity.Role;
import com.paymentservice.payment_service.repository.UserRepository;
import com.paymentservice.payment_service.repository.RoleRepository;
import com.paymentservice.payment_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public User saveUser(User user) {
        String raw = user.getPassword();
        String encoded = passwordEncoder.encode(raw);
        org.slf4j.LoggerFactory.getLogger(UserServiceImpl.class)
            .info("Guardando usuario: {} | password plano: {} | password cifrado: {}", user.getUsername(), raw, encoded);
        user.setPassword(encoded);
        return userRepository.save(user);
    }

    @Override
    public Role getOrCreateRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
    }
}
