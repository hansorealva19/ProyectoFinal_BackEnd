package com.ecommerce.frontend.service;

import com.ecommerce.frontend.model.ProductViewModel;
import org.springframework.stereotype.Service;
import java.util.List;
import com.ecommerce.frontend.service.ProductRestService;

@Service
public class ProductService {
    private final ProductRestService productRestService;

    public ProductService(ProductRestService productRestService) {
        this.productRestService = productRestService;
    }

    public com.ecommerce.frontend.model.ProductPage getProducts(String name, String category, int page, int size) {
        // Forward filters to the REST client (no JWT)
        return productRestService.getAllProducts(page, size, name, category, null);
    }

    public com.ecommerce.frontend.model.ProductPage getProducts(String name, String category, int page, int size, String jwt) {
        return productRestService.getAllProducts(page, size, name, category, jwt);
    }

    /**
     * Returns a list of available category names by calling the product-service stats endpoint.
     */
    public java.util.List<String> getCategories(String jwt) {
        return productRestService.getCategories(jwt);
    }
}
