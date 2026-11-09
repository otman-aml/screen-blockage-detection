package com.ocp.screenblockage.exception;

/**
 * Ressource demandee introuvable (ex. aucune analyse stockee).
 * Mappe a HTTP 404 par {@link GlobalExceptionHandler}.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
