package com.example.examguard.model.student.dashboard;

public record ExamCardVM(
        Long examId,
        String title,
        String courseCode,
        String courseDescription,
        String term,
        String academicYear,
        String classOfferingStatus,
        String mode,
        String faculty,
        Integer duration,
        String schedule,
        String status,
        Long questionCount,
        boolean actionable
) {}