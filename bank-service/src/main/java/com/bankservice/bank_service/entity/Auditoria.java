package com.bankservice.bank_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String usuario;

    private String accion;

    private LocalDateTime fecha;

    private String entidad;

    private String detalle;

    private String ip;
}
