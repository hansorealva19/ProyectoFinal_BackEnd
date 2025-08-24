package com.ecommerce.frontend.model;

import lombok.Data;

@Data
public class UserProfileViewModel {
    private String username;
    private String email;
    private String fullName;
    // Additional read-only/profile metadata
    private String roles; // comma separated
    // createdAt/lastLogin removed: not stored in DB in current schema
}
