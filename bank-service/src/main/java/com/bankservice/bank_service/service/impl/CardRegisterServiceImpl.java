package com.bankservice.bank_service.service.impl;

import com.bankservice.bank_service.dto.CardRegisterRequest;
import com.bankservice.bank_service.entity.Account;
import com.bankservice.bank_service.entity.Card;
import com.bankservice.bank_service.repository.AccountRepository;
import com.bankservice.bank_service.repository.CardRepository;
import com.bankservice.bank_service.service.CardRegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CardRegisterServiceImpl implements CardRegisterService {
    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;

    @Override
    public void registerCard(CardRegisterRequest request) {
        System.out.println("DEBUG CardRegisterRequest: " + request);
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber()).orElseThrow();
        String fullName = account.getUser().getFirstName() + " " + account.getUser().getLastName();
        Card card = Card.builder()
                .cardNumber(request.getCardNumber())
                .cardHolder(fullName)
                .cvv(request.getCvv())
                .expirationDate(LocalDate.parse(request.getExpirationDate()))
                .account(account)
                .active(true)
                .build();
        cardRepository.save(card);
    }
}
