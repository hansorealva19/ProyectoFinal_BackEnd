package com.bankservice.bank_service.repository;

import com.bankservice.bank_service.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {
    Optional<Card> findByCardNumber(String cardNumber);
    List<Card> findByAccountId(Long accountId);
}
