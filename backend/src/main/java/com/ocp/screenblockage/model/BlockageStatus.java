package com.ocp.screenblockage.model;

/**
 * Etats metier du systeme de detection de blocage.
 *
 * <p>Ces etats appartiennent exclusivement au BACKEND Spring Boot
 * (regle d'architecture : "L'IA detecte. Spring Boot decide.").
 * Le service IA ne les connait pas et ne doit jamais les produire.
 */
public enum BlockageStatus {
    NORMAL,
    WARNING,
    CRITICAL
}
