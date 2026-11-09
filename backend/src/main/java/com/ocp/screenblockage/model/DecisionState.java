package com.ocp.screenblockage.model;

/**
 * Etat courant du systeme : statut metier + indicateurs de la fenetre.
 * Objet de transfert interne entre {@code BlockageEvaluationService} et la
 * couche DTO (les entites JPA ne sont jamais exposees).
 */
public record DecisionState(
        BlockageStatus status,
        WindowIndicators indicators) {
}
