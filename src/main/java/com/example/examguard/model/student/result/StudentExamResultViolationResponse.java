package com.example.examguard.model.student.result;

import java.time.OffsetDateTime;

public class StudentExamResultViolationResponse {

    private Long violationId;
    private String violationType;
    private String severity;
    private String message;
    private String reviewStatus;
    private String reviewNotes;
    private String evidenceUrl;
    private OffsetDateTime occurredAt;

    public Long getViolationId() { return violationId; }
    public String getViolationType() { return violationType; }
    public String getSeverity() { return severity; }
    public String getMessage() { return message; }
    public String getReviewStatus() { return reviewStatus; }
    public String getReviewNotes() { return reviewNotes; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
}