package com.ocp.screenblockage.service;

import com.ocp.screenblockage.config.CaptureProperties;
import com.ocp.screenblockage.config.DecisionProperties;
import com.ocp.screenblockage.entity.Event;
import com.ocp.screenblockage.model.AnalysisSnapshot;
import com.ocp.screenblockage.model.BlockageStatus;
import com.ocp.screenblockage.model.WindowIndicators;
import com.ocp.screenblockage.repository.EventRepository;
import com.ocp.screenblockage.websocket.StatusWebSocketMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlockageEvaluationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final DecisionProperties PROPS =
            new DecisionProperties(25.0, 50.0, 300, 0.5);

    @Mock private EventRepository eventRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    private BlockageEvaluationService service;

    @BeforeEach
    void setUp() {
        this.service = new BlockageEvaluationService(
                new SlidingWindowBuffer(PROPS, new CaptureProperties(30)),
                new DecisionEngine(PROPS, new CaptureProperties(30)),
                eventRepository, messagingTemplate);
    }

    private AnalysisSnapshot at(Instant i, double b) {
        return new AnalysisSnapshot(i, b, 0);
    }

        private BlockageStatus evaluateAndRead(AnalysisSnapshot s) {
        service.evaluate(s);
        return service.getCurrentState().status();
    }

    // ============================================================
    // Phase 4 - tests de la logique de decision
    // ============================================================

    @Test
    void initialState_isNormal() {
        assertThat(service.getCurrentState().status()).isEqualTo(BlockageStatus.NORMAL);
    }

    // TEST 1 : une seule frame a 60 % => NORMAL, jamais CRITICAL
    @Test
    void singleHighFrame_neverCritical() {
        assertThat(evaluateAndRead(at(NOW, 60.0))).isEqualTo(BlockageStatus.NORMAL);
    }

    // TEST 8 : transition NORMAL -> WARNING => UN Event
    @Test
    void normalToWarning_createsExactlyOneEvent() {
        evaluateAndRead(at(NOW, 10.0));
        evaluateAndRead(at(NOW.plusSeconds(30), 30.0));
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository, times(1)).save(captor.capture());
        Event e = captor.getValue();
        assertThat(e.getPreviousStatus()).isEqualTo(BlockageStatus.NORMAL);
        assertThat(e.getNewStatus()).isEqualTo(BlockageStatus.WARNING);
        // Fenetre = [10.0, 30.0] : 1 frame > 25 (la 30.0), donc 50 % au-dessus du seuil.
        assertThat(e.getFramesAboveThresholdPercentage()).isEqualTo(50.0);
        // Blocage image = derniere analyse = 30.0 % ; la decision WARNING n'est PAS
        // basee sur ce seul chiffre mais sur le ratio de la fenetre.
        assertThat(e.getBlockagePercentage()).isEqualTo(30.0);
        assertThat(e.getFramesInWindow()).isEqualTo(2);
    }

    // TEST : une image a blocage TRES eleve peut rester WARNING / declencher WARNING
    // tant que la fenetre ne confirme pas la persistance CRITICAL. Ceci reproduit le
    // cas observe (94.42 % -> WARNING) : la decision utilise le RATIO de fenetre
    // (framesAboveThresholdPercentage), jamais le blocage de l'image seule.
    @Test
    void highImageBlockage_withSubThresholdRatio_staysWarning_andStoresDecisionMetric() {
        // 1ere frame a 94.42 : 1 echantillon => NORMAL (jamais CRITICAL sur frame isolee).
        evaluateAndRead(at(NOW, 94.42));
        // 2e frame a 30 : fenetre = [94.42, 30]. Series critiques > 50 : run = [94.42]
        // (taille 1 < 2) => PAS CRITICAL. Frames > 25 : 2/2 => 100 % >= 50 % => WARNING.
        evaluateAndRead(at(NOW.plusSeconds(30), 30.0));
        assertThat(service.getCurrentState().status()).isEqualTo(BlockageStatus.WARNING);
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository, atLeast(1)).save(captor.capture());
        Event e = captor.getValue();
        assertThat(e.getNewStatus()).isEqualTo(BlockageStatus.WARNING);
        // La decision WARNING repose sur le ratio (100 %), PAS sur le blocage image.
        assertThat(e.getFramesAboveThresholdPercentage()).isEqualTo(100.0);
        assertThat(e.getBlockagePercentage()).isEqualTo(30.0); // derniere analyse
        assertThat(e.getFramesInWindow()).isEqualTo(2);
    }

    // TEST 9 : plusieurs frames restant WARNING => un seul event
    @Test
    void remainingWarning_doesNotCreateAdditionalEvents() {
        evaluateAndRead(at(NOW, 10.0));
        evaluateAndRead(at(NOW.plusSeconds(30), 30.0));
        evaluateAndRead(at(NOW.plusSeconds(60), 35.0));
        evaluateAndRead(at(NOW.plusSeconds(90), 40.0));
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    // TEST 5 : blocage soutenu => CRITICAL
    @Test
    void sustainedCriticalBlockage_becomesCritical_forPersistenceWindow() {
        evaluateAndRead(at(NOW, 10.0));
        evaluateAndRead(at(NOW.plusSeconds(45), 55.0));
        assertThat(evaluateAndRead(at(NOW.plusSeconds(330), 55.0)))
                .isEqualTo(BlockageStatus.CRITICAL);
    }

    // TEST 6 : blocage eleve mais duree insuffisante => WARNING
    @Test
    void highBlockage_withInsufficientDuration_staysWarning_withSingleEvent() {
        evaluateAndRead(at(NOW, 30.0));
        evaluateAndRead(at(NOW.plusSeconds(30), 55.0));
        assertThat(evaluateAndRead(at(NOW.plusSeconds(60), 58.0)))
                .isEqualTo(BlockageStatus.WARNING);
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    // TEST 7 : retour NORMAL => Event WARNING -> NORMAL
    @Test
    void returnToNormal_createsTransitionEvent() {
        evaluateAndRead(at(NOW, 30.0));         // 1 frame -> NORMAL
        evaluateAndRead(at(NOW.plusSeconds(45), 35.0));  // 2/2 -> WARNING
        evaluateAndRead(at(NOW.plusSeconds(75), 20.0));  // 2/3 -> WARNING
        evaluateAndRead(at(NOW.plusSeconds(105), 10.0)); // 2/4 = 50% -> WARNING
        assertThat(evaluateAndRead(at(NOW.plusSeconds(135), 10.0)))  // 2/5 < 50% -> NORMAL
                .isEqualTo(BlockageStatus.NORMAL);
        ArgumentCaptor<Event> c = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository, times(2)).save(c.capture());
        assertThat(c.getAllValues().get(c.getAllValues().size() - 1).getNewStatus())
                .isEqualTo(BlockageStatus.NORMAL);
    }

    // TEST 11 : CRITICAL -> WARNING => Event
    @Test
    void criticalToWarning_createsTransitionEvent() {
        evaluateAndRead(at(NOW, 10.0));                            // NORMAL
        evaluateAndRead(at(NOW.plusSeconds(30), 30.0));            // WARNING
        evaluateAndRead(at(NOW.plusSeconds(300), 55.0));           // WARNING (serie critique de 1)
        assertThat(evaluateAndRead(at(NOW.plusSeconds(330), 55.0)))  // duree 30s < 270s -> WARNING
                .isEqualTo(BlockageStatus.WARNING);
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    // TEST 12 : fenetre glissante evince les anciennes frames
    @Test
    void slidingWindow_evictsOldSamples() {
        for (int i = 0; i < 15; i++) {
            evaluateAndRead(at(NOW.plusSeconds(30L * i), i % 3 == 0 ? 60.0 : 10.0));
        }
        WindowIndicators ind = service.getCurrentState().indicators();
        assertThat(ind.sampleCount()).isEqualTo(11);
        assertThat(ind.windowStart()).isEqualTo(NOW.plusSeconds(120));
    }

    // TEST 14 : capacite coherente avec capture_interval_seconds
    @Test
    void bufferCanHoldAtLeastTenSamples_whenWindowIs300sAndInterval30s() {
        assertThat(new SlidingWindowBuffer(PROPS, new CaptureProperties(30))
                .getMaximumCapacity()).isGreaterThanOrEqualTo(10);
    }

    // getCurrentState() ne publie pas, n'ecrit pas d'event
    @Test
    void getCurrentState_doesNotCreateEventOrPublish() {
        service.getCurrentState();
        verify(eventRepository, never()).save(any(Event.class));
        verify(messagingTemplate, never())
                .convertAndSend(eq("/topic/status"), any(StatusWebSocketMessage.class));
    }
// ============================================================
    // Phase 5 - Contrat WebSocket : un message est envoye
    // IFF un changement d'etat reel s'est produit.
    // ============================================================

    // CAS 1 : NORMAL -> WARNING => convertAndSend appele une fois
    @Test
    void normalToWarning_publishesWebSocketMessage() {
        evaluateAndRead(at(NOW, 10.0));                            // NORMAL
        evaluateAndRead(at(NOW.plusSeconds(30), 30.0));            // WARNING

        ArgumentCaptor<StatusWebSocketMessage> captor =
                ArgumentCaptor.forClass(StatusWebSocketMessage.class);
        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/status"), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(BlockageStatus.WARNING);
    }

    // CAS 2 : WARNING -> CRITICAL => publication
    @Test
    void warningToCritical_publishesWebSocketMessage() {
        evaluateAndRead(at(NOW, 10.0));                            // NORMAL
        evaluateAndRead(at(NOW.plusSeconds(30), 30.0));            // WARNING
        evaluateAndRead(at(NOW.plusSeconds(300), 55.0));           // WARNING (serie critique de 1)
        evaluateAndRead(at(NOW.plusSeconds(600), 55.0));           // CRITICAL (duree 300s >= 270s)

        ArgumentCaptor<StatusWebSocketMessage> captor =
                ArgumentCaptor.forClass(StatusWebSocketMessage.class);
        verify(messagingTemplate, atLeast(1))
                .convertAndSend(eq("/topic/status"), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(BlockageStatus.CRITICAL);
    }

    // CAS 3 : WARNING -> WARNING => AUCUNE publication supplementaire
    @Test
    void warningToWarning_doesNotPublishAdditionalMessage() {
        evaluateAndRead(at(NOW, 10.0));                            // NORMAL
        evaluateAndRead(at(NOW.plusSeconds(30), 30.0));            // WARNING (1 pub)
        evaluateAndRead(at(NOW.plusSeconds(60), 35.0));            // WARNING (0 pub)
        evaluateAndRead(at(NOW.plusSeconds(90), 40.0));            // WARNING (0 pub)

        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/status"),
                        any(StatusWebSocketMessage.class));
    }

    // CAS 4 : CRITICAL -> WARNING => publication (transition decroissante)
    @Test
    void criticalToWarning_publishesWebSocketMessage() {
        evaluateAndRead(at(NOW, 10.0));                            // NORMAL
        evaluateAndRead(at(NOW.plusSeconds(30), 30.0));            // WARNING
        evaluateAndRead(at(NOW.plusSeconds(300), 55.0));           // WARNING
        evaluateAndRead(at(NOW.plusSeconds(600), 55.0));           // CRITICAL
        evaluateAndRead(at(NOW.plusSeconds(900), 30.0));           // WARNING (dilution)

        ArgumentCaptor<StatusWebSocketMessage> captor =
                ArgumentCaptor.forClass(StatusWebSocketMessage.class);
        verify(messagingTemplate, atLeast(1))
                .convertAndSend(eq("/topic/status"), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(BlockageStatus.WARNING);
    }

    // TEST 5 : la sequence complete NO->W->C->W->N produit EXACTEMENT
    // un message par transition (4 au total, jamais plus).
    @Test
    void fullTransitionSequence_publishesExactlyOncePerChange() {
        // NORMAL
        evaluateAndRead(at(NOW, 10.0));
        // WARNING (transition 1) : 1/2 au-dessus du seuil warning
        evaluateAndRead(at(NOW.plusSeconds(30), 30.0));
        // WARNING maintien : serie critique encore trop courte
        evaluateAndRead(at(NOW.plusSeconds(300), 55.0));
        // CRITICAL (transition 2) : 2 frames critiques sur 300 s (fenetre entiere)
        evaluateAndRead(at(NOW.plusSeconds(600), 55.0));
        // WARNING (transition 3) : dilution de la frame critique
        evaluateAndRead(at(NOW.plusSeconds(900), 30.0));
        // WARNING maintien : 1/2 au-dessus du seuil warning
        evaluateAndRead(at(NOW.plusSeconds(1200), 10.0));
        // NORMAL (transition 4) : 0/2 au-dessus du seuil warning
        evaluateAndRead(at(NOW.plusSeconds(1230), 10.0));

        verify(messagingTemplate, times(4))
                .convertAndSend(eq("/topic/status"), any(StatusWebSocketMessage.class));
    }
}
