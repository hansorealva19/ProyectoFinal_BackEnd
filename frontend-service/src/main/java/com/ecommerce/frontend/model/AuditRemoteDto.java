package com.ecommerce.frontend.model;

import lombok.Data;

@Data
public class AuditRemoteDto {
    private Long id;
    private String whenRecorded; // keep as string to simplify mapping
    private String username;
    private String action;
    private String detail;
}
