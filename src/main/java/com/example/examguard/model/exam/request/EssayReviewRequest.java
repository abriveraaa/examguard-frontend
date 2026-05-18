package com.example.examguard.model.exam.request;

import java.util.ArrayList;
import java.util.List;

public class EssayReviewRequest {

    private Long answerId;

    private String facultyFeedback;

    private List<EssayRubricScoreRequest> rubricScores =
            new ArrayList<>();

    public Long getAnswerId() {
        return answerId;
    }

    public void setAnswerId(Long answerId) {
        this.answerId = answerId;
    }

    public String getFacultyFeedback() {
        return facultyFeedback;
    }

    public void setFacultyFeedback(String facultyFeedback) {
        this.facultyFeedback = facultyFeedback;
    }

    public List<EssayRubricScoreRequest> getRubricScores() {
        return rubricScores;
    }

    public void setRubricScores(
            List<EssayRubricScoreRequest> rubricScores
    ) {
        this.rubricScores = rubricScores;
    }
}
