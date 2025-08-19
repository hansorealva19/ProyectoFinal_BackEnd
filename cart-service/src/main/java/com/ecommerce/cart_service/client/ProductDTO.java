package com.ecommerce.cart_service.client;

import lombok.Data;

@Data
public class ProductDTO {
    private Long id;
    private String name;
    private double price;
    private String category;
    private int stock;
}
