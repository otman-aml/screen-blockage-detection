package com.ocp.screenblockage.service;

import com.ocp.screenblockage.config.CaptureProperties;
import com.ocp.screenblockage.config.DecisionProperties;
import com.ocp.screenblockage.model.AnalysisSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests du buffer glissant (Phase 4) : eviction temporelle, garde-fou de
 * capacite, insertion triee, coherence avec {@code capture.interval-seconds}.
 */
class SlidingWindowBufferTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    private static AnalysisSnapshot snapshot(Instant at, double blockage) {
        return new AnalysisSnapshot(at, blockage, 0);
    }

    // ------------------------------------------------------------------
    // TEST 12 : les anciennes frames sortent correctement du buffer
    // (fenetre [derniere analyse - 300 s, derniere analyse])
    // ------------------------------------------------------------------
    @Test
    void oldFramesLeaveTheWindow_asTimeMovesForward() {
        SlidingWindowBuffer buffer = new SlidingWindowBuffer(
                new DecisionProperties(25.0, 50.0, 300, 0.5),
                new CaptureProperties(30));

        // 15 frames espacees de 30 s : T0 a T0+420 s.
        for (int i = 0; i < 15; i++) {
            buffer.add(snapshot(T0.plusSeconds(30L * i), 10.0));
        }

        // Derniere analyse = T0+420 => coupure = T0+120.
        // Frames conservees : T0+120 .. T0+420 soit 11 echantillons.
        List<AnalysisSnapshot> samples = buffer.getSamples();
        assertThat(samples).hasSize(11);
        assertThat(samples.get(0).analyzedAt()).isEqualTo(T0.plusSeconds(120));
        assertThat(samples.get(samples.size() - 1).analyzedAt())
                .isEqualTo(T0.plusSeconds(420));
    }

    // ------------------------------------------------------------------
    // Garde-fou anti-saturelle : capacite maximale plafonnee
    // (15 frames dans une fenetre de 10 s avec intervalle 1 s => 12 max)
    // ------------------------------------------------------------------
    @Test
    void bufferCapacity_isCapped_evenDuringBursts() {
        SlidingWindowBuffer buffer = new SlidingWindowBuffer(
                new DecisionProperties(25.0, 50.0, 10, 0.5),
                new CaptureProperties(1));

        for (int i = 0; i < 15; i++) {
            buffer.add(snapshot(T0, 10.0));
        }

        assertThat(buffer.size()).isEqualTo(12);
    }

    // ------------------------------------------------------------------
    // Les analyses hors-ordre sont repositionnees chronologiquement
    // ------------------------------------------------------------------
    @Test
    void outOfOrderSamples_areKeptSortedByTimestamp() {
        SlidingWindowBuffer buffer = new SlidingWindowBuffer(
                new DecisionProperties(25.0, 50.0, 300, 0.5),
                new CaptureProperties(30));

        buffer.add(snapshot(T0.plusSeconds(60), 10.0));
        buffer.add(snapshot(T0.plusSeconds(30), 12.0));

        List<AnalysisSnapshot> samples = buffer.getSamples();
        assertThat(samples).hasSize(2);
        assertThat(samples.get(0).analyzedAt()).isEqualTo(T0.plusSeconds(30));
        assertThat(samples.get(0).blockagePercentage()).isEqualTo(12.0);
        assertThat(samples.get(1).analyzedAt()).isEqualTo(T0.plusSeconds(60));
    }

    // ------------------------------------------------------------------
    // getSamples retourne une copie defensive (immuable et isolee du buffer)
    // ------------------------------------------------------------------
    @Test
    void getSamples_returnsDefensiveCopy() {
        SlidingWindowBuffer buffer = new SlidingWindowBuffer(
                new DecisionProperties(25.0, 50.0, 300, 0.5),
                new CaptureProperties(30));
        buffer.add(snapshot(T0, 10.0));

        List<AnalysisSnapshot> copy = buffer.getSamples();

        // La copie est immuable : toute tentative de mutation est refusee.
        org.assertj.core.api.Assertions.assertThatThrownBy(copy::clear)
                .isInstanceOf(UnsupportedOperationException.class);
        // Le buffer interne n'est pas affecte.
        assertThat(buffer.size()).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // TEST 14 : capacite coherente avec capture_interval_seconds
    // (300 s / 30 s => 10 echantillons + marge de 2)
    // ------------------------------------------------------------------
    @Test
    void windowContainsMultipleSamples_coherentWithCaptureInterval() {
        SlidingWindowBuffer buffer = new SlidingWindowBuffer(
                new DecisionProperties(25.0, 50.0, 300, 0.5),
                new CaptureProperties(30));

        // 300 / 30 = 10 echantillons attendus ; capacite = 10 + 2 (marge).
        assertThat(buffer.getMaximumCapacity()).isEqualTo(12);
        assertThat(buffer.getMaximumCapacity()).isGreaterThanOrEqualTo(10);
    }
}
