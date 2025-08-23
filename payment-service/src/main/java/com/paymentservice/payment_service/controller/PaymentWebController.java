package com.paymentservice.payment_service.controller;

import com.paymentservice.payment_service.entity.Payment;
import com.paymentservice.payment_service.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;
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
    public String listPayments(Model model, @RequestParam(name = "page", required = false, defaultValue = "0") Integer page) {
        int pageNum = page == null ? 0 : Math.max(0, page);
        Pageable pageable = PageRequest.of(pageNum, 15, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Payment> paymentsPage = paymentService.findAll(pageable);
        model.addAttribute("paymentsPage", paymentsPage);
        model.addAttribute("payments", paymentsPage.getContent());
        model.addAttribute("currentPage", paymentsPage.getNumber());
        model.addAttribute("totalPages", paymentsPage.getTotalPages());
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
