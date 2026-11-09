package com.ocp.screenblockage.dto;

/**
 * Resume d'une detection pour la reponse POST /api/detections.
 * Ce DTO exclut le masque (polygone potentiellement volumineux)
 * dont on ne conserve le detail qu'en base.
 */
public record DetectionSummaryDto(
        Long id,
        int classId,
        String className,
        double confidence,
        double widthCm,
        double heightCm,
        boolean oversized) {
}