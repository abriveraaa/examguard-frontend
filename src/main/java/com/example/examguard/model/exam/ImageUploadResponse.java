package com.example.examguard.model.exam;

public class ImageUploadResponse {

    private boolean success;
    private String message;
    private String imageUrl;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}