package com.bank.los.bank.lead.exception;

/**
 * Lead-specific resource not found exception extending the common LOS ResourceNotFoundException.
 */
public class ResourceNotFoundException extends com.bank.los.common.exception.ResourceNotFoundException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(resourceName, fieldName, fieldValue);
    }
}
