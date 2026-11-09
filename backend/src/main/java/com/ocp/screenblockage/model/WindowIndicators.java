package com.ocp.screenblockage.model;

import java.time.Instant;

/**
 * Indicateurs calcules sur la fenetre glissante (Phase 4).
 *
 * @param framesAboveWarningThresholdPercentage pourcentage des frames de la
 *                                              fenetre dont le blocage est
 *                                              >= seuil WARNING (indicateur A)
 * @param oversizedRocks                        nombre de roches surdimensionnees
 *                                              dans la DERNIERE analyse
 *                                              (indicateur B, flag du ai-service)
 * @param currentBlockagePercentage             blocage de la derniere analyse
 * @param sampleCount                           nombre d'echantillons dans la fenetre
 * @param windowStart                           timestamp du plus ancien
 *                                              echantillon de la fenetre
 *                                              (null si fenetre vide)
 * @param latestAnalyzedAt                      timestamp de la derniere analyse
 *                                              (null si fenetre vide)
 */
public record WindowIndicators(
        double framesAboveWarningThresholdPercentage,
        int oversizedRocks,
        double currentBlockagePercentage,
        int sampleCount,
        Instant windowStart,
        Instant latestAnalyzedAt) {
}
