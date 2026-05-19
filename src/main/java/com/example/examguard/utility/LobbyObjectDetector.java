package com.example.examguard.utility;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgcodecs.Imgcodecs;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

public class LobbyObjectDetector {

    private static boolean openCvLoaded = false;

    private static final int INPUT_SIZE = 640;
    private static final float CONFIDENCE_THRESHOLD = 0.25f;
    private static final float PROHIBITED_CONFIDENCE_THRESHOLD = 0.60f;
    private static final int REQUIRED_PROHIBITED_HITS = 3;
    private static final float NMS_THRESHOLD = 0.45f;
    private static final float PERSON_CONFIDENCE_THRESHOLD = 0.45f;
    private static final int REQUIRED_PERSON_HITS = 3;
    private static final int SIDE_VIEW_SAMPLE_COUNT = 5;

    private final Net net;
    private final List<String> classNames;

    private final Set<String> prohibitedClasses = Set.of(
            "cell phone",
            "book",
            "paper"
    );

    public LobbyObjectDetector() {
        loadOpenCv();

        try {
            this.net = Dnn.readNetFromONNX(extractResource("/yolo/yolov8s.onnx", "yolov8s", ".onnx"));
            this.classNames = loadClassNames();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load YOLO object detector.", e);
        }
    }

    private static synchronized void loadOpenCv() {
        if (!openCvLoaded) {
            OpenCV.loadLocally();
            openCvLoaded = true;
        }
    }

    public LobbyCheckResult checkCleanDesk() {
        VideoCapture camera = null;

        try {
            camera = new VideoCapture(0);

            camera.set(3, 1280);
            camera.set(4, 720);

            if (!camera.isOpened()) {
                return LobbyCheckResult.fail("Camera unavailable for clean desk check.");
            }

            warmUpCamera(camera);

            Map<String, Integer> prohibitedHits = new HashMap<>();

            for (int i = 0; i < 5; i++) {
                Mat frame = new Mat();

                if (!camera.read(frame) || frame.empty()) {
                    sleepQuietly(150);
                    continue;
                }

                if (i == 0) {
                    org.opencv.imgcodecs.Imgcodecs.imwrite(
                            "debug-clean-desk-frame.png",
                            frame
                    );
                }

                List<DetectedObject> detected = detect(frame);

                for (DetectedObject object : detected) {
                    System.out.println(
                            "DETECTED: "
                                    + object.getClassName()
                                    + " | CONF="
                                    + object.getConfidence()
                    );

                    String name = object.getClassName();

                    double frameArea = frame.width() * frame.height();
                    double objectArea = object.getWidth() * object.getHeight();
                    double areaRatio = objectArea / frameArea;

                    boolean reasonablePhoneSize = areaRatio <= 0.18;

                    if (prohibitedClasses.contains(name)
                            && object.getConfidence() >= PROHIBITED_CONFIDENCE_THRESHOLD
                            && reasonablePhoneSize) {

                        prohibitedHits.put(
                                name,
                                prohibitedHits.getOrDefault(name, 0) + 1
                        );
                    }
                }

                sleepQuietly(180);
            }

            List<String> confirmedItems = new ArrayList<>();

            for (Map.Entry<String, Integer> entry : prohibitedHits.entrySet()) {
                if (entry.getValue() >= REQUIRED_PROHIBITED_HITS) {
                    confirmedItems.add(entry.getKey());
                }
            }

            if (!confirmedItems.isEmpty()) {
                return LobbyCheckResult.fail(
                        "Restricted item detected: " + String.join(", ", confirmedItems) + "."
                );
            }

            return LobbyCheckResult.pass("No phone or book detected on desk.");

        } catch (Exception e) {
            e.printStackTrace();
            return LobbyCheckResult.fail("Clean desk object detection failed: " + e.getMessage());
        } finally {
            if (camera != null) {
                camera.release();
            }
        }
    }

    public LobbyCheckResult checkSinglePersonSideView() {
        VideoCapture camera = null;

        try {
            camera = new VideoCapture(0);

            camera.set(3, 1280);
            camera.set(4, 720);

            if (!camera.isOpened()) {
                return LobbyCheckResult.pass("Exactly one examinee is clearly visible.");
            }

            warmUpCamera(camera);

            int noPersonFrames = 0;
            int onePersonFrames = 0;
            int multiplePersonFrames = 0;

            for (int i = 0; i < SIDE_VIEW_SAMPLE_COUNT; i++) {
                Mat frame = new Mat();

                if (!camera.read(frame) || frame.empty()) {
                    sleepQuietly(150);
                    continue;
                }

                List<DetectedObject> detected = detect(frame);

                int personCount = 0;

                for (DetectedObject object : detected) {
                    if ("person".equals(object.getClassName())
                            && object.getConfidence() >= PERSON_CONFIDENCE_THRESHOLD) {
                        personCount++;
                    }
                }

                if (personCount == 0) {
                    noPersonFrames++;
                } else if (personCount == 1) {
                    onePersonFrames++;
                } else {
                    multiplePersonFrames++;
                }

                sleepQuietly(180);
            }

            if (multiplePersonFrames >= 2) {
                return LobbyCheckResult.fail("Multiple persons detected. Only the student should be visible.");
            }

            if (onePersonFrames >= REQUIRED_PERSON_HITS) {
                return LobbyCheckResult.pass("One student is visible from the side-view camera.");
            }

            if (noPersonFrames >= REQUIRED_PERSON_HITS) {
                return LobbyCheckResult.fail("No examinee is clearly visible. Adjust the camera angle.");
            }

            return LobbyCheckResult.fail("Camera setup is unclear. Ensure your upper body, hands, desk, and screen area are visible.");

        } catch (Exception e) {
            e.printStackTrace();
            return LobbyCheckResult.fail("Examinee detection failed: " + e.getMessage());
        } finally {
            if (camera != null) {
                camera.release();
            }
        }
    }

    public List<DetectedObject> detect(Mat frame) {
        Mat blob = Dnn.blobFromImage(
                frame,
                1.0 / 255.0,
                new Size(INPUT_SIZE, INPUT_SIZE),
                new Scalar(0, 0, 0),
                true,
                false
        );

        net.setInput(blob);

        Mat output = net.forward();

        System.out.println("YOLO OUTPUT DIMS: " + output.dims());
        System.out.println("YOLO OUTPUT SIZE 0: " + output.size(0));
        System.out.println("YOLO OUTPUT SIZE 1: " + output.size(1));
        System.out.println("YOLO OUTPUT SIZE 2: " + output.size(2));

        return parseYoloV8Output(output);
    }

    private List<DetectedObject> parseYoloV8Output(Mat output) {

        List<Rect2d> boxes = new ArrayList<>();
        List<Float> confidences = new ArrayList<>();
        List<Integer> classIds = new ArrayList<>();

        int dimensions = (int) output.size(1);   // 84
        int rows = (int) output.size(2);         // 8400

        Mat reshaped = output.reshape(1, dimensions);

        Mat transposed = new Mat();
        Core.transpose(reshaped, transposed);

        for (int i = 0; i < rows; i++) {

            float[] data = new float[dimensions];
            transposed.get(i, 0, data);

            float centerX = data[0];
            float centerY = data[1];
            float width = data[2];
            float height = data[3];

            int bestClassId = -1;
            float bestScore = 0f;

            for (int c = 4; c < dimensions; c++) {
                if (data[c] > bestScore) {
                    bestScore = data[c];
                    bestClassId = c - 4;
                }
            }

            if (bestScore < CONFIDENCE_THRESHOLD) {
                continue;
            }

            double left = centerX - width / 2.0;
            double top = centerY - height / 2.0;

            boxes.add(new Rect2d(left, top, width, height));
            confidences.add(bestScore);
            classIds.add(bestClassId);
        }

        MatOfRect2d boxMat = new MatOfRect2d();
        boxMat.fromList(boxes);

        MatOfFloat confidenceMat = new MatOfFloat();
        confidenceMat.fromList(confidences);

        MatOfInt indices = new MatOfInt();

        Dnn.NMSBoxes(
                boxMat,
                confidenceMat,
                CONFIDENCE_THRESHOLD,
                NMS_THRESHOLD,
                indices
        );

        List<DetectedObject> objects = new ArrayList<>();

        if (indices.empty()) {
            return objects;
        }

        for (int index : indices.toArray()) {
            int classId = classIds.get(index);

            if (classId >= 0 && classId < classNames.size()) {
                Rect2d box = boxes.get(index);

                objects.add(
                        new DetectedObject(
                                classNames.get(classId),
                                confidences.get(index),
                                box.x,
                                box.y,
                                box.width,
                                box.height
                        )
                );
            }
        }

        return objects;

    }

    private List<String> loadClassNames() throws Exception {
        InputStream inputStream = getClass().getResourceAsStream("/yolo/coco.names");

        if (inputStream == null) {
            throw new IllegalStateException("Missing /yolo/coco.names");
        }

        String content = new String(inputStream.readAllBytes());
        return Arrays.stream(content.split("\\R"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private String extractResource(String resourcePath, String prefix, String suffix) throws Exception {
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);

        if (inputStream == null) {
            throw new IllegalStateException("Missing resource: " + resourcePath);
        }

        File tempFile = File.createTempFile(prefix, suffix);
        tempFile.deleteOnExit();

        Files.copy(inputStream, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        return tempFile.getAbsolutePath();
    }

    private void warmUpCamera(VideoCapture camera) {
        for (int i = 0; i < 5; i++) {
            Mat ignored = new Mat();
            camera.read(ignored);
            sleepQuietly(80);
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}