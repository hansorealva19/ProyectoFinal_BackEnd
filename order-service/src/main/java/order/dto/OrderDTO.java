package order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import order.domain.OrderStatus;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
//  id
//  userId
//  userName
//  status
//  totalAmount
//  itemsCount
//  notes
//  items
//  createdAt
//  updatedAt

  private Long id; // Identificador único del pedido
  private Long userId; // ID del usuario que realiza el pedido
  private String userName; // Nombre del usuario que realiza el pedido
  private OrderStatus status; // Estado del pedido (PENDING, COMPLETED, CANCELLED)
  private String totalAmount; // Monto total del pedido
  private Integer itemsCount; // Cantidad de artículos en el pedido
  private String notes; // Notas adicionales sobre el pedido
  private List<OrderItemDTO> items; // Lista de artículos del pedido
  private String createdAt; // Fecha y hora de creación del pedido
  private String updatedAt; // Fecha y hora de la última actualización del pedido

  //  campos calculados
  private String statusDisplayName; // Nombre para mostrar del estado del pedido
  private boolean canBeModified; // Indica si el pedido puede ser modificado

  //  metodos para mostrar el status
  public String getStatusDisplayName() {
    return status != null ? status.getDisplayName() : "";
  }

  //  metodo para ver si aun la orden se puede modificar
  public boolean getCanBeModified() {
    return status != null && status == OrderStatus.PENDING;
  }

}
