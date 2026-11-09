package com.ocp.screenblockage.entity;

import com.ocp.screenblockage.model.BlockageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Transition d'etat metier historisee (Phase 4).
 *
 * <p>Un {@code Event} est cree UNIQUEMENT lorsqu'un changement NORMAL /
 * WARNING / CRITICAL se produit. Les etats appartiennent exclusivement au
 * backend Spring Boot ("L'IA detecte. Spring Boot decide.").
 */
@Entity
@Table(name = "event")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 16)
    private BlockageStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 16)
    private BlockageStatus newStatus;

    /** Timestamp de l'analyse ayant declenche la transition. */
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    /** Contexte : blocage de la derniere analyse au moment de la transition. */
    @Column(name = "blockage_percentage", nullable = false)
    private double blockagePercentage;

    /** Contexte : roches surdimensionnees de la derniere analyse. */
    @Column(name = "oversized_rocks", nullable = false)
    private int oversizedRocks;

    /** Contexte : nombre d'echantillons presents dans la fenetre. */
    @Column(name = "frames_in_window", nullable = false)
    private int framesInWindow;

    /**
     * INDICATEUR DE DECISION WARNING : part (%) des frames de la fenetre
     * dont le blocage depasse le seuil WARNING au moment de la transition.
     * C'est LA metrique (avec la persistance temporelle pour CRITICAL) qui
     * justifie le changement d'etat, distincte du blocage de la seule image.
     */
    @Column(name = "frames_above_threshold_percentage", nullable = false,
            columnDefinition = "double precision not null default 0")
    private double framesAboveThresholdPercentage;

    protected Event() {
        // Requis par JPA.
    }

    public Event(BlockageStatus previousStatus,
                 BlockageStatus newStatus,
                 Instant occurredAt,
                 double blockagePercentage,
                 int oversizedRocks,
                 int framesInWindow,
                 double framesAboveThresholdPercentage) {
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.occurredAt = occurredAt;
        this.blockagePercentage = blockagePercentage;
        this.oversizedRocks = oversizedRocks;
        this.framesInWindow = framesInWindow;
        this.framesAboveThresholdPercentage = framesAboveThresholdPercentage;
    }

    public Long getId() {
        return id;
    }

    public BlockageStatus getPreviousStatus() {
        return previousStatus;
    }

    public BlockageStatus getNewStatus() {
        return newStatus;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public double getBlockagePercentage() {
        return blockagePercentage;
    }

    public int getOversizedRocks() {
        return oversizedRocks;
    }

    public int getFramesInWindow() {
        return framesInWindow;
    }

    public double getFramesAboveThresholdPercentage() {
        return framesAboveThresholdPercentage;
    }
}
