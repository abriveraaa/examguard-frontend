package com.example.examguard.model.faculty.dto.reports;

public record FacultyReportSummaryDTO(
        Double averageScore,
        Double submissionRate,
        Long totalViolations,
        Long pendingReview,
        Long penalized
) {}