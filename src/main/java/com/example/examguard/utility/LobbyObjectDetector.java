package com.example.examguard.utility;

import com.example.examguard.model.ai.AiRulesConfig;
import com.example.examguard.service.AiAssetSyncService;
import com.example.examguard.service.AiRulesService;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.videoio.VideoCapture;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LobbyObjectDetector {

    private static boolean openCvLoaded = false;

    private final int inputSize;
    private final float confidenceThreshold;
    private final float nmsThreshold;
    private final float phoneConfidenceThreshold;
    private final int requiredPhoneHits;
    private final float personConfidenceThreshold;
    private final int requiredPersonHits;
    private final int sideViewSampleCount;
    private final double maxPhoneAreaRatio;

    private final boolean detectBook;
    private final boolean detectPaper;

    private final Net net;
    private final List<String> classNames;
    private final boolean available;

    private final AiRulesConfig rules;

    public LobbyObjectDetector() {
        loadOpenCv();

        this.rules = AiRulesService.getRules();

        AiRulesConfig.Yolo yoloRules = rules.getYolo();

        this.inputSize = yoloRules.getInputSize();
        this.confidenceThreshold = yoloRules.getConfidenceThreshold();
        this.nmsThreshold = yoloRules.getNmsThreshold();
        this.phoneConfidenceThreshold = yoloRules.getPhoneConfidence();
        this.requiredPhoneHits = yoloRules.getRequiredPhoneHits();
        this.personConfidenceThreshold = yoloRules.getPersonConfidence();
        this.requiredPersonHits = yoloRules.getRequiredPersonHits();
        this.sideViewSampleCount = yoloRules.getSideViewSampleCount();
        this.maxPhoneAreaRatio = yoloRules.getMaxPhoneAreaRatio();
        this.detectBook = yoloRules.isDetectBook();
        this.detectPaper = yoloRules.isDetectPaper();

        Net loadedNet = null;
        List<String> loadedNames = List.of();
        boolean loaded = false;

        try {
            if (rules.isEnabled() && yoloRules.isEnabled()) {
                Path modelPath = AiAssetSyncService.getRequiredAssetPath(
                        yoloRules.getModelKey()
                );

                loadedNet = Dnn.readNetFromONNX(modelPath.toString());

                loadedNames = loadClassNames(yoloRules.getLabelsKey());

                loaded = loadedNet != null && !loadedNames.isEmpty();
            }

        } catch (Exception e) {
            e.printStackTrace();
            loaded = false;
        }

        this.net = loadedNet;
        this.classNames = loadedNames;
        this.available = loaded;
    }
    private static synchronized void loadOpenCv() {
        if (!openCvLoaded) {
            OpenCV.loadLocally();
            openCvLoaded = true;
        }
    }

    public boolean isAvailable() {
        return available && net != null && classNames != null && !classNames.isEmpty();
    }

    public LobbyCheckResult checkCleanDesk() {
        if (!isAvailable()) {
            return LobbyCheckResult.pass("AI object detection unavailable. Manual review mode enabled.");
        }

        System.out.println("===== USING YOLO DETECTOR =====");

        VideoCapture camera = null;

        try {
            camera = new VideoCapture(0);
            camera.set(3, 1280);
            camera.set(4, 720);

            if (!camera.isOpened()) {
                return LobbyCheckResult.fail("Camera unavailable for clean desk check.");
            }

            warmUpCamera(camera);

            int framesChecked = 0;
            int phoneFrameHits = 0;
            int bookFrameHits = 0;
            int paperFrameHits = 0;

            float bestPhoneConfidence = 0f;
            float bestBookConfidence = 0f;
            float bestPaperConfidence = 0f;

            for (int i = 0; i < 5; i++) {
                Mat frame = new Mat();

                if (!camera.read(frame) || frame.empty()) {
                    sleepQuietly(150);
                    continue;
                }

                framesChecked++;

                List<DetectedObject> detected = detect(frame);

                boolean phoneFoundThisFrame = false;
                boolean bookFoundThisFrame = false;
                boolean paperFoundThisFrame = false;

                for (DetectedObject object : detected) {
                    String name = object.getClassName();

                    if (name == null) {
                        continue;
                    }

                    name = name.trim().toLowerCase();

                    double frameArea = frame.width() * frame.height();
                    double objectArea = object.getWidth() * object.getHeight();
                    double areaRatio = objectArea / frameArea;

                    float confidence = object.getConfidence();

                    System.out.println(
                            "YOLO OBJECT: " + name +
                                    " | conf=" + confidence +
                                    " | areaRatio=" + areaRatio
                    );

                    boolean validSmallObject =
                            areaRatio > 0.0005 &&
                                    areaRatio <= maxPhoneAreaRatio;

                    if (!phoneFoundThisFrame
                            && isPhoneLabel(name)
                            && confidence >= phoneConfidenceThreshold
                            && validSmallObject) {

                        phoneFoundThisFrame = true;
                        bestPhoneConfidence = Math.max(bestPhoneConfidence, confidence);
                    }

                    if (detectBook
                            && !bookFoundThisFrame
                            && "book".equals(name)
                            && confidence >= phoneConfidenceThreshold
                            && validSmallObject) {

                        bookFoundThisFrame = true;
                        bestBookConfidence = Math.max(bestBookConfidence, confidence);
                    }

                    if (detectPaper
                            && !paperFoundThisFrame
                            && isPaperLabel(name)
                            && confidence >= phoneConfidenceThreshold
                            && validSmallObject) {

                        paperFoundThisFrame = true;
                        bestPaperConfidence = Math.max(bestPaperConfidence, confidence);
                    }
                }

                if (phoneFoundThisFrame) {
                    phoneFrameHits++;
                }

                if (bookFoundThisFrame) {
                    bookFrameHits++;
                }

                if (paperFoundThisFrame) {
                    paperFrameHits++;
                }

                sleepQuietly(180);
            }

            System.out.println("YOLO framesChecked=" + framesChecked);
            System.out.println("YOLO phoneFrameHits=" + phoneFrameHits + " | bestPhoneConfidence=" + bestPhoneConfidence);
            System.out.println("YOLO bookFrameHits=" + bookFrameHits + " | bestBookConfidence=" + bestBookConfidence);
            System.out.println("YOLO paperFrameHits=" + paperFrameHits + " | bestPaperConfidence=" + bestPaperConfidence);

            if (phoneFrameHits >= requiredPhoneHits) {
                return LobbyCheckResult.fail(
                        "Possible phone detected. Please remove any phone or second device from the desk. " +
                                "Detected in " + phoneFrameHits + "/" + framesChecked +
                                " frames. Confidence: " + String.format("%.2f", bestPhoneConfidence)
                );
            }

            if (detectBook && bookFrameHits >= requiredPhoneHits) {
                return LobbyCheckResult.fail(
                        "Possible book detected. Please clear your desk before starting. " +
                                "Detected in " + bookFrameHits + "/" + framesChecked +
                                " frames. Confidence: " + String.format("%.2f", bestBookConfidence)
                );
            }

            if (detectPaper && paperFrameHits >= requiredPhoneHits) {
                return LobbyCheckResult.fail(
                        "Possible reviewer or paper detected. Please clear your desk before starting. " +
                                "Detected in " + paperFrameHits + "/" + framesChecked +
                                " frames. Confidence: " + String.format("%.2f", bestPaperConfidence)
                );
            }

            return LobbyCheckResult.pass("No confirmed phone, book, or reviewer detected on desk.");

        } catch (Exception e) {
            e.printStackTrace();
            return LobbyCheckResult.pass("AI object detection unavailable. Manual review mode enabled.");
        } finally {
            if (camera != null) {
                camera.release();
            }
        }
    }

    public LobbyCheckResult checkSinglePersonSideView() {
        if (!isAvailable()) {
            return LobbyCheckResult.pass("AI presence detection unavailable. Manual review mode enabled.");
        }

        VideoCapture camera = null;

        try {
            camera = new VideoCapture(0);
            camera.set(3, 1280);
            camera.set(4, 720);

            if (!camera.isOpened()) {
                return LobbyCheckResult.fail("Camera unavailable for examinee presence check.");
            }

            warmUpCamera(camera);

            int noPersonFrames = 0;
            int onePersonFrames = 0;
            int multiplePersonFrames = 0;

            for (int i = 0; i < sideViewSampleCount; i++) {
                Mat frame = new Mat();

                if (!camera.read(frame) || frame.empty()) {
                    sleepQuietly(150);
                    continue;
                }

                List<DetectedObject> detected = detect(frame);

                int personCount = 0;

                for (DetectedObject object : detected) {
                    if ("person".equals(object.getClassName())
                            && object.getConfidence() >= personConfidenceThreshold) {
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

            if (onePersonFrames >= requiredPersonHits) {
                return LobbyCheckResult.pass("One student is visible from the side-view camera.");
            }

            if (noPersonFrames >= requiredPersonHits) {
                return LobbyCheckResult.fail("No examinee is clearly visible. Adjust the camera angle.");
            }

            return LobbyCheckResult.fail("Camera setup is unclear. Ensure your upper body, hands, desk, and screen area are visible.");

        } catch (Exception e) {
            e.printStackTrace();
            return LobbyCheckResult.pass("AI presence detection unavailable. Manual review mode enabled.");
        } finally {
            if (camera != null) {
                camera.release();
            }
        }
    }

    public List<DetectedObject> detect(Mat frame) {
        if (!isAvailable() || frame == null || frame.empty()) {
            return Collections.emptyList();
        }

        Mat blob = Dnn.blobFromImage(
                frame,
                1.0 / 255.0,
                new Size(inputSize, inputSize),
                new Scalar(0, 0, 0),
                true,
                false
        );

        net.setInput(blob);

        Mat output = net.forward();

        return parseYoloV8Output(output);
    }

    private List<DetectedObject> parseYoloV8Output(Mat output) {
        List<Rect2d> boxes = new ArrayList<>();
        List<Float> confidences = new ArrayList<>();
        List<Integer> classIds = new ArrayList<>();

        int dimensions = (int) output.size(1);
        int rows = (int) output.size(2);

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

            if (bestScore < confidenceThreshold) {
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
                confidenceThreshold,
                nmsThreshold,
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

    private List<String> loadClassNames(String labelsKey) throws Exception {
        Optional<Path> labelsPathOpt = AiAssetSyncService.getOptionalAssetPath(labelsKey);

        if (labelsPathOpt.isEmpty()) {
            return List.of();
        }

        String content = Files.readString(labelsPathOpt.get());

        return Arrays.stream(content.split("\\R"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
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

    private boolean isPhoneLabel(String name) {
        if (name == null) {
            return false;
        }

        String label = name.trim().toLowerCase();

        return label.equals("cell phone")
                || label.equals("phone")
                || label.equals("mobile phone")
                || label.equals("smartphone")
                || label.equals("tablet");
    }

    private boolean isPaperLabel(String name) {
        if (name == null) {
            return false;
        }

        String label = name.trim().toLowerCase();

        return label.equals("paper")
                || label.equals("document")
                || label.equals("notebook")
                || label.equals("reviewer")
                || label.equals("sheet");
    }
}