package com.ecommerce.frontend.service;

import com.ecommerce.frontend.model.AuditViewModel;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AuditService {
    public List<AuditViewModel> getAudits() {
        // Aquí se consumiría la auditoría real
        // Por ahora, retorna una lista vacía
        return List.of();
    }
}
