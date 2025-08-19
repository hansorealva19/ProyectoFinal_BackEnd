package com.ecommerce.frontend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductViewModel {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private String category;
    private Double price;
    // add stock because product-service returns it
    private Integer stock;
}
