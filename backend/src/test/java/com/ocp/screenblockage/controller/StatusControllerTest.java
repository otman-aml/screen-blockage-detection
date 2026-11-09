package com.ocp.screenblockage.controller;

import com.ocp.screenblockage.model.AnalysisSnapshot;
import com.ocp.screenblockage.model.BlockageStatus;
import com.ocp.screenblockage.model.DecisionState;
import com.ocp.screenblockage.model.WindowIndicators;
import com.ocp.screenblockage.service.BlockageEvaluationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests API Phase 4 : GET /api/status (structure JSON, etat, indicateurs).
 *
 * <p>L'ObjectMapper reproduit la configuration runtime de Spring Boot
 * (module JSR-310 enregistre, {@code WRITE_DATES_AS_TIMESTAMPS} desactive)
 * afin que les {@link Instant} soient serialises en ISO-8601 comme en
 * production.
 */
class StatusControllerTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BlockageEvaluationService service = mock(BlockageEvaluationService.class);
        when(service.getCurrentState()).thenReturn(new DecisionState(
                BlockageStatus.WARNING,
                new WindowIndicators(60.0, 3, 37.5, 5, T0, T0.plusSeconds(120))));
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(new StatusController(service))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void getStatus_returns200_withExpectedJsonStructure() throws Exception {
        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WARNING"))
                .andExpect(jsonPath("$.blockagePercentage").value(37.5))
                .andExpect(jsonPath("$.framesAboveThresholdPercentage").value(60.0))
                .andExpect(jsonPath("$.oversizedRocks").value(3))
                .andExpect(jsonPath("$.sampleCount").value(5))
                .andExpect(jsonPath("$.windowStart").value("2026-01-01T10:00:00Z"))
                .andExpect(jsonPath("$.analyzedAt").value("2026-01-01T10:02:00Z"));
    }
}
