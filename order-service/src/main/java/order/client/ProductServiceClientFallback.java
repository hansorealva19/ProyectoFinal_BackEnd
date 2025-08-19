package order.client;


import lombok.extern.slf4j.Slf4j;
import order.dto.ProductDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

// fallback para el cliente de product service
@Component
@Slf4j
public class ProductServiceClientFallback implements ProductServiceClient {

  @Override
  public ProductDTO getProductById(Long id) {
    log.warn("Fallback: Unable to retrieve product with ID {}", id);

    // retornar un producto por defecto o lanzar una excepcion controlada
    ProductDTO fallbackProduct = new ProductDTO();
    fallbackProduct.setId(id);
    fallbackProduct.setName("Producto no disponible");
    fallbackProduct.setPrice(0.0);
    fallbackProduct.setStock(0);
    fallbackProduct.setActive(false);

    return fallbackProduct;
  }

  @Override
  public void updateStock(Long id, int quantity) {
    log.warn("Fallback: Unable to update stock for product with ID {}", id);

//    en un escenario real esto podria enviar un evento a la cola para procesamiento
//    posterior
    throw new RuntimeException("Product Service no esta disponible para actualizar stock");
  }

  @Override
  public List<ProductDTO> availableProducts() {
    log.warn("Fallback: Unable to retrieve available products");
    return List.of(); // o lanzar una excepción personalizada
  }

}
