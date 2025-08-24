package com.ecommerce.cart_service.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException() { super(); }
    public InsufficientStockException(String message) { super(message); }
    public InsufficientStockException(String message, Throwable cause) { super(message, cause); }
}
