package com.udea.service_platform.modules.core.exception;

/**
 * Thrown when a request conflicts with the current state of the resource
 * (for example, cancelling an already terminal reservation). Mapped to
 * HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
