package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz del servicio del producto
 * <p>
 * Principios aplicados
 * - Interface Segregation Principle (ISP): Define una interfaz específica para el servicio de productos, evitando la implementación de métodos innecesarios.
 * - Dependency Inversion Principle (DIP): Permite que las implementaciones del servicio depend
 * - Single Responsibility Principle (SRP): La interfaz ProductService tiene una única responsabilidad, que es definir las operaciones relacionadas con los productos.
 *
 */

public interface ProductService {
  //  Operaciones CRUD
  ProductDTO createProduct(ProductDTO productDTO);
  ProductDTO createProduct(ProductDTO productDTO, org.springframework.web.multipart.MultipartFile imageFile) throws Exception;
  // update product image by id
  ProductDTO updateProductImage(Long id, org.springframework.web.multipart.MultipartFile imageFile) throws Exception;
  Optional<ProductDTO> getProductById(Long id);
  //  getAllProducts
  List<ProductDTO> getAllProducts();
  //  updateProduct
  ProductDTO updateProduct(Long id, ProductDTO productDTO);
  //  deleteProduct
  void deleteProduct(Long id);
//  Operqaciones de busqueda
  //  getProductsByCategory
  List<ProductDTO> getProductsByCategory(String category);
  //  searchProductsByName
  List<ProductDTO> searchProductsByName(String name);
  //  searchProducts
  Page<ProductDTO> searchProducts(String category, Double minPrice, Double maxPrice, String name, Pageable pageable);
  //  operaciones de stock
//  updateStock
  void updateStock(Long productId, int quantity);
  //  getLowStockProducts
  List<ProductDTO> getLowStockProducts(int threshold);
  //  operaciones de negocio
//  getAvailableProducts
  List<ProductDTO> getAvailableProducts();
  //  getTopSellingProducts
  List<ProductDTO> getTopSellingProducts();
  //  getProductsCountByCategory
  List<Object[]> getProductsCountByCategory();
}
