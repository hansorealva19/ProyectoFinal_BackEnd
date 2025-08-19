package com.ecommerce.frontend.model;

import lombok.Data;
import java.util.List;

@Data
public class OrderViewModel {
    private Long id;
    private String date;
    private List<CartItemViewModel> items;
    private double total;
    private String status;
    private String audit;
}
