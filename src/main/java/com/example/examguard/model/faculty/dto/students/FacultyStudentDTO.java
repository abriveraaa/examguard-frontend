package com.example.examguard.model.faculty.dto.students;

public record FacultyStudentDTO(
        String studentId,
        String fullName,
        String emailAddress,

        String collegeCode,
        String collegeName,

        String programCode,
        String programName,
        String yearLevel,
        String sectionName,

        String courseCode,
        String courseDescription,
        String classOfferingId
) {}