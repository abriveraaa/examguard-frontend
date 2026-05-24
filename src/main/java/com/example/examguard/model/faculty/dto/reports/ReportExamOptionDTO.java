package com.example.examguard.model.faculty.dto.reports;

public record ReportExamOptionDTO(
        Long examId,
        String title,
        Long classOfferingCount
) {
    @Override
    public String toString() {
        return title;
    }
}