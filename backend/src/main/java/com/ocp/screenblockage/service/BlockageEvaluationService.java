package com.ocp.screenblockage.service;

import com.ocp.screenblockage.entity.Event;
import com.ocp.screenblockage.model.AnalysisSnapshot;
import com.ocp.screenblockage.model.BlockageStatus;
import com.ocp.screenblockage.model.DecisionState;
import com.ocp.screenblockage.repository.EventRepository;
import com.ocp.screenblockage.websocket.StatusWebSocketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Orchestration Phase 4 + notification temps reel Phase 5.
 *
 * <p>Le buffer/DecisionEngine/Events (Phase 4) sont conserves tels quels.
 * Le seul ajout Phase 5 : lors d'une TRANSITION d'etat (et uniquement a ce
 * moment), {@link SimpMessagingTemplate} publie un message minimal sur le
 * topic WebSocket {@code /topic/status}. Pas de logique metier dans le WS :
 * il ne fait que DIFFUSER une decision deja produite par le backend.
 *
 * <ul>
 *   <li>Un {@link Event} est cree UNIQUEMENT lorsqu'un changement d'etat se
 *       produit (pas a chaque frame) ;</li>
 *   <li>l'etat initial du systeme est NORMAL ;</li>
 *   <li>{@link #getCurrentState()} est une lecture pure : elle recalcule
 *       l'etat courant sans jamais creer d'event (un GET est sans effet de
 *       bord) ;</li>
 *   <li>un message WebSocket est envoye IFF l'etat change (garantit qu'une
 *       serie d'etats identiques ne genere qu'un seul message, au moment du
 *       changement initial).</li>
 * </ul>
 */
@Service
public class BlockageEvaluationService {

    private static final Logger LOG =
            LoggerFactory.getLogger(BlockageEvaluationService.class);

    /** Topic WebSocket d'abonnement des clients. */
    static final String STATUS_TOPIC = "/topic/status";

    private final SlidingWindowBuffer buffer;
    private final DecisionEngine decisionEngine;
    private final EventRepository eventRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /** Etat metier courant (persisté dans les Event, initial NORMAL). */
    private BlockageStatus currentStatus = BlockageStatus.NORMAL;

    public BlockageEvaluationService(SlidingWindowBuffer buffer,
                                      DecisionEngine decisionEngine,
                                      EventRepository eventRepository,
                                      SimpMessagingTemplate messagingTemplate) {
        this.buffer = buffer;
        this.decisionEngine = decisionEngine;
        this.eventRepository = eventRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Integre une nouvelle analyse validee (deja persistee en Phase 3),
     * recalcule l'etat et enregistre un Event + notifie le WebSocket si une
     * transition se produit.
     *
     * <p>Une analyse dont le timestamp est ANTERIEUR a la derniere analyse
     * connue (frame tardive, ex. reseau retardé) est ignoree : l'integrer
     * fausserait la logique de persistance temporelle basee sur la serie
     * consecutive se terminant a la derniere frame.
     */
    public synchronized DecisionState evaluate(AnalysisSnapshot snapshot) {
        if (isLate(snapshot)) {
            LOG.warn("Late analysis (analyzedAt={} older than latest known) "
                            + "ignored by decision engine",
                    snapshot.analyzedAt());
            return decisionEngine.evaluate(buffer.getSamples());
        }
        buffer.add(snapshot);
        DecisionState state = decisionEngine.evaluate(buffer.getSamples());

        if (state.status() != currentStatus) {
            recordTransition(snapshot, state);
        }
        return state;
    }

    private boolean isLate(AnalysisSnapshot snapshot) {
        List<AnalysisSnapshot> samples = buffer.getSamples();
        return !samples.isEmpty()
                && snapshot.analyzedAt()
                        .isBefore(samples.get(samples.size() - 1).analyzedAt());
    }

    /** Etat courant recalcule sur la fenetre (lecture sans effet de bord). */
    public synchronized DecisionState getCurrentState() {
        return decisionEngine.evaluate(buffer.getSamples());
    }

    /**
     * Persiste l'Event de transition (Phase 4) ET notifie le WebSocket (Phase 5).
     * La publication n'a lieu qu'ici, donc IFF un changement d'etat reel s'est
     * produit.
     */
    private void recordTransition(AnalysisSnapshot snapshot, DecisionState state) {
        Event event = new Event(
                currentStatus,
                state.status(),
                snapshot.analyzedAt(),
                state.indicators().currentBlockagePercentage(),
                state.indicators().oversizedRocks(),
                state.indicators().sampleCount(),
                state.indicators().framesAboveWarningThresholdPercentage());
        eventRepository.save(event);

        LOG.info("Blockage state transition: {} -> {} (blockage={}%, oversized={}, framesInWindow={})",
                currentStatus, state.status(),
                state.indicators().currentBlockagePercentage(),
                state.indicators().oversizedRocks(),
                state.indicators().sampleCount());

        publishStatus(state.status(), state.indicators().currentBlockagePercentage(),
                snapshot.analyzedAt());

        currentStatus = state.status();
    }

    /**
     * Diffuse UNIQUEMENT un changement d'etat deja decide sur /topic/status.
     * Appele exclusivement depuis {@link #recordTransition}, donc une et une
     * seule fois par transition reelle.
     */
    private void publishStatus(BlockageStatus status, double blockagePercentage,
                               Instant timestamp) {
        StatusWebSocketMessage message = new StatusWebSocketMessage(
                status, blockagePercentage, timestamp);
        LOG.debug("Publishing WebSocket status {} to {}", message, STATUS_TOPIC);
        messagingTemplate.convertAndSend(STATUS_TOPIC, message);
    }
}
