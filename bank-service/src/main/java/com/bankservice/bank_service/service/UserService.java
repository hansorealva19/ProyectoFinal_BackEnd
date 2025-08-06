package com.bankservice.bank_service.service;

import com.bankservice.bank_service.entity.User;

public interface UserService {
    User findByUsername(String username);
}
