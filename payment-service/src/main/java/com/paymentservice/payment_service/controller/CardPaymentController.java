package com.paymentservice.payment_service.controller;

import com.paymentservice.payment_service.dto.CardPaymentRequest;
import com.paymentservice.payment_service.dto.CardPaymentResponse;
import com.paymentservice.payment_service.service.CardPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class CardPaymentController {
    private final CardPaymentService cardPaymentService;

    @GetMapping("/card-payment")
    public String showCardPaymentForm(Model model) {
        model.addAttribute("cardPaymentRequest", new CardPaymentRequest());
        return "card-payment";
    }

    @PostMapping("/card-payment")
    public String processCardPayment(@ModelAttribute CardPaymentRequest cardPaymentRequest, Model model) {
        CardPaymentResponse response = cardPaymentService.processCardPayment(cardPaymentRequest);
        model.addAttribute("cardPaymentRequest", cardPaymentRequest);
        model.addAttribute("response", response);
        return "card-payment";
    }
}
