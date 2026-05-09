package com.example.examguard.model.faculty;

import java.math.BigDecimal;
import java.util.List;

public class FacultyAttemptAnswerReviewDTO {

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
    private List<FacultyAttemptViolationDTO> violations;

    public Long getAnswerId() { return answerId; }
    public Long getQuestionId() { return questionId; }
    public Integer getQuestionNumber() { return questionNumber; }
    public String getQuestionType() { return questionType; }
    public String getQuestionText() { return questionText; }
    public String getQuestionImageUrl() { return questionImageUrl; }
    public String getStudentAnswer() { return studentAnswer; }
    public String getCorrectAnswer() { return correctAnswer; }
    public BigDecimal getPoints() { return points; }
    public BigDecimal getEarnedPoints() { return earnedPoints; }
    public Boolean getCorrect() { return correct; }
    public Boolean getNeedsManualCheck() { return needsManualCheck; }
    public Boolean getManuallyReviewed() { return manuallyReviewed; }
    public List<FacultyAttemptViolationDTO> getViolations() { return violations; }
    public String getStudentAnswerImageUrl() { return studentAnswerImageUrl; }
    public String getCorrectAnswerImageUrl() { return correctAnswerImageUrl; }
}
