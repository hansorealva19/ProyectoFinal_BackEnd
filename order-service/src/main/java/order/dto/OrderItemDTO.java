package order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {

  //id
  private Long id; // Identificador único del item

  //  productId
  @NotNull(message = "Product ID cannot be null it is mandatory")
  private Long productId; // ID del producto

  // productName
  private String productName; // Nombre del producto

  // quantity
  @NotNull(message = "Quantity cannot be null it is mandatory")
  @Min(value = 1, message = "Quantity must be at least 1")
  private Integer quantity; // Cantidad del producto en el item

  // unitPrice
  @NotNull(message = "Unit price cannot be null it is mandatory")
  private BigDecimal price; // Precio unitario del producto

  // subtotal
  private BigDecimal subtotal; // Subtotal del item (cantidad * precio unitario)

  // calculo para el subtotal
  public BigDecimal getSubtotal() {
    if (quantity != null && price != null) {
      return price.multiply(BigDecimal.valueOf(quantity));
    } else {
      return BigDecimal.ZERO;
    }
  }

}
