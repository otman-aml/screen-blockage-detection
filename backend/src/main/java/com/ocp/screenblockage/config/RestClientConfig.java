package com.ocp.screenblockage.config;

import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuration du client HTTP (RestClient) utilise pour appeler
 * le microservice IA (POST /analyze).
 *
 * <p>Les timeouts (connexion et lecture) sont configurables via
 * {@link AiServiceProperties} (application.yml).
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient aiServiceRestClient(RestClient.Builder builder,
                                          AiServiceProperties properties) {
        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(aiServiceClientHttpRequestFactory(properties))
                .build();
    }

    /**
     * Fabrique HTTP simple avec timeouts explicites.
     */
    private SimpleClientHttpRequestFactory aiServiceClientHttpRequestFactory(
            AiServiceProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofMillis(properties.connectTimeoutMs()).toMillis());
        factory.setReadTimeout((int) Duration.ofMillis(properties.readTimeoutMs()).toMillis());
        return factory;
    }
}