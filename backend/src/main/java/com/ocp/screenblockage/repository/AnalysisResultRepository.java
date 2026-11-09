package com.ocp.screenblockage.repository;

import com.ocp.screenblockage.entity.AnalysisResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    /** Derniere analyse enregistree (par id decroissant). */
    Optional<AnalysisResult> findTopByOrderByIdDesc();

    /**
     * Analyses recentes (Phase 7 : KPI + courbe de blocage du frontend).
     * Les plus recentes d'abord, quantite limitee par le Pageable.
     */
    @Query("select a from AnalysisResult a order by a.id desc")
    List<AnalysisResult> findRecent(Pageable pageable);
}