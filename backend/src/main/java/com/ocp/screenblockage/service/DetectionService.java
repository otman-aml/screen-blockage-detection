package com.ocp.screenblockage.service;

import com.ocp.screenblockage.config.DecisionProperties;
import com.ocp.screenblockage.dto.AiAnalysisResponseDto;
import com.ocp.screenblockage.dto.AiDetectionDto;
import com.ocp.screenblockage.dto.DetectionResultDto;
import com.ocp.screenblockage.dto.DetectionSummaryDto;
import com.ocp.screenblockage.dto.LatestAnalysisDto;
import com.ocp.screenblockage.dto.LatestDetectionDto;
import com.ocp.screenblockage.dto.RecentAnalysisDto;
import com.ocp.screenblockage.dto.StoredImageDto;
import com.ocp.screenblockage.entity.AnalysisResult;
import com.ocp.screenblockage.entity.Detection;
import com.ocp.screenblockage.exception.InvalidImageException;
import com.ocp.screenblockage.exception.NotFoundException;
import com.ocp.screenblockage.model.AnalysisSnapshot;
import com.ocp.screenblockage.model.BlockageStatus;
import com.ocp.screenblockage.repository.AnalysisResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Orchestration du flux Phase 3 + evaluation Phase 4 :
 *
 * <pre>
 * image -> validation -> AiServiceClient (POST /analyze)
 *       -> DTO IA -> Entity (AnalysisResult 1..N Detection)
 *       -> PostgreSQL -> DetectionResultDto
 *       -> BlockageEvaluationService (buffer glissant, etat, Event)
 * </pre>
 *
 * <p>La Phase 4 n'altere PAS la reponse Phase 3 : apres la persistance du
 * resultat brut de l'IA, l'analyse est soumise au moteur de decision
 * (NORMAL/WARNING/CRITICAL cote backend uniquement). Toute erreur
 * d'evaluation est journalisee sans faire echouer la requete.
 */
@Service
public class DetectionService {

    private static final Logger LOG = LoggerFactory.getLogger(DetectionService.class);

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private static final long MAX_IMAGE_BYTES = 15L * 1024 * 1024;

    private final AiServiceClient aiServiceClient;
    private final AnalysisResultRepository analysisResultRepository;
    private final BlockageEvaluationService blockageEvaluationService;
    private final DecisionProperties decisionProperties;

    public DetectionService(AiServiceClient aiServiceClient,
                            AnalysisResultRepository analysisResultRepository,
                            BlockageEvaluationService blockageEvaluationService,
                            DecisionProperties decisionProperties) {
        this.aiServiceClient = aiServiceClient;
        this.analysisResultRepository = analysisResultRepository;
        this.blockageEvaluationService = blockageEvaluationService;
        this.decisionProperties = decisionProperties;
    }

    /**
     * Analyse une image : validation, appel IA, mapping, persistance.
     */
    @Transactional
    public DetectionResultDto analyzeImage(MultipartFile file) {
        validateImage(file);

        byte[] imageBytes = readBytes(file);
        String filename = file.getOriginalFilename();

        LOG.info("Calling AI service for image '{}' ({} bytes)", filename, imageBytes.length);
        AiAnalysisResponseDto aiResponse = aiServiceClient.analyze(imageBytes, filename);

        LOG.info("AI service returned {} detection(s), blockage={}%",
                aiResponse.detections().size(), aiResponse.blockagePercentage());

        AnalysisResult result = toEntity(aiResponse);
        // Persistition de l'image analysee (necessaire au frontend Phase 6 :
        // affichage de l'image + overlay des roches aligne sur les masques).
        result.setImageData(imageBytes);
        result.setImageContentType(resolveContentType(file, extensionOf(filename)));
        AnalysisResult saved = analysisResultRepository.saveAndFlush(result);

        LOG.info("Persisted AnalysisResult id={} with {} detection(s)",
                saved.getId(), saved.getDetections().size());

        evaluateBlockage(saved);

        return toDto(saved);
    }

    // ------------------------------------------------------------------
    // Validation des entrees (coherente avec les regles du ai-service)
    // ------------------------------------------------------------------

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("The uploaded image is empty or missing.");
        }

        String filename = file.getOriginalFilename();
        String extension = extensionOf(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidImageException(
                    "Unsupported image format. Allowed formats: JPG, JPEG, PNG.");
        }

        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new InvalidImageException("The uploaded image exceeds the maximum allowed size.");
        }
    }

    private static String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new InvalidImageException("Unable to read the uploaded image.", e);
        }
    }

    // ------------------------------------------------------------------
    // Phase 4 : moteur de decision (L'IA detecte. Spring Boot decide.)
    // ------------------------------------------------------------------

    /**
     * Soumet l'analyse fraichement persistee au moteur de decision : buffer
     * glissant, indicateurs, etat NORMAL/WARNING/CRITICAL, Event si
     * transition. Le flag {@code oversized} vient du ai-service (aucune
     * logique de detection de roche reimplantee cote backend).
     *
     * <p>La reponse HTTP Phase 3 ne doit jamais etre cassee par la decision :
     * en cas d'erreur d'evaluation, l'erreur est journalisee et la requete
     * aboutit normalement (l'analyse est deja persistee).
     */
    private void evaluateBlockage(AnalysisResult saved) {
        try {
            AnalysisSnapshot snapshot = new AnalysisSnapshot(
                    saved.getAnalyzedAt(),
                    saved.getBlockagePercentage(),
                    (int) saved.getDetections().stream()
                            .filter(Detection::isOversized)
                            .count());
            blockageEvaluationService.evaluate(snapshot);
        } catch (Exception e) {
            LOG.error("Blockage evaluation failed for analysis id={} "
                            + "(analysis persisted, decision engine skipped)",
                    saved.getId(), e);
        }
    }

    // ------------------------------------------------------------------
    // Mapping DTO IA -> Entity
    // ------------------------------------------------------------------

    private AnalysisResult toEntity(AiAnalysisResponseDto dto) {
        AnalysisResult result = new AnalysisResult();
        result.setImageWidth(dto.imageWidth());
        result.setImageHeight(dto.imageHeight());
        result.setBlockagePercentage(dto.blockagePercentage());

        for (AiDetectionDto detectionDto : dto.detections()) {
            result.addDetection(toEntity(detectionDto));
        }
        return result;
    }

    private Detection toEntity(AiDetectionDto dto) {
        Detection detection = new Detection();
        detection.setClassId(dto.classId());
        detection.setClassName(dto.className());
        detection.setConfidence(dto.confidence());
        detection.setWidthCm(dto.widthCm());
        detection.setHeightCm(dto.heightCm());
        detection.setOversized(dto.oversized());
        detection.setMaskPolygon(dto.mask());
        return detection;
    }

    // ------------------------------------------------------------------
    // Mapping Entity -> DTO de reponse
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // Phase 6 : derniere analyse (JSON avec masques) + image stockee
    // ------------------------------------------------------------------

    /**
     * Retourne la derniere analyse avec ses detections incluant les
     * polygones de masques (pour l'overlay du frontend).
     */
    @Transactional(readOnly = true)
    public LatestAnalysisDto getLatestAnalysis() {
        AnalysisResult latest = analysisResultRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new NotFoundException("No analysis available yet."));
        return toLatestDto(latest);
    }

    /**
     * Retourne les octets de l'image de la derniere analyse + son type MIME.
     */
    @Transactional(readOnly = true)
    public StoredImageDto getLatestImage() {
        AnalysisResult latest = analysisResultRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new NotFoundException("No analysis available yet."));
        if (latest.getImageData() == null || latest.getImageData().length == 0) {
            throw new NotFoundException("The latest analysis has no stored image.");
        }
        String contentType = latest.getImageContentType() != null
                ? latest.getImageContentType()
                : "application/octet-stream";
        return new StoredImageDto(latest.getImageData(), contentType);
    }

    /**
     * Analyses recentes (Phase 7) : resume des N dernieres analyses (KPI +
     * courbe de blocage du frontend). Lecture pure, aucune decision metier.
     */
    @Transactional(readOnly = true)
    public List<RecentAnalysisDto> getRecentAnalyses(int limit) {
        return analysisResultRepository.findRecent(PageRequest.of(0, limit)).stream()
                .map(r -> new RecentAnalysisDto(
                        r.getId(),
                        r.getAnalyzedAt(),
                        r.getBlockagePercentage(),
                        r.getDetections().size(),
                        classifyImageStatus(r.getBlockagePercentage())))
                .toList();
    }

    /**
     * Classification d'affichage d'une image selon les seuils configures :
     * NORMAL &lt; warning &lt;= WARNING &lt; critical &lt;= CRITICAL.
     *
     * <p>Ce libelle decrit CETTE image ; la decision SYSTEME (fenetre
     * glissante + persistance) reste exclusivement dans
     * {@link BlockageEvaluationService}. Les seuils viennent de la
     * configuration : aucune valeur codee en dur ici.
     */
    private BlockageStatus classifyImageStatus(double blockagePercentage) {
        if (blockagePercentage >= decisionProperties.criticalBlockagePercentage()) {
            return BlockageStatus.CRITICAL;
        }
        if (blockagePercentage >= decisionProperties.warningBlockagePercentage()) {
            return BlockageStatus.WARNING;
        }
        return BlockageStatus.NORMAL;
    }

    private LatestAnalysisDto toLatestDto(AnalysisResult result) {
        List<LatestDetectionDto> detections = result.getDetections().stream()
                .map(d -> new LatestDetectionDto(
                        d.getId(),
                        d.getClassId(),
                        d.getClassName(),
                        d.getConfidence(),
                        d.getWidthCm(),
                        d.getHeightCm(),
                        d.isOversized(),
                        d.getMaskPolygon()))
                .toList();
        return new LatestAnalysisDto(
                result.getId(),
                result.getImageWidth(),
                result.getImageHeight(),
                result.getBlockagePercentage(),
                result.getAnalyzedAt(),
                detections.size(),
                detections);
    }

    private String resolveContentType(MultipartFile file, String extension) {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()
                || contentType.startsWith("application/octet-stream")) {
            return switch (extension) {
                case "jpg", "jpeg" -> "image/jpeg";
                case "png" -> "image/png";
                default -> "application/octet-stream";
            };
        }
        return contentType;
    }

    private DetectionResultDto toDto(AnalysisResult result) {
        List<DetectionSummaryDto> summaries = result.getDetections().stream()
                .map(this::toSummary)
                .toList();

        return new DetectionResultDto(
                result.getId(),
                result.getImageWidth(),
                result.getImageHeight(),
                result.getBlockagePercentage(),
                result.getAnalyzedAt(),
                summaries.size(),
                summaries);
    }

    private DetectionSummaryDto toSummary(Detection detection) {
        return new DetectionSummaryDto(
                detection.getId(),
                detection.getClassId(),
                detection.getClassName(),
                detection.getConfidence(),
                detection.getWidthCm(),
                detection.getHeightCm(),
                detection.isOversized());
    }
}