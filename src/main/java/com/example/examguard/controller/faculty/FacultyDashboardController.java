package com.example.examguard.controller.faculty;


import com.example.examguard.cache.FacultyLocalCacheKeys;
import com.example.examguard.cache.LocalCacheService;
import com.example.examguard.utility.Session;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.example.examguard.utility.OffsetDateTimeAdapter;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import com.example.examguard.controller.layout.DashboardShellController;
import com.example.examguard.controller.layout.ShellAwareController;
import com.example.examguard.model.faculty.dto.*;
import com.example.examguard.service.FacultyApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class FacultyDashboardController implements ShellAwareController {

    @FXML private VBox activeExamList;
    @FXML private VBox needsReviewList;
    @FXML private VBox handledClassList;

    private final FacultyApiService facultyApiService = new FacultyApiService();
    private final LocalCacheService localCacheService = new LocalCacheService();

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a");

    private DashboardShellController shellController;

    @Override
    public void setShellController(DashboardShellController shellController) {
        this.shellController = shellController;
    }

    @FXML
    private void initialize() {
        loadProfile();
        loadStats();
        loadActiveExams();
        loadNeedsReview();
        loadHandledClasses();
    }

    @FXML
    private void reloadActiveExams() {
        loadActiveExams();
        loadStats();
    }

    @FXML
    private void reloadNeedsReview() {
        loadNeedsReview();
        loadStats();
    }

    @FXML
    private void reloadHandledClasses() {
        loadHandledClasses();
        loadStats();
    }

    private void loadProfile() {
        boolean hasCache = loadProfileFromCache();

        runAsync(() -> {
            FacultyProfileDTO profile = facultyApiService.getDashboardProfile();

            localCacheService.save(
                    FacultyLocalCacheKeys.dashboardProfile(facultyId()),
                    FacultyLocalCacheKeys.VERSION,
                    profile
            );

            Platform.runLater(() -> renderProfile(profile));

        }, () -> {
            if (!hasCache) {
                Platform.runLater(() -> {
                    if (shellController != null) {
                        shellController.setGreeting(
                                "Good day, Faculty",
                                "Manage active exams, submissions, and integrity reviews."
                        );
                    }
                });
            }
        });
    }

    private boolean loadProfileFromCache() {
        FacultyProfileDTO cached = localCacheService.loadData(
                FacultyLocalCacheKeys.dashboardProfile(facultyId()),
                FacultyProfileDTO.class
        );

        if (cached == null) {
            return false;
        }

        renderProfile(cached);
        return true;
    }

    private void renderProfile(FacultyProfileDTO profile) {
        if (profile == null || shellController == null) {
            return;
        }

        String name = profile.getFullName();

        if (name == null || name.isBlank()) {
            name = "Faculty";
        }

        shellController.setGreeting(
                "Good day, Prof. " + name,
                "Manage active exams, submissions, and integrity reviews."
        );
    }

    private void loadStats() {
        boolean hasCache = loadStatsFromCache();

        runAsync(() -> {
            FacultyDashboardStatsDTO stats = facultyApiService.getDashboardStats();

            localCacheService.save(
                    FacultyLocalCacheKeys.dashboardStats(facultyId()),
                    FacultyLocalCacheKeys.VERSION,
                    stats
            );

            Platform.runLater(() -> renderStats(stats));

        }, () -> {
            if (!hasCache) {
                Platform.runLater(() -> {
                    if (shellController != null) {
                        shellController.updateHeroCards(
                                new DashboardShellController.HeroCardData("Active Exams", "—"),
                                new DashboardShellController.HeroCardData("Class Offerings", "—"),
                                new DashboardShellController.HeroCardData("Students Covered", "—"),
                                new DashboardShellController.HeroCardData("For Review", "—")
                        );
                    }
                });
            }
        });
    }

    private boolean loadStatsFromCache() {
        FacultyDashboardStatsDTO cached = localCacheService.loadData(
                FacultyLocalCacheKeys.dashboardStats(facultyId()),
                FacultyDashboardStatsDTO.class
        );

        if (cached == null) {
            return false;
        }

        renderStats(cached);
        return true;
    }

    private void loadActiveExams() {
        boolean hasCache = loadActiveExamsFromCache();

        if (!hasCache) {
            activeExamList.getChildren().setAll(
                    createEmptyRow("Loading active exams...")
            );
        }

        runAsync(() -> {
            List<FacultyExamSummaryDTO> exams =
                    facultyApiService.getDashboardActiveExams();

            localCacheService.save(
                    FacultyLocalCacheKeys.dashboardActiveExams(facultyId()),
                    FacultyLocalCacheKeys.VERSION,
                    exams
            );

            Platform.runLater(() -> renderActiveExams(exams));

        }, () -> {
            if (!hasCache) {
                Platform.runLater(() -> {
                    activeExamList.getChildren().setAll(
                            createEmptyRow("Unable to load active exams. Click reload.")
                    );
                });
            }
        });
    }

    private boolean loadActiveExamsFromCache() {
        List<FacultyExamSummaryDTO> cached = localCacheService.loadList(
                FacultyLocalCacheKeys.dashboardActiveExams(facultyId()),
                FacultyExamSummaryDTO.class
        );

        if (cached == null) {
            return false;
        }

        renderActiveExams(cached);
        return true;
    }

    private void loadNeedsReview() {
        boolean hasCache = loadNeedsReviewFromCache();

        if (!hasCache) {
            needsReviewList.getChildren().setAll(
                    createEmptyRow("Loading review items...")
            );
        }

        runAsync(() -> {
            List<FacultyViolationReviewDTO> reviews =
                    facultyApiService.getDashboardNeedsReview();

            localCacheService.save(
                    FacultyLocalCacheKeys.dashboardNeedsReview(facultyId()),
                    FacultyLocalCacheKeys.VERSION,
                    reviews
            );

            Platform.runLater(() -> renderNeedsReview(reviews));

        }, () -> {
            if (!hasCache) {
                Platform.runLater(() -> {
                    activeExamList.getChildren().setAll(
                            createEmptyRow("Unable to load data. Click reload.")
                    );
                });
            }
        });
    }

    private boolean loadNeedsReviewFromCache() {
        List<FacultyViolationReviewDTO> cached = localCacheService.loadList(
                FacultyLocalCacheKeys.dashboardNeedsReview(facultyId()),
                FacultyViolationReviewDTO.class
        );

        if (cached == null) {
            return false;
        }

        renderNeedsReview(cached);
        return true;
    }

    private void loadHandledClasses() {
        boolean hasCache = loadHandledClassesFromCache();

        if (!hasCache) {
            handledClassList.getChildren().setAll(
                    createEmptyRow("Loading handled classes...")
            );
        }

        runAsync(() -> {
            List<FacultyClassDTO> classes =
                    facultyApiService.getDashboardClasses();

            localCacheService.save(
                    FacultyLocalCacheKeys.dashboardClasses(facultyId()),
                    FacultyLocalCacheKeys.VERSION,
                    classes
            );

            Platform.runLater(() -> renderHandledClasses(classes));

        }, () -> {
            if (!hasCache) {
                Platform.runLater(() -> {
                    activeExamList.getChildren().setAll(
                            createEmptyRow("Unable to load data. Click reload.")
                    );
                });
            }
        });
    }

    private boolean loadHandledClassesFromCache() {
        List<FacultyClassDTO> cached = localCacheService.loadList(
                FacultyLocalCacheKeys.dashboardClasses(facultyId()),
                FacultyClassDTO.class
        );

        if (cached == null) {
            return false;
        }

        renderHandledClasses(cached);
        return true;
    }

    private void renderHandledClasses(
            List<FacultyClassDTO> classes
    ) {

        handledClassList.getChildren().clear();

        if (classes == null || classes.isEmpty()) {

            handledClassList.getChildren().add(
                    createEmptyRow("No handled classes.")
            );

            return;
        }

        for (int i = 0; i < classes.size(); i++) {

            handledClassList.getChildren().add(
                    createHandledClassRow(classes.get(i))
            );

            if (i < classes.size() - 1) {
                handledClassList.getChildren().add(
                        new Separator()
                );
            }
        }
    }

    private HBox createHandledClassRow(
            FacultyClassDTO item
    ) {

        Label course = new Label(
                safe(item.getCourseCode())
        );

        course.getStyleClass().add(
                "faculty-course-pill"
        );

        Label title = new Label(
                safe(item.getProgramCode()) +
                        " " +
                        item.getYearLevel() +
                        " - " +
                        safe(item.getSectionName())
        );

        title.getStyleClass().add(
                "faculty-row-title"
        );

        Label subtitle = new Label(
                safe(item.getCourseDescription())
        );

        subtitle.getStyleClass().add(
                "faculty-row-subtitle"
        );

        VBox infoBox = new VBox(
                3,
                title,
                subtitle
        );

        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label count = new Label(
                defaultLong(item.getEnrolledCount()) +
                        " students"
        );

        count.getStyleClass().add(
                "faculty-class-count"
        );

        HBox row = new HBox(
                12,
                course,
                infoBox,
                count
        );

        row.setAlignment(Pos.CENTER_LEFT);

        row.getStyleClass().add(
                "faculty-small-row"
        );

        return row;
    }

    private void renderStats(FacultyDashboardStatsDTO stats) {
        if (shellController == null || stats == null) {
            return;
        }

        shellController.updateHeroCards(
                new DashboardShellController.HeroCardData(
                        "Active Exams",
                        String.valueOf(defaultLong(stats.getActiveExamCount()))
                ),
                new DashboardShellController.HeroCardData(
                        "Class Offerings",
                        String.valueOf(defaultLong(stats.getClassOfferingCount()))
                ),
                new DashboardShellController.HeroCardData(
                        "Students Covered",
                        String.valueOf(defaultLong(stats.getTotalStudentCount()))
                ),
                new DashboardShellController.HeroCardData(
                        "For Review",
                        String.valueOf(defaultLong(stats.getReviewQueueCount()))
                )
        );
    }

    private void renderActiveExams(List<FacultyExamSummaryDTO> exams) {
        activeExamList.getChildren().clear();

        if (exams == null || exams.isEmpty()) {
            activeExamList.getChildren().add(createEmptyRow("No active exams."));
            return;
        }

        for (int i = 0; i < exams.size(); i++) {
            activeExamList.getChildren().add(createActiveExamRow(exams.get(i)));

            if (i < exams.size() - 1) {
                activeExamList.getChildren().add(new Separator());
            }
        }
    }

    private HBox createActiveExamRow(FacultyExamSummaryDTO exam) {

        long total = defaultLong(exam.getTotalAssigned());
        long submitted = defaultLong(exam.getSubmittedCount());
        long flagged = defaultLong(exam.getViolationCount());

        double progress =
                total == 0
                        ? 0
                        : Math.min(1.0, (double) submitted / total);

        boolean completedBySubmissions =
                total > 0 && submitted >= total;

        String dashboardStatus = getDashboardStatus(
                exam.getStartDateTime(),
                exam.getEndDateTime(),
                total,
                submitted
        );

        Label course = new Label(safe(exam.getCourseCode()));
        course.getStyleClass().add("faculty-course-pill");

        Label statusPill = new Label(dashboardStatus);
        statusPill.getStyleClass().add(
                statusPillClass(dashboardStatus)
        );

        VBox leftPills = new VBox(5, course, statusPill);
        leftPills.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(safe(exam.getTitle()));
        title.getStyleClass().add("faculty-row-title");

        Label details = new Label(
                safe(exam.getClassSections()) +
                        " • " +
                        formatStatus(exam.getStatus()) +
                        (flagged > 0
                                ? " • " + flagged + " flagged"
                                : "")
        );

        details.getStyleClass().add(
                flagged > 0
                        ? "faculty-meta-warning"
                        : "faculty-row-subtitle"
        );

        VBox infoBox = new VBox(3, title, details);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        ProgressBar progressBar = new ProgressBar(progress);
        progressBar.setPrefWidth(160);
        progressBar.setPrefHeight(12);
        progressBar.getStyleClass().add("faculty-progress-bar");

        Label percentLabel = new Label(
                completedBySubmissions
                        ? "Completed"
                        : ((int) Math.round(progress * 100)) + "%"
        );

        percentLabel.getStyleClass().add(
                completedBySubmissions
                        ? "faculty-progress-complete"
                        : "faculty-progress-percent"
        );

        Label countLabel = new Label(
                submitted + " of " + total + " submitted"
        );

        countLabel.getStyleClass().add(
                "faculty-progress-count"
        );

        VBox progressBox = new VBox(
                4,
                percentLabel,
                progressBar,
                countLabel
        );

        progressBox.setAlignment(Pos.CENTER_RIGHT);
        progressBox.setMinWidth(170);
        progressBox.setPrefWidth(170);
        progressBox.setMaxWidth(170);

        HBox row = new HBox(
                12,
                leftPills,
                infoBox,
                progressBox
        );

        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("faculty-row");

        row.setOnMouseClicked(event -> {
            openExamDetail(exam.getExamId());
        });

        row.setCursor(Cursor.HAND);

        return row;
    }

    private void renderNeedsReview(List<FacultyViolationReviewDTO> reviews) {
        needsReviewList.getChildren().clear();

        if (reviews == null || reviews.isEmpty()) {
            needsReviewList.getChildren().add(
                    createEmptyRow("All required reviews are completed.")
            );
            return;
        }

        for (int i = 0; i < reviews.size(); i++) {
            needsReviewList.getChildren().add(createReviewRow(reviews.get(i)));

            if (i < reviews.size() - 1) {
                needsReviewList.getChildren().add(new Separator());
            }
        }
    }

    private HBox createReviewRow(FacultyViolationReviewDTO review) {
        Label course = new Label(safe(review.getCourseCode()));
        course.getStyleClass().add("faculty-course-pill");

        String text =
                defaultLong(review.getStudentCount()) + " students flagged";

        Label title = new Label(text);
        title.getStyleClass().add("faculty-row-title");

        Label exam = new Label(safe(review.getExamTitle()));
        exam.getStyleClass().add("faculty-row-subtitle");

        Label status = new Label("FOR REVIEW");
        status.getStyleClass().add("faculty-review-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox top = new HBox(8, title, spacer, status);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(3, top, exam);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox row = new HBox(10, course, info);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("faculty-small-row");

        return row;
    }

    private void openExamDetail(Long examId) {
        if (shellController != null) {
            shellController.openExamManagementWorkspace(examId);
        }
    }

    private Label createEmptyRow(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("faculty-empty-text");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setTextAlignment(TextAlignment.CENTER);
        return label;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private long defaultLong(Long value) {
        return value == null ? 0 : value;
    }

    private String formatStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Unknown";
        }

        return status.replace("_", " ");
    }

    @FunctionalInterface
    private interface AsyncTask {
        void run() throws Exception;
    }

    private void runAsync(
            AsyncTask task,
            Runnable onError
    ) {

        new Thread(() -> {
            try {
                task.run();

            } catch (Exception e) {
                e.printStackTrace();

                if (onError != null) {
                    Platform.runLater(onError);
                }
            }

        }, "faculty-dashboard-loader").start();
    }

    private String getDashboardStatus(
            OffsetDateTime start,
            OffsetDateTime end,
            long total,
            long submitted
    ) {
        if (total > 0 && submitted >= total) {
            return "COMPLETED";
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (start == null || end == null) {
            return "UNKNOWN";
        }

        if (now.isBefore(start)) {
            return "UPCOMING";
        }

        if (now.isAfter(end)) {
            return "COMPLETED";
        }

        return "ONGOING";
    }

    private String facultyId() {
        String id = Session.getSchoolId();

        if (id == null || id.isBlank()) {
            return "unknown-faculty";
        }

        return id;
    }

    private String statusPillClass(String status) {

        if (status == null) {
            return "faculty-status-pill-default";
        }

        return switch (status.toUpperCase()) {
            case "ONGOING" -> "faculty-status-pill-active";
            case "UPCOMING" -> "faculty-status-pill-scheduled";
            case "DRAFT" -> "faculty-status-pill-draft";
            case "COMPLETED" -> "faculty-status-pill-completed";
            case "CANCELLED" -> "faculty-status-pill-cancelled";
            default -> "faculty-status-pill-default";
        };
    }
}
