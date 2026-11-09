package com.ocp.screenblockage.controller;

import com.ocp.screenblockage.dto.DetectionResultDto;
import com.ocp.screenblockage.dto.LatestAnalysisDto;
import com.ocp.screenblockage.dto.RecentAnalysisDto;
import com.ocp.screenblockage.dto.StoredImageDto;
import com.ocp.screenblockage.service.DetectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Endpoint POST /api/detections : recoit une image (multipart/form-data),
 * delegue l'analyse au {@link DetectionService} (qui appelle le ai-service
 * puis persiste le resultat brut).
 *
 * <p>Le controle reste <strong>mince</strong> : aucune logique metier,
 * aucun appel HTTP direct, aucune logique JPA ici.
 */
@RestController
@RequestMapping("/api/detections")
public class DetectionController {

    private final DetectionService detectionService;

    public DetectionController(DetectionService detectionService) {
        this.detectionService = detectionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DetectionResultDto> analyzeImage(
            @RequestPart("file") MultipartFile file) {
        DetectionResultDto result = detectionService.analyzeImage(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /** Derniere analyse avec masques (Phase 6 : overlay du frontend). */
    @GetMapping(value = "/latest", produces = MediaType.APPLICATION_JSON_VALUE)
    public LatestAnalysisDto getLatest() {
        return detectionService.getLatestAnalysis();
    }

    /** Image de la derniere analyse (octets bruts). */
    @GetMapping(value = "/latest/image")
    public ResponseEntity<byte[]> getLatestImage() {
        StoredImageDto image = detectionService.getLatestImage();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.data());
    }

    /**
     * Analyses recentes (Phase 7) : resume des N dernieres analyses pour les
     * KPI et la courbe d'evolution du blocage du frontend. Lecture pure.
     */
    @GetMapping(value = "/recent", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RecentAnalysisDto> getRecent() {
        return detectionService.getRecentAnalyses(50);
    }
}