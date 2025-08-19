package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductDTO;
import com.ecommerce.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *
 * Controlador REST para la gestión de productos
 * <p>
 * Principios aplicados:
 * - Single Responsibility Principle (SRP): Este controlador se encarga exclusivamente de manejar las solicitudes relacionadas con productos.
 * - RESTful API Design: Utiliza convenciones REST para las operaciones CRUD.
 * - Response Entity: Utiliza ResponseEntity para manejar las respuestas HTTP de manera adecuada.
 * - Validation: validación de entradas utilizando anotaciones de validación de Spring.
 * <p>
 * Patrones aplicados:
 * - Controller Pattern: Separa la lógica de negocio de la lógica de presentación.
 * - DTO Pattern: Utiliza DTOs para transferir datos entre el cliente y el servidor, evitando exponer directamente las entidades del dominio.
 * - Exception Handling: Manejo de excepciones global utilizando @ControllerAdvice.
 *
 */

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Permite solicitudes desde cualquier origen, se puede restringir a dominios específicos
public class ProductController {

  private final ProductService productService;

  //  POST /api/products
//  Principio: HTTP Post - Crear un nuevo recurso
//  crear un producto
  @PostMapping
  public ResponseEntity<ProductDTO> createProduct(
    @Valid @RequestBody ProductDTO productDTO
  ) {
    log.info("Creating product: {}", productDTO);

    try {
//      crear el producto y responder con el ResponseEntity
      ProductDTO createdProduct = productService.createProduct(productDTO);
      return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    } catch (Exception e) {
      log.error("Error creating product: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build(); // Error interno del servidor
    }
  }


  //  obtener todos los productos
//  GET /api/products
//  Principio: Paginación - Manejo eficiente de grandes volúmenes/conjunto de datos
  @GetMapping
  public ResponseEntity<Page<ProductDTO>> getAllProducts(
    @RequestParam(name = "page", defaultValue = "0") int page,
    @RequestParam(name = "size", defaultValue = "10") int size,
    @RequestParam(name = "minPrice", required = false) Double minPrice,
    @RequestParam(name = "maxPrice", required = false) Double maxPrice,
    @RequestParam(name = "name", required = false) String name,
    @RequestParam(name = "category", required = false) String category,
    @RequestParam(name = "sortDir", defaultValue = "asc") String sortDir,
    @RequestParam(name = "sortBy", defaultValue = "id") String sortBy) {
    log.info("Fetching all products - Page: {}, Size: {}", page, size);

    try {
      Sort sort = sortDir.equalsIgnoreCase("desc") ?
        Sort.by(sortBy).descending() :
        Sort.by(sortBy).ascending();
      Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, sort);
      Page<ProductDTO> products = productService.searchProducts(
        category, minPrice, maxPrice, name, pageable
      );
      return ResponseEntity.ok(products);
    } catch (Exception e) {
      log.error("Error fetching products: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  //  obtener un producto por ID
  @GetMapping("/{id}")
  public ResponseEntity<ProductDTO> getProductById(
  @PathVariable("id") Long id
  ) {
    log.info("Fetching product by ID: {}", id);

    try {
      return productService.getProductById(id)
        .map(product -> ResponseEntity.ok(product))
        .orElse(ResponseEntity.notFound().build());
    } catch (Exception e) {
      log.error("Error fetching product by ID: {}", e.getMessage());
      return ResponseEntity.notFound().build();
    }
  }

  //   actualizar un producto por ID
  @PutMapping("/{id}")
  public ResponseEntity<ProductDTO> updateProduct(
  @PathVariable("id") Long id,
    @Valid @RequestBody ProductDTO productDTO
  ) {
    log.info("Updating product with ID: {}", id);

    try {
      ProductDTO updatedProduct = productService.updateProduct(id, productDTO);
      return ResponseEntity.ok(updatedProduct);
    } catch (RuntimeException e) {
      log.error("Error updating product: {}", e.getMessage());
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Error interno al actualzar el producto: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  //  eliminar un producto por ID
//  DELETE /api/products/{id}
//
//  Principio: HTTP Delete - Eliminar un recurso
//
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteProduct(
  @PathVariable("id") Long id
  ) {
    log.info("Deleting product with ID: {}", id);
//    noContent es decir: 204 No Content
    try {
      productService.deleteProduct(id);
      return ResponseEntity.noContent().build();
    } catch (RuntimeException e) {
      log.error("Error al eliminar el producto: {}", e.getMessage());
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Error deleting product: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  //  obtener un producto por categoria
//  GET /api/products/category/{category}
//
//  PRINCIPIO: Filtrado de recursos, Resource Filtering
  @GetMapping("/category/{category}")
  public ResponseEntity<List<ProductDTO>> getProductsByCategory(
    @PathVariable String category
  ) {
    log.debug("Solicitud de productos por categoria: {}", category);

    try {
      List<ProductDTO> products = productService.getProductsByCategory(category);
      return ResponseEntity.ok(products);
    } catch (Exception e) {
      log.error("Error fetching products by category: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  //  buscar productos por nombre
//  GET /api/products/search?name={name}
//  PRINCIPIO: Search Functionality - Funcionalidad de busqueda
  @GetMapping("/search")
  public ResponseEntity<List<ProductDTO>> searchProductsByName(
    @RequestParam String name
  ) {
    log.debug("Buscando productos por nombre: {}", name);

    try {
//      invoco al servicio y devuelvo la lista de productos
      List<ProductDTO> products = productService.searchProductsByName(name);
      return ResponseEntity.ok(products);
    } catch (Exception e) {
      log.error("Error searching products by name: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  //  obtener productos disponibles
//  GET /api/products/available
//  PRINCIPIO: Business Logic Endpoint - Endpoint con lógica de negocio
  @GetMapping("/available")
  public ResponseEntity<List<ProductDTO>> getAvailableProducts() {
    log.debug("Fetching available products");

    try {
//      llamo al servicio que tiene la implementación de la lógica de negocio
      List<ProductDTO> availableProducts = productService.getAvailableProducts();
      return ResponseEntity.ok(availableProducts);
    } catch (Exception e) {
      log.error("Error fetching available products: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  //  obtener productos con stock bajo
//  GET /api/products/low-stock?threshold={threshold}
//  Principio: Endpoint de monitoreo
  @GetMapping("/low-stock")
  public ResponseEntity<List<ProductDTO>> getLowStockProducts(
    @RequestParam(name = "threshold", defaultValue = "5") int threshold
  ) {
    log.debug("Fetching low stock products with threshold: {}", threshold);

    try {
      List<ProductDTO> lowStockProducts = productService.getLowStockProducts(threshold);
      return ResponseEntity.ok(lowStockProducts);
    } catch (Exception e) {
      log.error("Error fetching low stock products: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  //  obtener productos mas vendidos
//  GET /api/products/top-selling
  public ResponseEntity<List<ProductDTO>> getTopSellingProducts() {
    log.debug("Fetching top selling products");

    try {
      List<ProductDTO> topSellingProducts = productService.getTopSellingProducts();
      return ResponseEntity.ok(topSellingProducts);
    } catch (Exception e) {
      log.error("Error fetching top selling products: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  //  actuqalizar stock
//  actualiza el stock de un producto
//  PATCH /api/products/{id}/stock
//  Principio: HTTP Patch - para actualizaciones parciales
  @PatchMapping("/{id}/stock")
//  ejemplo: /api/products/1/stock?quantity=5
  public ResponseEntity<Void> updateStock(
  @PathVariable("id") Long id,
    @RequestParam int quantity
  ) {
    log.debug("Updating stock for product ID: {}, Quantity: {}", id, quantity);

    try {
      productService.updateStock(id, quantity);
      return ResponseEntity.noContent().build();
    } catch (RuntimeException e) {
      log.error("Error updating stock: {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("Error updating stock: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  //  obtener estadisticas de productos por categoria
//  GET /api/products/stats/by-category
  @GetMapping("/stats/by-category")
  public ResponseEntity<List<Object[]>> getProductsCountByCategory() {
    log.debug("Fetching product count by category");

    try {
      List<Object[]> stats = productService.getProductsCountByCategory();
      return ResponseEntity.ok(stats);
    } catch (Exception e) {
      log.error("Error fetching product count by category: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }


}
