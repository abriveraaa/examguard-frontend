package com.example.examguard.utility;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import com.example.examguard.model.ai.MediaPipeFaceResult;
import com.example.examguard.utility.FaceBehaviorAnalyzer;
import javafx.scene.control.Label;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;
import java.util.List;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class LobbyCameraPreviewService {

    private static boolean openCvLoaded = false;

    private volatile boolean running = false;
    private Thread previewThread;
    private VideoCapture camera;
    private long gazeNotCenteredStartedAt = 0L;
    private static final long GAZE_RED_AFTER_MS = 3000L;

    private final ImageView imageView;
    private final Label placeholderLabel;
    private final LobbyObjectDetector objectDetector;
    private volatile Mat latestLaptopFrame;
    private volatile Mat latestPhoneFrame;

    public LobbyCameraPreviewService(
            ImageView imageView,
            Label placeholderLabel, LobbyObjectDetector objectDetector
    ) {
        this.imageView = imageView;
        this.placeholderLabel = placeholderLabel;
        this.objectDetector = objectDetector;
        loadOpenCv();
    }

    private static synchronized void loadOpenCv() {
        if (!openCvLoaded) {
            OpenCV.loadLocally();
            openCvLoaded = true;
        }
    }

    public void start() {
        if (running) {
            return;
        }

        running = true;

        previewThread = new Thread(() -> {
            try {
                camera = new VideoCapture(0);

                camera.set(3, 1280);
                camera.set(4, 720);

                if (!camera.isOpened()) {
                    Platform.runLater(() -> {
                        if (placeholderLabel != null) {
                            placeholderLabel.setText("Camera unavailable or permission denied.");
                            placeholderLabel.setVisible(true);
                            placeholderLabel.setManaged(true);
                        }
                    });
                    running = false;
                    return;
                }

                Platform.runLater(() -> {
                    if (placeholderLabel != null) {
                        placeholderLabel.setVisible(false);
                        placeholderLabel.setManaged(false);
                    }
                });

                Mat frame = new Mat();

                while (running) {
                    boolean captured = camera.read(frame);

                    if (captured && !frame.empty()) {
                        latestLaptopFrame = frame.clone();

                        drawMediaPipeOverlay(frame);

                        List<DetectedObject> objects = java.util.Collections.emptyList();

                        if (objectDetector != null && objectDetector.isAvailable()) {
                            objects = objectDetector.detect(frame);
                        }

                        objectDetector.drawDebugDetections(frame, objects);

                        BufferedImage bufferedImage =
                                matToBufferedImage(frame);

                        Platform.runLater(() -> {
                            if (imageView != null) {
                                imageView.setImage(
                                        SwingFXUtils.toFXImage(bufferedImage, null)
                                );
                            }
                        });
                    }

                    sleepQuietly(33);
                }

            } catch (Exception e) {
                e.printStackTrace();

                Platform.runLater(() -> {
                    if (placeholderLabel != null) {
                        placeholderLabel.setText("Camera preview failed.");
                        placeholderLabel.setVisible(true);
                        placeholderLabel.setManaged(true);
                    }
                });

            } finally {
                releaseCamera();
            }
        }, "lobby-camera-preview-thread");

        previewThread.setDaemon(true);
        previewThread.start();
    }

    public Mat getLatestFrame() {
        return getActiveFrame();
    }

    public Mat getActiveFrame() {

        if (latestPhoneFrame != null
                && !latestPhoneFrame.empty()) {

            return latestPhoneFrame.clone();
        }

        if (latestLaptopFrame != null
                && !latestLaptopFrame.empty()) {

            return latestLaptopFrame.clone();
        }

        return null;
    }

    public void updatePhoneFrame(Mat frame) {

        if (frame == null || frame.empty()) {
            return;
        }

        if (latestPhoneFrame != null) {
            latestPhoneFrame.release();
        }

        latestPhoneFrame = frame.clone();
    }

    public void drawOverlayForExternalFrame(Mat frame) {
        if (frame == null || frame.empty()) {
            return;
        }

        drawMediaPipeOverlay(frame);
    }

    public void clearPhoneFrame() {

        if (latestPhoneFrame != null) {
            latestPhoneFrame.release();
            latestPhoneFrame = null;
        }
    }

    public void stop() {
        running = false;

        if (previewThread != null) {
            try {
                previewThread.join(700);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            previewThread = null;
        }

        Platform.runLater(() -> {
            if (imageView != null) {
                imageView.setImage(null);
            }

            if (placeholderLabel != null) {
                placeholderLabel.setText("Camera preview will appear here");
                placeholderLabel.setVisible(true);
                placeholderLabel.setManaged(true);
            }
        });
    }

    private void releaseCamera() {
        try {
            if (camera != null) {
                camera.release();
                camera = null;
            }

            if (latestLaptopFrame != null) {
                latestLaptopFrame.release();
                latestLaptopFrame = null;
            }

            if (latestPhoneFrame != null) {
                latestPhoneFrame.release();
                latestPhoneFrame = null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_3BYTE_BGR;

        BufferedImage image = new BufferedImage(
                mat.width(),
                mat.height(),
                type
        );

        byte[] data =
                ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        mat.get(0, 0, data);

        return image;
    }

    private void drawMediaPipeOverlay(Mat frame) {
        MediaPipeFaceResult result = MediaPipeOverlayStore.getLatestResult();

        FaceBehaviorAnalyzer.Decision decision =
                FaceBehaviorAnalyzer.analyze(result);

        if (result == null) {
            gazeNotCenteredStartedAt = 0L;
            return;
        }

        if (!result.isFacePresent()) {

            gazeNotCenteredStartedAt = 0L;

            Scalar color = new Scalar(0, 0, 255);

            Imgproc.putText(
                    frame,
                    "RED | No face detected",
                    new Point(30, 40),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.8,
                    color,
                    2
            );

            Imgproc.putText(
                    frame,
                    "Ensure your face is visible to the camera",
                    new Point(30, 75),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.65,
                    color,
                    2
            );

            Imgproc.putText(
                    frame,
                    "brightness="
                            + String.format("%.1f", result.getBrightness())
                            + " blur="
                            + String.format("%.1f", result.getBlurScore()),
                    new Point(30, 110),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.55,
                    color,
                    2
            );

            return;
        }

        int frameWidth = frame.width();
        int frameHeight = frame.height();

        MediaPipeFaceResult.FaceBox box = result.getBox();

        if (box != null) {
            Point topLeft = new Point(
                    box.getX() * frameWidth,
                    box.getY() * frameHeight
            );

            Point bottomRight = new Point(
                    (box.getX() + box.getWidth()) * frameWidth,
                    (box.getY() + box.getHeight()) * frameHeight
            );

            boolean eyeGazeNotCentered =
                    result.isEyeGazeNotCentered()
                            || result.isEyeGazeAway()
                            || result.isEyeGazeDown()
                            || result.isEyeGazeUp()
                            || result.isLookingUp();

            long now = System.currentTimeMillis();

            if (eyeGazeNotCentered) {
                if (gazeNotCenteredStartedAt == 0L) {
                    gazeNotCenteredStartedAt = now;
                }
            } else {
                gazeNotCenteredStartedAt = 0L;
            }

            long gazeDurationMs =
                    gazeNotCenteredStartedAt > 0L
                            ? now - gazeNotCenteredStartedAt
                            : 0L;

            boolean gazeRedByDuration =
                    gazeDurationMs >= GAZE_RED_AFTER_MS;

            String reason = decision.reason();

            boolean strictViolation =
                    decision.level() == FaceBehaviorAnalyzer.Level.VIOLATION;

            boolean warning =
                    decision.level() == FaceBehaviorAnalyzer.Level.WARNING;

            if (result.isMultipleFaces()) {
                strictViolation = true;
                reason = "Multiple faces";
            } else if (result.isLookingAway()) {
                strictViolation = true;
                reason = "Head turned left/right";
            } else if (result.isLookingDown()) {
                strictViolation = true;
                reason = "Head looking down";
            } else if (gazeRedByDuration) {
                strictViolation = true;

                if (result.isEyeGazeAway()) {
                    reason = "Eye gaze left/right for 3s+";
                } else if (result.isEyeGazeDown()) {
                    reason = "Eye gaze down for 3s+";
                } else if (result.isEyeGazeUp() || result.isLookingUp()) {
                    reason = "Eye/head gaze up for 3s+";
                } else {
                    reason = "Gaze not centered for 3s+";
                }
            }


            if (!strictViolation) {
                if (eyeGazeNotCentered) {
                    warning = true;

                    if (result.isEyeGazeAway()) {
                        reason = "Eye gaze left/right";
                    } else if (result.isEyeGazeDown()) {
                        reason = "Eye gaze down";
                    } else if (result.isEyeGazeUp() || result.isLookingUp()) {
                        reason = "Eye/head gaze up";
                    } else {
                        reason = "Gaze not centered";
                    }

                } else if (result.isTooDark()) {
                    warning = true;
                    reason = "Camera too dark";
                } else if (result.isTooBright()) {
                    warning = true;
                    reason = "Camera too bright";
                } else if (result.isTooBlurry()) {
                    warning = true;
                    reason = "Camera blurry";
                } else if (result.isEyesProbablyCovered()) {
                    warning = true;
                    reason = "Eyes not clearly visible";
                } else if (result.isMouthProbablyCovered()) {
                    warning = true;
                    reason = "Lower face may be covered";
                } else if (result.isFacePartiallyObstructed()) {
                    warning = true;
                    reason = "Face partially obstructed";
                } else if (result.isFaceTooFar()) {
                    warning = true;
                    reason = "Adjust camera position";
                } else if (result.isFaceTooClose()) {
                    warning = true;
                    reason = "Face close to camera";
                } else if (result.isFaceNotCentered()) {
                    warning = true;
                    reason = "Face not centered";
                } else if (result.isWarning()) {
                    warning = true;
                    reason = result.getMessage() == null || result.getMessage().isBlank()
                            ? "Warning"
                            : result.getMessage();
                }
            }

            Scalar color;
            String state;

            if (strictViolation) {
                color = new Scalar(0, 0, 255);
                state = "RED";
            } else if (warning) {
                color = new Scalar(0, 165, 255);
                state = "WARNING";
            } else {
                color = new Scalar(0, 255, 0);
                state = "OK";
            }

            String overlayMessage =
                    state
                            + " | "
                            + reason
                            + " | faces="
                            + result.getFaceCount();

            String metricsMessage =
                    "yaw="
                            + String.format("%.3f", result.getYaw())
                            + " pitch="
                            + String.format("%.3f", result.getPitch())
                            + " gazeX="
                            + String.format("%.3f", result.getGazeX())
                            + " gazeY="
                            + String.format("%.3f", result.getGazeY())
                            + " gazeMs="
                            + gazeDurationMs;

            String qualityMessage =
                    "brightness="
                            + String.format("%.1f", result.getBrightness())
                            + " blur="
                            + String.format("%.1f", result.getBlurScore())
                            + " eyes="
                            + String.format("%.4f", result.getAvgEyeOpening())
                            + " mouth="
                            + String.format("%.4f", result.getMouthOpening());

            Imgproc.rectangle(
                    frame,
                    topLeft,
                    bottomRight,
                    color,
                    2
            );

            Imgproc.putText(
                    frame,
                    overlayMessage,
                    new Point(
                            topLeft.x,
                            Math.max(20, topLeft.y - 10)
                    ),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.6,
                    color,
                    2
            );

            Imgproc.putText(
                    frame,
                    metricsMessage,
                    new Point(
                            topLeft.x,
                            Math.min(frameHeight - 45, bottomRight.y + 25)
                    ),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.55,
                    color,
                    2
            );

            Imgproc.putText(
                    frame,
                    qualityMessage,
                    new Point(
                            topLeft.x,
                            Math.min(frameHeight - 20, bottomRight.y + 50)
                    ),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.55,
                    color,
                    2
            );
        }

        MediaPipeFaceResult.FaceLandmarks landmarks =
                result.getLandmarks();

        if (landmarks != null) {
            drawLandmark(frame, landmarks.getNose(), frameWidth, frameHeight);
            drawLandmark(frame, landmarks.getLeftEye(), frameWidth, frameHeight);
            drawLandmark(frame, landmarks.getRightEye(), frameWidth, frameHeight);
            drawLandmark(frame, landmarks.getMouth(), frameWidth, frameHeight);
            drawLandmark(frame, landmarks.getChin(), frameWidth, frameHeight);
            drawLandmark(frame, landmarks.getLeftIris(), frameWidth, frameHeight);
            drawLandmark(frame, landmarks.getRightIris(), frameWidth, frameHeight);
        }
    }

    private void drawLandmark(
            Mat frame,
            MediaPipeFaceResult.Landmark landmark,
            int frameWidth,
            int frameHeight
    ) {
        if (landmark == null) {
            return;
        }

        Imgproc.circle(
                frame,
                new Point(
                        landmark.getX() * frameWidth,
                        landmark.getY() * frameHeight
                ),
                5,
                new Scalar(255, 255, 0),
                -1
        );
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}