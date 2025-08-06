package com.bankservice.bank_service.repository;

import com.bankservice.bank_service.entity.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
}
