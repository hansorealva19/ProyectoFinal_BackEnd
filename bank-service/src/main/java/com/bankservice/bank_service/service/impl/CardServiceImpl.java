
package com.bankservice.bank_service.service.impl;

import com.bankservice.bank_service.dto.CardChargeRequest;
import com.bankservice.bank_service.dto.CardChargeResponse;
import com.bankservice.bank_service.entity.Card;
import com.bankservice.bank_service.repository.CardRepository;
import com.bankservice.bank_service.service.CardService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {
    private final CardRepository cardRepository;

    @Override
    @Transactional
    public void setInactive(Long cardId) {
        Card card = cardRepository.findById(cardId).orElse(null);
        if (card != null && card.isActive()) {
            card.setActive(false);
            cardRepository.save(card);
        }
    }

    @Override
    @Transactional
    public CardChargeResponse chargeCard(CardChargeRequest request) {
        Card card = cardRepository.findByCardNumber(request.getCardNumber()).orElse(null);
        if (card == null || !card.isActive()) {
            return new CardChargeResponse(false, "Tarjeta no encontrada o inactiva");
        }
        if (!card.getCardHolder().equalsIgnoreCase(request.getCardHolder())) {
            return new CardChargeResponse(false, "Nombre del titular incorrecto");
        }
        if (!card.getCvv().equals(request.getCvv())) {
            return new CardChargeResponse(false, "CVV incorrecto");
        }
        if (!card.getExpirationDate().toString().equals(request.getExpirationDate())) {
            return new CardChargeResponse(false, "Fecha de expiración incorrecta");
        }
        if (card.getAccount().getBalance().compareTo(request.getAmount()) < 0) {
            return new CardChargeResponse(false, "Fondos insuficientes");
        }
        // Descontar saldo
        card.getAccount().setBalance(card.getAccount().getBalance().subtract(request.getAmount()));
        return new CardChargeResponse(true, "Pago realizado correctamente");
    }
}
