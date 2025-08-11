package com.bankservice.bank_service.service;

import com.bankservice.bank_service.dto.CardChargeRequest;
import com.bankservice.bank_service.dto.CardChargeResponse;

public interface CardService {
    CardChargeResponse chargeCard(CardChargeRequest request);
    void setInactive(Long cardId);
}
