package com.example.examguard.model.exam.dto;

import com.google.gson.JsonObject;
import java.time.OffsetDateTime;

public class ExamAttemptViolationDTO {

    private Long violationId;
    private Long questionId;
    private String violationType;
    private String severity;
    private String violationMessage;
    private String evidenceUrl;
    private JsonObject evidenceMetadata;
    private Integer attemptNumber;
    private OffsetDateTime occurredAt;
    private String reviewStatus;
    private String reviewedBy;
    private OffsetDateTime reviewedAt;

    public Long getViolationId() { return violationId; }
    public Long getQuestionId() { return questionId; }
    public String getViolationType() { return violationType; }
    public String getSeverity() { return severity; }
    public String getViolationMessage() { return violationMessage; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public JsonObject getEvidenceMetadata() { return evidenceMetadata; }
    public Integer getAttemptNumber() { return attemptNumber; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public String getReviewStatus() { return reviewStatus; }
    public String getReviewedBy() { return reviewedBy; }
    public OffsetDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public void setReviewedAt(OffsetDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}