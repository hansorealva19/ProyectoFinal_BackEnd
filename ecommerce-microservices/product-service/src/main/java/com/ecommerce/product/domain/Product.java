package com.ecommerce.product.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Principios aplicados
 * - Single Responsibility Principle (SRP): La clase Product tiene una única responsabilidad, que es representar un producto en el sistema.
 * - Encapsulation: Campos privados con acceso controlado
 * - DDD (Domain-Driven Design): La clase Product es parte del DOMINIO de la aplicación
 * <p>
 * Patrones implementados
 * - Entity Pattern: Representa una entidad del dominio
 * - Builder Pattern: Construcción fluida de objetos
 *
 */

@Entity
@Table(name = "products")
@Data // Lombok genera getters, setters, toString, equals y hashCode
@NoArgsConstructor // constructor sin argumentos
@AllArgsConstructor // constructor con todos los argumentos
@Builder // permite crear instancias de Product de manera fluida
public class Product {

  //id
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
//  name
//  description
//  price no debe haber un precio negativo
//  stock tampoco puede ser negativo
//  category
//  imageUrl
//  active
//  createdAt
//  updatedAt

  @NotBlank(message = "Name cannot be blank")
  @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
  @Column(nullable = false, length = 100)
  private String name;

  @Size(max = 500, message = "Description must be up to 500 characters")
  @Column(length = 500)
  private String description;

  @NotNull(message = "Price cannot be null")
  @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
  @Digits(integer = 10, fraction = 2, message = "Price must be a valid monetary amount")
  @Column(nullable = false, precision = 12)
  private double price;

  @NotNull(message = "Stock cannot be null")
  @Min(value = 0, message = "Stock cannot be negative")
  @Column(nullable = false)
  private int stock;

  @NotBlank(message = "Category cannot be blank")
  @Size(max = 50, message = "Category must be up to 50 characters")
  @Column(nullable = false)
  private String category;

  @Column(name = "image_url")
  private String imageUrl;

  @Builder.Default
  @Column(nullable = false)
  private boolean active = true;


  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

//  metodo de negocio

  //  si el producto esta disponible
  public boolean isAvailable() {
    return active && stock > 0;
  }

  //  reducir stock
  public void reduceStock(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be greater than zero");
    }
    if (quantity > stock) {
      throw new IllegalArgumentException("Insufficient stock");
    }
//    -= significa que se le resta a stock la cantidad quantity
//     forma larga seria: this.stock = this.stock - quantity;
    this.stock -= quantity;
  }

  //  incrementar stock
  public void increaseStock(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be greater than zero");
    }
    this.stock += quantity;
  }
}
