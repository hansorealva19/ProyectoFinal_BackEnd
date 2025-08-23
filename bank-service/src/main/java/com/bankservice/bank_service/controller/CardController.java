package com.bankservice.bank_service.controller;

import com.bankservice.bank_service.dto.CardChargeRequest;
import com.bankservice.bank_service.dto.CardChargeResponse;
import com.bankservice.bank_service.service.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cards")
public class CardController {
    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/charge")
    public ResponseEntity<CardChargeResponse> chargeCard(@RequestBody CardChargeRequest request) {
        CardChargeResponse response = cardService.chargeCard(request);
        if (response == null) {
            return ResponseEntity.status(500).body(new CardChargeResponse(false, "Internal error"));
        }
        if (!response.isSuccess()) {
            return ResponseEntity.status(400).body(response);
        }
        return ResponseEntity.ok(response);
    }
}


