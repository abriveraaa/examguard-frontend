package com.example.examguard.model.exam;

public class ClassOffering {

    private String classOfferingId;
    private String programCode;
    private Integer yearLevel;
    private String sectionName;
    private String courseCode;
    private String courseDescription;

    public String getDisplayName() {
        return programCode + " | " + courseCode + " | " + courseDescription;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    public String getClassOfferingId() { return classOfferingId;
    }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
}