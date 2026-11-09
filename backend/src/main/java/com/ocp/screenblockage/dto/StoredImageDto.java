package com.ocp.screenblockage.dto;

/**
 * Contenu de l'image analysee telle que persiste en base, avec son type
 * MIME, pour l'endpoint GET /api/detections/latest/image.
 */
public record StoredImageDto(
        byte[] data,
        String contentType) {
}
