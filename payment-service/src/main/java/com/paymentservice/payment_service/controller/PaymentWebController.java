package com.paymentservice.payment_service.controller;

import com.paymentservice.payment_service.entity.Payment;
import com.paymentservice.payment_service.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
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
    public String listPayments(Model model,
                               @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
                               @RequestParam(name = "startDate", required = false) String startDateStr,
                               @RequestParam(name = "endDate", required = false) String endDateStr,
                               @RequestParam(name = "minAmount", required = false) Double minAmount,
                               @RequestParam(name = "maxAmount", required = false) Double maxAmount,
                               @RequestParam(name = "status", required = false) String status,
                               @RequestParam(name = "payerAccount", required = false) String payerAccountFilter,
                               @RequestParam(name = "payeeAccount", required = false) String payeeAccountFilter) {
        int pageNum = page == null ? 0 : Math.max(0, page);
        int pageSize = 15;

        LocalDateTime startDate = parseDateTimeParam(startDateStr);
        LocalDateTime endDate = parseDateTimeParam(endDateStr);

        List<Payment> all = paymentService.findAll();
        List<Payment> filtered = all.stream()
                .filter(p -> {
                    if (startDate != null && (p.getCreatedAt() == null || p.getCreatedAt().isBefore(startDate))) return false;
                    if (endDate != null && (p.getCreatedAt() == null || p.getCreatedAt().isAfter(endDate))) return false;
                    if (minAmount != null && (p.getAmount() == null || p.getAmount() < minAmount)) return false;
                    if (maxAmount != null && (p.getAmount() == null || p.getAmount() > maxAmount)) return false;
                    if (status != null && !status.isBlank() && (p.getStatus() == null || !p.getStatus().equalsIgnoreCase(status))) return false;
                    if (payerAccountFilter != null && !payerAccountFilter.isBlank()) {
                        String f = payerAccountFilter.trim().toLowerCase();
                        String pa = p.getPayerAccount() == null ? "" : p.getPayerAccount().toLowerCase();
                        String pc = p.getPayerCard() == null ? "" : p.getPayerCard().toLowerCase();
                        if (!pa.contains(f) && !pc.contains(f)) return false;
                    }
                    if (payeeAccountFilter != null && !payeeAccountFilter.isBlank()) {
                        String f = payeeAccountFilter.trim().toLowerCase();
                        String payee = p.getPayeeAccount() == null ? "" : p.getPayeeAccount().toLowerCase();
                        if (!payee.contains(f)) return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(Payment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        int total = filtered.size();
        int fromIndex = Math.min(pageNum * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<Payment> pageContent = filtered.subList(fromIndex, toIndex);
        Page<Payment> paymentsPage = new PageImpl<>(pageContent, PageRequest.of(pageNum, pageSize), total);

        model.addAttribute("paymentsPage", paymentsPage);
        model.addAttribute("payments", paymentsPage.getContent());
        model.addAttribute("currentPage", paymentsPage.getNumber());
        model.addAttribute("totalPages", paymentsPage.getTotalPages());

        java.util.Map<Long, String> originAccountMap = new java.util.HashMap<>();
        java.util.Map<Long, String> maskedCardMap = new java.util.HashMap<>();
        for (Payment p : paymentsPage.getContent()) {
            String payerAccount = p.getPayerAccount();
            if (payerAccount == null) payerAccount = "";
            String payerCard = p.getPayerCard();
            if (payerCard == null) payerCard = "";

            originAccountMap.put(p.getId(), payerAccount.isBlank() ? "" : payerAccount);
            if (!payerCard.isBlank()) {
                maskedCardMap.put(p.getId(), payerCard);
            } else if (!payerAccount.isBlank() && payerAccount.matches("\\d{12,19}")) {
                String digits = payerAccount.replaceAll("\\D", "");
                if (digits.length() > 8) {
                    String masked = digits.substring(0, Math.min(4, digits.length())) + " **** **** " + digits.substring(Math.max(0, digits.length() - 4));
                    maskedCardMap.put(p.getId(), masked);
                } else {
                    maskedCardMap.put(p.getId(), payerAccount);
                }
            } else {
                maskedCardMap.put(p.getId(), "");
            }
        }
        model.addAttribute("originAccountMap", originAccountMap);
        model.addAttribute("maskedCardMap", maskedCardMap);

        model.addAttribute("startDate", startDateStr == null ? "" : startDateStr);
        model.addAttribute("endDate", endDateStr == null ? "" : endDateStr);
        model.addAttribute("minAmount", minAmount == null ? "" : minAmount);
        model.addAttribute("maxAmount", maxAmount == null ? "" : maxAmount);
        model.addAttribute("statusFilter", status == null ? "" : status);
        model.addAttribute("payerAccountFilter", payerAccountFilter == null ? "" : payerAccountFilter);
        model.addAttribute("payeeAccountFilter", payeeAccountFilter == null ? "" : payeeAccountFilter);
        model.addAttribute("statuses", java.util.List.of("PENDING","COMPLETED","FAILED"));

        return "payments";
    }

    private LocalDateTime parseDateTimeParam(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            if (v.length() == 16) v = v + ":00";
            return LocalDateTime.parse(v, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            try {
                String alt = v.replace(' ', 'T');
                if (alt.length() == 16) alt = alt + ":00";
                return LocalDateTime.parse(alt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @GetMapping("/backoffice/payments/{id}")
    public String paymentDetail(@PathVariable Long id, Model model) {
        Optional<Payment> payment = paymentService.findById(id);
        if (payment.isPresent()) {
            Payment p = payment.get();
            model.addAttribute("payment", p);

            // derived attributes for the view
            boolean isCard = false;
            String maskedCard = null;
            String friendlyMessage = null;

            // Prefer an explicit masked payerCard if present
            if (p.getPayerCard() != null && !p.getPayerCard().isBlank()) {
                isCard = true;
                maskedCard = p.getPayerCard();
            } else if (p.getPayerAccount() != null) {
                String payer = p.getPayerAccount().trim();
                // heuristic: treat long numeric-like payerAccount as card
                if (payer.matches("\\d{12,19}")) {
                    isCard = true;
                    String digits = payer.replaceAll("\\D", "");
                    if (digits.length() > 8) {
                        String start = digits.substring(0, Math.min(4, digits.length()));
                        String end = digits.substring(Math.max(0, digits.length() - 4));
                        maskedCard = start + " **** **** " + end;
                    } else {
                        maskedCard = payer;
                    }
                }
            }

            if ("FAILED".equalsIgnoreCase(p.getStatus()) && p.getDescription() != null && p.getDescription().startsWith("FAILED:")) {
                friendlyMessage = p.getDescription().substring(Math.min(p.getDescription().length(), 8));
                // if message indicates insufficient funds, give a nicer suggestion
                if (friendlyMessage.toLowerCase().contains("fondos") || friendlyMessage.toLowerCase().contains("insuf")) {
                    friendlyMessage = "Fondos insuficientes en la tarjeta. Intente con otra tarjeta o use otra forma de pago.";
                }
            }

            model.addAttribute("isCard", isCard);
            model.addAttribute("maskedCard", maskedCard);
            model.addAttribute("friendlyMessage", friendlyMessage);

            return "payment-detail";
        } else {
            return "redirect:/backoffice/payments";
        }
    }
}
