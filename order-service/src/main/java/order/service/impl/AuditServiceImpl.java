package order.service.impl;

import order.domain.AuditEvent;
import order.repository.AuditRepository;
import order.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {
    private final AuditRepository auditRepository;

    @Override
    public AuditEvent record(String username, String action, String detail) {
        AuditEvent e = new AuditEvent();
        e.setWhenRecorded(LocalDateTime.now());
        e.setUsername(username);
        e.setAction(action);
        e.setDetail(detail);
        return auditRepository.save(e);
    }

    @Override
    public List<AuditEvent> latest(int limit) {
        List<AuditEvent> all = auditRepository.findLatest();
        if (all.size() <= limit) return all;
        return all.subList(0, limit);
    }
}
