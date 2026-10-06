package com.bank.los.bank.lead.exception;

import java.util.Collections;
import java.util.List;

/**
 * Custom runtime exception thrown when one or more lead validation rules fail.
 */
public class ValidationException extends RuntimeException {

    private final List<String> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = Collections.singletonList(message);
    }

    public ValidationException(List<String> errors) {
        super(errors != null && !errors.isEmpty() ? String.join("; ", errors) : "Validation failed");
        this.errors = errors != null ? errors : Collections.emptyList();
    }

    public List<String> getErrors() {
        return errors;
    }
}
