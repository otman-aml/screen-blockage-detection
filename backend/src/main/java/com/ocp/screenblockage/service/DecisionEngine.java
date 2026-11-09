package com.ocp.screenblockage.service;

import com.ocp.screenblockage.config.CaptureProperties;
import com.ocp.screenblockage.config.DecisionProperties;
import com.ocp.screenblockage.model.AnalysisSnapshot;
import com.ocp.screenblockage.model.BlockageStatus;
import com.ocp.screenblockage.model.DecisionState;
import com.ocp.screenblockage.model.WindowIndicators;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

/**
 * Moteur de decision pur (Phase 4) : transforme le contenu de la fenetre
 * glissante en un etat metier NORMAL / WARNING / CRITICAL.
 *
 * <p><strong>Classe volontairement sans etat et sans dependances techniques</strong>
 * (pas d'HTTP, pas de JPA) : entierement testable sur des sequences fixes.
 *
 * <h2>Regles</h2>
 * <ul>
 *   <li>Une frame isolee ne produit JAMAIS de decision autre que NORMAL :
 *       CRITICAL exige une duree de maintien (timestamps reels), WARNING exige
 *       au moins {@link #MIN_SAMPLES_FOR_DECISION} echantillons.</li>
 *   <li>CRITICAL : la serie CONSECUTIVE de frames strictement au-dessus du
 *       seuil critique se terminant a la derniere frame couvre une duree
 *       &gt;= {@code persistence-window-seconds} (duree mesuree sur les
 *       timestamps reels {@code analyzedAt}, tolérante aux intervalles
 *       irreguliers).</li>
 *   <li>WARNING (a defaut) : au moins {@code warning-min-frames-ratio} des
 *       frames de la fenetre depassent le seuil warning (et au moins 2
 *       echantillons).</li>
 *   <li>NORMAL : sinon.</li>
 * </ul>
 *
 * <p>Une frame &laquo; depasse &raquo; un seuil si son pourcentage de blocage
 * est STRICTEMENT superieur au seuil. Les roches surdimensionnees (flag du
 * ai-service) ne declenchent JAMAIS d'etat : elles sont uniquement exposees
 * comme indicateur.
 */
@Component
public class DecisionEngine {

    /**
     * Invariant architectural : une decision ne se base JAMAIS sur une frame
     * isolee (impose par la roadmap). Ce n'est pas un seuil metier : il n'est
     * pas configurable.
     */
    public static final int MIN_SAMPLES_FOR_DECISION = 2;

    private final DecisionProperties properties;
    private final CaptureProperties captureProperties;

    public DecisionEngine(DecisionProperties properties,
                          CaptureProperties captureProperties) {
        if (properties.criticalBlockagePercentage()
                <= properties.warningBlockagePercentage()) {
            throw new IllegalArgumentException(
                    "critical-blockage-percentage must be strictly greater than "
                            + "warning-blockage-percentage");
        }
        if (properties.persistenceWindowSeconds()
                <= captureProperties.intervalSeconds()) {
            throw new IllegalArgumentException(
                    "persistence-window-seconds must be strictly greater than "
                            + "capture.interval-seconds (otherwise the window would "
                            + "hold a single sample and the persistence logic would "
                            + "be statistically meaningless)");
        }
        this.properties = properties;
        this.captureProperties = captureProperties;
    }

    /**
     * Evalue l'etat metier a partir des echantillons de la fenetre
     * (tries chronologiquement, peu importe l'ordre d'entree).
     */
    public DecisionState evaluate(List<AnalysisSnapshot> samples) {
        List<AnalysisSnapshot> sorted = samples.stream()
                .sorted(Comparator.comparing(AnalysisSnapshot::analyzedAt))
                .toList();

        if (sorted.isEmpty()) {
            return new DecisionState(BlockageStatus.NORMAL,
                    new WindowIndicators(0.0, 0, 0.0, 0, null, null));
        }

        AnalysisSnapshot latest = sorted.get(sorted.size() - 1);
        long framesAboveWarning = sorted.stream()
                .filter(s -> s.blockagePercentage() > properties.warningBlockagePercentage())
                .count();
        double framesAboveWarningPercentage =
                framesAboveWarning * 100.0 / sorted.size();

        WindowIndicators indicators = new WindowIndicators(
                framesAboveWarningPercentage,
                latest.oversizedRocks(),
                latest.blockagePercentage(),
                sorted.size(),
                sorted.get(0).analyzedAt(),
                latest.analyzedAt());

        if (isCriticalSustained(sorted, latest)) {
            return new DecisionState(BlockageStatus.CRITICAL, indicators);
        }
        if (sorted.size() >= MIN_SAMPLES_FOR_DECISION
                && framesAboveWarningPercentage
                        >= properties.warningMinFramesRatio() * 100.0) {
            return new DecisionState(BlockageStatus.WARNING, indicators);
        }
        return new DecisionState(BlockageStatus.NORMAL, indicators);
    }

    /**
     * CRITICAL uniquement si la serie CONSECUTIVE de frames strictement
     * au-dessus du seuil critique (serie se terminant a la derniere frame)
     * contient au moins {@link #MIN_SAMPLES_FOR_DECISION} echantillons ET
     * couvre une duree d'au moins {@code persistence-window-seconds} mesuree
     * sur les timestamps reels des analyses.
     *
     * <p><strong>Tolerance d'un intervalle de capture</strong> : la duree
     * minimale exige est {@code persistence-window-seconds -
     * capture.interval-seconds}. Justification : la fenetre glissante ne
     * conserve que les echantillons de la derniere
     * {@code persistence-window-seconds} secondes, et les analyses n'arrivent
     * pas a intervalle parfaitement regulier — l'echantillon frontierre de la
     * serie peut donc etre espace (ou evince) de jusqu'a un intervalle de
     * capture. Sans cette tolerance, l'etat CRITICAL serait pratiquement
     * inatteignable en temps reel (constat de test manuel) tout en restant
     * atteignable sur une grille parfaitement reguliere. La tolerance reste
     * strictement inferieure a un cycle de capture et ne permet JAMAIS de
     * declarer CRITICAL sur une frame isolee (garde-fou
     * {@link #MIN_SAMPLES_FOR_DECISION}).
     */
    private boolean isCriticalSustained(List<AnalysisSnapshot> sorted,
                                        AnalysisSnapshot latest) {
        if (sorted.size() < MIN_SAMPLES_FOR_DECISION) {
            // Invariant : une frame isolee ne produit JAMAIS CRITICAL.
            return false;
        }
        int index = sorted.size() - 1;
        while (index >= 0
                && sorted.get(index).blockagePercentage()
                        > properties.criticalBlockagePercentage()) {
            index--;
        }
        int criticalRunStart = index + 1;
        // La serie doit contenir au moins MIN_SAMPLES_FOR_DECISION frames.
        if (sorted.size() - criticalRunStart < MIN_SAMPLES_FOR_DECISION) {
            return false;
        }
        Duration sustained = Duration.between(
                sorted.get(criticalRunStart).analyzedAt(), latest.analyzedAt());
        Duration required = Duration.ofSeconds(
                properties.persistenceWindowSeconds()
                        - captureProperties.intervalSeconds());
        return sustained.compareTo(required) >= 0;
    }
}
