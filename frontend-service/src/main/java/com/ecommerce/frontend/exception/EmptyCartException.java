package com.ecommerce.frontend.exception;

public class EmptyCartException extends RuntimeException {
    public EmptyCartException() { super(); }
    public EmptyCartException(String message) { super(message); }
    public EmptyCartException(String message, Throwable cause) { super(message, cause); }
}
