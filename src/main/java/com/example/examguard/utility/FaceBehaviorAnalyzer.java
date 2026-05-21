package com.example.examguard.utility;

import com.example.examguard.model.ai.MediaPipeFaceResult;

public final class FaceBehaviorAnalyzer {

    private FaceBehaviorAnalyzer() {}

    public enum Level {
        OK, WARNING, VIOLATION
    }

    public record Decision(
            Level level,
            String direction,
            String reason,
            String violationType,
            boolean faceBehaviorViolation
    ) {}

    public static Decision analyze(MediaPipeFaceResult result) {
        if (result == null) {
            return ok("No MediaPipe result yet.");
        }

        if (!result.isFacePresent()) {
            return violation(
                    "NO_FACE",
                    "No face detected.",
                    "FACE_BEHAVIOR_NO_FACE"
            );
        }

        if (result.isMultipleFaces()) {
            return violation(
                    "MULTIPLE",
                    "Multiple faces detected.",
                    "FACE_BEHAVIOR_MULTIPLE_FACES"
            );
        }

        if (result.isLookingDown() || result.isEyeGazeDown()) {
            return violation(
                    "DOWN",
                    "Looking down detected.",
                    "FACE_BEHAVIOR_DOWN_NO_INPUT"
            );
        }

        if (result.isLookingUp() || result.isEyeGazeUp()) {
            return violation(
                    "UP",
                    "Looking up detected.",
                    "FACE_BEHAVIOR_UP_NO_INPUT"
            );
        }

        /*
         * Your Python currently only has eyeGazeAway / lookingAway,
         * not separate left/right. For now we tag it as SIDE.
         * Later, when you add faceDirection=LEFT/RIGHT, only update this method.
         */
        if (result.isLookingAway() || result.isEyeGazeAway()) {
            return violation(
                    "SIDE",
                    "Looking left/right detected.",
                    "FACE_BEHAVIOR_SIDE_NO_INPUT"
            );
        }

        if (result.isTooDark()) {
            return warning("Camera too dark.");
        }

        if (result.isTooBright()) {
            return warning("Camera too bright.");
        }

        if (result.isTooBlurry()) {
            return warning("Camera blurry.");
        }

        if (result.isEyesProbablyCovered()) {
            return warning("Eyes are not clearly visible.");
        }

        if (result.isMouthProbablyCovered()) {
            return warning("Lower face may be covered.");
        }

        if (result.isFaceTooClose()) {
            return warning("Face is too close.");
        }

        if (result.isFaceTooFar()) {
            return warning("Face is too far.");
        }

        if (result.isFaceNotCentered()) {
            return warning("Face is not centered.");
        }

        return ok("Face monitoring state is normal.");
    }

    private static Decision ok(String reason) {
        return new Decision(Level.OK, "CENTER", reason, null, false);
    }

    private static Decision warning(String reason) {
        return new Decision(Level.WARNING, "CENTER", reason, null, false);
    }

    private static Decision violation(String direction, String reason, String type) {
        return new Decision(Level.VIOLATION, direction, reason, type, true);
    }
}