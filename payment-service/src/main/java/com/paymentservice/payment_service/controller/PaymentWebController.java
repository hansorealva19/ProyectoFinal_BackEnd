package com.paymentservice.payment_service.controller;

import com.paymentservice.payment_service.entity.Payment;
import com.paymentservice.payment_service.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
public class PaymentWebController {
    @Autowired
    private PaymentService paymentService;

    @GetMapping("/backoffice/payments")
    public String listPayments(Model model) {
        model.addAttribute("payments", paymentService.findAll());
        return "payments";
    }

    @GetMapping("/backoffice/payments/{id}")
    public String paymentDetail(@PathVariable Long id, Model model) {
        Optional<Payment> payment = paymentService.findById(id);
        if (payment.isPresent()) {
            model.addAttribute("payment", payment.get());
            return "payment-detail";
        } else {
            return "redirect:/backoffice/payments";
        }
    }
}
