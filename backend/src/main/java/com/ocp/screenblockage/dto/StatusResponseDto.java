package com.ocp.screenblockage.dto;

import com.ocp.screenblockage.model.BlockageStatus;
import com.ocp.screenblockage.model.DecisionState;

import java.time.Instant;

/**
 * Reponse de GET /api/status (Phase 4) : etat metier courant + indicateurs
 * de la fenetre glissante. Les entites JPA ne sont jamais exposees.
 *
 * @param status                             etat metier NORMAL/WARNING/CRITICAL
 * @param blockagePercentage                 blocage de la derniere analyse (%)
 * @param framesAboveThresholdPercentage     part des frames de la fenetre
 *                                           depassant le seuil WARNING (%)
 * @param oversizedRocks                     roches surdimensionnees de la
 *                                           derniere analyse (flag du ai-service)
 * @param sampleCount                        nombre d'echantillons dans la fenetre
 * @param windowStart                        debut de la fenetre (plus ancien
 *                                           echantillon), null si vide
 * @param analyzedAt                         timestamp de la derniere analyse
 */
public record StatusResponseDto(
        BlockageStatus status,
        double blockagePercentage,
        double framesAboveThresholdPercentage,
        int oversizedRocks,
        int sampleCount,
        Instant windowStart,
        Instant analyzedAt) {

    public static StatusResponseDto from(DecisionState state) {
        return new StatusResponseDto(
                state.status(),
                state.indicators().currentBlockagePercentage(),
                state.indicators().framesAboveWarningThresholdPercentage(),
                state.indicators().oversizedRocks(),
                state.indicators().sampleCount(),
                state.indicators().windowStart(),
                state.indicators().latestAnalyzedAt());
    }
}
