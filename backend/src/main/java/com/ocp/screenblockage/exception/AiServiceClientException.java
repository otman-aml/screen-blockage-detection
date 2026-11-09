package com.ocp.screenblockage.exception;

/**
 * Levee lorsque le ai-service repond par une erreur client (HTTP 4xx)
 * ou renvoie un contenu inutilisable (JSON invalide / inattendu).
 * Mappee sur HTTP 502 Bad Gateway.
 */
public class AiServiceClientException extends RuntimeException {

    public AiServiceClientException(String message) {
        super(message);
    }

    public AiServiceClientException(String message, Throwable cause) {
        super(message, cause);
    }
}