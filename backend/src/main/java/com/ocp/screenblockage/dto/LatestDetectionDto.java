package com.ocp.screenblockage.dto;

import java.util.List;

/**
 * Detail d'une detection pour GET /api/detections/latest, incluant le
 * polygone du masque (liste de paires [x, y]) fourni par le ai-service.
 */
public record LatestDetectionDto(
        Long id,
        int classId,
        String className,
        double confidence,
        double widthCm,
        double heightCm,
        boolean oversized,
        List<List<Double>> mask) {
}
