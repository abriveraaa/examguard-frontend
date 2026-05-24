package com.example.examguard.model.faculty.dto.reports;

public record ExamParticipationDTO(
        Long examId,
        String examTitle,
        Long totalTakers,
        Double averageScore
) {}