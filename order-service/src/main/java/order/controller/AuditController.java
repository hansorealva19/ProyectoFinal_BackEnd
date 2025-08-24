package order.controller;

import lombok.RequiredArgsConstructor;
import order.domain.AuditEvent;
import order.service.AuditService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AuditController {
    private final AuditService auditService;

    @GetMapping("/internal/audit")
    public List<AuditEvent> latest(@RequestParam(defaultValue = "50") int limit) {
        return auditService.latest(limit);
    }

    @PostMapping("/internal/audit")
    public AuditEvent record(@RequestBody AuditEvent ev) {
        // for simplicity accept body with username/action/detail
        return auditService.record(ev.getUsername(), ev.getAction(), ev.getDetail());
    }
}
