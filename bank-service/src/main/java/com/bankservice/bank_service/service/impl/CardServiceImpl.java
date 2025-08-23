
package com.bankservice.bank_service.service.impl;

import com.bankservice.bank_service.dto.CardChargeRequest;
import com.bankservice.bank_service.dto.CardChargeResponse;
import com.bankservice.bank_service.entity.Card;
import com.bankservice.bank_service.entity.Account;
import com.bankservice.bank_service.repository.CardRepository;
import com.bankservice.bank_service.repository.AccountRepository;
import com.bankservice.bank_service.entity.Transaction;
import com.bankservice.bank_service.repository.TransactionRepository;
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
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

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
        if (card == null) {
            return new CardChargeResponse(false, "Tarjeta no encontrada");
        }
        if (!card.isActive()) {
            return new CardChargeResponse(false, "Tarjeta inactiva");
        }
        // Validación de nombre de titular insensible a mayúsculas/minúsculas y espacios
        String dbHolder = card.getCardHolder().replaceAll("\\s+", "").toLowerCase();
        String reqHolder = request.getCardHolder().replaceAll("\\s+", "").toLowerCase();
        if (!dbHolder.equals(reqHolder)) {
            return new CardChargeResponse(false, "Nombre del titular incorrecto");
        }
        if (!card.getCvv().equals(request.getCvv())) {
            return new CardChargeResponse(false, "CVV incorrecto");
        }
        if (!card.getExpirationDate().toString().equals(request.getExpirationDate())) {
            return new CardChargeResponse(false, "Fecha de expiración incorrecta");
        }
        // Validate amount
        if (request.getAmount() == null || request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return new CardChargeResponse(false, "Monto inválido");
        }

        if (card.getAccount().getBalance().compareTo(request.getAmount()) < 0) {
            return new CardChargeResponse(false, "Fondos insuficientes");
        }
        // Descontar saldo y persistir la cuenta origen inmediatamente
        card.getAccount().setBalance(card.getAccount().getBalance().subtract(request.getAmount()));
        accountRepository.save(card.getAccount());
        // Registrar transacción con descripción personalizada + info de tarjeta
        String userDesc = request.getDescription();
        String cardInfo = "Pago con tarjeta: " + card.getCardNumber();
        String desc;
        if (userDesc != null && !userDesc.isBlank()) {
            String trimmed = userDesc.trim();
            // Quitar puntos finales extra
            trimmed = trimmed.replaceAll("\\.+$", "");
            desc = trimmed + ". " + cardInfo;
        } else {
            desc = cardInfo;
        }

        // If toAccountId is provided, credit that account (merchant)
    if (request.getToAccountId() != null) {
            Account toAcc = null;
            try {
                toAcc = accountRepository.findById(request.getToAccountId()).orElse(null);
            } catch (Exception ex) {
                toAcc = null;
            }
            if (toAcc == null) {
                try {
                    String possibleNumber = String.valueOf(request.getToAccountId());
                    toAcc = accountRepository.findByAccountNumber(possibleNumber).orElse(null);
                } catch (Exception ex) {
                    toAcc = null;
                }
            }
            if (toAcc != null) {
                toAcc.setBalance(toAcc.getBalance().add(request.getAmount()));
                accountRepository.save(toAcc);
            }
            Transaction transaction = Transaction.builder()
                    .fromAccount(card.getAccount())
                    .toAccount(toAcc)
                    .amount(request.getAmount())
                    .description(desc)
                    .timestamp(java.time.LocalDateTime.now())
                    .build();
            transactionRepository.save(transaction);
        } else {
            Transaction transaction = Transaction.builder()
                    .fromAccount(card.getAccount())
                    .toAccount(null) // Pago con tarjeta, no hay cuenta destino
                    .amount(request.getAmount())
                    .description(desc)
                    .timestamp(java.time.LocalDateTime.now())
                    .build();
            transactionRepository.save(transaction);
        }
        // return also the account id/number that was debited
        Long fromAccId = card.getAccount() != null ? card.getAccount().getId() : null;
        String fromAccNumber = card.getAccount() != null ? card.getAccount().getAccountNumber() : null;
        return new CardChargeResponse(true, "Pago realizado correctamente", fromAccId, fromAccNumber);
    }
}
