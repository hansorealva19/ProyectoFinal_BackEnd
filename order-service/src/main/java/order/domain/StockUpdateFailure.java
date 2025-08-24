package order.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_update_failures")
public class StockUpdateFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private Long productId;
    private int quantity;
    private String errorMessage;
    private LocalDateTime createdAt = LocalDateTime.now();

    public StockUpdateFailure() {}

    public StockUpdateFailure(Long orderId, Long productId, int quantity, String errorMessage) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.errorMessage = errorMessage;
    }

    // getters/setters
    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
