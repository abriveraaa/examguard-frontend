package com.example.examguard.model.exam.dto;

import com.example.examguard.model.exam.request.EssayRubricRequest;
import com.example.examguard.model.exam.response.EssayRubricScoreResponse;

import java.math.BigDecimal;
import java.util.List;

public class ExamAttemptAnswerReviewDTO {

    private Long answerId;
    private Long questionId;
    private Integer questionNumber;
    private String questionType;
    private String questionText;
    private String questionImageUrl;
    private String studentAnswer;
    private String studentAnswerImageUrl;
    private String correctAnswer;
    private String correctAnswerImageUrl;
    private BigDecimal points;
    private BigDecimal earnedPoints;
    private Boolean correct;
    private Boolean needsManualCheck;
    private Boolean manuallyReviewed;
    private List<ExamAttemptViolationDTO> violations;
    private String questionInstruction;
    private List<EssayRubricRequest> rubrics;
    private List<EssayRubricScoreResponse> rubricScores;
    private String facultyFeedback;
    private String reviewStatus;
    private Boolean needsChecking;

    public Long getAnswerId() {
        return answerId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public Integer getQuestionNumber() {
        return questionNumber;
    }

    public String getQuestionType() {
        return questionType;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getQuestionImageUrl() {
        return questionImageUrl;
    }

    public String getStudentAnswer() {
        return studentAnswer;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public BigDecimal getPoints() {
        return points;
    }

    public BigDecimal getEarnedPoints() {
        return earnedPoints;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public Boolean getNeedsManualCheck() {
        return needsManualCheck;
    }

    public Boolean getManuallyReviewed() {
        return manuallyReviewed;
    }

    public List<ExamAttemptViolationDTO> getViolations() {
        return violations;
    }

    public String getStudentAnswerImageUrl() {
        return studentAnswerImageUrl;
    }

    public String getCorrectAnswerImageUrl() {
        return correctAnswerImageUrl;
    }

    public String getQuestionInstruction() {
        return questionInstruction;
    }

    public void setQuestionInstruction(String questionInstruction) {
        this.questionInstruction = questionInstruction;
    }

    public List<EssayRubricRequest> getRubrics() {
        return rubrics;
    }

    public void setRubrics(List<EssayRubricRequest> rubrics) {
        this.rubrics = rubrics;
    }

    public List<EssayRubricScoreResponse> getRubricScores() {
        return rubricScores;
    }

    public void setRubricScores(
            List<EssayRubricScoreResponse> rubricScores
    ) {
        this.rubricScores = rubricScores;
    }

    public String getFacultyFeedback() {
        return facultyFeedback;
    }

    public void setFacultyFeedback(String facultyFeedback) {
        this.facultyFeedback = facultyFeedback;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public Boolean getNeedsChecking() {
        return needsChecking;
    }

    public void setNeedsChecking(Boolean needsChecking) {
        this.needsChecking = needsChecking;
    }

    public void setEarnedPoints(BigDecimal earnedPoints) {
        this.earnedPoints = earnedPoints;
    }

    public void setManuallyReviewed(boolean manuallyReviewed) {
        this.manuallyReviewed = manuallyReviewed;
    }
}
