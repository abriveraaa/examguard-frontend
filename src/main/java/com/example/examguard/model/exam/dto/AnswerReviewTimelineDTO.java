package com.example.examguard.model.exam.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class AnswerReviewTimelineDTO {

    private Long reviewLogId;
    private String actionType;
    private String previousValue;
    private String newValue;
    private BigDecimal scoreBefore;
    private BigDecimal scoreAfter;
    private BigDecimal deduction;
    private String notes;
    private String createdBy;
    private String createdByRole;
    private OffsetDateTime createdAt;

    public Long getReviewLogId() {
        return reviewLogId;
    }

    public String getActionType() {
        return actionType;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public BigDecimal getScoreBefore() {
        return scoreBefore;
    }

    public BigDecimal getScoreAfter() {
        return scoreAfter;
    }

    public BigDecimal getDeduction() {
        return deduction;
    }

    public String getNotes() {
        return notes;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getCreatedByRole() {
        return createdByRole;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}