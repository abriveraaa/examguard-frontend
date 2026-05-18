package com.example.examguard.model.exam.response;

import java.math.BigDecimal;

public class EssayRubricScoreResponse {

    private Long rubricId;
    private BigDecimal scorePercentage;
    private BigDecimal scoreAwarded;
    private String feedback;

    public Long getRubricId() {
        return rubricId;
    }

    public void setRubricId(Long rubricId) {
        this.rubricId = rubricId;
    }

    public BigDecimal getScorePercentage() {
        return scorePercentage;
    }

    public void setScorePercentage(BigDecimal scorePercentage) {
        this.scorePercentage = scorePercentage;
    }

    public BigDecimal getScoreAwarded() {
        return scoreAwarded;
    }

    public void setScoreAwarded(BigDecimal scoreAwarded) {
        this.scoreAwarded = scoreAwarded;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}