package com.ecommerce.cart_service.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientStock(InsufficientStockException ex) {
        String detail = ex.getMessage() != null ? ex.getMessage() : "";
        return ResponseEntity.status(400).body(Map.of("message", "La cantidad seleccionada supera el stock", "detail", detail));
    }

    // Other exception mappings can be added here as needed
}
