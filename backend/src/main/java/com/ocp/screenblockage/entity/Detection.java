package com.ocp.screenblockage.entity;

import com.ocp.screenblockage.entity.converter.MaskPolygonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.List;

/**
 * Une roche detectee par le ai-service pour une frame donnee.
 * Associee a {@link AnalysisResult} (relation ManyToOne).
 */
@Entity
@Table(name = "detection")
public class Detection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_result_id", nullable = false)
    private AnalysisResult analysisResult;

    @Column(name = "class_id", nullable = false)
    private int classId;

    @Column(name = "class_name", nullable = false)
    private String className;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "width_cm", nullable = false)
    private double widthCm;

    @Column(name = "height_cm", nullable = false)
    private double heightCm;

    @Column(name = "oversized", nullable = false)
    private boolean oversized;

    /**
     * Polygone du masque stocke sous forme JSON dans une colonne TEXT.
     * Structure : liste de paires [x, y].
     */
    @Convert(converter = MaskPolygonConverter.class)
    @Column(name = "mask_polygon", columnDefinition = "TEXT")
    private List<List<Double>> maskPolygon;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AnalysisResult getAnalysisResult() {
        return analysisResult;
    }

    public void setAnalysisResult(AnalysisResult analysisResult) {
        this.analysisResult = analysisResult;
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public double getWidthCm() {
        return widthCm;
    }

    public void setWidthCm(double widthCm) {
        this.widthCm = widthCm;
    }

    public double getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(double heightCm) {
        this.heightCm = heightCm;
    }

    public boolean isOversized() {
        return oversized;
    }

    public void setOversized(boolean oversized) {
        this.oversized = oversized;
    }

    public List<List<Double>> getMaskPolygon() {
        return maskPolygon;
    }

    public void setMaskPolygon(List<List<Double>> maskPolygon) {
        this.maskPolygon = maskPolygon;
    }
}