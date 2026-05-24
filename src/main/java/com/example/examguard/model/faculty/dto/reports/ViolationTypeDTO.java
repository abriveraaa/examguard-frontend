package com.example.examguard.model.faculty.dto.reports;

public record ViolationTypeDTO(
        String violationType,
        Long count
) {}