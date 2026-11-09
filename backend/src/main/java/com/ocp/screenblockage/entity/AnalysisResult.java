package com.ocp.screenblockage.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Une analyse correspond a UNE frame analysee par le ai-service.
 *
 * <p>Relation : 1 AnalysisResult -> N Detection.
 */
@Entity
@Table(name = "analysis_result")
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_width", nullable = false)
    private int imageWidth;

    @Column(name = "image_height", nullable = false)
    private int imageHeight;

    @Column(name = "blockage_percentage", nullable = false)
    private double blockagePercentage;

    @Column(name = "analyzed_at", nullable = false, updatable = false)
    private Instant analyzedAt;

    /**
     * Octets bruts de l'image analysee (utilises par le frontend Phase 6).
     *
     * <p>Stocke en BYTEA direct. IMPORTANT : {@code @Lob} serait contredit
     * ici — sur PostgreSQL il bascule Hibernate en mode LargeObject (OID de
     * type {@code oid}), incompatible avec une colonne {@code bytea}
     * (TYPE_ERROR: bytea vs bigint). Le mappage brut {@code byte[]} ->
     * {@code bytea} est correct.
     */
    @Column(name = "image_data", columnDefinition = "bytea")
    private byte[] imageData;

    /** Type MIME de l'image stockee (image/jpeg, image/png...). */
    @Column(name = "image_content_type")
    private String imageContentType;

    @OneToMany(mappedBy = "analysisResult",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Detection> detections = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (analyzedAt == null) {
            analyzedAt = Instant.now();
        }
    }

    public void addDetection(Detection detection) {
        detections.add(detection);
        detection.setAnalysisResult(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
    }

    public double getBlockagePercentage() {
        return blockagePercentage;
    }

    public void setBlockagePercentage(double blockagePercentage) {
        this.blockagePercentage = blockagePercentage;
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(Instant analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public void setImageContentType(String imageContentType) {
        this.imageContentType = imageContentType;
    }

    public List<Detection> getDetections() {
        return detections;
    }

    public void setDetections(List<Detection> detections) {
        this.detections = detections;
    }
}