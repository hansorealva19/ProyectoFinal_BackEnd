package com.bankservice.bank_service.service;

import com.bankservice.bank_service.entity.Card;
import java.util.List;

public interface CardQueryService {
    List<Card> getCardsByAccountId(Long accountId);
}
