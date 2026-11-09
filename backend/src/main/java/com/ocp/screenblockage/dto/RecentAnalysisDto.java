package com.ocp.screenblockage.dto;

import com.ocp.screenblockage.model.BlockageStatus;

import java.time.Instant;

/**
 * Resume d'une analyse recente (Phase 7) : reponse de
 * GET /api/detections/recent, utilisee par le frontend pour les KPI,
 * la courbe d'evolution du blocage et l'historique des analyses.
 *
 * <p>{@code status} est la classification de CETTE image selon les
 * seuils configures ({@code DecisionProperties}) : c'est un libelle
 * d'affichage calcule cote backend (le frontend ne recalcule jamais).
 * Il est distinct de la decision systeme NORMAL/WARNING/CRITICAL, qui
 * repose sur la fenetre glissante ({@code BlockageEvaluationService}).
 *
 * @param id                 identifiant de l'analyse
 * @param analyzedAt         timestamp de l'analyse
 * @param blockagePercentage taux de blocage de CETTE image (%)
 * @param numberOfDetections nombre de roches detectees
 * @param status             classification de l'image selon les seuils
 */
public record RecentAnalysisDto(
        long id,
        Instant analyzedAt,
        double blockagePercentage,
        long numberOfDetections,
        BlockageStatus status) {
}
