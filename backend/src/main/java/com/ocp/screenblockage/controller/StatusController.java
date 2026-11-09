package com.ocp.screenblockage.controller;

import com.ocp.screenblockage.dto.StatusResponseDto;
import com.ocp.screenblockage.service.BlockageEvaluationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint GET /api/status (Phase 4) : etat metier courant du systeme
 * (NORMAL/WARNING/CRITICAL) + indicateurs de la fenetre glissante.
 *
 * <p>Lecture pure : aucun Event n'est cree par un GET.
 */
@RestController
@RequestMapping("/api/status")
public class StatusController {

    private final BlockageEvaluationService blockageEvaluationService;

    public StatusController(BlockageEvaluationService blockageEvaluationService) {
        this.blockageEvaluationService = blockageEvaluationService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public StatusResponseDto getStatus() {
        return StatusResponseDto.from(blockageEvaluationService.getCurrentState());
    }
}
