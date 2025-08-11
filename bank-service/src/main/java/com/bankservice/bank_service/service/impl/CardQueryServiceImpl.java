package com.bankservice.bank_service.service.impl;

import com.bankservice.bank_service.entity.Card;
import com.bankservice.bank_service.repository.CardRepository;
import com.bankservice.bank_service.service.CardQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardQueryServiceImpl implements CardQueryService {
    private final CardRepository cardRepository;

    @Override
    public List<Card> getCardsByAccountId(Long accountId) {
        return cardRepository.findByAccountId(accountId);
    }
}
