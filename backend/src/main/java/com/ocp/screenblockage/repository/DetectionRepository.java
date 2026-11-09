package com.ocp.screenblockage.repository;

import com.ocp.screenblockage.entity.Detection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetectionRepository extends JpaRepository<Detection, Long> {
}