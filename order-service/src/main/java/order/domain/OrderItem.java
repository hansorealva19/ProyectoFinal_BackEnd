package order.domain;

// Representar un item dentro de un pedido

import com.mysql.cj.log.Log;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

  //  id
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; // Identificador único del item

  //  order
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
//  evitar la recursion infinita en toString
  @ToString.Exclude
  private Order order; // Relación con el pedido al que pertenece

//  productId
  @NotNull(message = "Product ID cannot be null it is mandatory")
  @Column(name = "product_id", nullable = false)
  private Long productId;

//  productName
  @Column(name = "product_name", nullable = false, length = 100)
  private String productName; // Nombre del producto

  //  quantity
  @NotNull(message = "Quantity cannot be null it is mandatory")
  @Min(value = 1, message = "Quantity must be at least 1")
  @Column(nullable = false)
  private Integer quantity;

  //  unitPrice
  @NotNull(message = "Unit price cannot be null it is mandatory")
  @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitPrice;

  //  subtotal
  @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
  private BigDecimal subtotal;

  //  caluclar el subtotal del item
  @PrePersist
  @PreUpdate
  public void calculateSubtotal() {
    if (quantity != null && unitPrice != null) {
      this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    } else {
      this.subtotal = BigDecimal.ZERO;
    }
  }

  //  obtiene el subtotal calculado
  public BigDecimal getSubtotal() {
    if (subtotal == null) {
      calculateSubtotal();
    }
    return subtotal;
  }
}
