package com.example.examguard.model.student.result;

import java.util.List;

public class StudentExamResultQuestionResponse {

    private Long questionId;
    private Integer questionNumber;
    private String questionType;
    private String questionText;
    private String questionImageUrl;

    private Double points;
    private Double earnedPoints;

    private String studentAnswer;
    private String correctAnswer;
    private Boolean correct;
    private String feedback;

    private List<StudentExamResultChoiceResponse> choices;
    private List<StudentExamResultViolationResponse> violations;
    private List<StudentExamResultRubricResponse> rubrics;

    public Long getQuestionId() { return questionId; }
    public Integer getQuestionNumber() { return questionNumber; }
    public String getQuestionType() { return questionType; }
    public String getQuestionText() { return questionText; }
    public String getQuestionImageUrl() { return questionImageUrl; }
    public Double getPoints() { return points; }
    public Double getEarnedPoints() { return earnedPoints; }
    public String getStudentAnswer() { return studentAnswer; }
    public String getCorrectAnswer() { return correctAnswer; }
    public Boolean getCorrect() { return correct; }
    public String getFeedback() { return feedback; }
    public List<StudentExamResultChoiceResponse> getChoices() { return choices; }
    public List<StudentExamResultViolationResponse> getViolations() { return violations; }
    public List<StudentExamResultRubricResponse> getRubrics() { return rubrics; }
}
