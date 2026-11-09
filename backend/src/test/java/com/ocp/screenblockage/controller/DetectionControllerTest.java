package com.ocp.screenblockage.controller;

import com.ocp.screenblockage.dto.DetectionResultDto;
import com.ocp.screenblockage.dto.LatestAnalysisDto;
import com.ocp.screenblockage.dto.LatestDetectionDto;
import com.ocp.screenblockage.dto.StoredImageDto;
import com.ocp.screenblockage.exception.GlobalExceptionHandler;
import com.ocp.screenblockage.exception.InvalidImageException;
import com.ocp.screenblockage.exception.NotFoundException;
import com.ocp.screenblockage.service.DetectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DetectionController.class)
@Import(GlobalExceptionHandler.class)
class DetectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DetectionService detectionService;

    @Test
    void postValidImage_returns201WithResult() throws Exception {
        DetectionResultDto result = new DetectionResultDto(
                1L, 640, 640, 54.84, Instant.parse("2026-01-01T10:00:00Z"),
                1, List.of());
        when(detectionService.analyzeImage(any())).thenReturn(result);

        mockMvc.perform(multipart("/api/detections")
                        .file(new MockMultipartFile("file", "test.jpg",
                                "image/jpeg", new byte[]{1, 2, 3})))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.imageWidth").value(640))
                .andExpect(jsonPath("$.blockagePercentage").value(54.84))
                .andExpect(jsonPath("$.numberOfDetections").value(1));
    }

    @Test
    void postInvalidImage_serviceThrows_returns400() throws Exception {
        when(detectionService.analyzeImage(any()))
                .thenThrow(new InvalidImageException("Unsupported image format."));

        mockMvc.perform(multipart("/api/detections")
                        .file(new MockMultipartFile("file", "bad.txt",
                                "text/plain", new byte[]{1})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid image"));
    }

    @Test
    void getLatest_returnsJsonWithMasks() throws Exception {
        LatestAnalysisDto latest = new LatestAnalysisDto(
                7L, 640, 640, 54.84, Instant.parse("2026-01-01T10:00:00Z"), 1,
                List.of(new LatestDetectionDto(1L, 0, "stone", 0.97, 13.12, 17.70, true,
                        List.of(List.of(138.0, 16.0), List.of(134.0, 20.0)))));
        when(detectionService.getLatestAnalysis()).thenReturn(latest);

        mockMvc.perform(get("/api/detections/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.imageWidth").value(640))
                .andExpect(jsonPath("$.detections[0].className").value("stone"))
                .andExpect(jsonPath("$.detections[0].oversized").value(true))
                .andExpect(jsonPath("$.detections[0].mask[0][0]").value(138.0))
                .andExpect(jsonPath("$.detections[0].mask[1][1]").value(20.0));
    }

    @Test
    void getLatestImage_returnsStoredBytes() throws Exception {
        when(detectionService.getLatestImage())
                .thenReturn(new StoredImageDto(new byte[]{1, 2, 3}, "image/jpeg"));

        mockMvc.perform(get("/api/detections/latest/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void getLatestImage_whenNone_returns404() throws Exception {
        when(detectionService.getLatestImage())
                .thenThrow(new NotFoundException("No analysis available yet."));

        mockMvc.perform(get("/api/detections/latest/image"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}