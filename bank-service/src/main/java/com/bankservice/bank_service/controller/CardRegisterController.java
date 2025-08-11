package com.bankservice.bank_service.controller;

import com.bankservice.bank_service.dto.CardRegisterRequest;
import com.bankservice.bank_service.service.CardRegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class CardRegisterController {
    private final CardRegisterService cardRegisterService;

    @GetMapping("/cards/register")
    public String showRegisterForm(@RequestParam(value = "accountNumber", required = false) String accountNumber, Model model) {
        CardRegisterRequest req = new CardRegisterRequest();
        if (accountNumber != null) {
            req.setAccountNumber(accountNumber);
        }
        model.addAttribute("cardRegisterRequest", req);
        return "card-register";
    }

    @PostMapping("/cards/register")
    public String registerCard(@ModelAttribute CardRegisterRequest cardRegisterRequest, Model model) {
        try {
            cardRegisterService.registerCard(cardRegisterRequest);
            model.addAttribute("success", "Tarjeta registrada correctamente");
            model.addAttribute("cardRegisterRequest", new CardRegisterRequest());
        } catch (Exception e) {
            String errorMsg = "Error al registrar la tarjeta: ";
            if (e instanceof java.util.NoSuchElementException) {
                errorMsg += "La cuenta asociada no existe.";
            } else if (e.getMessage() != null) {
                errorMsg += e.getMessage();
            } else {
                errorMsg += "Error desconocido.";
            }
            model.addAttribute("error", errorMsg);
            model.addAttribute("cardRegisterRequest", cardRegisterRequest);
        }
        return "card-register";
    }
}
