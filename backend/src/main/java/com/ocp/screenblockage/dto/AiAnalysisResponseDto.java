package com.ocp.screenblockage.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * Miroir exact du JSON retourne par POST /analyze (ai-service).
 *
 * <p>Le JSON utilise des noms snake_case ({@code image_width}, ...). Jackson
 * deserialise grace a {@link JsonNaming}{@code (SnakeCaseStrategy.class)} sans
 * changer le contrat du ai-service.
 *
 * <p>Contrat reel (ai-service/schemas/response.py - AnalysisResponse) :
 * <pre>
 * {
 *   "image_width": 640,
 *   "image_height": 640,
 *   "blockage_percentage": 54.84,
 *   "detections": [ ... ]
 * }
 * </pre>
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AiAnalysisResponseDto(
        int imageWidth,
        int imageHeight,
        double blockagePercentage,
        List<AiDetectionDto> detections) {
}