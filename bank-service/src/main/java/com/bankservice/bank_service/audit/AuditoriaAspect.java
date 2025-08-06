package com.bankservice.bank_service.audit;

import com.bankservice.bank_service.entity.Auditoria;
import com.bankservice.bank_service.repository.AuditoriaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Aspect
@Component
public class AuditoriaAspect {

    private final AuditoriaRepository auditoriaRepository;
    private final HttpServletRequest request;

    public AuditoriaAspect(AuditoriaRepository auditoriaRepository, HttpServletRequest request) {
        this.auditoriaRepository = auditoriaRepository;
        this.request = request;
    }

    @AfterReturning("execution(* com.bankservice.service.impl.*.*(..))")
    public void logAfter(JoinPoint joinPoint) {
        String metodo = joinPoint.getSignature().getName();
        String clase = joinPoint.getTarget().getClass().getSimpleName();
        String ip = request.getRemoteAddr();

        Auditoria auditoria = new Auditoria();
        auditoria.setAccion(clase + "." + metodo);
        auditoria.setFecha(LocalDateTime.now());
        auditoria.setIp(ip);

        auditoriaRepository.save(auditoria);
    }
}
