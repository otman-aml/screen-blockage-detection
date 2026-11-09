package com.ocp.screenblockage.repository;

import com.ocp.screenblockage.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    /** Historique chronologique complet des transitions d'etat. */
    List<Event> findAllByOrderByOccurredAtAscIdAsc();
}
