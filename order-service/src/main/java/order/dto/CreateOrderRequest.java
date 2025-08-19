package order.dto;

// DTO para solicitudes de creacion de pedidos

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

  @NotEmpty(message = "Customer ID cannot be empty")
  @Valid
  private List<OrderItemDTO> items;
  
  private String notes;
  
  // optional user context forwarded by cart-service
  private Long userId;
  private String userName;
}
