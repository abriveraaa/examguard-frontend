package com.example.examguard.utility;

import com.example.examguard.model.ai.AiRulesConfig;
import com.example.examguard.service.AiAssetSyncService;
import com.example.examguard.service.AiRulesService;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * PURPOSE:
 * Handles YOLO object detection for the exam lobby.
 *
 * LOBBY RULES ENFORCED:
 * 1. Exactly 1 person visible (upper-body / 45° angle expected — not full body).
 * 2. Exactly 1 monitor (tv) and at most 1 keyboard allowed.
 * 3. Exactly 1 laptop allowed.
 * 4. If a laptop is detected, extra electronic devices (remote, tablet, phone,
 *    additional laptop) are blocked.  Mouse and keyboard are exempt.
 * 5. No phone / cell-phone allowed regardless.
 * 6. No reviewer items: book, paper/notebook detected via "book" COCO class.
 * 7. 45° camera angle is validated by checking the person bounding-box
 *    aspect-ratio heuristic (landscape box = lying down / too far back;
 *    very tall narrow box = too straight-on / too close).
 */
public class LobbyObjectDetector {

    private static boolean openCvLoaded = false;

    private final int inputSize;
    private final float confidenceThreshold;
    private final float nmsThreshold;
    private final float personConfidenceThreshold;
    private final float phoneConfidenceThreshold;
    private final double maxPhoneAreaRatio;
    private final boolean detectBook;
    private final boolean detectPaper;

    private List<DetectedObject> lastGoodObjects = List.of();

    private final Net net;
    private final List<String> classNames;
    private final boolean available;

    /*
     * PURPOSE:
     * Stores recent raw detections so one random frame will not affect the lobby.
     */
    private final Deque<List<DetectedObject>> liveWindow = new ArrayDeque<>();

    private static final int LIVE_WINDOW_SIZE = 8;
    private static final int REQUIRED_PERSON_HITS = 3;
    private static final int REQUIRED_OBJECT_HITS = 5;
    private static final int MAX_MISS_FRAMES = 8;

    /*
     * PURPOSE:
     * 45-degree camera angle heuristic thresholds for the person bounding box.
     * At ~45° the person box should be roughly portrait but NOT extremely narrow.
     *
     * aspectRatio = width / height
     *   < MIN_PERSON_ASPECT → box is very narrow → camera likely too close / too vertical
     *   > MAX_PERSON_ASPECT → box is wide → camera likely too low / full-body / lying angle
     *   OK range             → upper-body portrait consistent with 45° desk setup
     */
    private static final double MIN_PERSON_ASPECT = 0.22;   // narrower = reject
    private static final double MAX_PERSON_ASPECT = 1.10;   // wider  = reject
    private static final double IDEAL_PERSON_ASPECT_MIN = 0.30; // sweet-spot start
    private static final double IDEAL_PERSON_ASPECT_MAX = 0.80; // sweet-spot end

    /*
     * PURPOSE:
     * Keeps last good stable result so UI does not flicker to "No examinee"
     * when YOLO misses for a few frames.
     */
    private List<DetectedObject> lastStableObjects = new ArrayList<>();
    private int missFrames = 0;

    private static final boolean DEBUG_LOBBY_AI = false;
    private static int debugFrameNo = 0;

    private void debug(String message) {
        if (DEBUG_LOBBY_AI) {
            System.out.println("[LOBBY-AI] " + message);
        }
    }

    /**
     * PURPOSE:
     * Result object used by the lobby UI.
     * reviewerItemCount now counts books / paper / notebooks detected.
     */
    public record DeskBaselineResult(
            boolean passed,
            String message,
            int personCount,
            int laptopCount,
            int keyboardCount,
            int monitorCount,
            int unauthorizedDeviceCount,
            int reviewerCount,            // reviewer notes / books / paper
            List<String> visibleItems,
            String signature
    ) {}

    /**
     * PURPOSE:
     * Loads OpenCV, YOLO model, and labels.
     */
    /**
     * PURPOSE:
     * Loads OpenCV, YOLO model, labels, and OpenCV DetectionModel.
     */
    public LobbyObjectDetector() {
        loadOpenCv();

        AiRulesConfig rules = AiRulesService.getRules();
        AiRulesConfig.Yolo yoloRules = rules.getYolo();

        this.inputSize                 = yoloRules.getInputSize();
        this.confidenceThreshold       = yoloRules.getConfidenceThreshold();
        this.nmsThreshold              = yoloRules.getNmsThreshold();
        this.personConfidenceThreshold = yoloRules.getPersonConfidence();
        this.phoneConfidenceThreshold  = yoloRules.getPhoneConfidence();
        this.maxPhoneAreaRatio         = yoloRules.getMaxPhoneAreaRatio();
        this.detectBook                = yoloRules.isDetectBook();
        this.detectPaper               = yoloRules.isDetectPaper();

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

    // ──────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * PURPOSE:
     * Main method called by the lobby.
     * Applies all desk-setup rules and returns a single result with a message.
     *
     * RULE ORDER (fail-fast, highest priority first):
     *  1. No person detected
     *  2. Multiple persons detected
     *  3. Camera angle check (person box aspect ratio)
     *  4. Phone / cell-phone detected
     *  5. Extra electronic device while laptop present
     *  6. More than 1 laptop
     *  7. More than 1 monitor
     *  8. More than 1 keyboard
     *  9. Reviewer notes / book detected
     * 10. PASS
     */
    public DeskBaselineResult analyzeDeskBaseline(Mat frame) {
        if (!isAvailable()) {
            return result(
                    true,
                    "Object detection unavailable. Manual review mode enabled.",
                    0, 0, 0, 0, 0, 0,
                    List.of(),
                    "UNAVAILABLE"
            );
        }

        if (frame == null || frame.empty()) {
            return result(
                    false,
                    "No camera frame available for desk setup check.",
                    0, 0, 0, 0, 0, 0,
                    List.of(),
                    "NO_FRAME"
            );
        }

        List<DetectedObject> stableObjects = detect(frame);

        List<DetectedObject> persons = getReliablePersons(stableObjects, frame.width(), frame.height());

        int laptopCount    = countStableObjects(stableObjects, "laptop",   frame);
        int keyboardCount  = countStableObjects(stableObjects, "keyboard", frame);
        int monitorCount   = countStableObjects(stableObjects, "monitor",  frame);
        int phoneCount     = countStableObjects(stableObjects, "phone",    frame);
        int reviewerCount  = countStableObjects(stableObjects, "reviewer", frame);
        int remoteCount    = countStableObjects(stableObjects, "remote",   frame);
        int extraDevices   = phoneCount + remoteCount;

        List<String> visibleItems = buildVisibleItems(stableObjects);
        String signature          = buildSignature(stableObjects);

        int frameNo = ++debugFrameNo;

        debug("==================================================");
        debug("FRAME #" + frameNo);
        debug("FRAME SIZE = " + frame.width() + "x" + frame.height());
        debug("STABLE OBJECTS = " + stableObjects.size());

        for (DetectedObject o : stableObjects) {
            double ar = o.getHeight() > 0 ? o.getWidth() / o.getHeight() : 0;
            double areaRatio = (o.getWidth() * o.getHeight()) / (frame.width() * (double) frame.height());

            debug(String.format(
                    "STABLE: label=%s conf=%.3f x=%.1f y=%.1f w=%.1f h=%.1f areaRatio=%.4f ar=%.2f",
                    normalizeLabel(o.getClassName()),
                    o.getConfidence(),
                    o.getX(),
                    o.getY(),
                    o.getWidth(),
                    o.getHeight(),
                    areaRatio,
                    ar
            ));
        }

        debug("RELIABLE PERSONS = " + persons.size());
        debug("COUNTS: person=" + persons.size()
                + ", laptop=" + laptopCount
                + ", keyboard=" + keyboardCount
                + ", monitor=" + monitorCount
                + ", phone=" + phoneCount
                + ", remote=" + remoteCount
                + ", reviewer=" + reviewerCount
                + ", extraDevices=" + extraDevices);

        debug("VISIBLE ITEMS = " + visibleItems);
        debug("SIGNATURE = " + signature);

        // ── Rule 1: No person ──────────────────────────────────────────────
        if (persons.isEmpty()) {
            return result(
                    false,
                    "No examinee detected. Adjust camera or lighting so your upper body, "
                            + "hands, desk, and screen are visible (45° angle).",
                    0, laptopCount, keyboardCount, monitorCount,
                    extraDevices, reviewerCount, visibleItems, signature);
        }

        // ── Rule 2: Multiple persons ───────────────────────────────────────
        if (persons.size() > 1) {
            return result(
                    false,
                    "Multiple persons detected (" + persons.size() + "). "
                            + "Only the examinee should be visible.",
                    persons.size(), laptopCount, keyboardCount, monitorCount,
                    extraDevices, reviewerCount, visibleItems, signature);
        }

        // ── Rule 3: Camera angle check ─────────────────────────────────────
        DetectedObject person = persons.get(0);
        CameraAngleCheck angleCheck = checkCameraAngle(person, frame.width(), frame.height());
        if (!angleCheck.ok()) {
            return result(
                    false,
                    angleCheck.message(),
                    1, laptopCount, keyboardCount, monitorCount,
                    extraDevices, reviewerCount, visibleItems, signature);
        }

        // ── Rule 4: Phone detected ─────────────────────────────────────────
        if (phoneCount > 0) {
            return result(
                    false,
                    "Mobile phone / tablet detected. Remove all unauthorized "
                            + "electronic devices from the desk.",
                    1, laptopCount, keyboardCount, monitorCount,
                    extraDevices, reviewerCount, visibleItems, signature);
        }

        // ── Rule 5: Device setup mode check ─────────────────────────────
        boolean laptopMode = laptopCount == 1;
        boolean desktopMode = laptopCount == 0 && monitorCount == 1 && keyboardCount == 1;

        if (laptopCount == 0 && monitorCount == 0) {
            return result(
                    false,
                    "No computer detected. Show either one laptop or one monitor with one keyboard.",
                    1, laptopCount, keyboardCount, monitorCount,
                    extraDevices, reviewerCount, visibleItems, signature
            );
        }

        if (laptopMode && monitorCount > 0) {
            return result(
                    false,
                    "External monitor / TV detected with laptop. For laptop mode, only one laptop is allowed.",
                    1, laptopCount, keyboardCount, monitorCount,
                    extraDevices, reviewerCount, visibleItems, signature
            );
        }

        if (!laptopMode && !desktopMode) {
            return result(
                    false,
                    "Invalid computer setup. Use either one laptop only, or one monitor/TV with one keyboard.",
                    1, laptopCount, keyboardCount, monitorCount,
                    extraDevices, reviewerCount, visibleItems, signature
            );
        }

        // ── Rule 5: Extra device while laptop present ─────────────────────
        if (laptopCount >= 1 && remoteCount > 0) {
            return result(
                    false,
                    "Extra electronic device detected alongside the laptop. "
                            + "Only a keyboard and mouse are allowed when using a laptop.",
                    1, laptopCount, keyboardCount, monitorCount,
                    extraDevices, reviewerCount, visibleItems, signature);
        }

        // ── Rule 6: More than 1 laptop ────────────────────────────────────
        if (laptopCount > 1) {
            return result(
                    false,
                    "More than one laptop detected (" + laptopCount + "). "
                            + "Only a single laptop is allowed.",
                    1, laptopCount, keyboardCount, monitorCount,
                    extraDevices, reviewerCount, visibleItems, signature);
        }

        // ── Rule 7: More than 1 monitor ───────────────────────────────────
        if (monitorCount > 1) {
            return result(
                    false,
                    "Multiple monitors detected (" + monitorCount + "). "
                            + "Only one monitor / display is allowed.",
                    1, laptopCount, keyboardCount, monitorCount,
                    extraDevices, reviewerCount, visibleItems, signature);
        }

        // ── Rule 8: More than 1 keyboard ─────────────────────────────────
        if (keyboardCount > 1) {
            return result(
                    false,
                    "Multiple keyboards detected (" + keyboardCount + "). "
                            + "Only one keyboard is allowed.",
                    1, laptopCount, keyboardCount, monitorCount,
                    extraDevices, reviewerCount, visibleItems, signature);
        }

        // ── Rule 9: Reviewer notes / book ─────────────────────────────────
        if (reviewerCount > 0) {
            return result(
                    false,
                    "Reviewer material detected (book / notes / paper). "
                            + "Clear the desk before starting.",
                    1, laptopCount, keyboardCount, monitorCount,
                    extraDevices, reviewerCount, visibleItems, signature);
        }

        // ── PASS ──────────────────────────────────────────────────────────
        // PASS only if:
        // Laptop mode: 1 person + 1 laptop + no monitor + no keyboard
        // Desktop mode: 1 person + 1 monitor + 1 keyboard + no laptop
        return result(
                true,
                "Desk setup verified. Ready to start the exam.",
                1, laptopCount, keyboardCount, monitorCount,
                0, 0, visibleItems, signature);
    }

    /**
     * Kept for backward-compatibility — delegates to analyzeDeskBaseline.
     */
    public DeskBaselineResult analyzeDeskBaselineStable(Mat frame) {
        return analyzeDeskBaseline(frame);
    }

    /**
     * PURPOSE:
     * Detects raw YOLO objects, then returns stable live-feed objects only.
     */
    public synchronized List<DetectedObject> detect(Mat frame) {
        if (!isAvailable() || frame == null || frame.empty()) {
            return List.of();
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

        List<DetectedObject> rawObjects = parseYoloV8Output(
                output,
                frame.width(),
                frame.height()
        );

        if (rawObjects.isEmpty()) { return lastGoodObjects; }

        if (rawObjects.size() > 20) { return lastGoodObjects; }

        List<DetectedObject> stableObjects = stabilizeLiveObjects(rawObjects);

        List<DetectedObject> cleaned = cleanStableLobbyObjects(
                stableObjects,
                frame.width(),
                frame.height()
        );

        if (!cleaned.isEmpty()) { lastGoodObjects = cleaned; }

        return cleaned.isEmpty() ? lastGoodObjects : cleaned;
    }

    private List<DetectedObject> cleanStableLobbyObjects(
            List<DetectedObject> objects,
            int frameWidth,
            int frameHeight
    ) {
        if (objects == null || objects.isEmpty()) {
            return List.of();
        }

        double frameArea = frameWidth * (double) frameHeight;

        List<DetectedObject> cleaned = new ArrayList<>();

        DetectedObject bestPerson = null;
        double bestPersonArea = 0;

        for (DetectedObject object : objects) {
            String label = normalizeLabel(object.getClassName());

            double area = object.getWidth() * object.getHeight();
            double areaRatio = area / frameArea;
            double aspect = object.getHeight() > 0
                    ? object.getWidth() / object.getHeight()
                    : 0;

            if (isPersonLabel(label)) {

                // Ignore small/partial fake person boxes.
                if (object.getConfidence() < 0.70f) {
                    continue;
                }

                if (areaRatio < 0.20) {
                    continue;
                }

                if (aspect < 0.45 || aspect > 1.45) {
                    continue;
                }

                if (area > bestPersonArea) {
                    bestPerson = object;
                    bestPersonArea = area;
                }

                continue;
            }

            cleaned.add(object);
        }

        if (bestPerson != null) {
            cleaned.add(bestPerson);
        }

        return cleaned;
    }

    /**
     * PURPOSE:
     * Debug overlay is intentionally disabled for production lobby view.
     *
     * IMPORTANT:
     * Do not delete this method if other camera-preview code still calls it.
     * Keeping it as a no-op removes the rectangle boxes and text labels
     * without changing the detection logic.
     */
    public void drawDebugDetections(Mat frame, List<DetectedObject> objects) {
        // No visual overlay in production.
        // Detection still runs through detect(...) and analyzeDeskBaseline(...).
    }

    public boolean isAvailable() {
        return available
                && net != null
                && classNames != null
                && !classNames.isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CAMERA ANGLE CHECK
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * PURPOSE:
     * Validates that the camera is positioned at ~45 degrees, showing the
     * examinee from upper-body / chest-up perspective.
     *
     * Heuristic: at a desk with a 45° camera angle (e.g. laptop lid cam or
     * monitor-top webcam), the visible person bounding box tends to be a
     * portrait rectangle with aspectRatio (w/h) roughly between 0.30 and 0.80.
     *
     * - Very wide box (AR > MAX)  → camera is too low / shows full body lying flat
     * - Very narrow box (AR < MIN) → camera is too high / too zoomed-in / wrong angle
     */
    private CameraAngleCheck checkCameraAngle(
            DetectedObject person,
            int frameWidth,
            int frameHeight
    ) {
        double w  = person.getWidth();
        double h  = person.getHeight();
        double ar = (h > 0) ? w / h : 0;

        if (ar < MIN_PERSON_ASPECT) {
            return new CameraAngleCheck(false,
                    "Camera angle too close to camera. "
                            + "Tilt the camera back so your upper body is visible "
                            + "(aim for a 45° angle from desk level).");
        }

        if (ar > MAX_PERSON_ASPECT) {
            return new CameraAngleCheck(false,
                    "Camera angle too low or examinee too far back. "
                            + "Raise the camera or sit closer so your upper body "
                            + "fills most of the frame (aim for a 45° angle).");
        }

        return new CameraAngleCheck(true, "OK");
    }

    private record CameraAngleCheck(boolean ok, String message) {}

    // ──────────────────────────────────────────────────────────────────────────
    // YOLO OUTPUT PARSING
    // ──────────────────────────────────────────────────────────────────────────

    private List<DetectedObject> parseYoloV8Output(
            Mat output,
            int frameWidth,
            int frameHeight
    ) {
        List<DetectedObject> detections = new ArrayList<>();

        int dimensions = (int) output.size(1); // 84
        int anchors = (int) output.size(2);    // 8400

        float[] data = new float[dimensions * anchors];
        output.get(new int[]{0, 0, 0}, data);

        List<Rect2d> boxes = new ArrayList<>();
        List<Float> scores = new ArrayList<>();
        List<Integer> classIds = new ArrayList<>();

        double scaleX = frameWidth / (double) inputSize;
        double scaleY = frameHeight / (double) inputSize;
        double frameArea = frameWidth * (double) frameHeight;

        for (int i = 0; i < anchors; i++) {

            float cx = data[i];
            float cy = data[anchors + i];
            float w  = data[2 * anchors + i];
            float h  = data[3 * anchors + i];

            int bestClassId = -1;
            float bestScore = 0f;

            for (int c = 4; c < dimensions; c++) {
                float score = data[c * anchors + i];

                if (score > bestScore) {
                    bestScore = score;
                    bestClassId = c - 4;
                }
            }

            if (bestClassId < 0 || bestClassId >= classNames.size()) {
                continue;
            }

            String label = normalizeLabel(classNames.get(bestClassId));

            if (!isRelevantLobbyClass(label)) {
                continue;
            }

            if (bestScore < requiredScore(label)) {
                continue;
            }

            double left = (cx - w / 2.0) * scaleX;
            double top = (cy - h / 2.0) * scaleY;
            double width = w * scaleX;
            double height = h * scaleY;

            if (width <= 0 || height <= 0) {
                continue;
            }

            double areaRatio = (width * height) / frameArea;
            double aspect = width / height;

            // IMPORTANT:
            // Remove fake tiny person boxes before NMS.
            if (isPersonLabel(label)) {
                if (areaRatio < 0.04) continue;
                if (width < 120) continue;
                if (height < 120) continue;
                if (aspect < 0.20 || aspect > 1.40) continue;
            }

            // Laptop / monitor / keyboard must also be meaningful size.
            if (isLaptopLabel(label) || isMonitorLabel(label) || isKeyboardLabel(label)) {
                if (areaRatio < 0.015) continue;
                if (width < 80 || height < 30) continue;
            }

            // Phone / remote / book may be smaller, but not microscopic.
            if (isPhoneLabel(label) || isRemoteLabel(label) || isReviewerMaterialLabel(label)) {
                if (areaRatio < 0.002) continue;
                if (width < 35 || height < 35) continue;
            }

            if (bestScore < 0.0f || bestScore > 1.0f) { continue; }

            if (Double.isNaN(left) || Double.isNaN(top) || Double.isNaN(width) || Double.isNaN(height)) { continue; }
            if (Double.isInfinite(left) || Double.isInfinite(top) || Double.isInfinite(width) || Double.isInfinite(height)) { continue; }
            if (width > frameWidth || height > frameHeight) { continue; }
            if (left < -50 || top < -50) { continue; }
            if (left + width > frameWidth + 50 || top + height > frameHeight + 50) { continue; }
            if (areaRatio > 0.75) { continue; }

            boxes.add(new Rect2d(left, top, width, height));
            scores.add(bestScore);
            classIds.add(bestClassId);
        }

        debug("PRE-NMS filtered boxes = " + boxes.size());

        if (boxes.isEmpty()) {
            return List.of();
        }

        MatOfRect2d boxMat = new MatOfRect2d();
        boxMat.fromList(boxes);

        MatOfFloat scoreMat = new MatOfFloat();
        scoreMat.fromArray(toPrimitive(scores));

        MatOfInt indices = new MatOfInt();

        Dnn.NMSBoxes(
                boxMat,
                scoreMat,
                confidenceThreshold,
                nmsThreshold,
                indices
        );

        debug("POST-NMS boxes = " + indices.rows());

        for (int index : indices.toArray()) {
            Rect2d box = boxes.get(index);
            int classId = classIds.get(index);
            float confidence = scores.get(index);
            String label = normalizeLabel(classNames.get(classId));

            detections.add(new DetectedObject(
                    label,
                    confidence,
                    box.x,
                    box.y,
                    box.width,
                    box.height
            ));
        }

        return detections;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // STABILIZATION
    // ──────────────────────────────────────────────────────────────────────────

    private List<DetectedObject> stabilizeLiveObjects(List<DetectedObject> currentObjects) {
        liveWindow.addLast(currentObjects);
        if (liveWindow.size() > LIVE_WINDOW_SIZE) liveWindow.removeFirst();
        if (liveWindow.size() < LIVE_WINDOW_SIZE) return lastStableObjects;

        List<DetectedObject> stable = new ArrayList<>();

        for (DetectedObject current : currentObjects) {
            String label = normalizeLabel(current.getClassName());
            int hits = 0;

            for (List<DetectedObject> previousFrame : liveWindow) {
                boolean found = previousFrame.stream()
                        .anyMatch(previous -> sameObject(current, previous));
                if (found) hits++;
            }

            int requiredHits = isPersonLabel(label)
                    ? REQUIRED_PERSON_HITS
                    : REQUIRED_OBJECT_HITS;

            if (hits >= requiredHits) stable.add(current);
        }

        if (!stable.isEmpty()) {
            lastStableObjects = stable;
            missFrames = 0;
            return stable;
        }

        missFrames++;
        if (!lastStableObjects.isEmpty() && missFrames <= MAX_MISS_FRAMES) {
            return lastStableObjects;
        }

        lastStableObjects = new ArrayList<>();
        return List.of();
    }

    private boolean sameObject(DetectedObject a, DetectedObject b) {
        String labelA = normalizeLabel(a.getClassName());
        String labelB = normalizeLabel(b.getClassName());
        if (!labelA.equals(labelB)) return false;
        return overlapBySmallerBox(a, b) >= 0.35;
    }

    private double overlapBySmallerBox(DetectedObject a, DetectedObject b) {
        double x1 = Math.max(a.getX(), b.getX());
        double y1 = Math.max(a.getY(), b.getY());
        double x2 = Math.min(a.getX() + a.getWidth(),  b.getX() + b.getWidth());
        double y2 = Math.min(a.getY() + a.getHeight(), b.getY() + b.getHeight());

        double iw = Math.max(0, x2 - x1);
        double ih = Math.max(0, y2 - y1);
        double intersection = iw * ih;

        double areaA   = a.getWidth() * a.getHeight();
        double areaB   = b.getWidth() * b.getHeight();
        double smaller = Math.min(areaA, areaB);

        return smaller <= 0 ? 0 : intersection / smaller;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // COUNTING & FILTERING HELPERS
    // ──────────────────────────────────────────────────────────────────────────

    private List<DetectedObject> getReliablePersons(
            List<DetectedObject> objects,
            double frameWidth,
            double frameHeight
    ) {
        double frameArea = frameWidth * frameHeight;
        List<DetectedObject> reliable = new ArrayList<>();

        for (DetectedObject o : objects) {
            String label = normalizeLabel(o.getClassName());

            if (!isPersonLabel(label)) {
                continue;
            }

            double area = o.getWidth() * o.getHeight();
            double ratio = frameArea <= 0 ? 0 : area / frameArea;
            double ar = o.getHeight() > 0 ? o.getWidth() / o.getHeight() : 0;

            boolean passConfidence = o.getConfidence() >= 0.80f;
            boolean passArea = ratio >= 0.05;

            debug(String.format(
                    "PERSON TEST: conf=%.3f areaRatio=%.4f ar=%.2f passConf=%s passArea=%s",
                    o.getConfidence(),
                    ratio,
                    ar,
                    passConfidence,
                    passArea
            ));

            if (!passConfidence) {
                debug("PERSON REJECTED: confidence below 0.40");
                continue;
            }

            if (!passArea) {
                debug("PERSON REJECTED: areaRatio below 0.05");
                continue;
            }

            debug("PERSON ACCEPTED");
            reliable.add(o);
        }

        return reliable.stream()
                .sorted(Comparator.comparingDouble(o -> -(o.getWidth() * o.getHeight())))
                .limit(2)
                .toList();
    }

    private int countStableObjects(List<DetectedObject> objects, String type, Mat frame) {
        int    count     = 0;
        double frameArea = frame.width() * (double) frame.height();

        for (DetectedObject object : objects) {
            String label    = normalizeLabel(object.getClassName());
            double area     = object.getWidth() * object.getHeight();
            double areaRatio = frameArea <= 0 ? 0 : area / frameArea;

            switch (type) {
                case "laptop"   -> { if (isLaptopLabel(label))   count++; }
                case "keyboard" -> { if (isKeyboardLabel(label)) count++; }
                case "monitor"  -> { if (isMonitorLabel(label))  count++; }
                case "remote"   -> { if (isRemoteLabel(label))   count++; }
                case "reviewer" -> { if (isReviewerMaterialLabel(label)) count++; }
                case "phone"    -> {
                    if (isPhoneLabel(label)
                            && areaRatio > 0.0005
                            && areaRatio <= maxPhoneAreaRatio) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    /**
     * PURPOSE:
     * Class-specific confidence thresholds.
     * Books / paper use a lower threshold because they are often partially
     * visible on a desk.
     */
    private float requiredScore(String label) {
        if (isPersonLabel(label))       return Math.max(0.25f, Math.min(personConfidenceThreshold, 0.45f));
        if (isPhoneLabel(label))        return Math.max(0.60f, phoneConfidenceThreshold);
        if (isRemoteLabel(label))       return 0.50f;
        if (isReviewerMaterialLabel(label)) return 0.30f;   // books/paper partial visibility
        if (isLaptopLabel(label)
                || isKeyboardLabel(label)
                || isMonitorLabel(label)) return 0.75f;
        return confidenceThreshold;
    }

    /**
     * PURPOSE:
     * All classes that the lobby cares about.
     * "book" (COCO index 73) covers printed books, notebooks, and thick papers
     * visible from a top/angle view.
     */
    private boolean isRelevantLobbyClass(String label) {
        return isPersonLabel(label)
                || isPhoneLabel(label)
                || isLaptopLabel(label)
                || isKeyboardLabel(label)
                || isMonitorLabel(label)
                || isRemoteLabel(label)
                || (detectBook   && isReviewerMaterialLabel(label));
    }

    private List<String> buildVisibleItems(List<DetectedObject> objects) {
        List<String> items = new ArrayList<>();
        for (DetectedObject object : objects) {
            items.add(normalizeLabel(object.getClassName())
                    + " " + String.format("%.2f", object.getConfidence()));
        }
        return items;
    }

    private String buildSignature(List<DetectedObject> objects) {
        List<String> parts = new ArrayList<>();
        for (DetectedObject object : objects) {
            String label = normalizeLabel(object.getClassName());
            int x = (int) (object.getX()      / 50);
            int y = (int) (object.getY()      / 50);
            int w = (int) (object.getWidth()  / 50);
            int h = (int) (object.getHeight() / 50);
            parts.add(label + ":" + x + ":" + y + ":" + w + ":" + h);
        }
        Collections.sort(parts);
        return String.join("|", parts);
    }

    private DeskBaselineResult result(
            boolean passed, String message,
            int personCount, int laptopCount, int keyboardCount,
            int monitorCount, int unauthorizedDeviceCount, int reviewerCount,
            List<String> visibleItems, String signature
    ) {
        debug("FINAL RESULT: passed=" + passed);
        debug("MESSAGE: " + message);
        debug("FINAL COUNTS: person=" + personCount
                + ", laptop=" + laptopCount
                + ", keyboard=" + keyboardCount
                + ", monitor=" + monitorCount
                + ", unauthorized=" + unauthorizedDeviceCount
                + ", reviewer=" + reviewerCount);
        debug("==================================================");

        return new DeskBaselineResult(
                passed, message,
                personCount, laptopCount, keyboardCount, monitorCount,
                unauthorizedDeviceCount, reviewerCount,
                visibleItems, signature
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // LABEL HELPERS
    // ──────────────────────────────────────────────────────────────────────────

    private boolean isPersonLabel(String label) {
        return "person".equals(label);
    }

    private boolean isLaptopLabel(String label) {
        return "laptop".equals(label);
    }

    private boolean isKeyboardLabel(String label) {
        return "keyboard".equals(label);
    }

    private boolean isMonitorLabel(String label) {
        return "tv".equals(label) || "monitor".equals(label) || "screen".equals(label);
    }

    private boolean isPhoneLabel(String label) {
        return "cell phone".equals(label)
                || "phone".equals(label)
                || "mobile phone".equals(label)
                || "smartphone".equals(label)
                || "tablet".equals(label);
    }

    /**
     * PURPOSE:
     * Remote controls, smart speakers, and any other loose electronic device
     * that is not a keyboard or mouse.
     * COCO: "remote" (class 65)
     */
    private boolean isRemoteLabel(String label) {
        return "remote".equals(label);
    }

    /**
     * PURPOSE:
     * Reviewer materials detectable via COCO.
     *
     * "book"  (COCO class 73) — covers hardcover books, thick notebooks,
     *         and stacked papers.
     *
     * NOTE: Sticky notes and loose single sheets of paper are generally too
     * small for YOLO to classify reliably from a webcam angle.  The safest
     * rule is "book" detection only; if you need finer detection you would
     * need a custom-trained model.
     */
    private boolean isReviewerMaterialLabel(String label) {
        return "book".equals(label);
    }

    private String normalizeLabel(String label) {
        return label == null ? "" : label.trim().toLowerCase();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // INFRASTRUCTURE
    // ──────────────────────────────────────────────────────────────────────────

    private List<String> loadClassNames(String labelsKey) throws Exception {
        Optional<Path> labelsPathOpt = AiAssetSyncService.getOptionalAssetPath(labelsKey);
        if (labelsPathOpt.isEmpty()) return List.of();

        String content = Files.readString(labelsPathOpt.get());
        return Arrays.stream(content.split("\\R"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private static synchronized void loadOpenCv() {
        if (!openCvLoaded) {
            OpenCV.loadLocally();
            openCvLoaded = true;
        }
    }

    private float[] toPrimitive(List<Float> list) {

        float[] arr = new float[list.size()];

        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }

        return arr;
    }

}

