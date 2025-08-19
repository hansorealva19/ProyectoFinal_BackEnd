package order.client;


// Cliente Feign para la comunicacion con Product Service

/*
 * Principios:
 * - Service Communication: Utiliza Feign para comunicarse con el servicio de productos.
 * - Circuit Breaker: Implementa un mecanismo de Circuit Breaker para manejar fallos en el servicio de productos.
 * - Load Balancing: Utiliza Spring Cloud Load Balancer para distribuir las solicitudes entre instancias del servicio de productos.
 *
 */

import order.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@FeignClient(name = "product-service", fallback = ProductServiceClientFallback.class)
public interface ProductServiceClient {

  //  obtiene un producto por ID
  @GetMapping("/api/products/{id}")
  ProductDTO getProductById(
    @PathVariable("id") Long id
  );

//  actualiza el stock de un producto
  @PatchMapping("/api/products/{id}/stock")
  void updateStock(
    @PathVariable("id") Long id,
    @RequestParam int quantity
  );

//  verifica los productos disponibles
  @GetMapping("/api/products/available")
  List<ProductDTO> availableProducts();
}

