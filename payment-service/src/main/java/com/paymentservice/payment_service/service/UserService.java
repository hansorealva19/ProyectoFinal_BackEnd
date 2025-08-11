package com.paymentservice.payment_service.service;

import com.paymentservice.payment_service.entity.User;
import com.paymentservice.payment_service.entity.Role;
import java.util.Optional;

public interface UserService {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    User saveUser(User user);
    Role getOrCreateRole(String roleName);
}
