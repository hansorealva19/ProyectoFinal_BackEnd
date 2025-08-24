package com.ecommerce.product.service.impl;

import com.ecommerce.product.domain.Product;
import com.ecommerce.product.dto.ProductDTO;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;


/**
 * Implementacion del servicio de productos
 * <p>
 * Principios aplicados
 * - Single Responsibility Principle (SRP): La clase ProductServiceImpl tiene una única responsabilidad,
 * - Dependency Inversion Principle (DIP): La clase ProductServiceImpl depende de la interfaz ProductService,
 * - Transaction management: Las transacciones se manejan a nivel de servicio,
 * - Logging: Se utiliza logging para registrar información relevante durante la ejecución del servicio.
 * <p>
 * Patrones implementados:
 * - Service Layer Pattern: La clase ProductServiceImpl actúa como la capa de servicio que contiene la lógica de negocio relacionada con los productos.
 * - Repository Pattern: La clase ProductServiceImpl utiliza un repositorio para acceder a los datos de los productos.
 * - DTO Pattern: Aunque no se muestra aquí, se espera que la implementación utilice DTOs (Data Transfer Objects) para transferir datos entre la capa de servicio y la capa de presentación.
 *
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true) // transacciones de solo lectura por defecto
public class ProductServiceImpl implements ProductService {

  private final ProductRepository productRepository;
  private final CloudinaryImageService imageService;

  //    Crea un nuevo producto
  @Override
  @Transactional // transacción de escritura
  public ProductDTO createProduct(ProductDTO productDTO) {
    try {
      log.info("Creating product: {}", productDTO);
      // Ensure boolean flags have sensible defaults to avoid NPEs when DTO fields are missing
      if (productDTO.getActive() == null) {
        productDTO.setActive(Boolean.TRUE);
      }
      // Aquí se implementaría la lógica para convertir ProductDTO a Product y guardarlo en el repositorio
      Product product = convertToEntity(productDTO);

//      validacion de negocio
      validateProductData(product);

//      persistencia
      Product savedProduct = productRepository.save(product);

      log.info("Product created successfully: {}", savedProduct);
      return convertToDTO(savedProduct); // ahorra lineas de codigo al reutilizar el metodo convertToDTO

    } catch (Exception e) {
      log.error("Error creating product: {}", e.getMessage());
      throw new RuntimeException("Error creating product", e);
    }
  }

  @Override
  @Transactional
  public ProductDTO createProduct(ProductDTO productDTO, MultipartFile imageFile) throws Exception {
    try {
      if (imageFile != null && !imageFile.isEmpty()) {
        Map<String, Object> result = imageService.upload(imageFile, "products");
        Object url = result.get("secure_url");
        if (url != null) {
          productDTO.setImageUrl(url.toString());
        }
      }
      // default active if not provided
      if (productDTO.getActive() == null) {
        productDTO.setActive(Boolean.TRUE);
      }
      return createProduct(productDTO);
    } catch (Exception e) {
      log.error("Error creating product with image: {}", e.getMessage());
      throw e;
    }
  }

  //    Obtiene un producto por su ID
  @Override
  public Optional<ProductDTO> getProductById(Long id) {
    log.debug("Buscar producto por ID: {}", id);
//    map me permite transformar el objeto Product a ProductDTO
    return productRepository.findById(id).map(this::convertToDTO);
  }

  //  obtener todos los productos activos
  @Override
  public List<ProductDTO> getAllProducts() {
    log.debug("Obteniendo todos los productos activos");

    return productRepository.findByActiveTrue().stream()
      .map(this::convertToDTO)
//      este enfoque es mas eficiente que usar collect(Collectors.toList())
//      .collect(Collectors.toList());
//    o sino toList() es una forma mas concisa de hacerlo
      .toList();
  }

//  actualiza un producto existente

  @Override
  @Transactional // transacción de escritura
  public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
    log.info("Actualizando producto con ID: {}", id);

    return productRepository.findById(id)
      .map(existingProduct -> {
        log.debug("Producto encontrado: {}", existingProduct);
        updateProductFields(existingProduct, productDTO); // actualiza los campos del producto existente

        validateProductData(existingProduct); // valida los datos del producto, con validaciones de negocio

        Product updatedProduct = productRepository.save(existingProduct); // guarda el producto actualizado

        log.info("Producto actualizado exitosamente: {}", updatedProduct.getId());
        return convertToDTO(updatedProduct); // convierte el producto actualizado a DTO y lo devuelve
      }).orElseThrow(() -> {
        log.warn("Producto con ID {} no encontrado", id);
        return new RuntimeException("Product not found with ID: " + id);
      });
  }

  //  eliminar el producto (soft delete)
//  eliminacion logica
  @Override
  @Transactional // transacción de escritura
  public void deleteProduct(Long id) {
    log.info("Eliminando producto con ID: {}", id);

    productRepository.findById(id)
      .ifPresentOrElse(product -> {

        product.setActive(false); // marca el producto como inactivo
        productRepository.save(product); // guarda el producto actualizado
        log.info("Producto eliminado (soft delete) exitosamente: {}", id);
      }, () -> {

        log.warn("Producto con ID {} no encontrado", id);
        throw new RuntimeException("Product not found with ID: " + id);
      });
  }

  @Override
  @Transactional
  public ProductDTO updateProductImage(Long id, org.springframework.web.multipart.MultipartFile imageFile) throws Exception {
    log.info("Updating product image for ID: {}", id);

    return productRepository.findById(id)
      .map(product -> {
        try {
          if (imageFile != null && !imageFile.isEmpty()) {
            Map<String, Object> result = imageService.upload(imageFile, "products");
            Object url = result.get("secure_url");
            if (url != null) {
              product.setImageUrl(url.toString());
            }
          }
          Product saved = productRepository.save(product);
          return convertToDTO(saved);
        } catch (Exception e) {
          log.error("Error uploading image for product {}: {}", id, e.getMessage());
          throw new RuntimeException(e);
        }
      }).orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));
  }

//buscar productos por categoria

  @Override
  public List<ProductDTO> getProductsByCategory(String category) {
    log.debug("Buscando productos por categoria: {}", category);

    return productRepository.findByCategoryAndActiveTrue(category).stream()
      .map(this::convertToDTO)
      .toList();
  }

  //  buscar productos por nombre
  @Override
  public List<ProductDTO> searchProductsByName(String name) {
    log.debug("Buscando productos por nombre: {}", name);

    return productRepository.findByNameContainingIgnoreCaseAndActiveTrue(name).stream()
      .map(this::convertToDTO)
      .toList();
  }

  //  busqueda avanzada con paginacion
  @Override
  public Page<ProductDTO> searchProducts(
    String category,
    Double minPrice,
    Double maxPrice,
    String name,
    Pageable pageable
  ) {
    log.debug("Buscando productos con criterios: category={}, minPrice={}, maxPrice={}, name={}", category, minPrice, maxPrice, name);

    return productRepository.findproductsByCriteria(category, minPrice, maxPrice, name, pageable)
      .map(this::convertToDTO);
  }

  //  actualizar el stock
  @Override
  @Transactional // transacción de escritura
  public void updateStock(Long productId, int quantity) {
    log.info("Actualizando stock del producto con ID: {} por cantidad: {}", productId, quantity);

    productRepository.findById(productId)
      .ifPresentOrElse(
        product -> {
//          aqui actualizo el stock
          if (quantity > 0) {
            product.increaseStock(quantity);
          } else {
            product.reduceStock(Math.abs(quantity));
          }

          productRepository.save(product); // guarda el producto actualizado
          log.info("Stock actualizado exitosamente para el producto con ID: {}", productId);

        },
        () -> {
          log.error("Producto con ID {} no encontrado", productId);
          throw new RuntimeException("Product not found with ID: " + productId);
        }
      );
  }

  //  obtener productos con stock bajo
  @Override
  public List<ProductDTO> getLowStockProducts(int threshold) {
    log.debug("Obteniendo productos con stock bajo (umbral: {})", threshold);

    return productRepository.findLowStockProducts(threshold).stream()
      .map(this::convertToDTO)
      .toList();
  }

  //  obtener los productos mas vendidos
  @Override
  public List<ProductDTO> getTopSellingProducts() {
    log.debug("Obteniendo los productos mas vendidos");

    return productRepository.findTopSellingProducts().stream()
      .map(this::convertToDTO)
      .toList();
  }

  //  obtener las estadisticas de productos por categoria
  @Override
  public List<Object[]> getProductsCountByCategory() {
    log.debug("Obteniendo conteo de productos por categoria");

    return productRepository.countProductsByCategory();
  }

  //  getAvailableProducts
  @Override
  public List<ProductDTO> getAvailableProducts() {
    log.debug("Obteniendo productos disponibles");

    return productRepository.findByStockGreaterThanAndActiveTrue(0).stream()
      .map(this::convertToDTO)
      .toList();
  }


  //    METODOS PRIVADOS DE UTILIDAD
//  convertToDTO
  private ProductDTO convertToDTO(Product product) {
    return ProductDTO.builder()
      .id(product.getId())
      .name(product.getName())
      .description(product.getDescription())
      .price(product.getPrice())
      .stock(product.getStock())
      .category(product.getCategory())
      .imageUrl(product.getImageUrl())
      .active(product.isActive())
      .createdAt(product.getCreatedAt())
      .updatedAt(product.getUpdatedAt())
      .build();
  }

  //  convertToEntity
  private Product convertToEntity(ProductDTO productDTO) {
    return Product.builder()
      .id(productDTO.getId())
      .name(productDTO.getName())
      .description(productDTO.getDescription())
      .price(productDTO.getPrice())
      .stock(productDTO.getStock())
      .category(productDTO.getCategory())
      .imageUrl(productDTO.getImageUrl())
      .active(productDTO.getActive())
      .createdAt(productDTO.getCreatedAt())
      .updatedAt(productDTO.getUpdatedAt())
      .build();
  }

  //  actualiza los campos de un producto existente
//  la razon de este metodo es:
//  verificar que los campos del producto no sean nulos
//  y actualizar solo los campos que no son nulos
  private void updateProductFields(Product product, ProductDTO dto) {
    product.setName(dto.getName());
    product.setDescription(dto.getDescription());
    product.setPrice(dto.getPrice());
    product.setStock(dto.getStock());
    product.setCategory(dto.getCategory());
    // Only update imageUrl when DTO provides a non-empty value.
    if (dto.getImageUrl() != null && !dto.getImageUrl().isBlank()) {
      product.setImageUrl(dto.getImageUrl());
    }
    if (dto.getActive() != null) {
      product.setActive(dto.getActive());
    }
  }

  //  validacion de reglas de negocio
  private void validateProductData(Product product) {
    if (product.getPrice() <= 0) {
      throw new IllegalArgumentException("Product price must be greater than zero");
    }
    if (product.getStock() < 0) {
      throw new IllegalArgumentException("Product stock cannot be negative");
    }
  }


}
