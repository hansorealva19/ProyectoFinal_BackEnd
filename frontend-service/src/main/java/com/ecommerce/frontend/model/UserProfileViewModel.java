package com.ecommerce.frontend.model;

import lombok.Data;

@Data
public class UserProfileViewModel {
    private String username;
    private String email;
    private String fullName;
    private String phone;
}
