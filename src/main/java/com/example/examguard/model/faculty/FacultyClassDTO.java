package com.example.examguard.model.faculty;

public class FacultyClassDTO {
    private String classOfferingId;
    private String courseCode;
    private String courseDescription;
    private String programCode;
    private String sectionName;
    private String yearLevel;
    private String academicYear;
    private String term;
    private Long enrolledCount;

    public String getClassOfferingId() { return classOfferingId; }
    public String getCourseCode() { return courseCode; }
    public String getCourseDescription() { return courseDescription; }
    public String getProgramCode() { return programCode; }
    public String getSectionName() { return sectionName; }
    public String getYearLevel() { return yearLevel; }
    public String getAcademicYear() { return academicYear; }
    public String getTerm() { return term; }
    public Long getEnrolledCount() { return enrolledCount; }
}
