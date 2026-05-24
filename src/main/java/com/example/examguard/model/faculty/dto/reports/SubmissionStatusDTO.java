package com.example.examguard.model.faculty.dto.reports;

public record SubmissionStatusDTO(
        String status,
        Long count
) {}