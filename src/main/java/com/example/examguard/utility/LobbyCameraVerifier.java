package com.example.examguard.utility;

import com.example.examguard.model.ai.MediaPipeFaceResult;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class LobbyCameraVerifier {

    private final LobbyCameraPreviewService previewService;

    private static final int SAMPLE_COUNT = 8;
    private static final int REQUIRED_VALID_FRAMES = 4;

    public LobbyCameraVerifier(LobbyCameraPreviewService previewService) {
        this.previewService = previewService;
    }

    public LobbyCheckResult checkCamera() {
        if (previewService == null) {
            return LobbyCheckResult.fail("Camera preview is not ready.");
        }

        Mat frame = previewService.getActiveFrame();

        if (frame == null || frame.empty()) {
            return LobbyCheckResult.fail("No camera frame detected. Please allow camera access or pair your phone camera.");
        }

        return LobbyCheckResult.pass("Camera is available.");
    }

    public LobbyCheckResult checkFace() {
        MediaPipeFaceResult result = MediaPipeOverlayStore.getLatestResult();

        if (result == null) {
            return LobbyCheckResult.fail("Face verification is still loading. Please wait a moment and try again.");
        }

        if (!result.isFacePresent()) {
            return LobbyCheckResult.fail("No face detected. Please center your face in the camera.");
        }

        if (result.isMultipleFaces()) {
            return LobbyCheckResult.fail("Multiple faces detected. Only one examinee should be visible.");
        }

        if (result.isLookingAway()) {
            return LobbyCheckResult.fail("Please face the camera directly.");
        }

        if (result.isLookingDown()) {
            return LobbyCheckResult.fail("Please keep your face visible and avoid looking down.");
        }

        if (result.isFaceTooFar()) {
            return LobbyCheckResult.fail("Please move closer or adjust the camera so your face is clearer.");
        }

        return LobbyCheckResult.pass("One examinee is clearly visible.");
    }

    public LobbyCheckResult checkDeskAndFrameQuality() {
        if (previewService == null) {
            return LobbyCheckResult.fail("Camera preview is not ready.");
        }

        int validFrames = 0;
        double totalBrightness = 0;
        double totalBlurScore = 0;

        for (int i = 0; i < SAMPLE_COUNT; i++) {
            Mat frame = previewService.getActiveFrame();

            if (frame == null || frame.empty()) {
                sleepQuietly(120);
                continue;
            }

            Mat gray = new Mat();

            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);

            totalBrightness += Core.mean(gray).val[0];
            totalBlurScore += calculateBlurScore(gray);

            validFrames++;

            gray.release();

            sleepQuietly(120);
        }

        if (validFrames < REQUIRED_VALID_FRAMES) {
            return LobbyCheckResult.fail("Camera view is unstable. Please retry.");
        }

        double avgBrightness = totalBrightness / validFrames;
        double avgBlurScore = totalBlurScore / validFrames;

        if (avgBrightness < 35) {
            return LobbyCheckResult.fail("Camera view is too dark. Improve lighting.");
        }

        if (avgBrightness > 230) {
            return LobbyCheckResult.fail("Camera view is too bright. Reduce glare or backlight.");
        }

        if (avgBlurScore < 15) {
            return LobbyCheckResult.fail("Camera view is blurry. Adjust the camera before starting.");
        }

        MediaPipeFaceResult result = MediaPipeOverlayStore.getLatestResult();

        if (result != null) {
            if (result.isTooDark()) {
                return LobbyCheckResult.fail("Camera view is too dark. Improve lighting.");
            }

            if (result.isTooBright()) {
                return LobbyCheckResult.fail("Camera view is too bright. Reduce glare or backlight.");
            }

            if (result.isTooBlurry()) {
                return LobbyCheckResult.fail("Camera view is blurry. Adjust the camera before starting.");
            }

            if (result.isEyesProbablyCovered()) {
                return LobbyCheckResult.fail("Eyes are not clearly visible. Please adjust lighting or camera angle.");
            }

            if (result.isFacePartiallyObstructed()) {
                return LobbyCheckResult.fail("Face is partially obstructed. Please keep your face visible.");
            }
        }

        return LobbyCheckResult.pass("Camera view is clear enough for monitoring.");
    }

    private double calculateBlurScore(Mat gray) {
        Mat laplacian = new Mat();

        Imgproc.Laplacian(
                gray,
                laplacian,
                CvType.CV_64F
        );

        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stdDev = new MatOfDouble();

        Core.meanStdDev(laplacian, mean, stdDev);

        double score = Math.pow(stdDev.toArray()[0], 2);

        laplacian.release();
        mean.release();
        stdDev.release();

        return score;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}