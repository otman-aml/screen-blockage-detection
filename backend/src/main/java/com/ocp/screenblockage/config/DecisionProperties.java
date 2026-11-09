package com.ocp.screenblockage.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Parametres metier du moteur de decision (Phase 4).
 *
 * <p>Toutes les valeurs viennent d'{@code application.yml} (prefixe
 * {@code decision}) via {@code @ConfigurationProperties} : AUCUN seuil n'est
 * code en dur dans les services. Modifier {@code application.yml} change le
 * comportement sans recompilation.
 *
 * <pre>
 * decision:
 *   warning-blockage-percentage: 25
 *   critical-blockage-percentage: 50
 *   persistence-window-seconds: 300
 *   warning-min-frames-ratio: 0.5
 * </pre>
 *
 * <p>Les valeurs 25 / 50 sont des valeurs indicatives de la roadmap, a ajuster
 * avec des experts industriels — d'ou leur externalisation.
 */
@Validated
@ConfigurationProperties(prefix = "decision")
public record DecisionProperties(

        /** Seuil WARNING en % de blocage (0-100). */
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "100.0")
        double warningBlockagePercentage,

        /** Seuil CRITICAL en % de blocage (0-100), doit etre > warning. */
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "100.0")
        double criticalBlockagePercentage,

        /** Duree de persistance en secondes (fenetre glissante + duree minimale
         *  de maintien de la condition critique pour CRITICAL). */
        @Positive
        int persistenceWindowSeconds,

        /** Part minimale (0 &lt; ratio &lt;= 1) des frames de la fenetre
         *  au-dessus du seuil WARNING pour declarer l'etat WARNING. */
        @DecimalMin(value = "0.0", inclusive = false)
        @DecimalMax(value = "1.0")
        double warningMinFramesRatio) {
}
