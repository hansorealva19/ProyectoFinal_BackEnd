package com.ecommerce.frontend.model;

import lombok.Data;
import java.util.List;

@Data
public class ProductPage {
    private List<ProductViewModel> content;
    private int page; // 0-based
    private int totalPages;
    private long totalElements;
}
