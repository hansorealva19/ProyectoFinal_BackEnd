package com.paymentservice.payment_service.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaymentTestViewController {
    @GetMapping("/test-card-purchase")
    public String showTestCardPurchase() {
        return "test-card-purchase";
    }
}
