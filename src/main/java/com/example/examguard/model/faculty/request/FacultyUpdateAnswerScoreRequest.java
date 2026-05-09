package com.example.examguard.model.faculty.request;

import java.math.BigDecimal;

public class FacultyUpdateAnswerScoreRequest {

    private BigDecimal pointsAwarded;

    public FacultyUpdateAnswerScoreRequest() {
    }

    public FacultyUpdateAnswerScoreRequest(BigDecimal pointsAwarded) {
        this.pointsAwarded = pointsAwarded;
    }

    public BigDecimal getPointsAwarded() {
        return pointsAwarded;
    }

    public void setPointsAwarded(BigDecimal pointsAwarded) {
        this.pointsAwarded = pointsAwarded;
    }
}
