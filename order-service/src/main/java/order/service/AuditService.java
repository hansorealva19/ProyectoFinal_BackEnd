package order.service;

import order.domain.AuditEvent;
import java.util.List;

public interface AuditService {
    AuditEvent record(String username, String action, String detail);
    List<AuditEvent> latest(int limit);
}
