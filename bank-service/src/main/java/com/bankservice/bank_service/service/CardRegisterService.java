package com.bankservice.bank_service.service;

import com.bankservice.bank_service.dto.CardRegisterRequest;

public interface CardRegisterService {
    void registerCard(CardRegisterRequest request);
}
