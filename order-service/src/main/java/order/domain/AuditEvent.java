package order.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime whenRecorded;
    private String username;
    private String action;
    @Column(length = 2000)
    private String detail;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getWhenRecorded() { return whenRecorded; }
    public void setWhenRecorded(LocalDateTime whenRecorded) { this.whenRecorded = whenRecorded; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
