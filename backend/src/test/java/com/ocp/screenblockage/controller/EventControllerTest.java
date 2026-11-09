package com.ocp.screenblockage.controller;

import com.ocp.screenblockage.dto.EventDto;
import com.ocp.screenblockage.model.BlockageStatus;
import com.ocp.screenblockage.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests API Phase 4 : GET /api/events (historique des transitions,
 * structure JSON, jamais les entites JPA directement).
 *
 * <p>L'ObjectMapper est construit via {@link Jackson2ObjectMapperBuilder}
 * (comme le fait Spring Boot) afin que les {@link Instant} soient serialises
 * en ISO-8601, identiquement au runtime reel.
 */
class EventControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        EventService eventService = mock(EventService.class);
        when(eventService.getEventHistory()).thenReturn(List.of(
                new EventDto(1L, BlockageStatus.NORMAL, BlockageStatus.WARNING,
                        Instant.parse("2026-01-01T10:00:30Z"), 30.5, 2, 2, 100.0),
                new EventDto(2L, BlockageStatus.WARNING, BlockageStatus.CRITICAL,
                        Instant.parse("2026-01-01T10:05:30Z"), 55.0, 4, 10, 80.0)));
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(new EventController(eventService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void getEvents_returns200_withTransitionHistory() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].from").value("NORMAL"))
                .andExpect(jsonPath("$[0].to").value("WARNING"))
                .andExpect(jsonPath("$[0].timestamp").value("2026-01-01T10:00:30Z"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].from").value("WARNING"))
                .andExpect(jsonPath("$[1].to").value("CRITICAL"))
                .andExpect(jsonPath("$[1].timestamp").value("2026-01-01T10:05:30Z"));
    }
}
