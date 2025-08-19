package com.ecommerce.product.dto;

/*
 * DTO (Data Transfer Object) para la entidad Product
 *
 * PRINCIPIOS APLICADOS:
 * - Data Transfer Object Pattern: Facilita la transferencia de datos entre capas o servicios
 * - Inmutability: Los DTOs suelen ser inmutables para evitar efectos secundarios no deseados
 * - Validation: Validación de datos para asegurar la integridad
 *
 */

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {

  private Long id;

  @NotBlank(message = "Name cannot be blank")
  @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
  private String name;

  @Size(max = 500, message = "Description must be up to 500 characters")
  private String description;

  @NotNull(message = "Price cannot be null")
  @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
  @Digits(integer = 10, fraction = 2, message = "Price must be a valid monetary amount")
  private Double price;

  @NotNull(message = "Stock cannot be null")
  @Min(value = 0, message = "Stock cannot be negative")
  private Integer stock;

  @NotBlank(message = "Category cannot be blank")
  @Size(max = 50, message = "Category must be up to 50 characters")
  private String category;

  private String imageUrl;
  private Boolean active;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

//  determinar el estado del stock

  //  tener algunos campos que se van a calcular en la vista
  private Boolean available;
  private String stockStatus; // "In Stock", "Low Stock", "Out of Stock"

  public String getStockStatus() {
    if (stock == null) return "UNKNOWN"; // muy raro, algo pasa??
//    == 0
    if (stock == 0) return "OUT_OF_STOCK";
//    <=5
    if (stock <= 5) return "LOW_STOCK";
//    <=20
    if (stock <= 20) return "MEDIUM_STOCK";

    return "HIGH_STOCK";
  }

  //  Verifica si el producto esta disponible
  public Boolean getAvailable() {
//    se lee de esta forma:
//    return true si active no es nulo y es true, y stock no es nulo y es mayor que 0
    return active != null && active && stock != null && stock > 0;
  }

}
