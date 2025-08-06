package com.bankservice.bank_service.service;

import com.bankservice.bank_service.dto.RegisterRequest;
import com.bankservice.bank_service.entity.User;

public interface AuthService {
    User register(RegisterRequest request);
}
