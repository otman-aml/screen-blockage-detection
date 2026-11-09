package com.ocp.screenblockage.service;

import com.ocp.screenblockage.config.CaptureProperties;
import com.ocp.screenblockage.config.DecisionProperties;
import com.ocp.screenblockage.model.AnalysisSnapshot;
import com.ocp.screenblockage.model.BlockageStatus;
import com.ocp.screenblockage.model.DecisionState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests du moteur de decision pur (Phase 4) sur des sequences fixes.
 * Verifie notamment : une frame isolee ne produit JAMAIS CRITICAL, la
 * persistance temporelle est requise pour CRITICAL, les seuils viennent
 * de la configuration (aucune valeur codee en dur).
 */
class DecisionEngineTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    private static final DecisionProperties DEFAULT_PROPS =
            new DecisionProperties(25.0, 50.0, 300, 0.5);

    private static final CaptureProperties CAPTURE_30 = new CaptureProperties(30);

    private final DecisionEngine engine =
            new DecisionEngine(DEFAULT_PROPS, CAPTURE_30);

    private static AnalysisSnapshot snapshot(Instant at, double blockage, int oversized) {
        return new AnalysisSnapshot(at, blockage, oversized);
    }

    private static DecisionState evaluate(DecisionEngine target, AnalysisSnapshot... snapshots) {
        return target.evaluate(List.of(snapshots));
    }

    // TEST 1 : une seule frame a 60 % => NORMAL, jamais CRITICAL
    @Test
    void singleHighFrame_neverCritical_staysNormal() {
        DecisionState state = evaluate(engine, snapshot(T0, 60.0, 2));

        assertThat(state.status()).isEqualTo(BlockageStatus.NORMAL);
    }

    // TEST 2 : une seule frame avec roches oversized => pas de CRITICAL
    @Test
    void singleFrameWithOversizedRocks_doesNotTriggerCritical() {
        DecisionState state = evaluate(engine, snapshot(T0, 10.0, 7));

        assertThat(state.status()).isEqualTo(BlockageStatus.NORMAL);
        assertThat(state.indicators().oversizedRocks()).isEqualTo(7);
    }

    // TEST 3 : plusieurs frames faibles => NORMAL
    @Test
    void severalLowFrames_stayNormal() {
        DecisionState state = evaluate(engine,
                snapshot(T0, 10.0, 0),
                snapshot(T0.plusSeconds(30), 12.0, 0),
                snapshot(T0.plusSeconds(60), 15.0, 0),
                snapshot(T0.plusSeconds(90), 18.0, 0));

        assertThat(state.status()).isEqualTo(BlockageStatus.NORMAL);
    }

    // TEST 4 : plusieurs frames au-dessus du seuil WARNING => WARNING
    @Test
    void severalFramesAboveWarningTrigger_warning() {
        DecisionState state = evaluate(engine,
                snapshot(T0, 30.0, 0),
                snapshot(T0.plusSeconds(30), 40.0, 0),
                snapshot(T0.plusSeconds(60), 35.0, 0),
                snapshot(T0.plusSeconds(90), 28.0, 0),
                snapshot(T0.plusSeconds(120), 32.0, 0));

        assertThat(state.status()).isEqualTo(BlockageStatus.WARNING);
    }

    // TEST 5 : blocage soutenu au-dessus du seuil CRITICAL pendant
    // la duree de persistance => CRITICAL
    @Test
    void sustainedCriticalBlockage_forFullPersistenceWindow_triggersCritical() {
        DecisionState state = evaluate(engine,
                snapshot(T0, 55.0, 0),
                snapshot(T0.plusSeconds(100), 58.0, 0),
                snapshot(T0.plusSeconds(200), 61.0, 0),
                snapshot(T0.plusSeconds(300), 57.0, 0));

        assertThat(state.status()).isEqualTo(BlockageStatus.CRITICAL);
    }

    // TEST 6 : blocage eleve mais duree insuffisante => pas de CRITICAL
    @Test
    void highBlockage_withInsufficientDuration_isNotCritical() {
        DecisionState state = evaluate(engine,
                snapshot(T0, 55.0, 0),
                snapshot(T0.plusSeconds(100), 58.0, 0),
                snapshot(T0.plusSeconds(200), 61.0, 0));

        assertThat(state.status()).isEqualTo(BlockageStatus.WARNING);
    }

    // TEST 7 : retour a une situation normale => NORMAL
    @Test
    void returnToLowBlockage_goesBackToNormal() {
        DecisionState state = evaluate(engine,
                snapshot(T0, 55.0, 0),
                snapshot(T0.plusSeconds(100), 57.0, 0),
                snapshot(T0.plusSeconds(200), 60.0, 0),
                snapshot(T0.plusSeconds(300), 10.0, 0),
                snapshot(T0.plusSeconds(400), 12.0, 0),
                snapshot(T0.plusSeconds(500), 11.0, 0),
                snapshot(T0.plusSeconds(600), 13.0, 0));

        assertThat(state.status()).isEqualTo(BlockageStatus.NORMAL);
    }

    // Borne : duree exactement egale a la fenetre => CRITICAL
    @Test
    void criticalDuration_exactlyEqualToWindow_triggersCritical() {
        DecisionState state = evaluate(engine,
                snapshot(T0, 55.0, 0),
                snapshot(T0.plusSeconds(300), 57.0, 0));

        assertThat(state.status()).isEqualTo(BlockageStatus.CRITICAL);
    }

    // Borne : une frame exactement au seuil critique ne le "depasse" pas
    // (comparaison stricte, comportement documente)
    @Test
    void blockageExactlyAtCriticalThreshold_isNotAboveThreshold() {
        DecisionState state = evaluate(engine,
                snapshot(T0, 50.0, 0),
                snapshot(T0.plusSeconds(300), 50.0, 0));

        assertThat(state.status()).isEqualTo(BlockageStatus.WARNING);
    }

    // TEST 13 : la modification de la configuration change le comportement
    @Test
    void configurationChange_changesDecisionBehaviour() {
        AnalysisSnapshot s1 = snapshot(T0, 30.0, 0);
        AnalysisSnapshot s2 = snapshot(T0.plusSeconds(30), 35.0, 0);

        // Seuil warning 40 -> frames non depassees => NORMAL.
        DecisionEngine higherWarning = new DecisionEngine(
                new DecisionProperties(40.0, 50.0, 300, 0.5), CAPTURE_30);
        assertThat(evaluate(higherWarning, s1, s2).status())
                .isEqualTo(BlockageStatus.NORMAL);

        // Seuil warning 20 -> frames depassees => WARNING.
        DecisionEngine lowerWarning = new DecisionEngine(
                new DecisionProperties(20.0, 50.0, 300, 0.5), CAPTURE_30);
        assertThat(evaluate(lowerWarning, s1, s2).status())
                .isEqualTo(BlockageStatus.WARNING);

        // Fenetre de persistance 60 s : 120 s soutenus (>= 60 - 30) => CRITICAL.
        DecisionEngine shortWindow = new DecisionEngine(
                new DecisionProperties(25.0, 50.0, 60, 0.5), CAPTURE_30);
        AnalysisSnapshot c1 = snapshot(T0, 55.0, 0);
        AnalysisSnapshot c2 = snapshot(T0.plusSeconds(120), 55.0, 0);
        assertThat(evaluate(shortWindow, c1, c2).status())
                .isEqualTo(BlockageStatus.CRITICAL);

        // Fenetre par defaut 300 s : les memes frames => WARNING seulement.
        assertThat(evaluate(engine, c1, c2).status())
                .isEqualTo(BlockageStatus.WARNING);
    }

    // TEST 18 (invariant) : meme avec une fenetre minuscule, une frame
    // isolee ne produit ni CRITICAL ni WARNING
    @Test
    void singleFrame_cannotTriggerAnyDecision_evenWithTinyWindow() {
        DecisionEngine tinyWindow = new DecisionEngine(
                new DecisionProperties(25.0, 50.0, 2, 0.5),
                new CaptureProperties(1));

        assertThat(evaluate(tinyWindow, snapshot(T0, 100.0, 5)).status())
                .isEqualTo(BlockageStatus.NORMAL);
    }

    // Fenetre vide => NORMAL, indicateurs neutres
    @Test
    void emptyWindow_isNormal() {
        DecisionState state = engine.evaluate(List.of());

        assertThat(state.status()).isEqualTo(BlockageStatus.NORMAL);
        assertThat(state.indicators().sampleCount()).isZero();
        assertThat(state.indicators().framesAboveWarningThresholdPercentage()).isZero();
    }

    // ------------------------------------------------------------------
    // Indicateur A : pourcentage de frames au-dessus du seuil WARNING
    // (10, 30, 40, 60, 70 avec seuil 25 => 4/5 = 80 %)
    // ------------------------------------------------------------------
    @Test
    void framesAboveThresholdPercentage_isComputedOnTheWholeWindow() {
        DecisionState state = evaluate(engine,
                snapshot(T0, 10.0, 0),
                snapshot(T0.plusSeconds(30), 30.0, 0),
                snapshot(T0.plusSeconds(60), 40.0, 0),
                snapshot(T0.plusSeconds(90), 60.0, 0),
                snapshot(T0.plusSeconds(120), 70.0, 0));

        assertThat(state.indicators().framesAboveWarningThresholdPercentage())
                .isEqualTo(80.0);
        assertThat(state.indicators().sampleCount()).isEqualTo(5);
        assertThat(state.indicators().windowStart()).isEqualTo(T0);
        assertThat(state.indicators().latestAnalyzedAt())
                .isEqualTo(T0.plusSeconds(120));
        assertThat(state.indicators().currentBlockagePercentage()).isEqualTo(70.0);
    }

    // ------------------------------------------------------------------
    // AUDIT FORENSIQUE : une frame a 94.42 % (seuil warning 25) est
    // bien comptee comme au-dessus du seuil. La valeur 0 % observee sur
    // d'anciens evenements provient de lignes creees avant l'ajout de la
    // colonne (backfill 0), PAS d'un calcul incorrect.
    // ------------------------------------------------------------------
    @Test
    void frame94_isAboveWarningThreshold_ratioIs100() {
        DecisionState state = evaluate(engine, snapshot(T0, 94.42, 0));
        // La comparaison utilise 94.42 > 25 => au-dessus du seuil.
        assertThat(state.indicators().framesAboveWarningThresholdPercentage())
                .isEqualTo(100.0);
        assertThat(state.indicators().sampleCount()).isEqualTo(1);
        // Une frame isolee ne declenche JAMAIS de statut : NORMAL.
        assertThat(state.status()).isEqualTo(BlockageStatus.NORMAL);
    }

    @Test
    void frames92and94areAboveWarningThreshold_windowOf4gives75() {
        DecisionState state = evaluate(engine,
                snapshot(T0, 94.42, 0),
                snapshot(T0.plusSeconds(30), 92.23, 0),
                snapshot(T0.plusSeconds(60), 30.00, 0),
                snapshot(T0.plusSeconds(90), 10.00, 0));
        // Seuil 25 : 94.42, 92.23, 30 au-dessus ; 10 en dessous => 3/4 = 75 %.
        assertThat(state.indicators().framesAboveWarningThresholdPercentage())
                .isEqualTo(75.0);
        assertThat(state.indicators().sampleCount()).isEqualTo(4);
        // >= 2 echantillons et 75 % >= 50 % => WARNING (la serie > 50 %
        // n'est pas soutenue assez longtemps pour CRITICAL).
        assertThat(state.status()).isEqualTo(BlockageStatus.WARNING);
    }

    @Test
    void frameExactly25_isNotAboveWarningThreshold_strictComparison() {
        DecisionState state = evaluate(engine,
                snapshot(T0, 24.99, 0),
                snapshot(T0.plusSeconds(30), 25.0, 0));
        // Comparaison STRICTE (>) : 25.0 n'est PAS > 25.
        assertThat(state.indicators().framesAboveWarningThresholdPercentage())
                .isEqualTo(0.0); // 24.99 et 25.0 non > 25 => 0/2
        // Au-dessous du ratio (50 %) => NORMAL.
        assertThat(state.status()).isEqualTo(BlockageStatus.NORMAL);
    }

    // ------------------------------------------------------------------
    // Les echantillons hors-ordre sont tries chronologiquement avant
    // l'evaluation (la duree de persistance reste correcte)
    // ------------------------------------------------------------------
    @Test
    void unsortedSamples_areEvaluatedChronologically() {
        DecisionState state = engine.evaluate(List.of(
                snapshot(T0.plusSeconds(300), 57.0, 0),
                snapshot(T0, 55.0, 0),
                snapshot(T0.plusSeconds(200), 61.0, 0),
                snapshot(T0.plusSeconds(100), 58.0, 0)));

        assertThat(state.status()).isEqualTo(BlockageStatus.CRITICAL);
        assertThat(state.indicators().windowStart()).isEqualTo(T0);
    }

    // Tolerance d'un intervalle de capture : une duree critique >=
    // (fenetre - intervalle de capture) suffit, car l'echantillon frontiere
    // de la serie peut etre espace ou evince en temps reel.
    @Test
    void criticalDuration_withinCaptureIntervalTolerance_triggersCritical() {
        // Serie critique d'etendue 280 s >= (300 - 30) => CRITICAL.
        DecisionState state = evaluate(engine,
                snapshot(T0.plusSeconds(20), 55.0, 0),
                snapshot(T0.plusSeconds(300), 57.0, 0));

        assertThat(state.status()).isEqualTo(BlockageStatus.CRITICAL);
    }

    // En dessous de la tolerance (etendue < fenetre - intervalle) :
    // pas de CRITICAL.
    @Test
    void criticalDuration_belowCaptureIntervalTolerance_isNotCritical() {
        // Serie critique d'etendue 260 s < 270 s requis => WARNING.
        DecisionState state = evaluate(engine,
                snapshot(T0.plusSeconds(40), 55.0, 0),
                snapshot(T0.plusSeconds(300), 57.0, 0));

        assertThat(state.status()).isEqualTo(BlockageStatus.WARNING);
    }

    // Configuration incoherente : fenetre de persistance <= intervalle de
    // capture (le buffer ne tiendrait qu'un seul echantillon) => rejet.
    @Test
    void persistenceWindowNotLongerThanCaptureInterval_isRejected() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> new DecisionEngine(
                                new DecisionProperties(25.0, 50.0, 30, 0.5),
                                new CaptureProperties(30)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------------
    // Configuration incoherente (critical <= warning) : rejet explicite
    // ------------------------------------------------------------------
    @Test
    void incoherentThresholds_areRejected() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> new DecisionEngine(
                                new DecisionProperties(50.0, 50.0, 300, 0.5), CAPTURE_30))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
