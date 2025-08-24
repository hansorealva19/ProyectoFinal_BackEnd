
package com.ecommerce.frontend.service;

import com.ecommerce.frontend.model.ProductViewModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;

@Service
public class ProductRestService {
    private static final Logger log = LoggerFactory.getLogger(ProductRestService.class);
    @Value("${microservices.product-service.url}")
    private String productServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public com.ecommerce.frontend.model.ProductPage getAllProducts(int page, int size, String name, String category, String jwt) {
        // build URL with optional query params
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(productServiceUrl).append("/api/products?page=").append(page).append("&size=").append(size);
            if (name != null && !name.isBlank()) sb.append("&name=").append(java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8.toString()));
            if (category != null && !category.isBlank()) sb.append("&category=").append(java.net.URLEncoder.encode(category, java.nio.charset.StandardCharsets.UTF_8.toString()));
            String url = sb.toString();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (jwt != null) headers.setBearerAuth(jwt);
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.http.ResponseEntity<java.util.Map> resp = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, java.util.Map.class);
            log.info("ProductService HTTP status: {}", resp.getStatusCode());
            java.util.Map response = resp.getBody();
            if (response == null) {
                log.warn("ProductService returned empty body for URL {}", url);
                log.debug("Full response: {}", resp);
                com.ecommerce.frontend.model.ProductPage empty = new com.ecommerce.frontend.model.ProductPage();
                empty.setContent(java.util.Collections.emptyList());
                empty.setPage(0);
                empty.setTotalPages(0);
                empty.setTotalElements(0);
                return empty;
            }
            if (!response.containsKey("content")) {
                log.warn("ProductService response has no 'content' key: {}", response.keySet());
                log.debug("Full response body: {}", response);
                com.ecommerce.frontend.model.ProductPage empty = new com.ecommerce.frontend.model.ProductPage();
                empty.setContent(java.util.Collections.emptyList());
                empty.setPage(0);
                empty.setTotalPages(0);
                empty.setTotalElements(0);
                return empty;
            }
            Object content = response.get("content");
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            // Normalize possible snake_case keys (image_url) to camelCase (imageUrl) so ProductViewModel is populated
            com.fasterxml.jackson.databind.JsonNode node = mapper.valueToTree(content);
            java.util.List<ProductViewModel> products = new java.util.ArrayList<>();
            if (node.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode item : node) {
                    if (item.isObject()) {
                        com.fasterxml.jackson.databind.node.ObjectNode obj = (com.fasterxml.jackson.databind.node.ObjectNode) item;
                        if (obj.has("image_url") && !obj.has("imageUrl")) {
                            obj.set("imageUrl", obj.get("image_url"));
                        }
                        ProductViewModel p = mapper.treeToValue(obj, ProductViewModel.class);
                        products.add(p);
                    }
                }
            }
            com.ecommerce.frontend.model.ProductPage pageObj = new com.ecommerce.frontend.model.ProductPage();
            pageObj.setContent(products);
            // try to read pageable metadata from the response
            Object number = response.get("number");
            Object totalPages = response.get("totalPages");
            Object totalElements = response.get("totalElements");
            pageObj.setPage(number instanceof Number ? ((Number) number).intValue() : 0);
            pageObj.setTotalPages(totalPages instanceof Number ? ((Number) totalPages).intValue() : (products.isEmpty() ? 0 : 1));
            pageObj.setTotalElements(totalElements instanceof Number ? ((Number) totalElements).longValue() : products.size());
            return pageObj;
        } catch (Exception ex) {
            log.error("Error al obtener productos desde ProductService: {}", ex.toString(), ex);
            throw new RuntimeException("Error al obtener productos", ex);
        }
    }

    public ProductViewModel getProductById(Long id, String jwt) {
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (jwt != null) headers.setBearerAuth(jwt);
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.http.ResponseEntity<ProductViewModel> resp = restTemplate.exchange(productServiceUrl + "/api/products/" + id, org.springframework.http.HttpMethod.GET, entity, ProductViewModel.class);
            return resp.getBody();
        } catch (Exception ex) {
            throw new RuntimeException("Error al obtener producto por id", ex);
        }
    }

    public ProductViewModel updateProduct(Long id, ProductViewModel dto, String jwt) {
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            if (jwt != null) headers.setBearerAuth(jwt);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String body = mapper.writeValueAsString(dto);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(body, headers);
            org.springframework.http.ResponseEntity<ProductViewModel> resp = restTemplate.exchange(productServiceUrl + "/api/products/" + id, org.springframework.http.HttpMethod.PUT, entity, ProductViewModel.class);
            return resp.getBody();
        } catch (Exception ex) {
            throw new RuntimeException("Error updating product", ex);
        }
    }

    public ProductViewModel updateProductImage(Long id, org.springframework.web.multipart.MultipartFile image, String jwt) {
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (jwt != null) headers.setBearerAuth(jwt);
            headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

            org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();

            // ByteArrayResource to preserve filename
            org.springframework.core.io.ByteArrayResource bar = new org.springframework.core.io.ByteArrayResource(image.getBytes()){
                @Override
                public String getFilename() { return image.getOriginalFilename(); }
            };
            body.add("image", bar);

            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, Object>> requestEntity = new org.springframework.http.HttpEntity<>(body, headers);

            org.springframework.http.ResponseEntity<ProductViewModel> resp = restTemplate.exchange(productServiceUrl + "/api/products/" + id + "/image", org.springframework.http.HttpMethod.PUT, requestEntity, ProductViewModel.class);
            return resp.getBody();
        } catch (Exception ex) {
            throw new RuntimeException("Error updating product image", ex);
        }
    }

    public ProductViewModel createProduct(java.util.Map<String, Object> productFields, org.springframework.web.multipart.MultipartFile image, String jwt) {
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (jwt != null) headers.setBearerAuth(jwt);
            headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

            org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();

            // product JSON as part named "product"
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String productJson = mapper.writeValueAsString(productFields);

            // Attach the JSON product part with application/json content type so @RequestPart("product") is parsed correctly
            org.springframework.http.HttpHeaders prodPartHeaders = new org.springframework.http.HttpHeaders();
            prodPartHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> productPart = new org.springframework.http.HttpEntity<>(productJson, prodPartHeaders);
            body.add("product", productPart);

            if (image != null && !image.isEmpty()) {
                org.springframework.core.io.ByteArrayResource bar = new org.springframework.core.io.ByteArrayResource(image.getBytes()){
                    @Override
                    public String getFilename() { return image.getOriginalFilename(); }
                };
                org.springframework.http.HttpHeaders imgPartHeaders = new org.springframework.http.HttpHeaders();
                try {
                    if (image.getContentType() != null) imgPartHeaders.setContentType(org.springframework.http.MediaType.parseMediaType(image.getContentType()));
                } catch (Exception ignored) { }
                org.springframework.http.HttpEntity<org.springframework.core.io.ByteArrayResource> imagePart = new org.springframework.http.HttpEntity<>(bar, imgPartHeaders);
                body.add("image", imagePart);
            }

            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, Object>> requestEntity = new org.springframework.http.HttpEntity<>(body, headers);
            org.springframework.http.ResponseEntity<ProductViewModel> resp = restTemplate.postForEntity(productServiceUrl + "/api/products", requestEntity, ProductViewModel.class);
            return resp.getBody();
        } catch (Exception ex) {
            throw new RuntimeException("Error creating product", ex);
        }
    }

    /**
     * Calls the product-service stats endpoint to obtain counts per category and
     * returns a simple list of category names.
     */
    public java.util.List<String> getCategories(String jwt) {
        try {
            String url = productServiceUrl + "/api/products/stats/by-category";
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (jwt != null) headers.setBearerAuth(jwt);
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.http.ResponseEntity<java.util.List> resp = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, java.util.List.class);
            java.util.List body = resp.getBody();
            if (body == null) return java.util.Collections.emptyList();
            java.util.List<String> categories = new java.util.ArrayList<>();
            for (Object o : body) {
                if (o instanceof java.util.List) {
                    java.util.List row = (java.util.List) o;
                    if (!row.isEmpty() && row.get(0) != null) {
                        categories.add(String.valueOf(row.get(0)));
                    }
                }
            }
            return categories;
        } catch (Exception ex) {
            log.error("Error al obtener categorias desde ProductService: {}", ex.toString(), ex);
            return java.util.Collections.emptyList();
        }
    }
}
