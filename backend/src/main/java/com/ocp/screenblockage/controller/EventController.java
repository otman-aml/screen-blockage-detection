package com.ocp.screenblockage.controller;

import com.ocp.screenblockage.dto.EventDto;
import com.ocp.screenblockage.service.EventService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint GET /api/events (Phase 4) : historique chronologique des
 * transitions d'etat NORMAL / WARNING / CRITICAL.
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<EventDto> getEvents() {
        return eventService.getEventHistory();
    }
}
