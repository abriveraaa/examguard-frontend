package com.example.examguard.model.exam.response;

import com.example.examguard.model.exam.request.QuestionRequest;

import java.util.List;

public class UploadExamTemplateResponse {

    private boolean success;
    private String message;
    private List<QuestionRequest> questions;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<QuestionRequest> getQuestions() {
        return questions;
    }
}