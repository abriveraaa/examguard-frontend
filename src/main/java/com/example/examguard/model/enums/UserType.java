package com.example.examguard.model.enums;

public enum UserType {

    ADMIN("admins"),
    STUDENT("students"),
    FACULTY("faculty");

    private final String path;

    // Constructor
    UserType(String path) {
        this.path = path;
    }

    // Getter
    public String getPath() {
        return path;
    }

    public static UserType fromString(String value) {
        for (UserType type : UserType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null; // if no match
    }
}