package com.ocp.screenblockage.service;

import com.ocp.screenblockage.config.CaptureProperties;
import com.ocp.screenblockage.config.DecisionProperties;
import com.ocp.screenblockage.model.AnalysisSnapshot;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Buffer glissant de la fenetre de decision (Phase 4).
 *
 * <p>Contient les analyses recentes utilisees par le moteur de decision.
 * Deux mecanismes d'eviction des anciennes frames :
 *
 * <ol>
 *   <li><strong>Temporel</strong> : une frame sort du buffer des qu'elle est
 *       anterieure a {@code derniere analyse - persistence-window-seconds}
 *       (la fenetre est donc toujours {@code [latest - window, latest]}) ;</li>
 *   <li><strong>Par capacite</strong> (garde-fou anti-saturelle) : la taille
 *       est plafonnee a {@code ceil(fenetre / capture-interval-seconds) + 2}
 *       echantillons (12 pour 300 s / 30 s), de sorte que la fenetre puisse
 *       contenir plusieurs cycles de capture completes.</li>
 * </ol>
 *
 * <p>Le buffer est <strong>en memoire</strong> : il represente la fenetre de
 * decision courante. L'historique permanent reste en PostgreSQL (entites
 * {@code AnalysisResult} / {@code Detection} de la Phase 3 + {@code Event}).
 *
 * <p>Thread-safe (methods synchronisees) : les analyses peuvent arriver
 * concurrentiellement via {@code POST /api/detections}.
 */
@Component
public class SlidingWindowBuffer {

    private final DecisionProperties decisionProperties;
    private final CaptureProperties captureProperties;

    /** Analyses triees par {@code analyzedAt} croissant. */
    private final List<AnalysisSnapshot> samples = new ArrayList<>();

    public SlidingWindowBuffer(DecisionProperties decisionProperties,
                               CaptureProperties captureProperties) {
        this.decisionProperties = decisionProperties;
        this.captureProperties = captureProperties;
    }

    /**
     * Ajoute une analyse au buffer puis applique l'eviction
     * (frames sortant de la fenetre temporelle + garde-fou de capacite).
     */
    public synchronized void add(AnalysisSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        insertSorted(snapshot);
        evict();
    }

    /** Copie defensive des echantillons courants, tries par timestamp croissant. */
    public synchronized List<AnalysisSnapshot> getSamples() {
        return List.copyOf(samples);
    }

    public synchronized int size() {
        return samples.size();
    }

    /** Capacite maximale theorique du buffer (fenetre / intervalle + marge). */
    public synchronized int getMaximumCapacity() {
        return maxSamples();
    }

    /** Vide le buffer (usage : tests, reinitialisation). */
    public synchronized void clear() {
        samples.clear();
    }

    private void insertSorted(AnalysisSnapshot snapshot) {
        // Cas nominal : les analyses arrivent dans l'ordre chronologique.
        if (samples.isEmpty()
                || !samples.get(samples.size() - 1).analyzedAt().isAfter(snapshot.analyzedAt())) {
            samples.add(snapshot);
            return;
        }
        // Analyse hors-ordre (rare) : insertion a sa place chronologique.
        int position = samples.size();
        while (position > 0
                && samples.get(position - 1).analyzedAt().isAfter(snapshot.analyzedAt())) {
            position--;
        }
        samples.add(position, snapshot);
    }

    private void evict() {
        if (samples.isEmpty()) {
            return;
        }
        Instant latest = samples.get(samples.size() - 1).analyzedAt();
        Instant cutoff = latest.minus(
                Duration.ofSeconds(decisionProperties.persistenceWindowSeconds()));
        samples.removeIf(sample -> sample.analyzedAt().isBefore(cutoff));

        int capacity = maxSamples();
        while (samples.size() > capacity) {
            samples.remove(0);
        }
    }

    private int maxSamples() {
        int windowSeconds = decisionProperties.persistenceWindowSeconds();
        int intervalSeconds = Math.max(1, captureProperties.intervalSeconds());
        return (int) Math.ceil((double) windowSeconds / intervalSeconds) + 2;
    }
}
