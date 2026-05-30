package com.example.examguard.controller.student;

import com.example.examguard.config.AppConfig;
import com.example.examguard.controller.layout.DashboardShellController;
import com.example.examguard.model.ai.MediaPipeFaceResult;
import com.example.examguard.model.camera.CameraSessionStatusResponse;
import com.example.examguard.model.camera.CreateCameraSessionResponse;
import com.example.examguard.model.core.response.BrandingResponse;
import com.example.examguard.model.enums.QuestionType;
import com.example.examguard.model.exam.request.EssayRubricRequest;
import com.example.examguard.model.exam.request.ExamActivityRequest;
import com.example.examguard.model.exam.request.ViolationLogRequest;
import com.example.examguard.model.exam.request.ViolationSettingRequest;
import com.example.examguard.model.exam.response.ImageUploadResponse;
import com.example.examguard.model.exam.take.ExamTakeChoice;
import com.example.examguard.model.exam.take.ExamTakeQuestion;
import com.example.examguard.model.exam.take.ExamTakingResponse;
import com.example.examguard.service.AiAssetSyncService;
import com.example.examguard.service.BrandingService;
import com.example.examguard.service.ExamApiService;
import com.example.examguard.utility.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.awt.image.BufferedImage;
import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import java.io.File;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExamTakingController {

    @FXML private StackPane lobbyView;
    @FXML private ImageView schoolLogoImageView;
    @FXML private Label brandSchoolNameLabel;
    @FXML private Label brandTaglineLabel;
    @FXML private Label lobbyExamTitleLabel;
    @FXML private Label lobbyExamSubtitleLabel;
    @FXML private Label lobbyStepLabel;
    @FXML private Label lobbyDurationLabel;
    @FXML private Label lobbyQuestionCountLabel;
    @FXML private VBox phoneCameraPairingBox;
    @FXML private ImageView phoneCameraQrImageView;
    @FXML private Label phoneCameraStatusLabel;

    @FXML private VBox lobbyAgreementStep;
    @FXML private VBox lobbySystemCheckStep;
    @FXML private Button continueToChecksButton;
    @FXML private Button backToAgreementButton;
    @FXML private CheckBox agreementCheckBox;
    @FXML private Button beginExamButton;
    @FXML private Button cancelLobbyButton;
    @FXML private Button pairPhoneCameraButton;
    @FXML private Label environmentStatusLabel;
    @FXML private Label environmentDetailLabel;

    @FXML private Label examTitleLabel;
    @FXML private Label examSubtitleLabel;
    @FXML private Label timerLabel;
    @FXML private FlowPane questionNumberPane;
    @FXML private Label answeredCountLabel;
    @FXML private Label markedCountLabel;
    @FXML private Label questionNumberLabel;
    @FXML private Label questionTypeLabel;
    @FXML private Label questionTextLabel;
    @FXML private ImageView questionImageView;
    @FXML private StackPane questionImageWrapper;
    @FXML private VBox answerContainer;
    @FXML private Button previousButton;
    @FXML private Button nextButton;
    @FXML private Button markReviewButton;
    @FXML private Button submitButton;

    @FXML private StackPane examRoot;
    @FXML private BorderPane examContent;
    @FXML private StackPane violationOverlay;
    @FXML private Label violationTitleLabel;
    @FXML private Label violationMessageLabel;

    @FXML private StackPane lobbyCameraPreviewPane;
    @FXML private ImageView lobbyCameraPreviewImageView;
    @FXML private Label lobbyCameraPreviewPlaceholder;

    private Long currentAttemptId;
    private Long currentExamId;
    private Long lobbyExamId;
    private int lobbyTimeLimitMinutes;
    private boolean examLoading = false;
    private volatile boolean examClosing = false;

    // DEBUG
    private boolean lobbyBehaviorDebugMode = false;


    private final List<ExamTakeQuestion> questions = new ArrayList<>();
    private final Map<String, ViolationSettingRequest> violationSettingMap = new HashMap<>();
    private final Map<String, Integer> violationAttemptMap = new HashMap<>();
    private final ExamApiService examApiService = new ExamApiService();
    private final BrandingService brandingService = new BrandingService();
    private LobbyCameraVerifier lobbyCameraVerifier;
    private LobbyObjectDetector lobbyObjectDetector;
    private LobbyCameraPreviewService lobbyCameraPreviewService;
    private AiRuntimeManager aiRuntimeManager;
//    private MediaPipeFaceServiceClient mediaPipeFaceServiceClient;
    private Timeline mediaPipePreviewTimeline;
    private Timeline phonePreviewTimeline;
    private Timeline evidenceBufferTimeline;


    private StackPane reviewOverlay;
    private Stage dashboardStage;
    private int violationCount = 0;
    private boolean internalDialogOpen = false;
    private boolean violationMonitoringReady = false;
    private int currentQuestionIndex = 0;
    private Long activeQuestionId = null;
    private long activeQuestionStartedAt = 0L;
    private ToggleGroup currentToggleGroup;
    private Timeline timerTimeline;
    private int remainingSeconds = 60 * 60;
    private boolean examStarted = false;
    private boolean examEnding = false;
    private boolean violationMonitoringEnabled = false;
    private boolean lobbyEnvironmentPassed = false;
    private Timeline lobbyAutoCheckTimeline;
    private int lobbyAutoCheckPassStreak = 0;
    private int lobbyAutoCheckFailStreak = 0;

    private static final int REQUIRED_LOBBY_PASS_STREAK = 3;
    private static final int REQUIRED_LOBBY_FAIL_STREAK = 2;
    private Timeline phoneCameraStatusTimeline;
    private String currentPhoneCameraToken;
    private boolean phoneCameraActive = false;
    private long lastMouseActivityAt = System.currentTimeMillis();
    private long lastKeyboardActivityAt = System.currentTimeMillis();

    private static final ZoneId MANILA_ZONE = ZoneId.of("Asia/Manila");
    private static final int LOBBY_OPEN_MINUTES_BEFORE_START = 15;

    private OffsetDateTime lobbyStartDateTime;
    private OffsetDateTime lobbyEndDateTime;
    private Timeline lobbyScheduleTimeline;

    /*
     * Face behavior correlation state.
     *
     * We do not flag a short glance immediately.
     * We only flag if the student keeps looking away for a few seconds
     * while there is no keyboard or mouse activity.
     */
    private String currentFaceBehaviorDirection = null;
    private long faceBehaviorStartedAt = 0L;
    private long lastFaceBehaviorViolationAt = 0L;

    private Timeline examDeskMonitorTimeline;
    private boolean examDeskMonitorRunning = false;

    private final Map<String, List<Long>> repeatedViolationWindowMap = new HashMap<>();

    private static final long REPEATED_VIOLATION_WINDOW_MS = 3000L;
    private static final int REQUIRED_REPEATED_VIOLATION_COUNT = 3;

    private Timeline behaviorCorrelationTimeline;

    private static final long FACE_BEHAVIOR_REQUIRED_MS = 4000L; // between 3-5 seconds
    private static final long INPUT_INACTIVE_REQUIRED_MS = 4000L;
    private static final long FACE_BEHAVIOR_COOLDOWN_MS = 30000L;

    private String baselineDeskSignature = null;
    private List<String> baselineDeskVisibleItems = new ArrayList<>();
    private boolean baselineDeskCaptured = false;

    private enum LobbyCheckStatus {
        PENDING,
        CHECKING,
        PASSED,
        FAILED,
        WARNING
    }

    private final Map<Long, Boolean> savingQuestionMap = new HashMap<>();

    private final List<BufferedEvidenceFrame> evidenceFrameBuffer = new ArrayList<>();
    private static final int EVIDENCE_FRAME_BUFFER_LIMIT = 20;

    private record BufferedEvidenceFrame(
            Mat frame,
            long capturedAtMs
    ) {
    }

    private static final DateTimeFormatter VIOLATION_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");

    @FXML
    private void initialize() {
        lobbyView.setVisible(true);
        lobbyView.setManaged(true);

        examContent.setVisible(false);
        examContent.setManaged(false);

        violationOverlay.setVisible(false);
        violationOverlay.setManaged(false);

        beginExamButton.setDisable(true);
        continueToChecksButton.setDisable(true);

        lobbyAgreementStep.setVisible(true);
        lobbyAgreementStep.setManaged(true);

        lobbySystemCheckStep.setVisible(false);
        lobbySystemCheckStep.setManaged(false);

        agreementCheckBox.selectedProperty().addListener((obs, oldVal, selected) -> {
            continueToChecksButton.setDisable(!selected);
        });

        loadBranding();
        showAgreementStep();
        resetLobbyEnvironmentCheck();
        initializeAiDetectors();

        Platform.runLater(this::bindLobbyCameraPreviewSize);
    }

    private void bindLobbyCameraPreviewSize() {
        if (lobbyCameraPreviewImageView == null || lobbyCameraPreviewPane == null) {
            return;
        }

        lobbyCameraPreviewImageView.fitWidthProperty().unbind();
        lobbyCameraPreviewImageView.fitHeightProperty().unbind();

        lobbyCameraPreviewImageView.fitWidthProperty()
                .bind(lobbyCameraPreviewPane.widthProperty().subtract(16));

        lobbyCameraPreviewImageView.fitHeightProperty()
                .bind(lobbyCameraPreviewPane.heightProperty().subtract(16));

        lobbyCameraPreviewImageView.setPreserveRatio(true);
        lobbyCameraPreviewImageView.setSmooth(true);
    }

    private void bufferEvidenceFrame() {
        if (lobbyCameraPreviewService == null) {
            return;
        }

        Mat frame = lobbyCameraPreviewService.getActiveFrame();

        if (frame == null || frame.empty()) {
            return;
        }

        evidenceFrameBuffer.add(new BufferedEvidenceFrame(frame, System.currentTimeMillis()));

        while (evidenceFrameBuffer.size() > EVIDENCE_FRAME_BUFFER_LIMIT) {
            BufferedEvidenceFrame old = evidenceFrameBuffer.remove(0);

            if (old.frame() != null) {
                old.frame().release();
            }
        }
    }

    private void initializeAiDetectors() {
        try {
            lobbyObjectDetector = new LobbyObjectDetector();
        } catch (Exception e) {
            e.printStackTrace();
            lobbyObjectDetector = null;
        }
    }

    private void loadBranding() {
        Task<BrandingResponse> task = new Task<>() {
            @Override
            protected BrandingResponse call() throws Exception {
                return brandingService.getBranding();
            }
        };

        task.setOnSucceeded(event -> applyBranding(task.getValue()));
        task.setOnFailed(event -> applyBranding(null));

        Thread thread = new Thread(task, "load-branding-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void applyBranding(BrandingResponse branding) {
        String defaultTitle = "ExamGuard";
        String defaultTagline = "Secure Digital Examination Platform";

        if (branding == null) {
            brandSchoolNameLabel.setText(defaultTitle);
            brandTaglineLabel.setText(defaultTagline);
            return;
        }

        String schoolName = firstNonBlank(
                branding.getSchoolName(),
                branding.getShortName(),
                defaultTitle
        );

        String tagline = firstNonBlank(
                branding.getTagline(),
                defaultTagline
        );

        brandSchoolNameLabel.setText(schoolName);
        brandTaglineLabel.setText(tagline);

        String logoUrl = branding.getLogoUrl();
        if (logoUrl != null && !logoUrl.isBlank()) {
            String fullLogoUrl = logoUrl.startsWith("http://") || logoUrl.startsWith("https://")
                    ? logoUrl
                    : AppConfig.BASE_URL + (logoUrl.startsWith("/") ? logoUrl : "/" + logoUrl);

            schoolLogoImageView.setImage(new Image(fullLogoUrl, true));
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        return "";
    }

    private void showAgreementStep() {
        stopLobbyAutoCheck();

        lobbyAgreementStep.setVisible(true);
        lobbyAgreementStep.setManaged(true);

        lobbySystemCheckStep.setVisible(false);
        lobbySystemCheckStep.setManaged(false);

        lobbyStepLabel.setText("Step 1 of 2");

        continueToChecksButton.setVisible(true);
        continueToChecksButton.setManaged(true);

        backToAgreementButton.setVisible(false);
        backToAgreementButton.setManaged(false);

        beginExamButton.setVisible(false);
        beginExamButton.setManaged(false);
        beginExamButton.setDisable(true);

        stopLobbyCameraPreview();
        stopMediaPipePreviewTracking();

        lobbyBehaviorDebugMode = false;
//        stopBehaviorCorrelationMonitoring();
    }

    private void showSystemCheckStep() {
        lobbyAgreementStep.setVisible(false);
        lobbyAgreementStep.setManaged(false);

        lobbySystemCheckStep.setVisible(true);
        lobbySystemCheckStep.setManaged(true);

        lobbyStepLabel.setText("Step 2 of 2");

        continueToChecksButton.setVisible(false);
        continueToChecksButton.setManaged(false);

        backToAgreementButton.setVisible(true);
        backToAgreementButton.setManaged(true);

        beginExamButton.setVisible(true);
        beginExamButton.setManaged(true);
        beginExamButton.setDisable(true);

        startLobbyCameraPreview();
        startLobbyAutoCheck();
    }

    private void startLobbyAutoCheck() {
        stopLobbyAutoCheck();

        lobbyEnvironmentPassed = false;
        lobbyAutoCheckPassStreak = 0;
        lobbyAutoCheckFailStreak = 0;

        baselineDeskSignature = null;
        baselineDeskVisibleItems.clear();
        baselineDeskCaptured = false;

        setEnvironmentStatus(
                LobbyCheckStatus.CHECKING,
                "Checking",
                "Checking camera, examinee, desk, devices, connection, and display setup..."
        );

        lobbyAutoCheckTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> runLobbyAutoCheckOnce())
        );

        lobbyAutoCheckTimeline.setCycleCount(Timeline.INDEFINITE);
        lobbyAutoCheckTimeline.play();

        runLobbyAutoCheckOnce();
    }

    private void stopLobbyAutoCheck() {
        if (lobbyAutoCheckTimeline != null) {
            lobbyAutoCheckTimeline.stop();
            lobbyAutoCheckTimeline = null;
        }

        lobbyAutoCheckPassStreak = 0;
        lobbyAutoCheckFailStreak = 0;
    }

    private void runLobbyAutoCheckOnce() {
        if (examLoading || examEnding) {
            return;
        }

        boolean connectionOk = checkBackendConnectionSilently();
        boolean screenOk = Screen.getScreens().size() <= 1;

        LobbyObjectDetector.DeskBaselineResult deskResult = analyzeCurrentDeskFrame();

        boolean passed =
                connectionOk
                        && screenOk
                        && deskResult != null
                        && deskResult.passed();

        if (passed) {
            lobbyAutoCheckPassStreak++;
            lobbyAutoCheckFailStreak = 0;

            baselineDeskSignature = deskResult.signature();
            baselineDeskVisibleItems = new ArrayList<>(deskResult.visibleItems());
            baselineDeskCaptured = true;

            if (lobbyAutoCheckPassStreak >= REQUIRED_LOBBY_PASS_STREAK) {
                lobbyEnvironmentPassed = true;

                setEnvironmentStatus(
                        LobbyCheckStatus.PASSED,
                        "Ready",
                        "Setup verified."
                );

                updateBeginExamButton();
                return;
            }

            setEnvironmentStatus(
                    LobbyCheckStatus.CHECKING,
                    "Checking",
                    "Good setup detected. Please hold steady... "
                            + lobbyAutoCheckPassStreak
                            + "/"
                            + REQUIRED_LOBBY_PASS_STREAK
            );

            updateBeginExamButton();
            return;
        }

        lobbyEnvironmentPassed = false;
        lobbyAutoCheckFailStreak++;
        lobbyAutoCheckPassStreak = 0;

        String message;

        if (!connectionOk) {
            message = "Cannot reach the ExamGuard server. Please check your connection.";
        } else if (!screenOk) {
            message = "Multiple monitors detected. Use only one display.";
        } else if (deskResult == null) {
            message = "Camera is still starting. Keep your setup visible.";
        } else {
            message = deskResult.message();
        }

        setEnvironmentStatus(
                lobbyAutoCheckFailStreak >= REQUIRED_LOBBY_FAIL_STREAK
                        ? LobbyCheckStatus.FAILED
                        : LobbyCheckStatus.CHECKING,
                lobbyAutoCheckFailStreak >= REQUIRED_LOBBY_FAIL_STREAK
                        ? "Needs Adjustment"
                        : "Checking",
                message
        );

        updateBeginExamButton();
    }

    private LobbyObjectDetector.DeskBaselineResult analyzeCurrentDeskFrame() {
        if (lobbyObjectDetector == null || !lobbyObjectDetector.isAvailable()) {
            return new LobbyObjectDetector.DeskBaselineResult(
                    false,
                    "Camera/object detection is not ready. Please restart the camera check.",
                    0, 0, 0, 0, 0, 0,
                    List.of(),
                    "DETECTOR_UNAVAILABLE"
            );
        }

        if (lobbyCameraPreviewService == null) {
            return null;
        }

        Mat frame = lobbyCameraPreviewService.getActiveFrame();

        if (frame == null || frame.empty()) {
            return null;
        }

        return lobbyObjectDetector.analyzeDeskBaselineStable(frame);
    }

    private boolean checkBackendConnectionSilently() {
        try {
            long latency = examApiService.checkBackendLatency();
            return latency > 0 && latency <= 3500;
        } catch (Exception e) {
            return false;
        }
    }

    private void setEnvironmentStatus(
            LobbyCheckStatus status,
            String title,
            String detail
    ) {
        if (environmentStatusLabel == null || environmentDetailLabel == null) {
            return;
        }

        environmentStatusLabel.setText(title);
        environmentDetailLabel.setText(detail == null ? "" : detail);

        environmentStatusLabel.getStyleClass().removeAll(
                "lobby-status-pending",
                "lobby-status-checking",
                "lobby-status-passed",
                "lobby-status-failed",
                "lobby-status-warning"
        );

        switch (status) {
            case PASSED -> environmentStatusLabel.getStyleClass().add("lobby-status-passed");
            case FAILED -> environmentStatusLabel.getStyleClass().add("lobby-status-failed");
            case WARNING -> environmentStatusLabel.getStyleClass().add("lobby-status-warning");
            case CHECKING -> environmentStatusLabel.getStyleClass().add("lobby-status-checking");
            default -> environmentStatusLabel.getStyleClass().add("lobby-status-pending");
        }
    }

    private String formatDuration(int minutes) {
        if (minutes <= 0) {
            return "--";
        }

        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;

        if (hours > 0 && remainingMinutes > 0) {
            return hours + " hr " + remainingMinutes + " min";
        }

        if (hours > 0) {
            return hours + " hr" + (hours == 1 ? "" : "s");
        }

        return minutes + " min";
    }

    // ACTIONS

    @FXML
    private void handlePairPhoneCamera() {
        if (currentExamId != null && currentAttemptId != null) {
            startPhonePairingSession();
            return;
        }

        if (lobbyExamId == null) {
            showError("Exam not found. Please go back and open the exam lobby again.");
            return;
        }

        phoneCameraPairingBox.setVisible(true);
        phoneCameraPairingBox.setManaged(true);

        setPhoneCameraStatus("Preparing", "warning");

        pairPhoneCameraButton.setDisable(true);
        beginExamButton.setDisable(true);

        Task<ExamTakingResponse> prepareTask = new Task<>() {
            @Override
            protected ExamTakingResponse call() throws Exception {
                new AiAssetSyncService().syncAssets();
                return examApiService.getExamForTaking(lobbyExamId);
            }
        };

        prepareTask.setOnSucceeded(event -> {
            ExamTakingResponse response = prepareTask.getValue();

            if (!applyExamTakingResponse(response)
                    || currentExamId == null
                    || currentAttemptId == null) {

                pairPhoneCameraButton.setDisable(false);
                setPhoneCameraStatus("Not Ready", "failed");

                showError(
                        "Phone camera pairing is not ready yet. "
                                + "The exam attempt was not created by the server. "
                                + "Please run verification again or reopen the lobby."
                );

                updateBeginExamButton();
                return;
            }

            startPhonePairingSession();
        });

        prepareTask.setOnFailed(event -> {
            pairPhoneCameraButton.setDisable(false);
            setPhoneCameraStatus("Failed", "failed");
            updateBeginExamButton();

            Throwable ex = prepareTask.getException();

            showError(
                    ex == null
                            ? "Failed to prepare phone camera pairing."
                            : ex.getMessage()
            );
        });

        Thread thread = new Thread(prepareTask, "prepare-phone-camera-pairing-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void startPhonePairingSession() {
        if (currentAttemptId == null || currentExamId == null) {
            pairPhoneCameraButton.setDisable(false);
            setPhoneCameraStatus("Not Ready", "failed");
            showError("Phone camera pairing is not ready. Please run the lobby setup first.");
            return;
        }

        phoneCameraPairingBox.setVisible(true);
        phoneCameraPairingBox.setManaged(true);

        setPhoneCameraStatus("Creating", "warning");

        pairPhoneCameraButton.setDisable(true);
        beginExamButton.setDisable(true);

        Task<CreateCameraSessionResponse> task = new Task<>() {
            @Override
            protected CreateCameraSessionResponse call() {
                return examApiService.createCameraSession(
                        currentAttemptId,
                        currentExamId,
                        Session.getSchoolId()
                );
            }
        };

        task.setOnSucceeded(event -> {
            CreateCameraSessionResponse response = task.getValue();

            if (response == null
                    || response.getPairingToken() == null
                    || response.getPairingToken().isBlank()
                    || response.getPairingUrl() == null
                    || response.getPairingUrl().isBlank()) {

                pairPhoneCameraButton.setDisable(false);
                setPhoneCameraStatus("Failed", "failed");
                updateBeginExamButton();
                showError("Failed to create phone camera pairing details.");
                return;
            }

            currentPhoneCameraToken = response.getPairingToken();

            phoneCameraQrImageView.setImage(generateQrCode(response.getPairingUrl()));

            setPhoneCameraStatus("Waiting", "warning");

            startPhoneCameraStatusPolling(currentPhoneCameraToken);
        });

        task.setOnFailed(event -> {
            pairPhoneCameraButton.setDisable(false);
            setPhoneCameraStatus("Failed", "failed");
            updateBeginExamButton();

            Throwable ex = task.getException();

            showError(
                    ex == null
                            ? "Failed to create phone camera session."
                            : ex.getMessage()
            );
        });

        Thread thread = new Thread(task, "phone-camera-pairing-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private Image generateQrCode(String text) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(
                    text,
                    BarcodeFormat.QR_CODE,
                    300,
                    300
            );

            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(matrix);

            return SwingFXUtils.toFXImage(bufferedImage, null);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate phone camera QR code.", e);
        }
    }

    private void startPhonePreviewPolling(String token) {
        stopLobbyCameraPreview();
        stopPhonePreviewPolling();

        lobbyCameraPreviewPlaceholder.setText("Receiving phone camera feed...");
        lobbyCameraPreviewPlaceholder.setVisible(true);
        lobbyCameraPreviewPlaceholder.setManaged(true);

        phonePreviewTimeline = new Timeline(
                new KeyFrame(Duration.millis(900), event -> loadPhonePreviewFrame(token))
        );

        phonePreviewTimeline.setCycleCount(Timeline.INDEFINITE);
        phonePreviewTimeline.play();

        loadPhonePreviewFrame(token);
    }

//    private void startMediaPipePreviewTracking() {
//        stopMediaPipePreviewTracking();
//
//        mediaPipePreviewTimeline = new Timeline(
//                new KeyFrame(Duration.millis(500), event -> {
//                    if (mediaPipeFaceServiceClient == null || lobbyCameraPreviewService == null) {
//                        return;
//                    }
//
//                    new Thread(() -> {
//                        try {
//                            mediaPipeFaceServiceClient.analyzeOnly(lobbyCameraPreviewService);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }, "mediapipe-preview-tracking-thread").start();
//                })
//        );
//
//        mediaPipePreviewTimeline.setCycleCount(Timeline.INDEFINITE);
//        mediaPipePreviewTimeline.play();
//    }

    private void stopMediaPipePreviewTracking() {
        if (mediaPipePreviewTimeline != null) {
            mediaPipePreviewTimeline.stop();
            mediaPipePreviewTimeline = null;
        }

        MediaPipeOverlayStore.clear();
    }

    private void loadPhonePreviewFrame(String token) {
        Task<byte[]> task = new Task<>() {
            @Override
            protected byte[] call() {
                return examApiService.getCameraPreviewFrame(token);
            }
        };

        task.setOnSucceeded(event -> {
            byte[] bytes = task.getValue();

            if (bytes == null || bytes.length == 0) {
                return;
            }

            Mat phoneMat = Imgcodecs.imdecode(
                    new MatOfByte(bytes),
                    Imgcodecs.IMREAD_COLOR
            );

            if (phoneMat == null || phoneMat.empty()) {
                return;
            }

            if (lobbyCameraPreviewService != null) {
                lobbyCameraPreviewService.updatePhoneFrame(phoneMat);
                lobbyCameraPreviewService.drawOverlayForExternalFrame(phoneMat);
            }

            BufferedImage bufferedImage = matToBufferedImage(phoneMat);
            Image image = SwingFXUtils.toFXImage(bufferedImage, null);

            lobbyCameraPreviewImageView.setImage(image);
            lobbyCameraPreviewPlaceholder.setVisible(false);
            lobbyCameraPreviewPlaceholder.setManaged(false);

            phoneMat.release();
        });

        Thread thread = new Thread(task, "phone-preview-frame-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void stopPhonePreviewPolling() {
        if (phonePreviewTimeline != null) {
            phonePreviewTimeline.stop();
            phonePreviewTimeline = null;
        }
    }

    private void startPhoneCameraStatusPolling(String token) {
        stopPhoneCameraStatusPolling();

        phoneCameraStatusTimeline = new Timeline(
                new KeyFrame(Duration.seconds(3), event -> checkPhoneCameraStatus(token))
        );

        phoneCameraStatusTimeline.setCycleCount(Timeline.INDEFINITE);
        phoneCameraStatusTimeline.play();

        checkPhoneCameraStatus(token);
    }

    private void checkPhoneCameraStatus(String token) {
        Task<CameraSessionStatusResponse> task = new Task<>() {
            @Override
            protected CameraSessionStatusResponse call() {
                return examApiService.getCameraSessionStatus(token);
            }
        };

        task.setOnSucceeded(event -> {
            CameraSessionStatusResponse response = task.getValue();
            String status = response.getStatus();

            if ("ACTIVE".equalsIgnoreCase(status)) {
                phoneCameraActive = true;
                setPhoneCameraStatus("Active", "passed");
                pairPhoneCameraButton.setDisable(true);

                startPhonePreviewPolling(token);

                stopPhoneCameraStatusPolling();
            } else if ("PAIRED".equalsIgnoreCase(status)) {
                phoneCameraActive = false;
                setPhoneCameraStatus("Paired", "warning");
            } else if ("EXPIRED".equalsIgnoreCase(status)) {
                phoneCameraActive = false;
                setPhoneCameraStatus("Expired", "failed");
                pairPhoneCameraButton.setDisable(false);
                stopPhoneCameraStatusPolling();
            } else {
                phoneCameraActive = false;
                setPhoneCameraStatus("Waiting", "warning");
            }

            updateBeginExamButton();
        });

        task.setOnFailed(event -> {
            phoneCameraActive = false;
            setPhoneCameraStatus("Offline", "failed");
            updateBeginExamButton();
        });

        Thread thread = new Thread(task, "phone-camera-status-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void stopPhoneCameraStatusPolling() {
        if (phoneCameraStatusTimeline != null) {
            phoneCameraStatusTimeline.stop();
            phoneCameraStatusTimeline = null;
        }
    }

    private void setPhoneCameraStatus(String text, String type) {
        phoneCameraStatusLabel.setText(text);

        phoneCameraStatusLabel.getStyleClass().removeAll(
                "lobby-status-pending",
                "lobby-status-checking",
                "lobby-status-passed",
                "lobby-status-failed",
                "lobby-status-warning"
        );

        switch (type) {
            case "passed" -> phoneCameraStatusLabel.getStyleClass().add("lobby-status-passed");
            case "failed" -> phoneCameraStatusLabel.getStyleClass().add("lobby-status-failed");
            case "checking" -> phoneCameraStatusLabel.getStyleClass().add("lobby-status-checking");
            default -> phoneCameraStatusLabel.getStyleClass().add("lobby-status-warning");
        }
    }

    @FXML
    private void handleContinueToSystemChecks() {
        /*
         * Step 1 -> Step 2 is still lobby flow.
         *
         * Allowed:
         * - Student accepted agreement.
         * - Exam exists.
         * - Current time is already inside lobby window.
         *
         * Not required yet:
         * - Actual exam start time.
         *
         * Begin Exam button will separately check if the real start time has arrived.
         */

        if (!agreementCheckBox.isSelected()) {
            showError("Please read and accept the exam rules and data privacy agreement before continuing.");
            return;
        }

        if (lobbyExamId == null) {
            showError("Exam not found.");
            return;
        }

        if (!canEnterLobbyNow()) {
            showError("Lobby opens 15 minutes before the exam start time.");
            return;
        }

        continueToChecksButton.setDisable(true);

        LoadingSpinner.setLoading(
                continueToChecksButton,
                true,
                "Preparing...",
                "Continue"
        );

        Task<ExamTakingResponse> task = new Task<>() {
            @Override
            protected ExamTakingResponse call() throws Exception {
                new AiAssetSyncService().syncAssets();

                /*
                 * This backend call prepares the lobby attempt shell.
                 * It should allow access during lobby window.
                 * It should NOT require actual start time yet.
                 */
                return examApiService.getExamForTaking(lobbyExamId);
            }
        };

        task.setOnSucceeded(event -> {
            LoadingSpinner.setLoading(
                    continueToChecksButton,
                    false,
                    "Preparing...",
                    "Continue"
            );

            ExamTakingResponse response = task.getValue();

            if (!applyExamTakingResponse(response)) {
                continueToChecksButton.setDisable(false);
                showError("Failed to prepare exam lobby.");
                return;
            }

            resetLobbyEnvironmentCheck();
            showSystemCheckStep();
            updateBeginExamButton();
        });

        task.setOnFailed(event -> {
            LoadingSpinner.setLoading(
                    continueToChecksButton,
                    false,
                    "Preparing...",
                    "Continue"
            );

            continueToChecksButton.setDisable(false);

            Throwable ex = task.getException();

            showError(
                    ex == null || ex.getMessage() == null
                            ? "Failed to prepare exam lobby."
                            : ex.getMessage()
            );
        });

        Thread thread = new Thread(task, "prepare-lobby-attempt-thread");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleBackToAgreement() {
        stopLobbyAutoCheck();
        showAgreementStep();
        resetLobbyEnvironmentCheck();

        phoneCameraPairingBox.setVisible(false);
        phoneCameraPairingBox.setManaged(false);
    }

    @FXML
    private void handlePrevious() {
        saveCurrentAnswer();
        stopQuestionTimer();

        if (currentQuestionIndex > 0) {
            showQuestion(currentQuestionIndex - 1);
        }
    }

    @FXML
    private void handleNext() {
        saveCurrentAnswer();
        stopQuestionTimer();

        if (currentQuestionIndex < questions.size() - 1) {
            showQuestion(currentQuestionIndex + 1);
        } else {
            showReviewDialog();
        }
    }

    @FXML
    private void handleMarkForReview() {
        ExamTakeQuestion question = questions.get(currentQuestionIndex);
        question.setMarkedForReview(!question.isMarkedForReview());

        markReviewButton.setText(question.isMarkedForReview() ? "Unmark Review" : "Mark for Review");

        refreshQuestionNavigationStyles();
        updateProgress();
    }

    @FXML
    private void handleSubmitExam() {
        saveCurrentAnswer();
        showReviewDialog();
    }

    @FXML
    private void handleBeginExamFromLobby() {

        if (!canBeginExamNow()) {
            showError("You cannot begin yet. The exam can only start at the scheduled start time.");
            updateBeginExamButton();
            return;
        }

        if (beginExamButton.isDisabled() || examLoading) {
            return;
        }

        examLoading = true;

        LoadingSpinner.setLoading(
                beginExamButton,
                true,
                "Preparing...",
                "Begin Exam"
        );

        Task<ExamTakingResponse> task = new Task<>() {
            @Override
            protected ExamTakingResponse call() throws Exception {
                return examApiService.beginExam(lobbyExamId);
            }
        };

        task.setOnSucceeded(event -> {

            ExamTakingResponse response = task.getValue();

            if (!applyExamTakingResponse(response)) {
                examLoading = false;
                updateBeginExamButton();
                showError("Failed to load exam attempt.");
                return;
            }

            remainingSeconds =
                    response.getRemainingSeconds() == null
                            ? lobbyTimeLimitMinutes * 60
                            : response.getRemainingSeconds().intValue();

            LoadingSpinner.setLoading(
                    beginExamButton,
                    false,
                    "Preparing...",
                    "Begin Exam"
            );

            continueToChecksButton.setDisable(true);
            backToAgreementButton.setDisable(true);
            cancelLobbyButton.setDisable(true);

            lobbyBehaviorDebugMode = false;

            startExam(
                    lobbyExamId,
                    response.getTimeLimitMinutes()
            );

        });

        task.setOnFailed(event -> {

            LoadingSpinner.setLoading(
                    beginExamButton,
                    false,
                    "Preparing...",
                    "Begin Exam"
            );

            examLoading = false;

            Throwable ex = task.getException();

            showError(
                    ex == null
                            ? "Failed to begin exam."
                            : ex.getMessage()
            );
        });

        Thread thread = new Thread(task, "begin-exam-thread");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleCancelLobby() {
        examClosing = true;
        stopLobbyScheduleWatcher();
        stopLobbyAutoCheck();
        stopEvidenceBuffering();
        stopMediaPipePreviewTracking();
        cleanupPhoneCameraSession();
        stopLobbyCameraPreview();
        returnToDashboard();
    }

    private void installWindowCloseHandler() {
        if (examRoot == null || examRoot.getScene() == null) {
            return;
        }

        Stage stage = (Stage) examRoot.getScene().getWindow();

        stage.setOnCloseRequest(event -> {
            cleanupPhoneCameraSession();
        });
    }

    private void startEvidenceBuffering() {
        stopEvidenceBuffering();

        evidenceFrameBuffer.clear();

        evidenceBufferTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> bufferEvidenceFrame())
        );

        evidenceBufferTimeline.setCycleCount(Timeline.INDEFINITE);
        evidenceBufferTimeline.play();

        bufferEvidenceFrame();
    }

    private void stopEvidenceBuffering() {
        if (evidenceBufferTimeline != null) {
            evidenceBufferTimeline.stop();
            evidenceBufferTimeline = null;
        }

        for (BufferedEvidenceFrame item : evidenceFrameBuffer) {
            if (item.frame() != null) {
                item.frame().release();
            }
        }

        evidenceFrameBuffer.clear();
    }

    private void cleanupPhoneCameraSession() {
        stopPhoneCameraStatusPolling();
        stopPhonePreviewPolling();

        if (currentPhoneCameraToken != null && !currentPhoneCameraToken.isBlank()) {
            new Thread(() -> {
                try {
                    examApiService.endPhoneCameraSession(currentPhoneCameraToken);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "end-phone-camera-session-thread").start();
        }

        currentPhoneCameraToken = null;
        phoneCameraActive = false;
    }

    // VIOLATION

    private void setupViolationMonitoring() {
        if (violationMonitoringReady || examRoot.getScene() == null) {
            return;
        }

        violationMonitoringReady = true;

        Stage stage = (Stage) examRoot.getScene().getWindow();
        Scene scene = examRoot.getScene();

        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        stage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!examStarted || internalDialogOpen || examEnding) {
                return;
            }

            if (!isFocused) {
                recordViolation(
                        "FOCUS_LOST",
                        "Focus Lost",
                        "Please stay on the exam screen. Leaving the exam window may be recorded as a violation.",
                        true
                );
            }
        });

        stage.fullScreenProperty().addListener((obs, wasFullScreen, isFullScreen) -> {
            if (!examStarted || internalDialogOpen || examEnding) {
                return;
            }

            if (!isFullScreen) {
                recordViolation(
                        "FULLSCREEN_EXIT",
                        "Fullscreen Exit Detected",
                        "The exam must remain in fullscreen mode.",
                        true
                );

                Platform.runLater(() -> {
                    if (!examEnding && examStarted) {
                        stage.setFullScreen(true);
                    }
                });
            }
        });

        stage.iconifiedProperty().addListener((obs, wasMinimized, isMinimized) -> {
            if (!examStarted || internalDialogOpen || examEnding) {
                return;
            }

            if (isMinimized) {
                recordViolation(
                        "WINDOW_MINIMIZED",
                        "Window Minimized",
                        "Minimizing the exam window is not allowed.",
                        true
                );

                Platform.runLater(() -> {
                    if (!examEnding && examStarted) {
                        stage.setIconified(false);
                        stage.setFullScreen(true);
                        stage.toFront();
                    }
                });
            }
        });

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            lastKeyboardActivityAt = System.currentTimeMillis();
            if (!examStarted || internalDialogOpen || examEnding) {
                return;
            }

            boolean blockedShortcut =
                    event.isShortcutDown() &&
                            (
                                    event.getCode() == KeyCode.C ||
                                            event.getCode() == KeyCode.V ||
                                            event.getCode() == KeyCode.X ||
                                            event.getCode() == KeyCode.A ||
                                            event.getCode() == KeyCode.P ||
                                            event.getCode() == KeyCode.S
                            );

            boolean suspiciousShortcut =
                    event.isAltDown() ||
                            event.isMetaDown() ||
                            event.getCode() == KeyCode.PRINTSCREEN ||
                            event.getCode() == KeyCode.ESCAPE;

            if (blockedShortcut || suspiciousShortcut) {
                event.consume();

                recordViolation(
                        "RESTRICTED_KEY",
                        "Restricted Key Detected",
                        "Keyboard shortcuts are restricted during the exam.",
                        false
                );
            }
        });

        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            if (!examStarted || internalDialogOpen || examEnding) {
                return;
            }

            if (event.isSecondaryButtonDown()) {
                event.consume();

                recordViolation(
                        "RIGHT_CLICK",
                        "Right Click Blocked",
                        "Right click is disabled during the exam.",
                        false
                );
            }
        });

        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, event -> {
            lastMouseActivityAt = System.currentTimeMillis();
        });

        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, event -> {
            lastMouseActivityAt = System.currentTimeMillis();
        });

        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, event -> {
            lastMouseActivityAt = System.currentTimeMillis();
        });

        if (Screen.getScreens().size() > 1 && examStarted && !internalDialogOpen && !examEnding) {
            recordViolation(
                    "MULTIPLE_MONITORS",
                    "Multiple Monitors Detected",
                    "Please use only one display during the exam.",
                    true
            );
        }
    }

    private Mat getBufferedFrameBefore(long targetMs) {
        BufferedEvidenceFrame best = null;

        for (BufferedEvidenceFrame item : evidenceFrameBuffer) {
            if (item.capturedAtMs() <= targetMs) {
                best = item;
            }
        }

        return best == null || best.frame() == null
                ? null
                : best.frame().clone();
    }

    private String uploadFrame(Mat frame, String label) {
        if (frame == null || frame.empty()) {
            return null;
        }

        try {
            File file = File.createTempFile("examguard-" + label + "-", ".jpg");

            Imgcodecs.imwrite(file.getAbsolutePath(), frame);

            ImageUploadResponse response =
                    examApiService.uploadViolationEvidence(file);

            file.delete();

            if (response == null || !response.isSuccess()) {
                return null;
            }

            return response.getImageUrl();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String frameJson(String label, String url, int offsetMs) {
        if (url == null) {
            return "{"
                    + "\"label\":\"" + label + "\","
                    + "\"url\":null,"
                    + "\"offsetMs\":" + offsetMs
                    + "}";
        }

        return "{"
                + "\"label\":\"" + label + "\","
                + "\"url\":\"" + escapeJson(url) + "\","
                + "\"offsetMs\":" + offsetMs
                + "}";
    }

    private void releaseQuietly(Mat frame) {
        if (frame != null) {
            frame.release();
        }
    }

    public void setDashboardStage(Stage dashboardStage) {
        this.dashboardStage = dashboardStage;
    }

    public void setExamLobby(
            Long examId,
            String examTitle,
            int timeLimitMinutes,
            Long questionCount,
            OffsetDateTime startDateTime,
            OffsetDateTime endDateTime
    ) {

        this.lobbyStartDateTime = startDateTime;
        this.lobbyEndDateTime = endDateTime;

        if (!canEnterLobbyNow()) {
            showError("Lobby opens 15 minutes before the exam start time.");
            returnToDashboard();
            return;
        }

        startLobbyScheduleWatcher();

        this.lobbyExamId = examId;
        this.lobbyTimeLimitMinutes = timeLimitMinutes;

        lobbyExamTitleLabel.setText(
                examTitle == null || examTitle.isBlank()
                        ? "Exam"
                        : examTitle
        );

        lobbyDurationLabel.setText(formatDuration(timeLimitMinutes));
        lobbyQuestionCountLabel.setText(questionCount + " questions");

        this.lobbyExamId = examId;
        this.lobbyTimeLimitMinutes = timeLimitMinutes;
        this.remainingSeconds = 0;

        examStarted = false;
        examEnding = false;
        internalDialogOpen = false;
        examLoading = false;

        questions.clear();

        lobbyView.setVisible(true);
        lobbyView.setManaged(true);

        examContent.setVisible(false);
        examContent.setManaged(false);

        violationOverlay.setVisible(false);
        violationOverlay.setManaged(false);

        updateTimerLabel();

        lobbyExamSubtitleLabel.setText(
                "Run the required system checks before starting. Timer will begin only after you click Begin Exam."
        );

        showAgreementStep();
        resetLobbyEnvironmentCheck();

        updateBeginExamButton();

        Platform.runLater(this::installWindowCloseHandler);
    }

    public void startExam(Long examId, int timeLimitMinutes) {
        stopLobbyScheduleWatcher();
        startEvidenceBuffering();
        try {
            if (this.remainingSeconds <= 0) {
                showError("Your exam time has already ended.");
                autoSubmitExam();
                return;
            }

            if (currentAttemptId == null || currentExamId == null || questions.isEmpty()) {
                examLoading = false;

                LoadingSpinner.setLoading(
                        beginExamButton,
                        false,
                        "Preparing...",
                        "Begin Exam"
                );

                continueToChecksButton.setDisable(false);
                backToAgreementButton.setDisable(false);
                cancelLobbyButton.setDisable(false);

                updateBeginExamButton();
                return;
            }

            examStarted = true;
            stopLobbyAutoCheck();

            /*
             * Keep camera and MediaPipe running during the exam.
             * Face behavior violations need live camera frames and live MediaPipe results.
             */
            startExamCameraMonitoring();


            lobbyView.setVisible(false);
            lobbyView.setManaged(false);

            examContent.setVisible(true);
            examContent.setManaged(true);

            if (violationMonitoringEnabled) {
                setupViolationMonitoring();
            }

            LoadingSpinner.setLoading(
                    beginExamButton,
                    false,
                    "Preparing...",
                    "Begin Exam"
            );

            setupTimer();
            startExamDeskMonitoring();
            renderQuestionNavigation();

            showQuestion(0);

            updateProgress();
            updateTimerLabel();

        } catch (Exception e) {
            e.printStackTrace();
            examLoading = false;
            updateBeginExamButton();
            showError("Failed to start exam.");
        }
    }

    private void resetLobbyEnvironmentCheck() {
        lobbyEnvironmentPassed = false;
        lobbyAutoCheckPassStreak = 0;
        lobbyAutoCheckFailStreak = 0;

        baselineDeskSignature = null;
        baselineDeskVisibleItems.clear();
        baselineDeskCaptured = false;

        setEnvironmentStatus(
                LobbyCheckStatus.PENDING,
                "Pending",
                "Camera setup check has not started yet."
        );

        updateBeginExamButton();
    }

    private boolean shouldUsePhoneCamera() {
        return currentPhoneCameraToken != null
                && !currentPhoneCameraToken.isBlank()
                && phoneCameraActive;
    }

    private void startExamCameraMonitoring() {

        if (shouldUsePhoneCamera()) {

            stopLobbyCameraPreview();

            startPhonePreviewPolling(currentPhoneCameraToken);

            return;
        }

        stopPhonePreviewPolling();

        startLobbyCameraPreview();
    }

    private void startLobbyCameraPreview() {
        if (lobbyCameraPreviewService == null) {
            lobbyCameraPreviewService = new LobbyCameraPreviewService(
                    lobbyCameraPreviewImageView,
                    lobbyCameraPreviewPlaceholder,
                    lobbyObjectDetector
            );
            lobbyCameraVerifier =
                    new LobbyCameraVerifier(
                            lobbyCameraPreviewService
                    );
        }

        lobbyCameraPreviewService.start();
    }

    private void stopLobbyCameraPreview() {
        if (lobbyCameraPreviewService != null) {
            lobbyCameraPreviewService.stop();
        }
    }

    private void updateBeginExamButton() {
        boolean checksReady =
                agreementCheckBox.isSelected()
                        && lobbyEnvironmentPassed;

        boolean scheduleReady = canBeginExamNow();

        beginExamButton.setDisable(
                !checksReady
                        || !scheduleReady
                        || examLoading
        );

        if (!scheduleReady && lobbyStartDateTime != null) {
            beginExamButton.setText("Begin Exam");
            lobbyExamSubtitleLabel.setText(
                    "Lobby is open. Complete the camera setup now. The exam can only begin at the scheduled start time."
            );
        } else {
            beginExamButton.setText("Begin Exam");
            lobbyExamSubtitleLabel.setText(
                    "Keep your environment visible. ExamGuard will continue monitoring during the exam."
            );
        }
    }

    private boolean loadExamFromBackend(Long examId) {
        try {
            ExamTakingResponse response = examApiService.getExamForTaking(examId);
            return applyExamTakingResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load exam content.");
            return false;
        }
    }

    private boolean applyExamTakingResponse(ExamTakingResponse response) {
        try {
            if (response == null) {
                return false;
            }

            currentAttemptId = response.getAttemptId();
            currentExamId = response.getExamId();

            examTitleLabel.setText(response.getTitle());
            lobbyExamTitleLabel.setText(response.getTitle());

            if (response.getDescription() != null && !response.getDescription().isBlank()) {
                examSubtitleLabel.setText(response.getDescription());
            } else {
                examSubtitleLabel.setText("Answer all questions. You can review before submitting.");
            }

            int minutes = response.getTimeLimitMinutes() != null
                    ? response.getTimeLimitMinutes()
                    : 60;

            this.remainingSeconds =
                    response.getRemainingSeconds() == null
                            ? minutes * 60
                            : response.getRemainingSeconds().intValue();

            questions.clear();

            if (response.getQuestions() != null) {
                questions.addAll(response.getQuestions());
            }

            lobbyQuestionCountLabel.setText(
                    questions.size() + " question" + (questions.size() == 1 ? "" : "s")
            );

            lobbyDurationLabel.setText(formatDuration(minutes));

            violationSettingMap.clear();
            violationAttemptMap.clear();
            violationMonitoringEnabled = false;

            if (response.getViolationSettings() != null && !response.getViolationSettings().isEmpty()) {
                for (ViolationSettingRequest setting : response.getViolationSettings()) {
                    if (setting != null
                            && setting.getViolationType() != null
                            && Boolean.TRUE.equals(setting.getEnabled())) {

                        violationSettingMap.put(setting.getViolationType(), setting);
                        violationMonitoringEnabled = true;
                    }
                }
            }

            return currentExamId != null && !questions.isEmpty();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void setupTimer() {
        updateTimerLabel();

        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingSeconds--;
            updateTimerLabel();

            if (remainingSeconds <= 0) {
                timerTimeline.stop();
                autoSubmitExam();
            }
        }));

        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
    }

    private void updateTimerLabel() {

        int hours = remainingSeconds / 3600;
        int minutes = (remainingSeconds % 3600) / 60;
        int seconds = remainingSeconds % 60;

        if (remainingSeconds >= 3600) {

            timerLabel.setText(
                    String.format("%02d:%02d:%02d", hours, minutes, seconds)
            );

        } else {

            int totalMinutes = remainingSeconds / 60;

            timerLabel.setText(
                    String.format("%02d:%02d", totalMinutes, seconds)
            );
        }
    }

    private String buildImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        String path = imagePath.trim();

        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("file:")) {
            return path;
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return AppConfig.BASE_URL + path;
    }

    private void renderQuestionNavigation() {
        questionNumberPane.getChildren().clear();

        for (int i = 0; i < questions.size(); i++) {
            int index = i;

            Button button = new Button(String.valueOf(i + 1));
            button.getStyleClass().add("question-nav-button");
            button.setCursor(Cursor.HAND);

            button.setOnAction(event -> {
                saveCurrentAnswer();
                stopQuestionTimer();
                showQuestion(index);
            });

            questionNumberPane.getChildren().add(button);
        }

        refreshQuestionNavigationStyles();
    }

    private void refreshQuestionNavigationStyles() {
        for (int i = 0; i < questionNumberPane.getChildren().size(); i++) {
            Button button = (Button) questionNumberPane.getChildren().get(i);
            ExamTakeQuestion question = questions.get(i);

            button.getStyleClass().removeAll(
                    "question-nav-current",
                    "question-nav-answered",
                    "question-nav-review",
                    "question-nav-unanswered"
            );

            if (i == currentQuestionIndex) {
                button.getStyleClass().add("question-nav-current");
            } else if (question.isMarkedForReview()) {
                button.getStyleClass().add("question-nav-review");
            } else if (question.isAnswered()) {
                button.getStyleClass().add("question-nav-answered");
            } else {
                button.getStyleClass().add("question-nav-unanswered");
            }
        }
    }

    private void showQuestion(int index) {

        if (index < 0 || index >= questions.size()) {
            return;
        }

        currentQuestionIndex = index;

        ExamTakeQuestion question = questions.get(currentQuestionIndex);

        questionNumberLabel.setText("Question " + (currentQuestionIndex + 1));
        questionTypeLabel.setText(formatQuestionType(question.getQuestionType()) + " • " + question.getPoints() + " point(s)");
        questionTextLabel.setText(question.getQuestionText());
        renderQuestionInstructionAndRubric(question, answerContainer);

        String fullUrl = buildImageUrl(question.getQuestionImageUrl());

        if (fullUrl != null) {
            questionImageView.setImage(new Image(fullUrl, true));
            questionImageWrapper.setVisible(true);
            questionImageWrapper.setManaged(true);
        } else {
            questionImageView.setImage(null);
            questionImageWrapper.setVisible(false);
            questionImageWrapper.setManaged(false);
        }

        markReviewButton.setText(question.isMarkedForReview() ? "Unmark Review" : "Mark for Review");

        renderAnswerInput(question);

        previousButton.setDisable(currentQuestionIndex == 0);
        nextButton.setText(currentQuestionIndex == questions.size() - 1 ? "Review" : "Next");

        refreshQuestionNavigationStyles();
        updateProgress();
        startQuestionTimer(question);
    }

    private void saveCurrentAnswer() {
        if (questions.isEmpty() || currentQuestionIndex < 0 || currentQuestionIndex >= questions.size()) {
            return;
        }

        ExamTakeQuestion question = questions.get(currentQuestionIndex);

        Long selectedChoiceId = null;
        String answerText = null;

        if (currentToggleGroup != null) {
            Toggle selectedToggle = currentToggleGroup.getSelectedToggle();

            if (selectedToggle instanceof RadioButton selectedRadio) {
                Object value = selectedRadio.getUserData();

                if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                    selectedChoiceId = Long.valueOf(value.toString());
                    question.setStudentAnswer(value.toString());
                } else {
                    answerText = value == null ? selectedRadio.getText() : value.toString();
                    question.setStudentAnswer(answerText);
                }
            }
        } else {
            answerText = question.getStudentAnswer();
        }

        if (selectedChoiceId == null && (answerText == null || answerText.isBlank())) {
            return;
        }

        Long finalSelectedChoiceId = selectedChoiceId;
        String finalAnswerText = answerText;
        Long finalQuestionId = question.getQuestionId();

        if (Boolean.TRUE.equals(savingQuestionMap.get(finalQuestionId))) {
            return;
        }

        savingQuestionMap.put(finalQuestionId, true);

        Platform.runLater(() -> {
            nextButton.setDisable(true);
            previousButton.setDisable(true);
        });

        new Thread(() -> {
            try {
                examApiService.saveAnswer(
                        currentAttemptId,
                        finalQuestionId,
                        finalSelectedChoiceId,
                        finalAnswerText
                );
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                savingQuestionMap.put(finalQuestionId, false);

                Platform.runLater(() -> {
                    nextButton.setDisable(false);
                    previousButton.setDisable(currentQuestionIndex == 0);
                });
            }
        }, "save-answer-thread").start();
    }

    private void updateProgress() {
        long answeredCount = questions.stream()
                .filter(ExamTakeQuestion::isAnswered)
                .count();

        long markedCount = questions.stream()
                .filter(ExamTakeQuestion::isMarkedForReview)
                .count();

        answeredCountLabel.setText("Answered: " + answeredCount + "/" + questions.size());
        markedCountLabel.setText("Marked: " + markedCount);
    }

    private void showReviewDialog() {
        saveCurrentAnswer();

        if (questions.isEmpty()) {
            showError("No questions found.");
            return;
        }

        long unansweredCount = questions.stream()
                .filter(q -> !q.isAnswered())
                .count();

        long markedCount = questions.stream()
                .filter(ExamTakeQuestion::isMarkedForReview)
                .count();

        examContent.setEffect(new GaussianBlur(12));

        reviewOverlay = new StackPane();
        reviewOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.35);");
        reviewOverlay.setPickOnBounds(true);

        VBox card = new VBox(16);
        card.setAlignment(Pos.TOP_LEFT);
        card.setFillWidth(true);
        card.maxWidthProperty().bind(examRoot.widthProperty().multiply(0.55));
        card.prefWidthProperty().bind(examRoot.widthProperty().multiply(0.48));
        card.maxHeightProperty().bind(examRoot.heightProperty().multiply(0.82));

        card.setStyle("""
                -fx-background-color: rgba(255, 255, 255, 0.96);
                -fx-padding: 24;
                -fx-background-radius: 22;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.30), 30, 0, 0, 8);
                """);

        Label title = new Label("Review Your Answers");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2F1B1B;");

        Label summary = new Label(
                "Answered: " + (questions.size() - unansweredCount) + "/" + questions.size() +
                        "\nUnanswered: " + unansweredCount +
                        "\nMarked for Review: " + markedCount
        );
        summary.setStyle("-fx-font-size: 14px; -fx-text-fill: #444;");

        Label hint = new Label("Click a question number to go back and review it.");
        hint.setStyle("-fx-font-size: 13px; -fx-text-fill: #777;");

        FlowPane questionGrid = new FlowPane();
        questionGrid.setHgap(8);
        questionGrid.setVgap(8);
        questionGrid.prefWrapLengthProperty().bind(card.widthProperty().subtract(48));

        for (int i = 0; i < questions.size(); i++) {
            ExamTakeQuestion question = questions.get(i);
            int index = i;

            Button qButton = new Button(String.valueOf(i + 1));
            qButton.setPrefSize(44, 38);
            qButton.setCursor(Cursor.HAND);

            if (!question.isAnswered()) {
                qButton.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-background-radius: 10;");
            } else if (question.isMarkedForReview()) {
                qButton.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-font-weight: bold; -fx-background-radius: 10;");
            } else {
                qButton.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-font-weight: bold; -fx-background-radius: 10;");
            }

            qButton.setOnAction(e -> {
                closeReviewOverlay();
                showQuestion(index);
            });

            questionGrid.getChildren().add(qButton);
        }

        ScrollPane questionScroll = new ScrollPane(questionGrid);
        questionScroll.setFitToWidth(true);
        questionScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        questionScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        questionScroll.maxHeightProperty().bind(examRoot.heightProperty().multiply(0.35));
        questionScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        Label warning = new Label(
                unansweredCount > 0
                        ? "You still have unanswered questions. You may submit, but unanswered items will be counted as blank."
                        : "All questions have answers. You may now submit your exam."
        );
        warning.setWrapText(true);
        warning.setStyle(unansweredCount > 0
                ? "-fx-background-color: #FEF2F2; -fx-text-fill: #991B1B; -fx-padding: 12; -fx-background-radius: 12;"
                : "-fx-background-color: #F0FDF4; -fx-text-fill: #166534; -fx-padding: 12; -fx-background-radius: 12;"
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button backButton = new Button("Back to Exam");
        backButton.setCursor(Cursor.HAND);
        backButton.setStyle("""
                -fx-background-color: transparent;
                -fx-border-color: #800000;
                -fx-text-fill: #800000;
                -fx-border-radius: 12;
                -fx-background-radius: 12;
                -fx-padding: 10 18;
                -fx-font-weight: bold;
                """);
        backButton.setOnAction(e -> closeReviewOverlay());

        Button submitFinalButton = new Button("Submit Final");
        submitFinalButton.setCursor(Cursor.HAND);
        submitFinalButton.setStyle("""
                -fx-background-color: #800000;
                -fx-text-fill: white;
                -fx-background-radius: 12;
                -fx-padding: 10 20;
                -fx-font-weight: bold;
                """);
        submitFinalButton.setOnAction(e -> {
            LoadingSpinner.setLoading(
                    submitFinalButton,
                    true,
                    "Submitting...",
                    "Submit Final"
            );

            PauseTransition delay = new PauseTransition(Duration.millis(120));
            delay.setOnFinished(event -> {
                closeReviewOverlay();

                internalDialogOpen = true;

                Platform.runLater(this::submitExam);
            });

            delay.play();
        });

        HBox buttons = new HBox(12, backButton, submitFinalButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(title, summary, hint, questionScroll, warning, spacer, buttons);

        reviewOverlay.getChildren().add(card);
        StackPane.setAlignment(card, Pos.CENTER);

        examRoot.getChildren().add(reviewOverlay);
    }

    private void closeReviewOverlay() {
        internalDialogOpen = false;
        examContent.setEffect(null);

        if (reviewOverlay != null) {
            examRoot.getChildren().remove(reviewOverlay);
            reviewOverlay = null;
        }
    }

    private void submitExam() {

        examEnding = true;
        internalDialogOpen = true;
        examStarted = false;
        examClosing = true;

        if (timerTimeline != null) {
            timerTimeline.stop();
        }

        stopQuestionTimer();
        stopEvidenceBuffering();
        stopMediaPipePreviewTracking();
        stopExamDeskMonitoring();
        stopLobbyCameraPreview();
        saveCurrentAnswer();

        try {
            examApiService.submitExam(currentAttemptId);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to submit exam. Please check your connection and try again.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Exam Submitted");
        alert.setHeaderText("Your exam has been submitted successfully.");
        alert.setContentText("Your answers were saved.");

        if (examRoot.getScene() != null && examRoot.getScene().getWindow() != null) {
            alert.initOwner(examRoot.getScene().getWindow());
        }

        alert.setOnHidden(e -> {
            internalDialogOpen = false;
            returnToDashboard();
        });

        alert.show();
    }

    private void autoSubmitExam() {
        examEnding = true;
        internalDialogOpen = true;
        examStarted = false;
        examClosing = true;

        stopQuestionTimer();
        stopEvidenceBuffering();
        stopMediaPipePreviewTracking();
        stopExamDeskMonitoring();
        stopLobbyCameraPreview();
        saveCurrentAnswer();

        try {
            examApiService.submitExam(currentAttemptId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Time Finished");
        alert.setHeaderText("Time is up.");
        alert.setContentText("Your exam has been submitted automatically.");

        if (examRoot.getScene() != null && examRoot.getScene().getWindow() != null) {
            alert.initOwner(examRoot.getScene().getWindow());
        }

        alert.setOnHidden(e -> {
            internalDialogOpen = false;
            returnToDashboard();
        });

        alert.show();
    }

    private void returnToDashboard() {
        examClosing = true;
        stopLobbyScheduleWatcher();
        stopEvidenceBuffering();
        stopExamDeskMonitoring();
        stopMediaPipePreviewTracking();
        stopLobbyCameraPreview();
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/layout/dashboard-shell.fxml")
            );

            Parent root = loader.load();

            DashboardShellController shellController = loader.getController();
            shellController.loadStudentView();

            Stage stage = (Stage) examRoot.getScene().getWindow();

            Scene scene = new Scene(root);

            stage.setFullScreen(false);
            stage.setScene(scene);
            stage.setTitle("ExamGuard - Student Dashboard");

            stage.setMaximized(false);
            stage.setMaximized(true);

            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to return to dashboard.");
        }
    }

    // helpers

    private void recordViolation(
            String violationType,
            String title,
            String message,
            boolean defaultCritical
    ) {
        if (!violationMonitoringEnabled || violationSettingMap.isEmpty()) {
            return;
        }

        if (examEnding || internalDialogOpen || !examStarted) {
            return;
        }

        ViolationSettingRequest setting =
                violationSettingMap.get(violationType);

        if (setting != null && Boolean.FALSE.equals(setting.getEnabled())) {
            return;
        }

        int currentAttempts =
                violationAttemptMap.getOrDefault(violationType, 0) + 1;

        violationAttemptMap.put(violationType, currentAttempts);

        int allowedAttempts =
                setting == null || setting.getMaxAllowedCount() == 0
                        ? 0
                        : setting.getMaxAllowedCount();

        boolean shouldRecord = currentAttempts > allowedAttempts;

        if (!shouldRecord) {
            return;
        }

        violationCount++;

        String severity =
                setting == null || setting.getSeverity() == null || setting.getSeverity().isBlank()
                        ? (defaultCritical ? "MAJOR" : "MINOR")
                        : setting.getSeverity();

        boolean critical =
                "MAJOR".equalsIgnoreCase(severity)
                        || "CRITICAL".equalsIgnoreCase(severity);

        String timestamp =
                OffsetDateTime.now(ZoneId.of("Asia/Manila"))
                        .format(VIOLATION_TIME_FORMAT);


        if (shouldShowViolationOverlay(violationType)) {
            showViolationWarning(
                    buildOverlayTitle(violationType, title),
                    buildOverlayMessage(violationType, message),
                    critical
            );
        }

        sendViolationLogToBackend(
                violationType,
                severity,
                message,
                currentAttempts
        );

    }

    private boolean shouldShowViolationOverlay(String violationType) {
        if (violationType == null) {
            return false;
        }

        String type = violationType.trim().toUpperCase();

        /*
         * Show overlay only for screen/system violations
         * and hard face presence violations.
         *
         * Do not show overlay for looking down/up/side.
         * Those are logged silently for faculty review.
         */
        return switch (type) {
            case "FOCUS_LOST",
                 "FULLSCREEN_EXIT",
                 "WINDOW_MINIMIZED",
                 "RESTRICTED_KEY",
                 "RIGHT_CLICK",
                 "MULTIPLE_MONITORS",
                 "NO_PERSON_DETECTED",
                 "MULTIPLE_PERSONS" -> true;

            default -> false;
        };
    }

    private String buildOverlayTitle(String violationType, String fallbackTitle) {
        if (violationType == null) {
            return fallbackTitle;
        }

        return switch (violationType.trim().toUpperCase()) {
            case "FACE_BEHAVIOR_NO_FACE" -> "No Face Detected";
            case "FACE_BEHAVIOR_MULTIPLE_FACES" -> "Multiple Faces Detected";
            case "FOCUS_LOST" -> "Focus Lost";
            case "FULLSCREEN_EXIT" -> "Fullscreen Exit";
            case "WINDOW_MINIMIZED" -> "Window Minimized";
            case "RESTRICTED_KEY" -> "Restricted Key";
            case "RIGHT_CLICK" -> "Right Click Blocked";
            case "MULTIPLE_MONITORS" -> "Multiple Monitors Detected";
            case "NO_PERSON_DETECTED" -> "No Examinee Detected";
            case "MULTIPLE_PERSONS" -> "Multiple Persons Detected";
            default -> fallbackTitle;
        };
    }

    private String buildOverlayMessage(String violationType, String fallbackMessage) {
        if (violationType == null) {
            return fallbackMessage;
        }

        return switch (violationType.trim().toUpperCase()) {
            case "FACE_BEHAVIOR_NO_FACE" -> "Please keep your face visible.";

            case "FACE_BEHAVIOR_MULTIPLE_FACES", "MULTIPLE_PERSONS" -> "Only the examinee should be visible.";

            case "FOCUS_LOST" -> "Please stay on the exam screen.";

            case "FULLSCREEN_EXIT" -> "Please remain in fullscreen mode.";

            case "WINDOW_MINIMIZED" -> "Please do not minimize the exam window.";

            case "RESTRICTED_KEY" -> "Keyboard shortcuts are restricted during the exam.";

            case "RIGHT_CLICK" -> "Right click is disabled during the exam.";

            case "MULTIPLE_MONITORS" -> "Please use only one display during the exam.";

            case "NO_PERSON_DETECTED" -> "Please keep yourself visible during the exam.";

            default -> fallbackMessage;
        };
    }

    private void showViolationWarning(String title, String message, boolean critical) {
        violationTitleLabel.setText(title);
        violationMessageLabel.setText(message);

        violationOverlay.getStyleClass().removeAll(
                "violation-overlay-warning",
                "violation-overlay-critical"
        );

        violationOverlay.getStyleClass().add(
                critical ? "violation-overlay-critical" : "violation-overlay-warning"
        );

        examContent.setEffect(new GaussianBlur(10));

        violationOverlay.setVisible(true);
        violationOverlay.setManaged(true);

        PauseTransition delay = new PauseTransition(Duration.seconds(2.5));
        delay.setOnFinished(e -> hideViolationWarning());
        delay.play();
    }

    private void hideViolationWarning() {
        violationOverlay.setVisible(false);
        violationOverlay.setManaged(false);
        examContent.setEffect(null);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Exam Error");
        alert.setHeaderText("Something went wrong");
        alert.setContentText(message);

        if (examRoot != null && examRoot.getScene() != null && examRoot.getScene().getWindow() != null) {
            alert.initOwner(examRoot.getScene().getWindow());
        }

        alert.showAndWait();
    }

    private String formatQuestionType(QuestionType type) {
        return switch (type) {
            case MULTIPLE_CHOICE -> "MULTIPLE CHOICE";
            case TRUE_FALSE -> "TRUE/FALSE";
            case IDENTIFICATION -> "IDENTIFICATION";
            case ESSAY -> "ESSAY";
        };
    }

    private void renderAnswerInput(ExamTakeQuestion question) {
        answerContainer.getChildren().clear();
        currentToggleGroup = null;

        renderQuestionInstructionAndRubric(question, answerContainer);

        switch (question.getQuestionType()) {
            case MULTIPLE_CHOICE, TRUE_FALSE -> renderRadioChoices(question);
            case IDENTIFICATION -> renderIdentificationField(question);
            case ESSAY -> renderEssayArea(question);
        }
    }

    private void renderRadioChoices(ExamTakeQuestion question) {
        currentToggleGroup = new ToggleGroup();

        VBox choiceRows = new VBox(18);
        choiceRows.setMaxWidth(Double.MAX_VALUE);
        choiceRows.prefWidthProperty().bind(answerContainer.widthProperty());

        List<ExamTakeChoice> choices = new ArrayList<>();

        if (question.getQuestionType() == QuestionType.TRUE_FALSE) {
            choices.add(createTempChoice("True"));
            choices.add(createTempChoice("False"));
        } else if (question.getChoices() != null) {
            choices.addAll(question.getChoices());
        }

        HBox currentRow = null;

        for (int i = 0; i < choices.size(); i++) {
            if (i % 2 == 0) {
                currentRow = new HBox(18);
                currentRow.setMaxWidth(Double.MAX_VALUE);
                currentRow.prefWidthProperty().bind(choiceRows.widthProperty());
                choiceRows.getChildren().add(currentRow);
            }

            ExamTakeChoice choice = choices.get(i);

            String choiceText = choice.getChoiceText() == null ? "" : choice.getChoiceText().trim();

            boolean hasImage = choice.getChoiceImageUrl() != null
                    && !choice.getChoiceImageUrl().isBlank();

            String displayText = choiceText.isBlank()
                    ? "Choice " + (i + 1)
                    : choiceText;

            Object answerValue;

            if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                answerValue = choice.getChoiceId();
            } else {
                answerValue = choiceText.toUpperCase();
            }

            RadioButton radioButton = new RadioButton(displayText);
            radioButton.setUserData(answerValue);

            radioButton.setToggleGroup(currentToggleGroup);
            radioButton.getStyleClass().add("choice-radio-large");
            radioButton.setWrapText(true);
            radioButton.setMaxWidth(Double.MAX_VALUE);

            VBox choiceBox = new VBox(12);
            choiceBox.getStyleClass().add("choice-card");

            PseudoClass selectedClass = PseudoClass.getPseudoClass("selected");

            radioButton.selectedProperty().addListener((obs, oldVal, selected) -> {
                choiceBox.pseudoClassStateChanged(selectedClass, selected);
            });

            choiceBox.setOnMouseClicked(e -> {
                radioButton.setSelected(true);
                question.setStudentAnswer(String.valueOf(answerValue));
                refreshQuestionNavigationStyles();
                updateProgress();
            });

            if (String.valueOf(answerValue).equals(question.getStudentAnswer())) {
                radioButton.setSelected(true);
            }

            radioButton.setOnAction(event -> {
                question.setStudentAnswer(String.valueOf(answerValue));
                refreshQuestionNavigationStyles();
                updateProgress();
            });

            if (!hasImage) {
                choiceBox.getStyleClass().add("choice-card-text-only");
                choiceBox.setAlignment(Pos.CENTER_LEFT);
            }

            if (hasImage) {
                choiceBox.setMinHeight(240);
                choiceBox.setPrefHeight(240);
                choiceBox.setMaxHeight(240);
            } else {
                choiceBox.setMinHeight(55);
                choiceBox.setPrefHeight(70);
                choiceBox.setMaxHeight(70);
            }

            choiceBox.prefWidthProperty().bind(
                    currentRow.widthProperty().subtract(18).divide(2)
            );

            choiceBox.getChildren().add(radioButton);

            String fullUrl = buildImageUrl(choice.getChoiceImageUrl());

            if (fullUrl != null) {
                try {
                    ImageView choiceImage = new ImageView();
                    choiceImage.setPreserveRatio(true);
                    choiceImage.setSmooth(true);
                    choiceImage.setFitWidth(260);
                    choiceImage.setFitHeight(140);
                    choiceImage.setPickOnBounds(true);
                    choiceImage.getStyleClass().add("choice-image");

                    StackPane imageBox = new StackPane(choiceImage);
                    imageBox.getStyleClass().add("choice-image-box");
                    imageBox.setMinHeight(150);
                    imageBox.setPrefHeight(150);
                    imageBox.setMaxHeight(150);
                    imageBox.setMaxWidth(Double.MAX_VALUE);

                    choiceImage.setImage(new Image(fullUrl, true));
                    choiceBox.getChildren().add(imageBox);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            currentRow.getChildren().add(choiceBox);
        }

        answerContainer.getChildren().add(choiceRows);
        answerContainer.requestLayout();
    }

    private ExamTakeChoice createTempChoice(String text) {
        ExamTakeChoice choice = new ExamTakeChoice();
        choice.setChoiceText(text);
        return choice;
    }

    private void startExamDeskMonitoring() {
        stopExamDeskMonitoring();

        repeatedViolationWindowMap.clear();
        examDeskMonitorRunning = false;

        examDeskMonitorTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> checkExamDeskOnce())
        );

        examDeskMonitorTimeline.setCycleCount(Timeline.INDEFINITE);
        examDeskMonitorTimeline.play();
    }

    private void stopExamDeskMonitoring() {
        if (examDeskMonitorTimeline != null) {
            examDeskMonitorTimeline.stop();
            examDeskMonitorTimeline = null;
        }

        examDeskMonitorRunning = false;
        repeatedViolationWindowMap.clear();
    }

    private void checkExamDeskOnce() {
        if (examDeskMonitorRunning) {
            return;
        }

        if (!examStarted || examEnding || internalDialogOpen) {
            return;
        }

        if (lobbyObjectDetector == null || lobbyCameraPreviewService == null) {
            return;
        }

        Mat frame = lobbyCameraPreviewService.getActiveFrame();

        if (frame == null || frame.empty()) {
            return;
        }

        examDeskMonitorRunning = true;

        try {
            LobbyObjectDetector.DeskBaselineResult result =
                    lobbyObjectDetector.analyzeDeskBaselineStable(frame);

            if (result == null || result.passed()) {
                return;
            }

            String violationType = mapDeskResultToViolationType(result.message());

            if (!shouldLogRepeatedViolation(violationType)) {
                return;
            }

            repeatedViolationWindowMap.remove(violationType);

            recordViolation(
                    violationType,
                    "Camera / Desk Violation",
                    result.message(),
                    true
            );

        } finally {
            examDeskMonitorRunning = false;
        }
    }

    private String mapDeskResultToViolationType(String message) {
        if (message == null) {
            return "DESK_SETUP_CHANGED";
        }

        String text = message.toLowerCase();

        if (text.contains("multiple persons")) {
            return "MULTIPLE_PERSONS";
        }

        if (text.contains("no examinee")) {
            return "NO_PERSON_DETECTED";
        }

        if (text.contains("phone") || text.contains("tablet")) {
            return "PHONE_DETECTED";
        }

        if (text.contains("reviewer") || text.contains("book") || text.contains("paper")) {
            return "REVIEWER_MATERIAL_DETECTED";
        }

        if (text.contains("monitor")
                || text.contains("keyboard")
                || text.contains("laptop")
                || text.contains("device")
                || text.contains("computer setup")) {
            return "DEVICE_SETUP_CHANGED";
        }

        return "DESK_SETUP_CHANGED";
    }

    private void renderIdentificationField(ExamTakeQuestion question) {
        TextField answerField = new TextField();
        answerField.setPromptText("Type your answer here");
        answerField.getStyleClass().add("identification-field");

        if (question.getStudentAnswer() != null) {
            answerField.setText(question.getStudentAnswer());
        }

        answerField.textProperty().addListener((obs, oldValue, newValue) -> {
            question.setStudentAnswer(newValue);
            refreshQuestionNavigationStyles();
            updateProgress();
        });

        answerContainer.getChildren().add(answerField);
    }

    private void renderEssayArea(ExamTakeQuestion question) {
        TextArea essayArea = new TextArea();
        essayArea.setPromptText("Write your answer here");
        essayArea.setWrapText(true);
        essayArea.setPrefRowCount(10);
        essayArea.getStyleClass().add("essay-area");

        if (question.getStudentAnswer() != null) {
            essayArea.setText(question.getStudentAnswer());
        }

        essayArea.textProperty().addListener((obs, oldValue, newValue) -> {
            question.setStudentAnswer(newValue);
            refreshQuestionNavigationStyles();
            updateProgress();
        });

        answerContainer.getChildren().add(essayArea);
    }

    private void sendViolationLogToBackend(
            String violationType,
            String severity,
            String message,
            int attemptNumber
    ) {
        try {
            ExamTakeQuestion currentQuestion = null;

            if (!questions.isEmpty()
                    && currentQuestionIndex >= 0
                    && currentQuestionIndex < questions.size()) {
                currentQuestion = questions.get(currentQuestionIndex);
            }

            Long questionId = currentQuestion == null ? null : currentQuestion.getQuestionId();

            new Thread(() -> {
                try {
                    if (examClosing) {
                        return;
                    }

                    EvidencePayload evidence =
                            captureAndUploadEvidence(violationType, questionId);

                    ViolationLogRequest request = new ViolationLogRequest();
                    request.setAttemptId(currentAttemptId);
                    request.setExamId(currentExamId);
                    request.setQuestionId(questionId);
                    request.setViolationType(violationType);
                    request.setSeverity(severity);
                    request.setViolationMessage(message);
                    request.setAttemptNumber(attemptNumber);
                    request.setOccurredAt(OffsetDateTime.now(ZoneOffset.UTC));

                    if (evidence != null) {
                        request.setEvidenceUrl(evidence.mainUrl);
                        request.setEvidenceType(evidence.evidenceType);
                        request.setEvidenceSource(evidence.evidenceSource);
                        request.setEvidenceMetadata(evidence.metadataJson);
                    }

                    examApiService.logViolation(request);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "log-violation-with-evidence-thread").start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderQuestionInstructionAndRubric(
            ExamTakeQuestion question,
            VBox questionContainer
    ) {
        if (question.getQuestionInstruction() != null &&
                !question.getQuestionInstruction().isBlank()) {

            Label instruction = new Label(question.getQuestionInstruction());
            instruction.setWrapText(true);
            instruction.getStyleClass().add("exam-instruction-box");

            questionContainer.getChildren().add(instruction);
        }

        if (question.getRubrics() != null &&
                !question.getRubrics().isEmpty()) {

            VBox rubricBox = new VBox(6);
            rubricBox.getStyleClass().add("exam-rubric-box");

            Label title = new Label("Grading Rubric");
            title.getStyleClass().add("exam-rubric-title");

            rubricBox.getChildren().add(title);

            for (EssayRubricRequest rubric : question.getRubrics()) {
                Label row = new Label(
                        rubric.getCriterionName()
                                + " - "
                                + rubric.getWeightPercentage()
                                + "%"
                );
                row.getStyleClass().add("exam-rubric-row");
                rubricBox.getChildren().add(row);
            }

            questionContainer.getChildren().add(rubricBox);
        }
    }


    private void startQuestionTimer(ExamTakeQuestion question) {
        if (question == null || question.getQuestionId() == null) {
            return;
        }

        if (question.getQuestionId().equals(activeQuestionId)) {
            return;
        }

        activeQuestionId = question.getQuestionId();
        activeQuestionStartedAt = System.currentTimeMillis();
    }

    private void stopQuestionTimer() {
        if (activeQuestionId == null || activeQuestionStartedAt <= 0) {
            return;
        }

        long durationMs =
                System.currentTimeMillis() - activeQuestionStartedAt;

        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        String durationText =
                minutes > 0
                        ? minutes + "m " + remainingSeconds + "s"
                        : remainingSeconds + "s";

        logExamActivity(
                "QUESTION_DURATION",
                "Stayed for " + durationText,
                activeQuestionId,
                durationMs
        );

        activeQuestionId = null;
        activeQuestionStartedAt = 0L;
    }

    private void logExamActivity(
            String action,
            String message,
            Long questionId,
            Long durationMs
    ) {
        ExamActivityRequest request = new ExamActivityRequest();
        request.setExamId(currentExamId);
        request.setAttemptId(currentAttemptId);
        request.setQuestionId(questionId);
        request.setAction(action);
        request.setMessage(message);
        request.setDurationMs(durationMs);

        new Thread(() -> {
            try {
                examApiService.logActivity(request);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "exam-activity-log-thread").start();
    }

    private void setLobbyStatus(
            Label statusLabel,
            Label detailLabel,
            LobbyCheckStatus status,
            String statusText,
            String detailText
    ) {
        if (statusLabel == null) {
            return;
        }

        Platform.runLater(() -> {
            statusLabel.setText(statusText);

            statusLabel.getStyleClass().removeAll(
                    "lobby-status-pending",
                    "lobby-status-checking",
                    "lobby-status-passed",
                    "lobby-status-failed",
                    "lobby-status-warning"
            );

            switch (status) {
                case CHECKING -> statusLabel.getStyleClass().add("lobby-status-checking");
                case PASSED -> statusLabel.getStyleClass().add("lobby-status-passed");
                case FAILED -> statusLabel.getStyleClass().add("lobby-status-failed");
                case WARNING -> statusLabel.getStyleClass().add("lobby-status-warning");
                default -> statusLabel.getStyleClass().add("lobby-status-pending");
            }

            if (detailLabel != null) {
                boolean hasDetail = detailText != null && !detailText.isBlank();

                detailLabel.setText(hasDetail ? detailText : "");
                detailLabel.setVisible(hasDetail);
                detailLabel.setManaged(hasDetail);
            }
        });
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_3BYTE_BGR;

        BufferedImage image = new BufferedImage(
                mat.width(),
                mat.height(),
                type
        );

        byte[] data =
                ((java.awt.image.DataBufferByte) image.getRaster()
                        .getDataBuffer())
                        .getData();

        mat.get(0, 0, data);

        return image;
    }

    private static class EvidencePayload {
        String mainUrl;
        String evidenceType;
        String evidenceSource;
        String metadataJson;
    }

    private enum EvidenceCaptureMode {
        FACE_CAMERA("CAMERA_CAPTURE", "CAMERA"),
        PHONE_FACE_CAMERA("CAMERA_CAPTURE", "PHONE_CAMERA"),
        SCREEN("SCREENSHOT", "SCREEN");

        final String evidenceType;
        final String evidenceSource;

        EvidenceCaptureMode(String evidenceType, String evidenceSource) {
            this.evidenceType = evidenceType;
            this.evidenceSource = evidenceSource;
        }
    }

    private EvidenceCaptureMode resolveEvidenceCaptureMode(String violationType) {
        /*
         * Face behavior violations should use camera evidence.
         * Screen activity violations should use screen evidence.
         */
        if (violationType == null) {
            return EvidenceCaptureMode.SCREEN;
        }

        String type = violationType.toUpperCase();

        if (type.startsWith("FACE_BEHAVIOR")
                || type.contains("LOOKING")
                || type.contains("MULTIPLE_FACES")
                || type.contains("NO_FACE")) {

            return phoneCameraActive
                    ? EvidenceCaptureMode.PHONE_FACE_CAMERA
                    : EvidenceCaptureMode.FACE_CAMERA;
        }

        return EvidenceCaptureMode.SCREEN;
    }

    private EvidencePayload captureAndUploadEvidence(
            String violationType,
            Long questionId
    ) {
        try {
            boolean screenDuring = shouldUseScreenDuringEvidence(violationType);

            Mat beforeCamera = getBufferedFrameBefore(System.currentTimeMillis() - 2000);

            File beforeFile = matToTempFile(beforeCamera, violationType + "-before-camera");

            File duringFile;

            if (screenDuring) {
                duringFile = captureScreenToTempFile(violationType + "-during-screen");
            } else {
                Mat duringCamera = lobbyCameraPreviewService == null
                        ? null
                        : lobbyCameraPreviewService.getActiveFrame();

                duringFile = matToTempFile(duringCamera, violationType + "-during-camera");
            }

            sleepQuietly(1000);

            Mat afterCamera = lobbyCameraPreviewService == null
                    ? null
                    : lobbyCameraPreviewService.getActiveFrame();

            File afterFile = matToTempFile(afterCamera, violationType + "-after-camera");

            String beforeUrl = uploadEvidenceFile(beforeFile);
            String duringUrl = uploadEvidenceFile(duringFile);
            String afterUrl = uploadEvidenceFile(afterFile);

            deleteQuietly(beforeFile);
            deleteQuietly(duringFile);
            deleteQuietly(afterFile);

            releaseQuietly(beforeCamera);
            releaseQuietly(afterCamera);

            if (beforeUrl == null && duringUrl == null && afterUrl == null) {
                return null;
            }

            EvidencePayload payload = new EvidencePayload();

            payload.mainUrl =
                    duringUrl != null
                            ? duringUrl
                            : beforeUrl != null
                              ? beforeUrl
                              : afterUrl;

            payload.evidenceType = screenDuring
                    ? "MIXED_SCREEN_CAMERA_SEQUENCE"
                    : "CAMERA_SEQUENCE";

            payload.evidenceSource = screenDuring
                    ? "SCREEN_AND_CAMERA"
                    : (phoneCameraActive ? "PHONE_CAMERA" : "CAMERA");

            payload.metadataJson = "{"
                    + "\"violationType\":\"" + escapeJson(violationType) + "\","
                    + "\"questionId\":" + (questionId == null ? "null" : questionId) + ","
                    + "\"evidenceMode\":\"" + (screenDuring ? "SYSTEM_SCREEN_DURING" : "CAMERA_DURING") + "\","
                    + "\"frames\":["
                    + frameJson("before", beforeUrl, -2000) + ","
                    + frameJson(screenDuring ? "during" : "during", duringUrl, 0) + ","
                    + frameJson("after", afterUrl, 1000)
                    + "],"
                    + "\"capturedAt\":\"" + OffsetDateTime.now(ZoneOffset.UTC) + "\""
                    + "}";

            return payload;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean shouldUseScreenDuringEvidence(String violationType) {
        if (violationType == null) {
            return false;
        }

        return switch (violationType.trim().toUpperCase()) {
            case "FOCUS_LOST",
                 "FULLSCREEN_EXIT",
                 "WINDOW_MINIMIZED",
                 "RESTRICTED_KEY",
                 "RIGHT_CLICK",
                 "MULTIPLE_MONITORS" -> true;

            default -> false;
        };
    }

    private File matToTempFile(Mat frame, String label) {
        if (frame == null || frame.empty()) {
            return null;
        }

        try {
            File file = File.createTempFile("examguard-" + safeFileLabel(label) + "-", ".jpg");
            Imgcodecs.imwrite(file.getAbsolutePath(), frame);
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private File captureScreenToTempFile(String label) {

        try {

            if (examClosing) {
                return null;
            }

            if (examRoot == null) {
                return null;
            }

            if (examRoot.getScene() == null || examRoot.getScene().getWindow() == null) {
                return null;
            }

            if (!examRoot.getScene().getWindow().isShowing()) {
                return null;
            }

            if (Platform.isFxApplicationThread()) {
                return doScreenSnapshot(label);
            }

            CompletableFuture<File> future = new CompletableFuture<>();

            Platform.runLater(() -> {

                try {

                    if (examClosing || examRoot == null) {
                        future.complete(null);
                        return;
                    }

                    if (examRoot.getScene() == null || examRoot.getScene().getWindow() == null) {
                        future.complete(null);
                        return;
                    }

                    if (!examRoot.getScene().getWindow().isShowing()) {
                        future.complete(null);
                        return;
                    }

                    File file = doScreenSnapshot(label);
                    future.complete(file);

                } catch (Exception e) {

                    future.completeExceptionally(e);
                }
            });

            return future.get(5, TimeUnit.SECONDS);

        } catch (Exception e) {

            e.printStackTrace();
            return null;
        }
    }

    private File doScreenSnapshot(String label) throws Exception {

        WritableImage snapshot = examRoot.snapshot(
                new SnapshotParameters(),
                null
        );

        BufferedImage bufferedImage =
                SwingFXUtils.fromFXImage(snapshot, null);

        File file = File.createTempFile(
                "examguard-" + safeFileLabel(label) + "-",
                ".jpg"
        );

        ImageIO.write(bufferedImage, "jpg", file);

        return file;
    }

    private String uploadEvidenceFile(File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        try {
            ImageUploadResponse response = examApiService.uploadViolationEvidence(file);

            if (response == null || !response.isSuccess()) {
                return null;
            }

            return response.getImageUrl();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void deleteQuietly(File file) {
        if (file != null && file.exists()) {
            try {
                file.delete();
            } catch (Exception ignored) {
            }
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String safeFileLabel(String value) {
        if (value == null || value.isBlank()) {
            return "evidence";
        }

        return value.replaceAll("[^a-zA-Z0-9-_]", "-");
    }

    private File captureEvidenceFrameToFile(
            String violationType,
            EvidenceCaptureMode captureMode
    ) {
        try {
            File file = File.createTempFile(
                    "examguard-" + violationType.toLowerCase() + "-",
                    ".jpg"
            );

            if (captureMode == EvidenceCaptureMode.FACE_CAMERA
                    || captureMode == EvidenceCaptureMode.PHONE_FACE_CAMERA) {

                /*
                 * Face behavior evidence:
                 * Use the latest camera/phone frame.
                 */
                Mat frame = null;

                if (lobbyCameraPreviewService != null) {
                    frame = lobbyCameraPreviewService.getActiveFrame();
                }

                if (frame != null && !frame.empty()) {
                    Imgcodecs.imwrite(file.getAbsolutePath(), frame);
                    frame.release();
                    return file;
                }
            }

            /*
             * Screen activity evidence:
             * Use JavaFX screenshot of the exam window.
             */
            return captureExamScreenSnapshot(file);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private File captureExamScreenSnapshot(File file) {
        try {
            java.util.concurrent.CountDownLatch latch =
                    new java.util.concurrent.CountDownLatch(1);

            final Exception[] snapshotError = new Exception[1];

            Platform.runLater(() -> {
                try {
                    WritableImage snapshot =
                            examRoot.snapshot(null, null);

                    BufferedImage bufferedImage =
                            SwingFXUtils.fromFXImage(snapshot, null);

                    BufferedImage rgbImage = new BufferedImage(
                            bufferedImage.getWidth(),
                            bufferedImage.getHeight(),
                            BufferedImage.TYPE_INT_RGB
                    );

                    java.awt.Graphics2D graphics = rgbImage.createGraphics();
                    graphics.setColor(java.awt.Color.WHITE);
                    graphics.fillRect(0, 0, rgbImage.getWidth(), rgbImage.getHeight());
                    graphics.drawImage(bufferedImage, 0, 0, null);
                    graphics.dispose();

                    javax.imageio.ImageIO.write(
                            rgbImage,
                            "jpg",
                            file
                    );

                } catch (Exception e) {
                    snapshotError[0] = e;
                } finally {
                    latch.countDown();
                }
            });

            latch.await();

            if (snapshotError[0] != null) {
                throw snapshotError[0];
            }

            if (!file.exists() || file.length() == 0) {
                return null;
            }

            return file;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private OffsetDateTime nowManila() {
        return OffsetDateTime.now(MANILA_ZONE);
    }

    private boolean canEnterLobbyNow() {
        if (lobbyStartDateTime == null || lobbyEndDateTime == null) {
            return true;
        }

        OffsetDateTime now = nowManila();
        OffsetDateTime start = lobbyStartDateTime.atZoneSameInstant(MANILA_ZONE).toOffsetDateTime();
        OffsetDateTime end = lobbyEndDateTime.atZoneSameInstant(MANILA_ZONE).toOffsetDateTime();

        return !now.isBefore(start.minusMinutes(LOBBY_OPEN_MINUTES_BEFORE_START)) && !now.isAfter(end);
    }

    private boolean canBeginExamNow() {
        if (lobbyStartDateTime == null || lobbyEndDateTime == null) {
            return false;
        }

        OffsetDateTime now = nowManila();
        OffsetDateTime start = lobbyStartDateTime.atZoneSameInstant(MANILA_ZONE).toOffsetDateTime();
        OffsetDateTime end = lobbyEndDateTime.atZoneSameInstant(MANILA_ZONE).toOffsetDateTime();

        return !now.isBefore(start) && !now.isAfter(end);
    }

    private void startLobbyScheduleWatcher() {
        stopLobbyScheduleWatcher();

        lobbyScheduleTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> updateBeginExamButton())
        );

        lobbyScheduleTimeline.setCycleCount(Timeline.INDEFINITE);
        lobbyScheduleTimeline.play();

        updateBeginExamButton();
    }

    private void stopLobbyScheduleWatcher() {
        if (lobbyScheduleTimeline != null) {
            lobbyScheduleTimeline.stop();
            lobbyScheduleTimeline = null;
        }
    }

    private void resetFaceBehaviorTracking() {
        currentFaceBehaviorDirection = null;
        faceBehaviorStartedAt = 0L;
    }

    private boolean shouldLogRepeatedViolation(String violationType) {
        long now = System.currentTimeMillis();

        List<Long> hits = repeatedViolationWindowMap
                .computeIfAbsent(violationType, key -> new ArrayList<>());

        hits.add(now);

        hits.removeIf(time -> now - time > REPEATED_VIOLATION_WINDOW_MS);

        return hits.size() >= REQUIRED_REPEATED_VIOLATION_COUNT;
    }

    private FaceBehaviorAnalyzer.Decision detectFaceBehavior(MediaPipeFaceResult result) {
        /*
         * One shared analyzer is used for both lobby preview and real exam monitoring.
         * This keeps testing and actual violation tagging consistent.
         */
        return FaceBehaviorAnalyzer.analyze(result);
    }

}