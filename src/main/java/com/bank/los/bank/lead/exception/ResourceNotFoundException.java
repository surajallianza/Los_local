package com.example.demo.lead.exception;

/**
 * Exception thrown when a requested resource (e.g. Lead, Employee) cannot be found.
 * Mapped to HTTP 404 Not Found in GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
