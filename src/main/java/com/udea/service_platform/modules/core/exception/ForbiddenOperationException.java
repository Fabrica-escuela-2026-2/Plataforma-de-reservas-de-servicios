package com.udea.service_platform.modules.core.exception;

/**
 * Thrown when an authenticated user is not allowed to perform an operation
 * and the client needs a specific explanation (not the generic
 * "Acceso denegado"). Mapped to HTTP 403 by {@link GlobalExceptionHandler}.
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
