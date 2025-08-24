package order.repository;

import order.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface AuditRepository extends JpaRepository<AuditEvent, Long> {
    @Query("select a from AuditEvent a order by a.whenRecorded desc")
    List<AuditEvent> findLatest();
}
