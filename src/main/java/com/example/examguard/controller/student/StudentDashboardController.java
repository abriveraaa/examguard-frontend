package com.example.examguard.controller.student;

import com.example.examguard.cache.LocalCacheService;
import com.example.examguard.cache.LocalImageCacheService;
import com.example.examguard.config.AppConfig;
import com.example.examguard.model.profile.ProfileResponseDTO;
import com.example.examguard.cache.StudentLocalCacheKeys;
import com.example.examguard.utility.Session;
import com.example.examguard.service.AuthApiService;
import javafx.application.Platform;
import com.example.examguard.controller.layout.DashboardShellController;
import com.example.examguard.controller.layout.ShellAwareController;
import com.example.examguard.model.student.StudentDashboardResponse;
import com.example.examguard.model.student.dashboard.*;
import com.example.examguard.service.StudentApiService;
import com.example.examguard.utility.LoadingSpinner;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.shape.Rectangle;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StudentDashboardController implements ShellAwareController {

    private DashboardShellController shellController;
    private final AuthApiService authApiService = new AuthApiService();
    private final LocalCacheService localCacheService = new LocalCacheService();
    private final LocalImageCacheService localImageCacheService = new LocalImageCacheService();
    private boolean dashboardCacheShown = false;

    @FXML private ImageView studentAvatarImageView;
    @FXML private Label studentInitialsLabel;
    @FXML private Label studentNameLabel;
    @FXML private Label studentEmailLabel;
    @FXML private Label studentIDLabel;
    @FXML private Label studentProgramLabel;
    @FXML private Label studentCollegeLabel;
    @FXML private Label studentCurrentTermLabel;
    @FXML private Label viewAllUpcomingExamsLabel;
    @FXML private Label viewAllResultsReleasedLabel;
    @FXML private VBox upcomingExamList;
    @FXML private VBox resultSummaryList;

    private final StudentApiService dashboardApiService = new StudentApiService();

    private static final ZoneId MANILA_ZONE = ZoneId.of("Asia/Manila");
    private static final int LOBBY_OPEN_MINUTES_BEFORE_START = 15;

    private OffsetDateTime nowManila() {
        return ZonedDateTime.now(MANILA_ZONE).toOffsetDateTime();
    }

    private OffsetDateTime toManila(OffsetDateTime value) {
        if (value == null) {
            return null;
        }

        return value.atZoneSameInstant(MANILA_ZONE).toOffsetDateTime();
    }

    @Override
    public void setShellController(DashboardShellController shellController) {
        this.shellController = shellController;
    }

    @FXML
    public void initialize() {
        setupAvatarClip();
        setupViewAllRedirects();

        dashboardCacheShown = loadCachedDashboardPartsFirst();

        if (!dashboardCacheShown) {
            showLoadingState();
        }

        loadDashboardAsync();
    }

    private boolean loadCachedDashboardPartsFirst() {
        String schoolId = Session.getSchoolId();

        if (schoolId == null || schoolId.isBlank()) {
            return false;
        }

        StudentProfile cachedDashboardProfile =
                localCacheService.loadData(
                        StudentLocalCacheKeys.dashboardProfile(schoolId),
                        StudentProfile.class
                );

        ProfileResponseDTO cachedProfile =
                localCacheService.loadData(
                        StudentLocalCacheKeys.profile(schoolId),
                        ProfileResponseDTO.class
                );

        List<StudentUpcomingExam> cachedUpcoming =
                localCacheService.loadList(
                        StudentLocalCacheKeys.dashboardUpcoming(schoolId),
                        StudentUpcomingExam.class
                );

        List<StudentResultSummary> cachedResults =
                localCacheService.loadList(
                        StudentLocalCacheKeys.dashboardResults(schoolId),
                        StudentResultSummary.class
                );

        StudentDashboardStats cachedStats =
                localCacheService.loadData(
                        StudentLocalCacheKeys.dashboardStats(schoolId),
                        StudentDashboardStats.class
                );

        if (cachedDashboardProfile != null) {
            loadProfile(cachedDashboardProfile);
        }

        if (cachedProfile != null && cachedProfile.getProfileImageUrl() != null) {
            loadAvatarImage(cachedProfile.getProfileImageUrl());
        }

        if (cachedUpcoming != null) {
            loadUpcomingExams(cachedUpcoming);
        }

        if (cachedResults != null) {
            loadResultSummary(cachedResults, cachedStats);
        }

        return cachedProfile != null
                || cachedUpcoming != null
                || cachedResults != null
                || cachedStats != null;
    }

    private void setupAvatarClip() {
        studentAvatarImageView.setFitWidth(120);
        studentAvatarImageView.setFitHeight(120);
        studentAvatarImageView.setPreserveRatio(false);
        studentAvatarImageView.setSmooth(true);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(5);
        clip.setArcHeight(5);

        clip.widthProperty().bind(studentAvatarImageView.fitWidthProperty());
        clip.heightProperty().bind(studentAvatarImageView.fitHeightProperty());

        studentAvatarImageView.setClip(clip);
    }

    @FXML
    private void handleProfile() {

        if (shellController == null) {
            return;
        }

        shellController.setGreeting("Profile Settings", "Manage your account details and password.");

        shellController.hideHeroCards();

        shellController.loadContent("/fxml/common/profile-view.fxml"
        );
    }

    @FXML
    private void handleReloadUpcomingExams() {
        upcomingExamList.getChildren().setAll(createEmptyRow("Reloading upcoming exams..."));
        reloadDashboardPart("upcoming");
    }

    @FXML
    private void handleReloadResults() {
        resultSummaryList.getChildren().setAll(createEmptyRow("Reloading results..."));
        reloadDashboardPart("results");
    }

    private void reloadDashboardPart(String part) {
        Task<StudentDashboardResponse> task = new Task<>() {
            @Override
            protected StudentDashboardResponse call() throws Exception {
                return dashboardApiService.fetchDashboard();
            }
        };

        task.setOnSucceeded(event -> {
            String schoolId = Session.getSchoolId();

            if (schoolId == null || schoolId.isBlank()) {
                return;
            }

            String version = String.valueOf(System.currentTimeMillis());

            StudentDashboardResponse dashboard = task.getValue();

            switch (part) {
                case "profile" -> {
                    loadProfile(dashboard.getProfile());
                }

                case "upcoming" -> {
                    loadUpcomingExams(dashboard.getUpcomingExams());

                    localCacheService.save(
                            StudentLocalCacheKeys.dashboardUpcoming(schoolId),
                            version,
                            dashboard.getUpcomingExams()
                    );
                }

                case "results" -> {
                    loadResultSummary(
                            dashboard.getResultSummary(),
                            dashboard.getStats()
                    );

                    localCacheService.save(
                            StudentLocalCacheKeys.dashboardResults(schoolId),
                            version,
                            dashboard.getResultSummary()
                    );

                    localCacheService.save(
                            StudentLocalCacheKeys.dashboardStats(schoolId),
                            version,
                            dashboard.getStats()
                    );
                }
            }
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();

            switch (part) {
                case "upcoming" ->
                        upcomingExamList.getChildren().setAll(createEmptyRow("Unable to reload upcoming exams."));
                case "results" ->
                        resultSummaryList.getChildren().setAll(createEmptyRow("Unable to reload results."));
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void loadDashboardAsync() {
        Task<StudentDashboardResponse> task = new Task<>() {
            @Override
            protected StudentDashboardResponse call() throws Exception {
                return dashboardApiService.fetchDashboard();
            }
        };

        task.setOnSucceeded(event -> {
            StudentDashboardResponse dashboard = task.getValue();

            loadProfile(dashboard.getProfile());
            loadUpcomingExams(dashboard.getUpcomingExams());
            loadResultSummary(dashboard.getResultSummary(), dashboard.getStats());

            String schoolId = Session.getSchoolId();

            if (schoolId != null && !schoolId.isBlank()) {
                String version = String.valueOf(System.currentTimeMillis());

                localCacheService.save(
                        StudentLocalCacheKeys.dashboardProfile(schoolId),
                        version,
                        dashboard.getProfile()
                );

                localCacheService.save(
                        StudentLocalCacheKeys.dashboardUpcoming(schoolId),
                        version,
                        dashboard.getUpcomingExams()
                );

                localCacheService.save(
                        StudentLocalCacheKeys.dashboardResults(schoolId),
                        version,
                        dashboard.getResultSummary()
                );

                localCacheService.save(
                        StudentLocalCacheKeys.dashboardStats(schoolId),
                        version,
                        dashboard.getStats()
                );
            }

            hideLoadingState();
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            hideLoadingState();

            if (!dashboardCacheShown) {
                upcomingExamList.getChildren().setAll(createEmptyRow("Unable to load upcoming exams."));
                resultSummaryList.getChildren().setAll(createEmptyRow("Unable to load result summary."));
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void showLoadingState() {
        if (upcomingExamList != null) {
            upcomingExamList.getChildren().setAll(createEmptyRow("Loading upcoming exams..."));
        }

        if (resultSummaryList != null) {
            resultSummaryList.getChildren().setAll(createEmptyRow("Loading result summary..."));
        }
    }

    private void hideLoadingState() { }

    private void loadProfile(StudentProfile profile) {
        if (profile == null) {
            return;
        }

        String firstName = safe(profile.getFirstName());
        String lastName = safe(profile.getLastName());
        String fullName = (firstName + " " + lastName).trim();

        loadStudentAvatar(profile, firstName, lastName);

        studentNameLabel.setText(fullName.isBlank() ? "Student" : fullName);
        studentInitialsLabel.setText(getInitials(firstName, lastName));

        studentIDLabel.setText(safe(profile.getSchoolId()));

        studentEmailLabel.setText(safe(profile.getEmailAddress()));

        studentProgramLabel.setText(safe(profile.getProgramName()));

        studentCollegeLabel.setText(
                !safe(profile.getCollegeName()).isBlank()
                        ? safe(profile.getCollegeName())
                        : safe(profile.getCollegeCode())
        );

        studentCurrentTermLabel.setText(safe(profile.getCurrentTerm()));

    }

    private void loadAvatarImage(String imageUrl) {
        String schoolId = Session.getSchoolId();

        if (schoolId != null && !schoolId.isBlank()
                && localImageCacheService.hasAvatar(schoolId)) {

            Image cachedImage = new Image(localImageCacheService.getAvatarUri(schoolId), true);
            showImageAvatar(cachedImage);
            return;
        }

        String finalUrl = imageUrl.startsWith("http")
                ? imageUrl
                : AppConfig.BASE_URL + imageUrl;

        Image image = new Image(finalUrl, true);

        image.errorProperty().addListener((obs, oldValue, hasError) -> {
            if (hasError) {
            }
        });

        image.progressProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.doubleValue() >= 1.0 && !image.isError()) {
                Platform.runLater(() -> {
                    showImageAvatar(image);

                    new Thread(() -> {
                        localImageCacheService.saveAvatarFromUrl(schoolId, finalUrl);
                    }).start();
                });
            }
        });
    }

    private void loadUpcomingExams(List<StudentUpcomingExam> exams) {
        upcomingExamList.getChildren().clear();

        List<StudentUpcomingExam> visibleExams =
                exams == null
                        ? List.of()
                        : exams.stream()
                        .filter(exam -> {
                            String status = formatExamStatus(exam);

                            return "Scheduled".equalsIgnoreCase(status)
                                   || "Lobby Open".equalsIgnoreCase(status)
                                   || "Available".equalsIgnoreCase(status)
                                   || "Resume".equalsIgnoreCase(status);
                        })
                        .toList();

        if (visibleExams.isEmpty()) {
            upcomingExamList.getChildren().add(createEmptyRow("No upcoming exams."));
            return;
        }

        for (int i = 0; i < visibleExams.size(); i++) {
            upcomingExamList.getChildren().add(createUpcomingExamRow(visibleExams.get(i)));

            if (i < visibleExams.size() - 1) {
                upcomingExamList.getChildren().add(new Separator());
            }
        }
    }

    private HBox createUpcomingExamRow(StudentUpcomingExam exam) {
        Label course = new Label(safe(exam.getCourseCode()));
        course.getStyleClass().add("student-course-pill");

        Label title = new Label(safe(exam.getTitle()));
        title.getStyleClass().add("oracle-row-title");
        title.setWrapText(false);
        title.setTextOverrun(OverrunStyle.ELLIPSIS);
        title.setMaxWidth(Double.MAX_VALUE);

        HBox titleBox = new HBox(8, course, title);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label schedule = new Label(
                formatDateRange(exam.getStartDateTime(), exam.getEndDateTime())
                        + " • "
                        + defaultInt(exam.getTimeLimitMinutes())
                        + " min • "
                        + safe(exam.getExamMode())
        );
        schedule.getStyleClass().add("oracle-row-subtitle");
        schedule.setWrapText(false);
        schedule.setTextOverrun(OverrunStyle.ELLIPSIS);

        VBox infoBox = new VBox(4, titleBox, schedule);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        String computedStatus = formatExamStatus(exam);

        Label status = new Label(computedStatus);
        status.getStyleClass().add(
                switch (computedStatus) {
                    case "Available", "Resume" -> "student-status-available";
                    case "Scheduled" -> "student-status-scheduled";
                    case "Expired" -> "student-status-expired";
                    case "Completed" -> "student-status-completed";
                    default -> "student-status-default";
                }
        );

        Button actionButton = null;

        switch (computedStatus) {
            case "Lobby Open", "Available", "Resume" -> {
                Button enterButton = new Button(
                        "Resume".equals(computedStatus)
                                ? "Resume"
                                : "Enter Lobby"
                );

                enterButton.getStyleClass().add("student-view-exams-button");
                enterButton.setOnAction(e -> openExamPage(exam, enterButton));

                actionButton = enterButton;
            }

            case "Completed" -> {
                Button reviewButton = new Button("Summary");
                reviewButton.getStyleClass().add("student-outline-button");
                reviewButton.setOnAction(e -> openExamReview(exam));

                actionButton = reviewButton;
            }
        }

        HBox row = actionButton != null
                ? new HBox(14, infoBox, status, actionButton)
                : new HBox(14, infoBox, status);

        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("oracle-row");

        return row;
    }

    private void loadResultSummary(
            List<StudentResultSummary> results,
            StudentDashboardStats stats
    ) {
        resultSummaryList.getChildren().clear();

        if (results == null || results.isEmpty()) {
            resultSummaryList.getChildren().add(createEmptyRow("No exam results yet."));
            return;
        }

        for (int i = 0; i < results.size(); i++) {
            resultSummaryList.getChildren().add(createResultRow(results.get(i)));

            if (i < results.size() - 1) {
                resultSummaryList.getChildren().add(new Separator());
            }
        }
    }

    private HBox createResultRow(StudentResultSummary result) {

        Label title = new Label(safe(result.getExamTitle()));
        title.getStyleClass().add("oracle-row-title");

        Label course = new Label(safe(result.getCourseCode()));
        course.getStyleClass().add("oracle-row-subtitle");

        VBox textBox = new VBox(3, title, course);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label status = new Label(safe(result.getStatus()));
        status.getStyleClass().add("result-status-released");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(
                12,
                textBox,
                spacer,
                status
        );

        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("oracle-small-row");

        row.setOnMouseClicked(event -> {
            openResultWorkspace(result);
        });

        return row;
    }

    private HBox createEmptyRow(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("student-muted-text");

        HBox row = new HBox(label);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("oracle-row");

        return row;
    }

    private String formatStatus(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String cleaned = value.replace("_", " ").toLowerCase();
        String[] words = cleaned.split(" ");

        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isBlank()) {
                result.append(word.substring(0, 1).toUpperCase())
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    private String formatExamStatus(StudentUpcomingExam exam) {
        String baseStatus = formatStatus(exam.getStatus());

        try {
            if (!"PUBLISHED".equalsIgnoreCase(exam.getStatus())) {
                return baseStatus;
            }

            OffsetDateTime start = exam.getStartDateTime();
            OffsetDateTime end = exam.getEndDateTime();

            if (start == null || end == null) {
                return baseStatus;
            }

            OffsetDateTime now = nowManila();
            OffsetDateTime startManila = toManila(start);
            OffsetDateTime endManila = toManila(end);
            OffsetDateTime lobbyOpenAt = startManila.minusMinutes(15);

            if (now.isAfter(endManila)) {
                return "Expired";
            }

            if (now.isBefore(lobbyOpenAt)) {
                return "Scheduled";
            }

            if (now.isBefore(startManila)) {
                return "Lobby Open";
            }

            if ("IN_PROGRESS".equalsIgnoreCase(exam.getAttemptStatus())) {
                return "Resume";
            }

            return "Available";

        } catch (Exception e) {
            return baseStatus;
        }
    }

    private String formatDateRange(OffsetDateTime startValue, OffsetDateTime endValue) {
        if (startValue == null || endValue == null) {
            return "";
        }

        try {
            ZoneId manila = ZoneId.of("Asia/Manila");

            ZonedDateTime start = startValue.atZoneSameInstant(manila);
            ZonedDateTime end = endValue.atZoneSameInstant(manila);

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

            if (start.toLocalDate().equals(end.toLocalDate())) {
                return start.format(dateFormatter) + " • " +
                        start.format(timeFormatter) + " - " +
                        end.format(timeFormatter);
            }

            return start.format(dateFormatter) + " " + start.format(timeFormatter) +
                    " → " +
                    end.format(dateFormatter) + " " + end.format(timeFormatter);

        } catch (Exception e) {
            return startValue + " - " + endValue;
        }
    }

    private String getInitials(String firstName, String lastName) {
        String first = firstName == null || firstName.isBlank()
                ? ""
                : firstName.substring(0, 1);

        String last = lastName == null || lastName.isBlank()
                ? ""
                : lastName.substring(0, 1);

        String initials = (first + last).toUpperCase();

        return initials.isBlank() ? "ST" : initials;
    }

    private void loadStudentAvatar(
            StudentProfile profile,
            String firstName,
            String lastName
    ) {
        String schoolId = Session.getSchoolId();

        if (schoolId != null
                && !schoolId.isBlank()
                && localImageCacheService.hasAvatar(schoolId)) {

            showImageAvatar(
                    new Image(
                            localImageCacheService.getAvatarUri(schoolId),
                            true
                    )
            );
            return;
        }

        String imageUrl = safe(profile.getProfileImageUrl());

        if (imageUrl.isBlank()) {
            showInitialsFallback(firstName, lastName);
            return;
        }

        String finalUrl = buildProfileImageUrl(imageUrl);

        Image image = new Image(finalUrl, true);

        image.errorProperty().addListener((obs, oldValue, hasError) -> {
            if (hasError) {
                Platform.runLater(() -> showInitialsFallback(firstName, lastName));
            }
        });

        image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
            if (newProgress.doubleValue() >= 1.0 && !image.isError()) {
                Platform.runLater(() -> {
                    showImageAvatar(image);

                    new Thread(() -> {
                        localImageCacheService.saveAvatarFromUrl(
                                schoolId,
                                finalUrl
                        );
                    }).start();
                });
            }
        });
    }

    private String buildProfileImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return "";
        }

        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }

        if (imageUrl.startsWith("/")) {
            return AppConfig.BASE_URL + imageUrl;
        }

        return AppConfig.BASE_URL + "/" + imageUrl;
    }

    private void showImageAvatar(Image image) {
        studentAvatarImageView.setImage(image);

        studentAvatarImageView.setFitWidth(124);
        studentAvatarImageView.setFitHeight(124);
        studentAvatarImageView.setPreserveRatio(false);
        studentAvatarImageView.setSmooth(true);

        studentAvatarImageView.setVisible(true);
        studentAvatarImageView.setManaged(true);

        studentInitialsLabel.setVisible(false);
        studentInitialsLabel.setManaged(false);
    }

    private void showInitialsFallback(String firstName, String lastName) {
        studentInitialsLabel.setText(getInitials(firstName, lastName));

        studentInitialsLabel.setVisible(true);
        studentInitialsLabel.setManaged(true);

        studentAvatarImageView.setImage(null);
        studentAvatarImageView.setVisible(false);
        studentAvatarImageView.setManaged(false);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private void openResultWorkspace(StudentResultSummary result) {

        if (result == null || result.getExamId() == null) {
            return;
        }

        try {
            dashboardApiService.markResultViewed(result.getExamId());

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/fxml/student/student-results-workspace.fxml"
                    )
            );

            Parent content = loader.load();

            StudentResultsWorkspaceController controller =
                    loader.getController();

            if (shellController != null) {
                controller.setShellController(shellController);

                shellController.hideHeroSection();
                shellController.hideHeroCards();

                shellController.setGreeting(
                        "Released Exam Results",
                        "Review your submitted answers, feedback, and rubric evaluation."
                );

                shellController.setWorkspaceContent(content);
            }

            controller.loadResult(result.getExamId());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openExamPage(StudentUpcomingExam exam, Button clickedButton) {
        if (exam == null || exam.getExamId() == null) {
            return;
        }

        LoadingSpinner.setLoading(clickedButton, true, "Loading...", "Enter Lobby");

        PauseTransition delay = new PauseTransition(Duration.millis(120));
        delay.setOnFinished(event -> {
            try {
                Stage currentStage = (Stage) upcomingExamList.getScene().getWindow();

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/student/exam-taking.fxml")
                );

                Parent root = loader.load();

                ExamTakingController controller = loader.getController();
                controller.setExamLobby(
                        exam.getExamId(),
                        exam.getTitle(),
                        exam.getTimeLimitMinutes(),
                        exam.getQuestionCount(),
                        exam.getStartDateTime(),
                        exam.getEndDateTime()
                );

                Scene examScene = new Scene(root);

                currentStage.setTitle("ExamGuard - Taking Exam");
                currentStage.setScene(examScene);
                currentStage.setMaximized(true);
                currentStage.show();

                currentStage.setFullScreenExitHint("");
                currentStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
                currentStage.setFullScreen(true);

            } catch (Exception e) {
                e.printStackTrace();
                LoadingSpinner.setLoading(clickedButton, false, "Loading...", "Enter Lobby");
            }
        });

        delay.play();
    }

    private void setupViewAllRedirects() {

        setupViewAllLabel(
                viewAllUpcomingExamsLabel,
                "UPCOMING"
        );

        setupViewAllLabel(
                viewAllResultsReleasedLabel,
                "RESULTS RELEASED"
        );
    }

    private void setupViewAllLabel(Label label, String filter) {

        if (label == null) {
            return;
        }

        label.setOnMouseClicked(event -> {

            if (shellController != null) {
                shellController.openStudentExamsPageWithFilter(filter);
            }
        });

        label.setOnMouseEntered(event -> {
            label.setStyle("-fx-cursor: hand;");
        });
    }

    private void openExamReview(StudentUpcomingExam exam) { }
}