package com.ocp.screenblockage.model;

import java.time.Instant;

/**
 * Vue immuable d'une analyse (une frame) telle que vue par le moteur de
 * decision Phase 4.
 *
 * <p>Objectif : decoupler la logique de decision des entites JPA (pas de
 * lazy-loading, testabilite totale). {@code oversizedRocks} est compte a
 * partir du flag {@code oversized} fourni par le ai-service (aucune logique
 * de detection de roche n'est reimplantee cote backend).
 *
 * @param analyzedAt         timestamp de l'analyse (fiable, base de la
 *                           logique de persistance temporelle)
 * @param blockagePercentage pourcentage de blocage fourni par le ai-service
 * @param oversizedRocks     nombre de roches surdimensionnees de la frame
 */
public record AnalysisSnapshot(
        Instant analyzedAt,
        double blockagePercentage,
        int oversizedRocks) {
}
