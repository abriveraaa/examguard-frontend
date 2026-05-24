package com.example.examguard.model.faculty.dto.reports;

public record FacultyReportFilter(
        String academicYear,
        String term,
        String courseCode,
        String classOfferingId,
        Long examId
) {}