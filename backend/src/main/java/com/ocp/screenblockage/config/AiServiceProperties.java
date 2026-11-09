package com.ocp.screenblockage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Proprietes configurables du microservice IA.
 *
 * <p>Prefixe {@code ai-service} dans application.yml :
 * <pre>
 * ai-service:
 *   base-url: http://127.0.0.1:8000
 *   analyze-path: /analyze
 *   connect-timeout-ms: 5000
 *   read-timeout-ms: 30000
 * </pre>
 *
 * <p>L'URL du ai-service n'est donc jamais codee en dur dans le code Java.
 */
@ConfigurationProperties(prefix = "ai-service")
public record AiServiceProperties(
        String baseUrl,
        String analyzePath,
        int connectTimeoutMs,
        int readTimeoutMs) {
}