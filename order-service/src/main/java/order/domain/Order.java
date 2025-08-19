package order.domain;

// Representa un pedido en el sistema

// Principios aplicados:
// - State Management: La clase puede ser extendida para manejar el estado del pedido.
//-  BUsiness Logic: La clase puede contener la lógica de negocio relacionada con los pedidos.

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.aspectj.weaver.ast.Or;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

  @Id
  @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
  private Long id; // Identificador único del pedido

  //  userId
  @NotNull(message = "User ID cannot be null it is mandatory")
  @Column(name = "user_id", nullable = false)
  private Long userId;

  //  userName
  @Column(name = "user_name", nullable = false)
  private String userName;

  //  status
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private OrderStatus status = OrderStatus.PENDING;

  //  totalAmount
  @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount;

  //  itemsCount
  @Column(name = "items_count", nullable = false)
  private Integer itemsCount;

  //  notes
  @Column(length = 1000)
  private String notes;

  //  items
//  hay una relacion entre tablas de Orden y OrdenItem
//   por lo tanto debemos hacer la relación en la entidad
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private List<OrderItem> items = new ArrayList<>();

  //  createdAt
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  //  updatedAt
  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  //  agregar un item al pedido
//  anade un item al pedido
//  busienss logic in domain
  public void addItem(OrderItem item) {
    if (item == null) {
      items = new ArrayList<>();
    }
    items.add(item);
    item.setOrder(this);

    recalculateTotal();
  }

  //  recalcular el total del pedido
  public void recalculateTotal() {
    if (items == null || items.isEmpty()) {
      totalAmount = BigDecimal.ZERO;
      itemsCount = 0;
      return;
    }

    totalAmount = items.stream()
//      significa que: //  para cada item en la lista de items, obtiene el subtotal
      .map(OrderItem::getSubtotal)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

//  calcula el total de items
    itemsCount = items.stream()
      .mapToInt(OrderItem::getQuantity)
      .sum();
  }

  //  confirma el pedido
  public void confirm() {
    if (status != OrderStatus.PENDING) {
      throw new IllegalStateException("Order cannot be confirmed in its current state: " + status);
    }
    status = OrderStatus.CONFIRMED;
  }

  //  cancelo el pedido
  public void cancel() {
    // no deberia estar en delivered o cancelled
    if (status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED) {
      throw new IllegalStateException("Order cannot be cancelled in its current state: " + status);
    }
    status = OrderStatus.CANCELLED;
  }

  //  marca el pedido como enviado
  public void ship() {
    if (status != OrderStatus.CONFIRMED) {
      throw new IllegalStateException("Order cannot be shipped in its current state: " + status);
    }
    status = OrderStatus.SHIPPED;
  }

  //  marca el pedido como entregado
  public void deliver() {
    if (status != OrderStatus.SHIPPED) {
      throw new IllegalStateException("Order cannot be delivered in its current state: " + status);
    }

    status = OrderStatus.DELIVERED;
  }

  //  verifica el pedido si puede ser modificado
  public boolean canBeModified() {
    return status == OrderStatus.PENDING;
  }

}

















