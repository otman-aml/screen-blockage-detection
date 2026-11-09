package com.ocp.screenblockage.exception;

/**
 * Levee lorsque l'image envoyee a POST /api/detections est invalide
 * (absente, vide, format non supporte, trop grande).
 * Mappee sur HTTP 400.
 */
public class InvalidImageException extends RuntimeException {

    public InvalidImageException(String message) {
        super(message);
    }

    public InvalidImageException(String message, Throwable cause) {
        super(message, cause);
    }
}