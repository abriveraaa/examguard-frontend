package com.example.examguard.model.faculty;

import java.time.OffsetDateTime;

public class FacultyAttemptViolationDTO {

    private Long violationId;
    private Long questionId;
    private String violationType;
    private String severity;
    private String violationMessage;
    private String evidenceUrl;
    private Integer attemptNumber;
    private OffsetDateTime occurredAt;

    public Long getViolationId() { return violationId; }
    public Long getQuestionId() { return questionId; }
    public String getViolationType() { return violationType; }
    public String getSeverity() { return severity; }
    public String getViolationMessage() { return violationMessage; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public Integer getAttemptNumber() { return attemptNumber; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
}