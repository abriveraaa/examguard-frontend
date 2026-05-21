package com.example.examguard.model.ai;

public class MediaPipeFaceResult {

    private boolean available;
    private boolean facePresent;
    private int faceCount;
    private boolean multipleFaces;
    private boolean lookingAway;
    private boolean lookingDown;
    private double yaw;
    private double pitch;
    private String version;
    private String message;

    private FaceBox box;
    private FaceLandmarks landmarks;

    private boolean lookingUp;
    private boolean eyeGazeAway;
    private boolean eyeGazeDown;
    private boolean eyeGazeUp;
    private boolean eyeGazeNotCentered;
    private boolean faceTooClose;
    private boolean faceTooFar;
    private boolean faceNotCentered;
    private boolean warning;
    private boolean redViolation;
    private double gazeX;
    private double gazeY;
    private String severity;

    private double brightness;
    private double blurScore;
    private boolean tooDark;
    private boolean tooBright;
    private boolean tooBlurry;

    private boolean eyesProbablyCovered;
    private boolean mouthProbablyCovered;
    private boolean facePartiallyObstructed;
    private double avgEyeOpening;
    private double mouthOpening;
    private double mouthWidth;

    public boolean isAvailable() {
        return available;
    }

    public boolean isFacePresent() {
        return facePresent;
    }

    public int getFaceCount() {
        return faceCount;
    }

    public boolean isMultipleFaces() {
        return multipleFaces;
    }

    public boolean isLookingAway() {
        return lookingAway;
    }

    public boolean isLookingDown() {
        return lookingDown;
    }

    public double getYaw() {
        return yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public String getVersion() {
        return version;
    }

    public String getMessage() {
        return message;
    }

    public FaceBox getBox() {
        return box;
    }

    public FaceLandmarks getLandmarks() {
        return landmarks;
    }

    public boolean isLookingUp() {
        return lookingUp;
    }

    public boolean isEyeGazeAway() {
        return eyeGazeAway;
    }

    public boolean isEyeGazeDown() {
        return eyeGazeDown;
    }

    public boolean isEyeGazeUp() {
        return eyeGazeUp;
    }

    public boolean isEyeGazeNotCentered() {
        return eyeGazeNotCentered;
    }

    public boolean isFaceTooClose() {
        return faceTooClose;
    }

    public boolean isFaceTooFar() {
        return faceTooFar;
    }

    public boolean isFaceNotCentered() {
        return faceNotCentered;
    }

    public boolean isWarning() {
        return warning;
    }

    public boolean isRedViolation() {
        return redViolation;
    }

    public double getGazeX() {
        return gazeX;
    }

    public double getGazeY() {
        return gazeY;
    }

    public String getSeverity() {
        return severity;
    }

    public double getBrightness() { return brightness; }
    public double getBlurScore() { return blurScore; }
    public boolean isTooDark() { return tooDark; }
    public boolean isTooBright() { return tooBright; }
    public boolean isTooBlurry() { return tooBlurry; }

    public boolean isEyesProbablyCovered() {
        return eyesProbablyCovered;
    }

    public boolean isMouthProbablyCovered() {
        return mouthProbablyCovered;
    }

    public boolean isFacePartiallyObstructed() {
        return facePartiallyObstructed;
    }

    public double getAvgEyeOpening() {
        return avgEyeOpening;
    }

    public double getMouthOpening() {
        return mouthOpening;
    }

    public double getMouthWidth() {
        return mouthWidth;
    }

    public static class FaceBox {

        private double x;
        private double y;
        private double width;
        private double height;

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

    public static class FaceLandmarks {

        private Landmark nose;
        private Landmark leftEye;
        private Landmark rightEye;
        private Landmark mouth;
        private Landmark chin;
        private Landmark leftIris;
        private Landmark rightIris;

        public Landmark getNose() {
            return nose;
        }

        public Landmark getLeftEye() {
            return leftEye;
        }

        public Landmark getRightEye() {
            return rightEye;
        }

        public Landmark getMouth() {
            return mouth;
        }

        public Landmark getChin() {
            return chin;
        }

        public Landmark getLeftIris() {
            return leftIris;
        }

        public Landmark getRightIris() {
            return rightIris;
        }
    }

    public static class Landmark {

        private double x;
        private double y;

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }
}