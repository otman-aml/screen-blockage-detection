package com.ocp.screenblockage.service;

import com.ocp.screenblockage.config.DecisionProperties;
import com.ocp.screenblockage.dto.AiAnalysisResponseDto;
import com.ocp.screenblockage.dto.AiDetectionDto;
import com.ocp.screenblockage.dto.DetectionResultDto;
import com.ocp.screenblockage.dto.LatestAnalysisDto;
import com.ocp.screenblockage.dto.StoredImageDto;
import com.ocp.screenblockage.entity.AnalysisResult;
import com.ocp.screenblockage.entity.Detection;
import com.ocp.screenblockage.exception.InvalidImageException;
import com.ocp.screenblockage.exception.NotFoundException;
import com.ocp.screenblockage.model.BlockageStatus;
import com.ocp.screenblockage.repository.AnalysisResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetectionServiceTest {

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private AnalysisResultRepository analysisResultRepository;

    @Mock
    private BlockageEvaluationService blockageEvaluationService;

    @InjectMocks
    private DetectionService detectionService;

    @BeforeEach
    void configureSeuils() {
        // Seuils par defaut : WARNING >= 25%, CRITICAL >= 50%
        DecisionProperties props = new DecisionProperties(25.0, 50.0, 300, 0.5);
        // Utilisation de ReflectionTestUtils pour injecter le champ final
        org.springframework.test.util.ReflectionTestUtils.setField(
                detectionService, "decisionProperties", props);
    }

    @Test
    void analyzeImage_withValidImage_callsAiAndPersists() throws Exception {
        AiAnalysisResponseDto aiResponse = new AiAnalysisResponseDto(
                640, 640, 54.84, List.of(
                        new AiDetectionDto(0, "stone", 0.97, 13.12, 17.70, true,
                                List.of(List.of(138.0, 16.0))),
                        new AiDetectionDto(0, "stone", 0.91, 5.20, 4.10, true,
                                List.of(List.of(10.0, 5.0)))));

        when(aiServiceClient.analyze(any(byte[].class), any(String.class)))
                .thenReturn(aiResponse);
        when(analysisResultRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file =
                new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1, 2, 3});
        DetectionResultDto result = detectionService.analyzeImage(file);

        assertThat(result.imageWidth()).isEqualTo(640);
        assertThat(result.imageHeight()).isEqualTo(640);
        assertThat(result.blockagePercentage()).isEqualTo(54.84);
        assertThat(result.numberOfDetections()).isEqualTo(2);
        assertThat(result.detections()).hasSize(2);
        assertThat(result.detections().get(0).className()).isEqualTo("stone");

        verify(aiServiceClient).analyze(any(byte[].class), eq("test.jpg"));
        verify(analysisResultRepository).saveAndFlush(any());
    }

    @Test
    void analyzeImage_withEmptyFile_rejectsWithoutCallingAi() {
        MockMultipartFile empty =
                new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> detectionService.analyzeImage(empty))
                .isInstanceOf(InvalidImageException.class);

        verify(aiServiceClient, never()).analyze(any(), any());
    }

    @Test
    void analyzeImage_withUnsupportedExtension_rejectsWithoutCallingAi() {
        MockMultipartFile file =
                new MockMultipartFile("file", "document.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> detectionService.analyzeImage(file))
                .isInstanceOf(InvalidImageException.class);

        verify(aiServiceClient, never()).analyze(any(), any());
    }

    // ------------------------------------------------------------------
    // Phase 6 - derniere analyse + image stockee
    // ------------------------------------------------------------------

    @Test
    void analyzeImage_persistsImageBytes_andContentType() throws Exception {
        AiAnalysisResponseDto aiResponse =
                new AiAnalysisResponseDto(640, 640, 54.84, List.of());
        when(aiServiceClient.analyze(any(byte[].class), any(String.class)))
                .thenReturn(aiResponse);
        when(analysisResultRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});
        detectionService.analyzeImage(file);

        ArgumentCaptor<AnalysisResult> captor = ArgumentCaptor.forClass(AnalysisResult.class);
        verify(analysisResultRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getImageData()).containsExactly(1, 2, 3, 4);
        assertThat(captor.getValue().getImageContentType()).isEqualTo("image/jpeg");
    }

    @Test
    void getLatestAnalysis_mapsDetectionsWithMasks() {
        AnalysisResult result = new AnalysisResult();
        result.setId(7L);
        result.setImageWidth(640);
        result.setImageHeight(640);
        result.setBlockagePercentage(54.84);
        result.setAnalyzedAt(Instant.parse("2026-01-01T10:00:00Z"));
        Detection d = new Detection();
        d.setId(1L);
        d.setClassId(0);
        d.setClassName("stone");
        d.setConfidence(0.97);
        d.setWidthCm(13.12);
        d.setHeightCm(17.70);
        d.setOversized(true);
        d.setMaskPolygon(List.of(List.of(138.0, 16.0), List.of(134.0, 20.0)));
        result.addDetection(d);

        when(analysisResultRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.of(result));

        LatestAnalysisDto dto = detectionService.getLatestAnalysis();
        assertThat(dto.id()).isEqualTo(7L);
        assertThat(dto.numberOfDetections()).isEqualTo(1);
        assertThat(dto.detections().get(0).className()).isEqualTo("stone");
        assertThat(dto.detections().get(0).oversized()).isTrue();
        assertThat(dto.detections().get(0).mask())
                .containsExactly(List.of(138.0, 16.0), List.of(134.0, 20.0));
    }

    @Test
    void getLatestAnalysis_whenNone_throwsNotFound() {
        when(analysisResultRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> detectionService.getLatestAnalysis())
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getLatestImage_returnsStoredBytesAndType() {
        AnalysisResult result = new AnalysisResult();
        result.setImageData(new byte[]{1, 2, 3});
        result.setImageContentType("image/png");
        when(analysisResultRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.of(result));

        StoredImageDto image = detectionService.getLatestImage();
        assertThat(image.data()).containsExactly(1, 2, 3);
        assertThat(image.contentType()).isEqualTo("image/png");
    }

    @Test
    void getLatestImage_whenNoImageStored_throwsNotFound() {
        AnalysisResult result = new AnalysisResult();
        result.setImageData(null);
        when(analysisResultRepository.findTopByOrderByIdDesc())
                .thenReturn(Optional.of(result));

        assertThatThrownBy(() -> detectionService.getLatestImage())
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getRecentAnalyses_mapsStatusFromBlockagePercentage() {
        //NORMAL: < 25%
        AnalysisResult normal = new AnalysisResult();
        normal.setId(1L);
        normal.setImageWidth(640);
        normal.setImageHeight(640);
        normal.setBlockagePercentage(23.93);
        normal.setAnalyzedAt(Instant.parse("2026-01-01T10:00:00Z"));

        //WARNING: >= 25% et < 50%
        AnalysisResult warning = new AnalysisResult();
        warning.setId(2L);
        warning.setImageWidth(640);
        warning.setImageHeight(640);
        warning.setBlockagePercentage(35.0);
        warning.setAnalyzedAt(Instant.parse("2026-01-01T10:01:00Z"));

        //CRITICAL: >= 50%
        AnalysisResult critical = new AnalysisResult();
        critical.setId(3L);
        critical.setImageWidth(640);
        critical.setImageHeight(640);
        critical.setBlockagePercentage(94.42);
        critical.setAnalyzedAt(Instant.parse("2026-01-01T10:02:00Z"));

        when(analysisResultRepository.findRecent(any()))
                .thenReturn(List.of(critical, warning, normal));

        List<com.ocp.screenblockage.dto.RecentAnalysisDto> result =
                detectionService.getRecentAnalyses(50);

        assertThat(result).hasSize(3);

        //CRITICAL
        assertThat(result.get(0).status()).isEqualTo(BlockageStatus.CRITICAL);
        assertThat(result.get(0).blockagePercentage()).isEqualTo(94.42);

        //WARNING
        assertThat(result.get(1).status()).isEqualTo(BlockageStatus.WARNING);
        assertThat(result.get(1).blockagePercentage()).isEqualTo(35.0);

        //NORMAL
        assertThat(result.get(2).status()).isEqualTo(BlockageStatus.NORMAL);
        assertThat(result.get(2).blockagePercentage()).isEqualTo(23.93);
    }

    @Test
    void getRecentAnalyses_seuilsCoherence() {
        // Test des seuils exacts
        AnalysisResult seuilWarning = new AnalysisResult();
        seuilWarning.setId(10L);
        seuilWarning.setBlockagePercentage(25.0);
        seuilWarning.setAnalyzedAt(Instant.parse("2026-01-01T10:00:00Z"));

        AnalysisResult seuilCritical = new AnalysisResult();
        seuilCritical.setId(11L);
        seuilCritical.setBlockagePercentage(50.0);
        seuilCritical.setAnalyzedAt(Instant.parse("2026-01-01T10:01:00Z"));

        AnalysisResult justBelowWarning = new AnalysisResult();
        justBelowWarning.setId(12L);
        justBelowWarning.setBlockagePercentage(24.99);
        justBelowWarning.setAnalyzedAt(Instant.parse("2026-01-01T10:02:00Z"));

        AnalysisResult justBelowCritical = new AnalysisResult();
        justBelowCritical.setId(13L);
        justBelowCritical.setBlockagePercentage(49.99);
        justBelowCritical.setAnalyzedAt(Instant.parse("2026-01-01T10:03:00Z"));

        when(analysisResultRepository.findRecent(any()))
                .thenReturn(List.of(seuilWarning, seuilCritical, justBelowWarning, justBelowCritical));

        List<com.ocp.screenblockage.dto.RecentAnalysisDto> result =
                detectionService.getRecentAnalyses(50);

        assertThat(result).hasSize(4);
        assertThat(result.get(0).status()).isEqualTo(BlockageStatus.WARNING); // 25.0
        assertThat(result.get(1).status()).isEqualTo(BlockageStatus.CRITICAL); // 50.0
        assertThat(result.get(2).status()).isEqualTo(BlockageStatus.NORMAL); // 24.99
        assertThat(result.get(3).status()).isEqualTo(BlockageStatus.WARNING); // 49.99
    }
}