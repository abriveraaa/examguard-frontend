package com.example.examguard.model.faculty.dto.reports;

public record ExamSubmissionBreakdownDTO(
        Long examId,
        String examTitle,
        String status,
        Long count
) {}