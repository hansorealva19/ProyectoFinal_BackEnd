package com.bankservice.bank_service.mapper;

import com.bankservice.bank_service.dto.CardDTO;
import com.bankservice.bank_service.entity.Card;

public class CardMapper {
    public static CardDTO toDTO(Card card) {
        CardDTO dto = new CardDTO();
        dto.setId(card.getId());
        dto.setCardNumber(card.getCardNumber());
        dto.setCardHolder(card.getCardHolder());
        dto.setCvv(card.getCvv());
        dto.setExpirationDate(card.getExpirationDate());
        dto.setActive(card.isActive());
        return dto;
    }
}
