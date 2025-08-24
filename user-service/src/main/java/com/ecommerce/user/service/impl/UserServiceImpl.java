package com.ecommerce.user.service.impl;

import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    @Override
    public User updateUserByUsername(String username, String email, String fullName) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return null;
        User u = opt.get();
        if (email != null) u.setEmail(email);
        if (fullName != null) u.setFullName(fullName);
        return userRepository.save(u);
    }

    @Override
    public boolean changePassword(String username, String currentPassword, String newPassword) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return false;
        User u = opt.get();
        if (!passwordEncoder.matches(currentPassword, u.getPassword())) return false;
        u.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(u);
        return true;
    }
}
