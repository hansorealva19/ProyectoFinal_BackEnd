package com.ecommerce.frontend.model;

import lombok.Data;

@Data
public class CartItemViewModel {
    private ProductViewModel product;
    private int quantity;
    private double total;
    private Long productId;
    private String username;

    public CartItemViewModel() {}

    public CartItemViewModel(Long productId, int quantity, String username) {
        this.productId = productId;
        this.quantity = quantity;
        this.username = username;
    }
}
