package com.example.examguard.controller.student;

import com.example.examguard.controller.layout.DashboardShellController;
import com.example.examguard.model.exam.request.EssayRubricRequest;
import com.example.examguard.model.exam.request.ViolationLogRequest;
import com.example.examguard.model.exam.request.ViolationSettingRequest;
import com.example.examguard.model.exam.take.ExamTakeChoice;
import com.example.examguard.model.exam.take.ExamTakeQuestion;
import com.example.examguard.model.enums.QuestionType;
import com.example.examguard.model.exam.take.ExamTakingResponse;
import com.example.examguard.model.core.response.BrandingResponse;
import com.example.examguard.model.exam.request.ExamActivityRequest;
import com.example.examguard.utility.LobbyCameraVerifier;
import com.example.examguard.utility.LobbyCheckResult;
import com.example.examguard.service.BrandingService;
import com.example.examguard.service.ExamApiService;
import com.example.examguard.utility.LoadingSpinner;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.css.PseudoClass;
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
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.concurrent.Task;

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
    @FXML private Label cameraStatusLabel;
    @FXML private Label faceStatusLabel;
    @FXML private Label deskStatusLabel;
    @FXML private Label internetStatusLabel;
    @FXML private Label systemStatusLabel;
    @FXML private Label cameraDetailLabel;
    @FXML private Label faceDetailLabel;
    @FXML private Label deskDetailLabel;
    @FXML private Label internetDetailLabel;
    @FXML private Label systemDetailLabel;

    @FXML private VBox lobbyAgreementStep;
    @FXML private VBox lobbySystemCheckStep;
    @FXML private Button continueToChecksButton;
    @FXML private Button backToAgreementButton;
    @FXML private CheckBox agreementCheckBox;
    @FXML private Button beginExamButton;
    @FXML private Button cancelLobbyButton;

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
    @FXML private Button runChecksButton;
    @FXML private Button requestCameraPermissionButton;
    @FXML private Label cameraPermissionStatusLabel;
    @FXML private Button submitButton;

    @FXML private StackPane examRoot;
    @FXML private BorderPane examContent;
    @FXML private StackPane violationOverlay;
    @FXML private Label violationTitleLabel;
    @FXML private Label violationMessageLabel;

    private Long currentAttemptId;
    private Long currentExamId;
    private Long lobbyExamId;
    private int lobbyTimeLimitMinutes;
    private boolean examLoading = false;


    private final List<ExamTakeQuestion> questions = new ArrayList<>();
    private final Map<String, ViolationSettingRequest> violationSettingMap = new HashMap<>();
    private final Map<String, Integer> violationAttemptMap = new HashMap<>();
    private final ExamApiService examApiService = new ExamApiService();
    private final BrandingService brandingService = new BrandingService();
    private final LobbyCameraVerifier lobbyCameraVerifier = new LobbyCameraVerifier();

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
    private boolean cameraPassed = false;
    private boolean facePassed = false;
    private boolean deskPassed = false;
    private boolean internetPassed = false;
    private boolean systemPassed = false;
    private boolean lobbyChecksRunning = false;
    private boolean lobbyChecksCompletedOnce = false;
    private enum LobbyCheckStatus {
        PENDING,
        CHECKING,
        PASSED,
        FAILED,
        WARNING
    }
    private final Map<Long, Boolean> savingQuestionMap = new HashMap<>();

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
        resetLobbyChecks();
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
                    : ExamApiService.BASE_URL + (logoUrl.startsWith("/") ? logoUrl : "/" + logoUrl);

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

        runChecksButton.setVisible(false);
        runChecksButton.setManaged(false);

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
    }

    private void showSystemCheckStep() {
        runChecksButton.setVisible(true);
        runChecksButton.setManaged(true);
        runChecksButton.setDisable(false);

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
    private void handleRequestCameraPermission() {
        requestCameraPermissionButton.setDisable(true);
        cameraPermissionStatusLabel.setText("Requesting camera access...");

        Task<LobbyCheckResult> task = new Task<>() {
            @Override
            protected LobbyCheckResult call() {
                return lobbyCameraVerifier.checkCamera();
            }
        };

        task.setOnSucceeded(event -> {
            LobbyCheckResult result = task.getValue();

            if (result.isPassed()) {
                cameraPermissionStatusLabel.setText("Camera access granted.");
            } else {
                cameraPermissionStatusLabel.setText(result.getMessage());
                requestCameraPermissionButton.setDisable(false);
            }
        });

        task.setOnFailed(event -> {
            cameraPermissionStatusLabel.setText("Camera permission check failed.");
            requestCameraPermissionButton.setDisable(false);
        });

        Thread thread = new Thread(task, "camera-permission-check-thread");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleRunSystemCheck() {
        if (!agreementCheckBox.isSelected()) {
            showError("Please read and accept the exam rules and data privacy agreement before continuing.");
            return;
        }

        showSystemCheckStep();
        handleRunLobbyChecks();
    }

    @FXML
    private void handleContinueToSystemChecks() {
        if (!agreementCheckBox.isSelected()) {
            showError("Please read and accept the exam rules and data privacy agreement before continuing.");
            return;
        }

        showSystemCheckStep();
        resetLobbyChecks();
    }

    @FXML
    private void handleBackToAgreement() {
        showAgreementStep();
        resetLobbyChecks();
    }

    @FXML
    private void handleRunLobbyChecks() {
        if (lobbyChecksRunning || examLoading) {
            return;
        }

        lobbyChecksRunning = true;

        backToAgreementButton.setDisable(true);
        runChecksButton.setDisable(true);
        beginExamButton.setDisable(true);

        cameraPassed = false;
        facePassed = false;
        deskPassed = false;
        internetPassed = false;
        systemPassed = false;

        setLobbyStatus(cameraStatusLabel, cameraDetailLabel, LobbyCheckStatus.PENDING, "Pending", "");
        setLobbyStatus(faceStatusLabel, faceDetailLabel, LobbyCheckStatus.PENDING, "Pending", "");
        setLobbyStatus(deskStatusLabel, deskDetailLabel, LobbyCheckStatus.PENDING, "Pending", "");
        setLobbyStatus(internetStatusLabel, internetDetailLabel, LobbyCheckStatus.PENDING, "Pending", "");
        setLobbyStatus(systemStatusLabel, systemDetailLabel, LobbyCheckStatus.PENDING, "Pending", "");

        updateBeginExamButton();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                runCameraCheck();
                runFaceCheck();
                runDeskCheck();
                runInternetCheck();
                runSystemCheck();
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            lobbyChecksRunning = false;
            lobbyChecksCompletedOnce = true;

            runChecksButton.setText("Recheck All");
            runChecksButton.setDisable(false);
            backToAgreementButton.setDisable(false);

            updateBeginExamButton();
        });

        task.setOnFailed(event -> {
            lobbyChecksRunning = false;
            lobbyChecksCompletedOnce = true;

            runChecksButton.setText("Recheck All");
            runChecksButton.setDisable(false);
            backToAgreementButton.setDisable(false);

            updateBeginExamButton();

            if (task.getException() != null) {
                task.getException().printStackTrace();
            }
        });

        Thread thread = new Thread(task, "lobby-system-check-thread");
        thread.setDaemon(true);
        thread.start();
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

        continueToChecksButton.setDisable(true);
        backToAgreementButton.setDisable(true);
        cancelLobbyButton.setDisable(true);

        startExam(lobbyExamId, lobbyTimeLimitMinutes);
    }

    @FXML
    private void handleCancelLobby() {
        returnToDashboard();
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

        if (Screen.getScreens().size() > 1 && examStarted && !internalDialogOpen && !examEnding) {
            recordViolation(
                    "MULTIPLE_MONITORS",
                    "Multiple Monitors Detected",
                    "Please use only one display during the exam.",
                    true
            );
        }
    }


    public void setDashboardStage(Stage dashboardStage) {
        this.dashboardStage = dashboardStage;
    }

    public void setExamLobby(
            Long examId,
            String examTitle,
            int timeLimitMinutes,
            Long questionCount
    ) {
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
        this.remainingSeconds = timeLimitMinutes * 60;

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
        resetLobbyChecks();

        updateBeginExamButton();
    }

    public void startExam(Long examId, int timeLimitMinutes) {
        try {
            this.remainingSeconds = timeLimitMinutes * 60;

            boolean loaded = loadExamFromBackend(examId);

            if (!loaded || questions.isEmpty()) {
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

    private void resetLobbyChecks() {
        cameraPassed = false;
        facePassed = false;
        deskPassed = false;
        internetPassed = false;
        systemPassed = false;
        lobbyChecksRunning = false;
        lobbyChecksCompletedOnce = false;

        if (runChecksButton != null) {
            runChecksButton.setText("Run System Checks");
            runChecksButton.setDisable(false);
        }

        setLobbyStatus(cameraStatusLabel, cameraDetailLabel, LobbyCheckStatus.PENDING, "Pending", "");
        setLobbyStatus(faceStatusLabel, faceDetailLabel, LobbyCheckStatus.PENDING, "Pending", "");
        setLobbyStatus(deskStatusLabel, deskDetailLabel, LobbyCheckStatus.PENDING, "Pending", "");
        setLobbyStatus(internetStatusLabel, internetDetailLabel, LobbyCheckStatus.PENDING, "Pending", "");
        setLobbyStatus(systemStatusLabel, systemDetailLabel, LobbyCheckStatus.PENDING, "Pending", "");

        updateBeginExamButton();
    }

    private void runCameraCheck() {
        setLobbyStatus(cameraStatusLabel, cameraDetailLabel, LobbyCheckStatus.CHECKING, "Checking", "Opening camera...");

        LobbyCheckResult result = lobbyCameraVerifier.checkCamera();

        cameraPassed = result.isPassed();

        setLobbyStatus(
                cameraStatusLabel,
                cameraDetailLabel,
                cameraPassed ? LobbyCheckStatus.PASSED : LobbyCheckStatus.FAILED,
                cameraPassed ? "Passed" : "Failed",
                result.getMessage()
        );
    }

    private void runFaceCheck() {
        setLobbyStatus(faceStatusLabel, faceDetailLabel, LobbyCheckStatus.CHECKING, "Checking", "Detecting face...");

        LobbyCheckResult result = lobbyCameraVerifier.checkFace();

        facePassed = result.isPassed();

        setLobbyStatus(
                faceStatusLabel,
                faceDetailLabel,
                facePassed ? LobbyCheckStatus.PASSED : LobbyCheckStatus.FAILED,
                facePassed ? "Passed" : "Failed",
                result.getMessage()
        );
    }

    private void runDeskCheck() {
        setLobbyStatus(deskStatusLabel, deskDetailLabel, LobbyCheckStatus.CHECKING, "Checking", "Checking camera view quality...");

        LobbyCheckResult result = lobbyCameraVerifier.checkDeskAndFrameQuality();

        deskPassed = result.isPassed();

        setLobbyStatus(
                deskStatusLabel,
                deskDetailLabel,
                deskPassed ? LobbyCheckStatus.PASSED : LobbyCheckStatus.FAILED,
                deskPassed ? "Passed" : "Failed",
                result.getMessage()
        );
    }

    private void runInternetCheck() {
        setLobbyStatus(
                internetStatusLabel,
                internetDetailLabel,
                LobbyCheckStatus.CHECKING,
                "Checking",
                "Connecting to the ExamGuard server..."
        );

        try {
            long latencyMs = examApiService.checkBackendLatency();

            if (latencyMs <= 1000) {
                internetPassed = true;
                setLobbyStatus(
                        internetStatusLabel,
                        internetDetailLabel,
                        LobbyCheckStatus.PASSED,
                        "Passed",
                        "Server connection is stable. Latency: " + latencyMs + " ms."
                );
            } else if (latencyMs <= 2500) {
                internetPassed = true;
                setLobbyStatus(
                        internetStatusLabel,
                        internetDetailLabel,
                        LobbyCheckStatus.WARNING,
                        "Warning",
                        "Connection is slow but usable. Latency: " + latencyMs + " ms."
                );
            } else {
                internetPassed = false;
                setLobbyStatus(
                        internetStatusLabel,
                        internetDetailLabel,
                        LobbyCheckStatus.FAILED,
                        "Failed",
                        "Connection is too slow. Latency: " + latencyMs + " ms."
                );
            }

        } catch (Exception e) {
            internetPassed = false;
            setLobbyStatus(
                    internetStatusLabel,
                    internetDetailLabel,
                    LobbyCheckStatus.FAILED,
                    "Failed",
                    "This device cannot reach the ExamGuard server."
            );
        }
    }

    private void runSystemCheck() {
        setLobbyStatus(systemStatusLabel, systemDetailLabel, LobbyCheckStatus.CHECKING, "Checking","Checking system...");

        try {
            Thread.sleep(400);

            int screenCount = Screen.getScreens().size();

            if (screenCount > 1) {
                systemPassed = false;
                setLobbyStatus(systemStatusLabel, systemDetailLabel, LobbyCheckStatus.FAILED, "Failed", "Multiple monitors detected.");

                return;
            }

            systemPassed = true;
            setLobbyStatus(systemStatusLabel, systemDetailLabel, LobbyCheckStatus.PASSED, "Passed", "System Ready.");
        } catch (Exception e) {
            systemPassed = false;
            setLobbyStatus(systemStatusLabel, systemDetailLabel, LobbyCheckStatus.FAILED, "Failed", "System check failed.");
        }
    }

    private void updateBeginExamButton() {
        boolean ready =
                agreementCheckBox.isSelected()
                        && cameraPassed
                        && facePassed
                        && deskPassed
                        && internetPassed
                        && systemPassed;

        beginExamButton.setDisable(!ready || examLoading || lobbyChecksRunning);
    }

    private boolean loadExamFromBackend(Long examId) {
        try {
            ExamTakingResponse response = examApiService.getExamForTaking(examId);

            if (response == null) {
                showError("Unable to load exam.");
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

            this.remainingSeconds = minutes * 60;

            questions.clear();

            if (response.getQuestions() != null) {
                questions.addAll(response.getQuestions());
            }

            lobbyQuestionCountLabel.setText(questions.size() + " question" + (questions.size() == 1 ? "" : "s"));
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

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load exam content.");
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

        return ExamApiService.BASE_URL + path;
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

        if (timerTimeline != null) {
            timerTimeline.stop();
        }

        stopQuestionTimer();
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

        stopQuestionTimer();
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
            System.out.println(
                    "[VIOLATION ALLOWED] " +
                            violationType +
                            " | Attempt: " + currentAttempts +
                            " / Allowed: " + allowedAttempts
            );
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

        System.out.println(
                "[VIOLATION RECORDED] " +
                        violationType +
                        " | Severity: " + severity +
                        " | Attempts: " + currentAttempts +
                        " | Allowed: " + allowedAttempts +
                        " | Total Violations: " + violationCount +
                        " | Time: " + timestamp
        );

        showViolationWarning(title, message, critical);

        sendViolationLogToBackend(
                violationType,
                severity,
                message,
                currentAttempts
        );

        if ("CRITICAL".equalsIgnoreCase(severity)) {
            System.out.println("[CRITICAL VIOLATION] " + violationType);
        }
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

            ViolationLogRequest request = new ViolationLogRequest();
            request.setAttemptId(currentAttemptId);
            request.setExamId(currentExamId);
            request.setQuestionId( currentQuestion == null ? null : currentQuestion.getQuestionId() );
            request.setViolationType(violationType);
            request.setSeverity(severity);
            request.setViolationMessage(message);
            request.setAttemptNumber(attemptNumber);
            request.setOccurredAt(OffsetDateTime.now(ZoneOffset.UTC));

            new Thread(() -> {
                try {
                    examApiService.logViolation(request);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "log-violation-thread").start();

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


}