package com.example.examguard.model.exam.take;

import com.example.examguard.model.enums.QuestionType;

import java.util.ArrayList;
import java.util.List;

public class ExamTakeQuestion {

    private Long questionId;
    private int questionNumber;
    private QuestionType questionType;
    private String questionText;
    private String questionImageUrl;
    private List<ExamTakeChoice> choices = new ArrayList<>();
    private int points;

    private String studentAnswer;
    private boolean markedForReview;

    public ExamTakeQuestion() {
    }

    public ExamTakeQuestion(Long questionId, int questionNumber, QuestionType questionType,
                            String questionText, List<ExamTakeChoice> choices, int points) {
        this.questionId = questionId;
        this.questionNumber = questionNumber;
        this.questionType = questionType;
        this.questionText = questionText;
        this.choices = choices;
        this.points = points;
    }

    public boolean isAnswered() {
        return studentAnswer != null && !studentAnswer.trim().isEmpty();
    }

    public Long getQuestionId() {
        return questionId;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getQuestionImageUrl() {
        return questionImageUrl;
    }

    public List<ExamTakeChoice> getChoices() {
        return choices;
    }

    public int getPoints() {
        return points;
    }

    public String getStudentAnswer() {
        return studentAnswer;
    }

    public boolean isMarkedForReview() {
        return markedForReview;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public void setQuestionImageUrl(String questionImageUrl) {
        this.questionImageUrl = questionImageUrl;
    }

    public void setChoices(List<ExamTakeChoice> choices) {
        this.choices = choices;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void setStudentAnswer(String studentAnswer) {
        this.studentAnswer = studentAnswer;
    }

    public void setMarkedForReview(boolean markedForReview) {
        this.markedForReview = markedForReview;
    }
}