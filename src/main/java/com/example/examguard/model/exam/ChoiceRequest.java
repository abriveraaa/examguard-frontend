package com.example.examguard.model.exam;

public class ChoiceRequest {

    private String choiceLabel;
    private String choiceText;
    private String choiceImageUrl;
    private Boolean correct;

    public ChoiceRequest() {}

    public ChoiceRequest(String choiceText, Boolean correct) {
        this.choiceText = choiceText;
        this.correct = correct;
    }

    public ChoiceRequest(String choiceText, String choiceImageUrl, Boolean correct) {
        this.choiceText = choiceText;
        this.choiceImageUrl = choiceImageUrl;
        this.correct = correct;
    }

    public String getChoiceLabel() {
        return choiceLabel;
    }

    public String getChoiceText() {
        return choiceText;
    }

    public void setChoiceText(String choiceText) {
        this.choiceText = choiceText;
    }

    public String getChoiceImageUrl() {
        return choiceImageUrl;
    }

    public void setChoiceImageUrl(String choiceImageUrl) {
        this.choiceImageUrl = choiceImageUrl;
    }

    public boolean isCorrect() {
        return Boolean.TRUE.equals(correct);
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }
}