package com.budgettracker.backend.exception;

/**
 * Thrown when a requested resource cannot be found.
 * Handled globally by {@link GlobalExceptionHandler} to return a 404 response.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, String id) {
        super(resourceName + " not found with id: " + id);
    }
}
