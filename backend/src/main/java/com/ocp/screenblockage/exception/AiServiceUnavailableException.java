package com.ocp.screenblockage.exception;

/**
 * Levee lorsque le ai-service est indisponible ou en erreur serveur :
 * - connexion refusee ;
 * - timeout (connexion / lecture) ;
 * - reponse HTTP 5xx.
 * Mappee sur HTTP 503 Service Unavailable.
 */
public class AiServiceUnavailableException extends RuntimeException {

    public AiServiceUnavailableException(String message) {
        super(message);
    }

    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}