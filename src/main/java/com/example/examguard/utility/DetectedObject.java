package com.example.examguard.utility;

public class DetectedObject {

    private final String className;
    private final float confidence;
    private final double x;
    private final double y;
    private final double width;
    private final double height;

    public DetectedObject(
            String className,
            float confidence,
            double x,
            double y,
            double width,
            double height
    ) {
        this.className = className;
        this.confidence = confidence;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public String getClassName() {
        return className;
    }

    public float getConfidence() {
        return confidence;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }
}