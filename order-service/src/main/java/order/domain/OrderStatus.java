package order.domain;


// Estados posibles de un pedido
// Principios aplicados:

//  - State Pattern: patron de estados
// - Type Safety: uso de enumeraciones para definir estados
public enum OrderStatus {
  PENDING("Pendiente"),
  CONFIRMED("Confirmado"),
  SHIPPED("Enviado"),
  //  como sabe el proveedor de que ha sido entregado al usuario??
  DELIVERED("Entregado"),
  CANCELLED("Cancelado");

  private final String displayName;

  OrderStatus(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  //  verificar si el estado es final (no puede cambiar)
  public boolean isFinal() {
    return this == DELIVERED || this == CANCELLED;
  }

  //  verifica si el pedido esta activo
  public boolean isActive() {
    return this != CANCELLED;
  }
}
