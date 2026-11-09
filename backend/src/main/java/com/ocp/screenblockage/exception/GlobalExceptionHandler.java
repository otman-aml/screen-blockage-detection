package com.ocp.screenblockage.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;

/**
 * Gestion globale des erreurs HTTP.
 *
 * <ul>
 *   <li>image invalide / multipart invalide -> 400</li>
 *   <li>ai-service en erreur client ou reponse inutilisable -> 502</li>
 *   <li>ai-service indisponible / timeout / 5xx -> 503</li>
 *   <li>toute autre erreur -> 500</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidImage(InvalidImageException e,
                                                            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Invalid image", e.getMessage(), request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException e,
                                                           HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Missing file part",
                "The 'file' part is required.", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUpload(MaxUploadSizeExceededException e,
                                                         HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "File too large",
                "The uploaded image exceeds the maximum allowed size.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e,
                                                          HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Validation failed",
                "Invalid request parameters.", request);
    }

    @ExceptionHandler(AiServiceClientException.class)
    public ResponseEntity<ErrorResponse> handleAiClient(AiServiceClientException e,
                                                        HttpServletRequest request) {
        return build(HttpStatus.BAD_GATEWAY, "Bad Gateway", e.getMessage(), request);
    }

    @ExceptionHandler(AiServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleAiUnavailable(AiServiceUnavailableException e,
                                                             HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
                e.getMessage(), request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e,
                                                        HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Not Found", e.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e,
                                                       HttpServletRequest request) {
        LOG.error("Unexpected server error", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred.", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status,
                                                String error,
                                                String message,
                                                HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                error,
                message,
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}