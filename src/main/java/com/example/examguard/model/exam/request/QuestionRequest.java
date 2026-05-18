package com.example.examguard.model.exam.request;

import java.util.ArrayList;
import java.util.List;

public class QuestionRequest {

    private String questionText;
    private String questionImageUrl;
    private String questionType;
    private Integer points;
    private String correctAnswer;
    private String questionInstruction;

    private List<ChoiceRequest> choices;
    private List<EssayRubricRequest> rubrics = new ArrayList<>();

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getQuestionImageUrl() {
        return questionImageUrl;
    }

    public void setQuestionImageUrl(String questionImageUrl) {
        this.questionImageUrl = questionImageUrl;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getQuestionInstruction() {
        return questionInstruction;
    }

    public void setQuestionInstruction(String questionInstruction) {
        this.questionInstruction = questionInstruction;
    }

    public List<ChoiceRequest> getChoices() {
        return choices;
    }

    public void setChoices(List<ChoiceRequest> choices) {
        this.choices = choices;
    }

    public List<EssayRubricRequest> getRubrics() {
        return rubrics;
    }

    public void setRubrics(List<EssayRubricRequest> rubrics) {
        this.rubrics = rubrics == null ? new ArrayList<>() : rubrics;
    }
}