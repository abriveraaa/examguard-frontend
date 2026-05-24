package com.example.examguard.model.faculty.dto.students;

public record FacultySectionDTO(
        String classOfferingId,
        String programCode,
        Integer yearLevel,
        String sectionName,
        String label
) {}