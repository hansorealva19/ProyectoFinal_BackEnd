package com.ecommerce.product.repository;

import com.ecommerce.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad Product
 * <p>
 * PRINCIPIOS APLICADOS
 * - Repository Pattern: Abstracción de la capa de acceso a datos
 * - Interface Segregation Principle (ISP): Define una interfaz específica para las operaciones de Product
 * - Dependency Inversion Principle (DIP): Las capas superiores dependen de esta interfaz, no de implementaciones concretas
 * <p>
 * Características:
 * - Query Methods: Métodos de consulta personalizados basados en el nombre del método
 * - Custom Queries: Definición de consultas JPQL o SQL personalizadas
 * - Named Queries: Uso de consultas predefinidas en la entidad
 *
 */

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

//  query methods

  //  buscar productos activos
  List<Product> findByActiveTrue();

  //  buscar productos por categoria
  List<Product> findByCategoryAndActiveTrue(String category);

  //  buscar productos por nombre
  List<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name);

  //  buscar productos en un rango de precios
  List<Product> findByPriceBetweenAndActiveTrue(Double minPrice, Double maxPrice);

  //  buscar productos con stock disponible
  List<Product> findByStockGreaterThanAndActiveTrue(Integer minStock);

//  Custom Query - Consultas JPQL personalizadas

  //  buscar productos por multiples criterios JPQL
  @Query("SELECT p FROM Product p WHERE " +
    "(:category IS NULL OR p.category = :category) AND " +
    "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
    "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
    "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
    "p.active = true"
  )
  Page<Product> findproductsByCriteria(
//    category
    @Param("category") String category,

//    minPrice
    @Param("minPrice") Double minPrice,
//    maxPrice
    @Param("maxPrice") Double maxPrice,
    // name
    @Param("name") String name,
    Pageable pageable
  );
//  obtener productos mas vendidos
//  Query Nativa: SQL Nativa
  @Query(value = "SELECT * FROM products p WHERE p.active = true AND p.stock < 10 ORDER BY p.stock ASC LIMIT 5", nativeQuery = true)
  List<Product> findTopSellingProducts();

  //  contar productos por categoria
//  Consulta de agregación
//  es decir: contar cuantos productos hay en cada categoria
  @Query("SELECT p.category, COUNT(p) FROM Product p WHERE p.active = true GROUP BY p.category")
  List<Object[]> countProductsByCategory();

//  buscar productos con stock bajo
//  los productos que esten por debajo de un umbral
  @Query("SELECT p FROM Product p WHERE p.stock <= :threshold AND p.active = true")
  List<Product> findLowStockProducts(
//    threshold en espanol significa: umbral
    @Param("threshold") Integer threshold
  );

//  actualizar el stock
//  Principio de operaciones atomicas
//  la lectura de este query seria:
//
//  Se actualiza el stock del producto restando la cantidad
//  donde el id del producto es igual al id del producto pasado como parametro
//  y el stock actual es mayor o igual a la cantidad a restar
  @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :productId AND p.stock >= :quantity")
  int updateStock(
    @Param("productId") Long productId,
    @Param("quantity") Integer quantity
  );

}
