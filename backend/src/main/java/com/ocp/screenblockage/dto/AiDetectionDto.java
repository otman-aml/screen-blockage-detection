package com.ocp.screenblockage.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * Miroir exact d'une detection du JSON retourne par POST /analyze (ai-service).
 *
 * <p>Contrat reel (ai-service/schemas/response.py - DetectionResponse) :
 * <pre>
 * {
 *   "class_id": 0,
 *   "class_name": "stone",
 *   "confidence": 0.97,
 *   "width_cm": 13.12,
 *   "height_cm": 17.70,
 *   "oversized": true,
 *   "mask": [[138.0, 16.0], [134.0, 20.0], ...]
 * }
 * </pre>
 *
 * <p>{@code mask} est un polygone (liste de paires [x, y]) et non une matrice
 * binaire ; il est conserve tel quel.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AiDetectionDto(
        int classId,
        String className,
        double confidence,
        double widthCm,
        double heightCm,
        boolean oversized,
        List<List<Double>> mask) {
}