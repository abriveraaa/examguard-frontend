package com.example.examguard.utility;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class LobbyCameraVerifier {

    private static boolean openCvLoaded = false;

    private Mat lastFrame;
    private int lastFaceCount = -1;

    private static final int SAMPLE_COUNT = 8;
    private static final int WARMUP_FRAMES = 5;
    private static final int REQUIRED_ONE_FACE_FRAMES = 5;

    public LobbyCameraVerifier() {
        loadOpenCv();
    }

    private static synchronized void loadOpenCv() {
        if (!openCvLoaded) {
            OpenCV.loadLocally();
            openCvLoaded = true;
        }
    }

    public LobbyCheckResult checkCamera() {
        VideoCapture camera = null;

        try {
            camera = new VideoCapture(0);

            if (!camera.isOpened()) {
                return LobbyCheckResult.fail("No camera detected or camera permission was denied.");
            }

            Mat frame = captureStableFrame(camera);

            if (frame == null || frame.empty()) {
                return LobbyCheckResult.fail("Camera opened, but no clear frame was captured.");
            }

            lastFrame = frame.clone();
            return LobbyCheckResult.pass("Camera is available.");

        } catch (Exception e) {
            return LobbyCheckResult.fail("Camera unavailable or permission denied.");
        } finally {
            if (camera != null) {
                camera.release();
            }
        }
    }

    public LobbyCheckResult checkFace() {
        VideoCapture camera = null;

        try {
            camera = new VideoCapture(0);

            if (!camera.isOpened()) {
                return LobbyCheckResult.fail("No camera detected or camera permission was denied.");
            }

            CascadeClassifier faceDetector = new CascadeClassifier(extractCascadeFile());

            if (faceDetector.empty()) {
                return LobbyCheckResult.fail("Face detector file failed to load.");
            }

            warmUpCamera(camera);

            int oneFaceFrames = 0;
            int noFaceFrames = 0;
            int multipleFaceFrames = 0;
            Mat bestFrame = null;

            for (int i = 0; i < SAMPLE_COUNT; i++) {
                Mat frame = new Mat();
                boolean captured = camera.read(frame);

                if (!captured || frame.empty()) {
                    sleepQuietly(120);
                    continue;
                }

                int faceCount = countFaces(frame, faceDetector);

                if (faceCount == 1) {
                    oneFaceFrames++;
                    bestFrame = frame.clone();
                } else if (faceCount == 0) {
                    noFaceFrames++;
                } else {
                    multipleFaceFrames++;
                }

                sleepQuietly(150);
            }

            if (multipleFaceFrames >= 2) {
                lastFaceCount = 2;
                return LobbyCheckResult.fail("Multiple faces detected. Only one student is allowed.");
            }

            if (oneFaceFrames >= REQUIRED_ONE_FACE_FRAMES) {
                lastFaceCount = 1;

                if (bestFrame != null) {
                    lastFrame = bestFrame.clone();
                }

                return LobbyCheckResult.pass("One face detected consistently.");
            }

            lastFaceCount = 0;

            if (noFaceFrames >= oneFaceFrames) {
                return LobbyCheckResult.fail("No face detected consistently. Sit in front of the camera.");
            }

            return LobbyCheckResult.fail("Face detection is unstable. Center your face and improve lighting.");

        } catch (Exception e) {
            return LobbyCheckResult.fail("Face detection failed.");
        } finally {
            if (camera != null) {
                camera.release();
            }
        }
    }

    public LobbyCheckResult checkDeskAndFrameQuality() {
        VideoCapture camera = null;

        try {
            camera = new VideoCapture(0);

            if (!camera.isOpened()) {
                return LobbyCheckResult.fail("No camera detected or camera permission was denied.");
            }

            warmUpCamera(camera);

            int validFrames = 0;
            double totalBrightness = 0;
            double totalBlurScore = 0;

            for (int i = 0; i < SAMPLE_COUNT; i++) {
                Mat frame = new Mat();
                boolean captured = camera.read(frame);

                if (!captured || frame.empty()) {
                    sleepQuietly(120);
                    continue;
                }

                lastFrame = frame.clone();

                Mat gray = new Mat();
                Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);

                totalBrightness += Core.mean(gray).val[0];
                totalBlurScore += calculateBlurScore(gray);
                validFrames++;

                sleepQuietly(150);
            }

            if (validFrames < 4) {
                return LobbyCheckResult.fail("Camera view is unstable. Please retry.");
            }

            double avgBrightness = totalBrightness / validFrames;
            double avgBlurScore = totalBlurScore / validFrames;

            if (avgBrightness < 45) {
                return LobbyCheckResult.fail("Camera view is too dark. Improve lighting.");
            }

            if (avgBrightness > 230) {
                return LobbyCheckResult.fail("Camera view is too bright. Reduce glare or backlight.");
            }

            if (avgBlurScore < 80) {
                return LobbyCheckResult.fail("Camera view is blurry. Adjust the camera before starting.");
            }

            return LobbyCheckResult.pass("Camera view is clear enough for monitoring.");

        } catch (Exception e) {
            return LobbyCheckResult.fail("Camera view quality check failed.");
        } finally {
            if (camera != null) {
                camera.release();
            }
        }
    }

    private Mat captureStableFrame(VideoCapture camera) {
        warmUpCamera(camera);

        for (int i = 0; i < SAMPLE_COUNT; i++) {
            Mat frame = new Mat();
            boolean captured = camera.read(frame);

            if (captured && !frame.empty()) {
                return frame;
            }

            sleepQuietly(120);
        }

        return null;
    }

    private void warmUpCamera(VideoCapture camera) {
        for (int i = 0; i < WARMUP_FRAMES; i++) {
            Mat ignored = new Mat();
            camera.read(ignored);
            sleepQuietly(80);
        }
    }

    private int countFaces(Mat frame, CascadeClassifier faceDetector) {
        Mat gray = new Mat();

        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(gray, gray);

        MatOfRect faces = new MatOfRect();

        faceDetector.detectMultiScale(
                gray,
                faces,
                1.08,
                4,
                0,
                new Size(70, 70),
                new Size()
        );

        return faces.toArray().length;
    }

    private double calculateBlurScore(Mat gray) {
        Mat laplacian = new Mat();
        Imgproc.Laplacian(gray, laplacian, CvType.CV_64F);

        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stdDev = new MatOfDouble();

        Core.meanStdDev(laplacian, mean, stdDev);

        return Math.pow(stdDev.toArray()[0], 2);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private String extractCascadeFile() throws Exception {
        InputStream inputStream =
                getClass().getResourceAsStream("/opencv/haarcascade_frontalface_default.xml");

        if (inputStream == null) {
            throw new IllegalStateException("Missing haarcascade_frontalface_default.xml");
        }

        File tempFile = File.createTempFile("haarcascade_frontalface_default", ".xml");
        tempFile.deleteOnExit();

        Files.copy(
                inputStream,
                tempFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
        );

        return tempFile.getAbsolutePath();
    }
}