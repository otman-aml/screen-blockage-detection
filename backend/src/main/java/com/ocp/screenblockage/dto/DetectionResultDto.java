package com.ocp.screenblockage.dto;

import java.time.Instant;
import java.util.List;

/**
 * Reponse du backend pour POST /api/detections.
 *
 * <p>Contient un resume de l'analyse stockee (frame + detections).
 * Aucun etat NORMAL/WARNING/CRITICAL n'est calcule : la Phase 3 stocke
 * uniquement le resultat brut fourni par le ai-service.
 */
public record DetectionResultDto(
        Long id,
        int imageWidth,
        int imageHeight,
        double blockagePercentage,
        Instant analyzedAt,
        int numberOfDetections,
        List<DetectionSummaryDto> detections) {
}