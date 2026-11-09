package com.ocp.screenblockage.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cadence de capture des frames.
 *
 * <p>Miroir de {@code ai-service/config.yaml} ({@code capture_interval_seconds:
 * 30}) : le backend ne peut pas lire la configuration du ai-service, la valeur
 * est donc declaree ici aussi et sert uniquement a dimensionner la taille
 * maximale du buffer glissant (fenetre / intervalle + marge).
 *
 * <pre>
 * capture:
 *   interval-seconds: 30
 * </pre>
 */
@Validated
@ConfigurationProperties(prefix = "capture")
public record CaptureProperties(

        /** Intervalle entre deux captures de frames, en secondes. */
        @Positive
        int intervalSeconds) {
}
