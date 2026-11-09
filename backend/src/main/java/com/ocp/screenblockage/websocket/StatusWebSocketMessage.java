package com.ocp.screenblockage.websocket;

import com.ocp.screenblockage.model.BlockageStatus;

import java.time.Instant;

/**
 * DTO destine exclusivement au WebSocket (topic /topic/status).
 *
 * <p>Minimaliste : status, blockagePercentage, timestamp. C'est le backend
 * qui DECIDE (BlockageEvaluationService) ; le WebSocket ne fait que DIFFUSER
 * la decision deja produite. Aucune logique metier ici.
 */
public record StatusWebSocketMessage(
        BlockageStatus status,
        double blockagePercentage,
        Instant timestamp) {
}
