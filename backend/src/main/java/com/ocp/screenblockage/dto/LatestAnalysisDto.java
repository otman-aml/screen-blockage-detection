package com.ocp.screenblockage.dto;

import java.time.Instant;
import java.util.List;

/**
 * Detail complet de la derniere analyse (GET /api/detections/latest).
 *
 * <p>Contrairement a {@link DetectionResultDto} (resume POST sans masque),
 * ce DTO inclut les polygones de masques afin que le frontend puisse dessiner
 * l'overlay des roches sur l'image analysee (Phase 6). Aucune decision
 * metier ici.
 */
public record LatestAnalysisDto(
        Long id,
        int imageWidth,
        int imageHeight,
        double blockagePercentage,
        Instant analyzedAt,
        int numberOfDetections,
        List<LatestDetectionDto> detections) {
}
