package com.example.examguard.controller.student;

import com.example.examguard.model.student.*;
import com.example.examguard.model.student.response.StudentDashboardResponse;
import com.example.examguard.service.StudentApiService;
import com.example.examguard.utility.LoadingSpinner;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StudentDashboardController {

    @FXML private ImageView studentAvatarImageView;
    @FXML private Label studentInitialsLabel;
    @FXML private Label studentNameLabel;
    @FXML private Label studentEmailLabel;
    @FXML private Label studentIDLabel;
    @FXML private Label studentProgramLabel;
    @FXML private Label studentCollegeLabel;
    @FXML private Label studentCurrentTermLabel;
    @FXML private Label studentIntegrityStatusLabel;
    @FXML private Label studentIntegritySubtitleLabel;

    @FXML private VBox upcomingExamList;
    @FXML private VBox violationList;
    @FXML private VBox resultSummaryList;

    private final StudentApiService dashboardApiService =
            new StudentApiService();

    @FXML
    public void initialize() {
        showLoadingState();
        loadDashboardAsync();
    }

    @FXML
    private void handleReloadProfile() {
        reloadDashboardPart("profile");
    }

    @FXML
    private void handleReloadUpcomingExams() {
        upcomingExamList.getChildren().setAll(createEmptyRow("Reloading upcoming exams..."));
        reloadDashboardPart("upcoming");
    }

    @FXML
    private void handleReloadViolations() {
        violationList.getChildren().setAll(createEmptyRow("Reloading violations..."));
        reloadDashboardPart("violations");
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
            StudentDashboardResponse dashboard = task.getValue();

            switch (part) {
                case "profile" -> loadProfile(dashboard.getProfile());
                case "upcoming" -> loadUpcomingExams(dashboard.getUpcomingExams());
                case "violations" -> loadViolations(dashboard.getViolations());
                case "results" -> loadResultSummary(
                        dashboard.getResultSummary(),
                        dashboard.getStats()
                );
            }
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();

            switch (part) {
                case "upcoming" ->
                        upcomingExamList.getChildren().setAll(createEmptyRow("Unable to reload upcoming exams."));
                case "violations" ->
                        violationList.getChildren().setAll(createEmptyRow("Unable to reload violations."));
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
            loadViolations(dashboard.getViolations());
            loadResultSummary(dashboard.getResultSummary(), dashboard.getStats());

            hideLoadingState();
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            hideLoadingState();

            upcomingExamList.getChildren().setAll(createEmptyRow("Unable to load upcoming exams."));
            violationList.getChildren().setAll(createEmptyRow("Unable to load violations."));
            resultSummaryList.getChildren().setAll(createEmptyRow("Unable to load result summary."));
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void showLoadingState() {
        if (upcomingExamList != null) {
            upcomingExamList.getChildren().setAll(createEmptyRow("Loading upcoming exams..."));
        }

        if (violationList != null) {
            violationList.getChildren().setAll(createEmptyRow("Loading violations..."));
        }

        if (resultSummaryList != null) {
            resultSummaryList.getChildren().setAll(createEmptyRow("Loading result summary..."));
        }
    }

    private void hideLoadingState() {
        // no need for now because real data replaces loading rows
    }

    private void loadProfile(StudentProfile profile) {
        if (profile == null) {
            return;
        }

        String firstName = safe(profile.getFirstName());
        String lastName = safe(profile.getLastName());
        String fullName = (firstName + " " + lastName).trim();

        String imageUrl = safe(profile.getProfileImageUrl());

        if (!imageUrl.isBlank()) {
            try {
                Image image = new Image(imageUrl, true);

                studentAvatarImageView.setImage(image);
                studentAvatarImageView.setVisible(true);
                studentAvatarImageView.setManaged(true);

                studentInitialsLabel.setVisible(false);
                studentInitialsLabel.setManaged(false);

            } catch (Exception e) {
                showInitialsFallback(firstName, lastName);
            }
        } else {
            showInitialsFallback(firstName, lastName);
        }

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

        studentIntegrityStatusLabel.setText(
                safe(profile.getIntegrityStatus()).isBlank()
                        ? "Good Standing"
                        : profile.getIntegrityStatus()
        );

        studentIntegritySubtitleLabel.setText(
                safe(profile.getIntegritySubtitle()).isBlank()
                        ? "No unresolved major violation."
                        : profile.getIntegritySubtitle()
        );
    }

    private void loadUpcomingExams(List<StudentUpcomingExam> exams) {
        upcomingExamList.getChildren().clear();

        if (exams == null || exams.isEmpty()) {
            upcomingExamList.getChildren().add(createEmptyRow("No upcoming exams."));
            return;
        }

        for (int i = 0; i < exams.size(); i++) {
            upcomingExamList.getChildren().add(createUpcomingExamRow(exams.get(i)));

            if (i < exams.size() - 1) {
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
            case "Available", "Resume" -> {
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

    private void loadViolations(List<StudentViolationSummary> violations) {
        violationList.getChildren().clear();

        if (violations == null || violations.isEmpty()) {
            violationList.getChildren().add(createEmptyRow("No violations detected."));
            return;
        }

        for (int i = 0; i < violations.size(); i++) {
            StudentViolationSummary violation = violations.get(i);

            violationList.getChildren().add(createViolationRow(violation));

            if (i < violations.size() - 1) {
                Separator separator = new Separator();
                separator.setOpacity(0.45);
                violationList.getChildren().add(separator);
            }
        }
    }

    private HBox createViolationRow(StudentViolationSummary violation) {

        String label = violation.getViolationType();

        if (label == null || label.isBlank()) {
            label = "VIOLATION";
        }

        label = label.replace("_", " ");

        String displayText =
                violation.getCountViolation() + " - " + label;

        Label course = new Label(safe(violation.getCourseCode()));
        course.getStyleClass().add("student-course-pill");

        Label text = new Label(displayText);
        text.getStyleClass().add("oracle-row-title");

        Label reviewText = new Label("PENDING REVIEW");
        reviewText.getStyleClass().add("review-text");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        HBox topRow = new HBox(
                8,
                text,
                topSpacer,
                reviewText
        );

        topRow.setAlignment(Pos.TOP_LEFT);

        Label exam = new Label(safe(violation.getExamTitle()));
        exam.getStyleClass().add("oracle-row-subtitle");
        exam.setWrapText(true);

        VBox infoBox = new VBox(3, topRow, exam);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        HBox row = new HBox(10, course, infoBox);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("oracle-small-row");

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

        Label course = new Label(safe(result.getCourseCode()));
        course.getStyleClass().add("student-course-pill");

        course.setMinWidth(Region.USE_PREF_SIZE);
        course.setMaxWidth(Region.USE_PREF_SIZE);
        course.setWrapText(false);

        String scoreText = result.getScorePercentage() == null
                ? "RESULT PENDING"
                : String.format("%.0f%%", defaultDouble(result.getScorePercentage()));

        Label score = new Label(scoreText);
        score.getStyleClass().add("oracle-row-title");

        Label statusText = new Label(
                result.getScorePercentage() == null
                        ? "FOR CHECKING"
                        : formatStatus(result.getAttemptStatus())
        );

        statusText.getStyleClass().add(
                result.getScorePercentage() == null
                        ? "result-status-checking"
                        : "result-status-released"
        );

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        HBox topRow = new HBox(8, score, topSpacer, statusText);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label exam = new Label(safe(result.getExamTitle()));
        exam.getStyleClass().add("oracle-row-subtitle");
        exam.setWrapText(false);
        exam.setTextOverrun(OverrunStyle.ELLIPSIS);
        exam.setMaxWidth(Double.MAX_VALUE);

        VBox infoBox = new VBox(3, topRow, exam);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        HBox row = new HBox(10, course, infoBox);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("oracle-small-row");

        return row;
    }

    private HBox createAverageRow(double average) {
        Label label = new Label("Average Score");
        label.getStyleClass().add("oracle-row-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label value = new Label(String.format("%.0f%%", average));
        value.getStyleClass().add("student-course-score");

        HBox row = new HBox(12, label, spacer, value);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("oracle-small-row");

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

        if ("IN_PROGRESS".equalsIgnoreCase(exam.getAttemptStatus())) {
            return "Resume";
        }

        try {
            if (!"PUBLISHED".equalsIgnoreCase(exam.getStatus())) {
                return baseStatus;
            }

            ZoneId manila = ZoneId.of("Asia/Manila");

            ZonedDateTime now = ZonedDateTime.now(manila);
            ZonedDateTime start = exam.getStartDateTime()
                    .atZoneSameInstant(manila);
            ZonedDateTime end = exam.getEndDateTime()
                    .atZoneSameInstant(manila);

            if (now.isBefore(start)) {
                return "Scheduled";
            } else if (now.isAfter(end)) {
                return "Expired";
            } else {
                return "Available";
            }

        } catch (Exception e) {
            return baseStatus;
        }
    }

    private String formatDateTime(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        try {
            return OffsetDateTime.parse(value)
                    .atZoneSameInstant(java.time.ZoneId.of("Asia/Manila"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
        } catch (Exception e) {
            return value;
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

    private double computeAverage(List<StudentResultSummary> results) {
        if (results == null || results.isEmpty()) {
            return 0;
        }

        double total = 0;

        for (StudentResultSummary result : results) {
            total += defaultDouble(result.getScorePercentage());
        }

        return total / results.size();
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

    private void showInitialsFallback(String firstName, String lastName) {
        studentInitialsLabel.setText(getInitials(firstName, lastName));

        studentInitialsLabel.setVisible(true);
        studentInitialsLabel.setManaged(true);

        studentAvatarImageView.setVisible(false);
        studentAvatarImageView.setManaged(false);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double defaultDouble(Double value) {
        return value == null ? 0 : value;
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
                        defaultInt(exam.getTimeLimitMinutes())
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

    private void openExamReview(StudentUpcomingExam exam) {
        System.out.println("Open exam page. Exam ID: " + exam.getExamId());

        // TODO next:
        // redirect to exam-taking page and pass exam.getExamId()
    }

    private void openViolationReview(StudentViolationSummary violation) {
        System.out.println("Open violation review. Violation ID: " + violation.getViolationId());

        // TODO next:
        // open result summary page with violation proof/details
    }
}