package com.udea.service_platform.modules.core.exception;

/**
 * Thrown when the request is valid but collides with the current state of
 * the system (e.g. a time slot that was taken by someone else). Mapped to
 * HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
