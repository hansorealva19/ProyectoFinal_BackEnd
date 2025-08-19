package com.ecommerce.frontend.controller;

import com.ecommerce.frontend.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AuditController {
    private final AuditService auditService;

    @GetMapping("/audit")
    public String audit(Model model) {
        model.addAttribute("audits", auditService.getAudits());
        return "audit";
    }
}
