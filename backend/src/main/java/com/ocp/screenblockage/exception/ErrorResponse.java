package com.ocp.screenblockage.exception;

import java.time.Instant;

/**
 * Corps de reponse d'erreur standard renvoye par l'API.
 * Les details internes (stack traces) ne sont pas exposes.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path) {
}