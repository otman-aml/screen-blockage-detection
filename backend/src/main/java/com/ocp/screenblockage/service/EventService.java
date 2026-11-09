package com.ocp.screenblockage.service;

import com.ocp.screenblockage.dto.EventDto;
import com.ocp.screenblockage.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Lecture de l'historique des transitions d'etat (GET /api/events). */
@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /** Historique chronologique des transitions, sous forme de DTO. */
    public List<EventDto> getEventHistory() {
        return eventRepository.findAllByOrderByOccurredAtAscIdAsc().stream()
                .map(EventDto::from)
                .toList();
    }
}
