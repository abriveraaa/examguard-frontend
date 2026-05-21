package com.example.examguard.model.exam.request;

import java.time.OffsetDateTime;

public class ViolationLogRequest {

    private Long attemptId;
    private Long examId;
    private Long questionId;
    private String violationType;
    private String severity;
    private String violationMessage;
    private Integer attemptNumber;
    private OffsetDateTime occurredAt;
    private String evidenceType;
    private String evidenceSource;

    // =========================
    // EVIDENCE
    // =========================

    private String evidenceUrl;
    private String evidenceMetadata;

    // =========================
    // AI / CAMERA
    // =========================

    private Double yaw;
    private Double pitch;
    private Double gazeX;
    private Double gazeY;
    private Integer faceCount;
    private Boolean suspicious;
    private String cameraSource;

    // =========================
    // GETTERS
    // =========================

    public Long getAttemptId() {
        return attemptId;
    }

    public Long getExamId() {
        return examId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public String getViolationType() {
        return violationType;
    }

    public String getSeverity() {
        return severity;
    }

    public String getViolationMessage() {
        return violationMessage;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public String getEvidenceMetadata() {
        return evidenceMetadata;
    }

    public Double getYaw() {
        return yaw;
    }

    public Double getPitch() {
        return pitch;
    }

    public Double getGazeX() {
        return gazeX;
    }

    public Double getGazeY() {
        return gazeY;
    }

    public Integer getFaceCount() {
        return faceCount;
    }

    public Boolean getSuspicious() {
        return suspicious;
    }

    public String getCameraSource() {
        return cameraSource;
    }

    // =========================
    // SETTERS
    // =========================

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public void setViolationType(String violationType) {
        this.violationType = violationType;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public void setViolationMessage(String violationMessage) {
        this.violationMessage = violationMessage;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
    }

    public void setEvidenceMetadata(String evidenceMetadata) {
        this.evidenceMetadata = evidenceMetadata;
    }

    public void setYaw(Double yaw) {
        this.yaw = yaw;
    }

    public void setPitch(Double pitch) {
        this.pitch = pitch;
    }

    public void setGazeX(Double gazeX) {
        this.gazeX = gazeX;
    }

    public void setGazeY(Double gazeY) {
        this.gazeY = gazeY;
    }

    public void setFaceCount(Integer faceCount) {
        this.faceCount = faceCount;
    }

    public void setSuspicious(Boolean suspicious) {
        this.suspicious = suspicious;
    }

    public void setCameraSource(String cameraSource) {
        this.cameraSource = cameraSource;
    }

    public String getEvidenceType() { return evidenceType; }

    public String getEvidenceSource() { return evidenceSource; }


    public void setEvidenceType(String evidenceType) { this.evidenceType = evidenceType; }

    public void setEvidenceSource(String evidenceSource) { this.evidenceSource = evidenceSource; }
}