package com.ecommerce.frontend.model;

import lombok.Data;

@Data
public class AuditViewModel {
    private String date;
    private String user;
    private String action;
    private String detail;
}
