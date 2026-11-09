package com.ocp.screenblockage.dto;

import com.ocp.screenblockage.entity.Event;
import com.ocp.screenblockage.model.BlockageStatus;

import java.time.Instant;

/**
 * Reponse de GET /api/events (Phase 4) : une transition d'etat historisee.
 *
 * @param id                 identifiant de l'event
 * @param from               etat avant la transition
 * @param to                 etat apres la transition
 * @param timestamp          timestamp de l'analyse ayant declenche la transition
 * @param blockagePercentage blocage de la derniere analyse au moment de la transition (image)
 * @param oversizedRocks     roches surdimensionnees de la derniere analyse
 * @param framesInWindow     nombre d'echantillons dans la fenetre
 * @param framesAboveThresholdPercentage indicateur de decision WARNING (% des frames
 *                                        de la fenetre au-dessus du seuil)
 */
public record EventDto(
        long id,
        BlockageStatus from,
        BlockageStatus to,
        Instant timestamp,
        double blockagePercentage,
        int oversizedRocks,
        int framesInWindow,
        double framesAboveThresholdPercentage) {

    public static EventDto from(Event event) {
        return new EventDto(
                event.getId(),
                event.getPreviousStatus(),
                event.getNewStatus(),
                event.getOccurredAt(),
                event.getBlockagePercentage(),
                event.getOversizedRocks(),
                event.getFramesInWindow(),
                event.getFramesAboveThresholdPercentage());
    }
}
