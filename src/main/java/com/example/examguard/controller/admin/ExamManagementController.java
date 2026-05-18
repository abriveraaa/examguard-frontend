package com.example.examguard.controller.admin;

import com.example.examguard.controller.layout.DashboardShellController;
import com.example.examguard.controller.layout.ShellAwareController;
import com.example.examguard.controller.exam.CreateExamController;
import com.example.examguard.model.exam.dto.ExamActivityLogDTO;
import com.example.examguard.model.exam.dto.ExamAttemptAnswerReviewDTO;
import com.example.examguard.model.exam.dto.ExamAttemptViolationDTO;
import com.example.examguard.model.exam.dto.ExamLeaderboardDTO;
import com.example.examguard.model.exam.request.EssayReviewRequest;
import com.example.examguard.model.exam.request.EssayRubricRequest;
import com.example.examguard.model.exam.request.EssayRubricScoreRequest;
import com.example.examguard.model.exam.response.EssayRubricScoreResponse;
import com.example.examguard.model.faculty.dto.FacultyClassDTO;
import com.example.examguard.model.faculty.dto.FacultyExamStudentDTO;
import com.example.examguard.model.faculty.response.FacultyAttemptReviewResponse;
import com.example.examguard.model.faculty.response.FacultyExamDetailResponse;
import com.example.examguard.model.faculty.response.SimpleMessageResponse;
import com.example.examguard.service.FacultyApiService;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import com.example.examguard.model.exam.response.ExamResponse;
import com.example.examguard.model.exam.result.ExamResult;
import com.example.examguard.model.exam.result.ExamRow;
import com.example.examguard.service.ExamApiService;
import com.example.examguard.utility.LoadingSpinner;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.beans.property.SimpleStringProperty;

import java.util.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

public class ExamManagementController implements ShellAwareController {

    @FXML private TableView<ExamRow> examTable;
    @FXML private TableColumn<ExamRow, String> dateCreatedColumn;
    @FXML private TableColumn<ExamRow, String> validityColumn;
    @FXML private TableColumn<ExamRow, String> titleColumn;
    @FXML private TableColumn<ExamRow, String> statusColumn;
    @FXML private TableColumn<ExamRow, String> durationColumn;
    @FXML private TableColumn<ExamRow, String> assignedColumn;
    @FXML private TableColumn<ExamRow, String> takersColumn;
    @FXML private TableColumn<ExamRow, String> createdByColumn;
    @FXML private TableColumn<ExamRow, String> updatedByColumn;
    @FXML private TableColumn<ExamRow, Void> actionsColumn;
    @FXML private BorderPane listModeContainer;
    @FXML private BorderPane workspaceModeContainer;
    @FXML private StackPane workspaceContent;
    @FXML private Label workspaceTitleLabel;
    @FXML private Label workspaceSubtitleLabel;
    @FXML private Button releaseResultsButton;
    @FXML private Button reportsTabButton;
    @FXML private Label itemCountLabel;
    @FXML private TextField searchField;
    @FXML private Button reloadButton;
    @FXML private StackPane loadingOverlay;
    @FXML private Button overviewTabButton;
    @FXML private Button studentsTabButton;
    @FXML private Button proctorTabButton;
    @FXML private Button activityLogTabButton;
    @FXML private Button leaderboardTabButton;

    private final ExamApiService examApiService = new ExamApiService();
    private final FacultyApiService facultyApiService = new FacultyApiService();

    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(250));
    private final ObservableList<ExamRow> examRows = FXCollections.observableArrayList();
    private FacultyExamStudentDTO currentReviewStudent;
    private FacultyAttemptReviewResponse currentAttemptReview;
    private FilteredList<ExamRow> filteredRows;
    private DashboardShellController shellController;
    private Task<List<ExamRow>> currentLoadTask;
    private FacultyExamDetailResponse currentWorkspaceDetail;

    private boolean loading = false;
    private Long selectedWorkspaceExamId;


    @FXML
    public void initialize() {
        filteredRows = new FilteredList<>(examRows, row -> true);

        setupTableColumns();
        setupCenteredColumns();
        setupColumnWidths();
        setupTitleStyle();
        setupStatusStyle();
        setupTakersStyle();
        setupValidityStyle();
        setupActionButtons();
        setupSearch();

        examTable.setItems(filteredRows);
        examTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        loadExamsFromBackend();
    }

    @FXML
    private void showWorkspaceOverview() {
        setActiveWorkspaceTab(overviewTabButton);
        if (selectedWorkspaceExamId == null) {
            return;
        }

        workspaceContent.getChildren().setAll(
                createLoadingBox("Loading exam overview...")
        );

        Task<FacultyExamDetailResponse> task = new Task<>() {
            @Override
            protected FacultyExamDetailResponse call() throws Exception {
                return examApiService.getExamDetail(selectedWorkspaceExamId);
            }
        };

        task.setOnSucceeded(event -> {
            FacultyExamDetailResponse detail = task.getValue();
            renderWorkspaceOverview(detail);
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();

            workspaceContent.getChildren().setAll(
                    createLoadingBox("Unable to load exam overview.")
            );
        });

        Thread thread = new Thread(task, "load-exam-overview-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void renderWorkspaceOverview(FacultyExamDetailResponse detail) {
        this.currentWorkspaceDetail = detail;

        if (detail == null) {
            workspaceContent.getChildren().setAll(
                    createLoadingBox("No exam data found.")
            );
            return;
        }

        workspaceTitleLabel.setText(safe(detail.getTitle()));

        boolean released = Boolean.TRUE.equals(detail.getResultsReleased());

        releaseResultsButton.setDisable(released);
        releaseResultsButton.setText(
                released ? "Results Released" : "Release Results"
        );

        long total = defaultLong(detail.getTotalStudents());
        long submitted = defaultLong(detail.getSubmittedCount());
        long notSubmitted = defaultLong(detail.getNotSubmittedCount());
        long studentsWithViolations = defaultLong(detail.getStudentsWithViolations());
        long totalViolations = defaultLong(detail.getTotalViolations());

        int submissionRate = total == 0 ? 0 : (int) (((double) submitted / total) * 100);
        int pendingRate = total == 0 ? 0 : (int) (((double) notSubmitted / total) * 100);
        int violationRate = total == 0 ? 0 : (int) (((double) studentsWithViolations / total) * 100);

        VBox root = new VBox(18);
        root.getStyleClass().add("workspace-overview-root");

        Label pageTitle = new Label("Overview");
        pageTitle.getStyleClass().add("workspace-page-title");

        Label pageSubtitle = new Label("Real-time summary of submissions, violations, schedule, and assigned classes.");
        pageSubtitle.getStyleClass().add("workspace-page-subtitle");

        VBox pageHeader = new VBox(3, pageTitle, pageSubtitle);

        HBox analyticsRow = new HBox(16);
        analyticsRow.setMaxWidth(Double.MAX_VALUE);

        VBox submissionCard = createOverviewCard(
                "Submission Rate",
                submissionRate + "%",
                submitted + "/" + total + " submitted"
        );

        VBox pendingCard = createOverviewCard(
                "Pending",
                pendingRate + "%",
                notSubmitted + " not submitted"
        );

        VBox flaggedCard = createOverviewCard(
                "Students Flagged",
                violationRate + "%",
                studentsWithViolations + " student(s)"
        );

        VBox violationCard = createOverviewCard(
                "Total Violations",
                String.valueOf(totalViolations),
                "Recorded violations"
        );

        for (VBox card : List.of(submissionCard, pendingCard, flaggedCard, violationCard)) {
            HBox.setHgrow(card, Priority.ALWAYS);
            card.setMaxWidth(Double.MAX_VALUE);
        }

        analyticsRow.getChildren().addAll(
                submissionCard,
                pendingCard,
                flaggedCard,
                violationCard
        );

        VBox examInfoCard = createLargeOverviewCard(
                "Exam Information",
                List.of(
                        "Status: " + safe(detail.getStatus()),
                        "Schedule: " + formatDateTime(detail.getStartDateTime()) + " - " + formatDateTime(detail.getEndDateTime()),
                        "Time Limit: " + defaultInt(detail.getTimeLimitMinutes()) + " minutes",
                        "Shuffle Questions: " + yesNo(detail.getShuffleQuestions()),
                        "Shuffle Choices: " + yesNo(detail.getShuffleChoices()),
                        "Results Released: " + yesNo(detail.getResultsReleased())
                )
        );

        VBox classCard = createAssignedClassesCard(detail.getAssignedClasses());

        root.getChildren().addAll(
                pageHeader,
                analyticsRow,
                examInfoCard,
                classCard
        );

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("workspace-scroll");

        workspaceContent.getChildren().setAll(scrollPane);
    }

    private VBox createAssignedClassesCard(List<FacultyClassDTO> classes) {
        Label titleLabel = new Label("Assigned Classes");
        titleLabel.getStyleClass().add("workspace-section-title");

        VBox content = new VBox(8);

        if (classes == null || classes.isEmpty()) {
            Label empty = new Label("No assigned classes found.");
            empty.getStyleClass().add("overview-detail-row");
            content.getChildren().add(empty);
        } else {
            for (FacultyClassDTO item : classes) {
                Label row = new Label(
                        safe(item.getCourseCode()) +
                                " • " +
                                safe(item.getCourseDescription()) +
                                " • " +
                                safe(item.getProgramCode()) + " " + item.getYearLevel() + "-" + item.getSectionName() +
                                " • " +
                                defaultLong(item.getEnrolledCount()) +
                                " enrolled"
                );

                row.getStyleClass().add("overview-detail-row");
                row.setWrapText(true);

                content.getChildren().add(row);
            }
        }

        VBox card = new VBox(18, titleLabel, content);
        card.getStyleClass().add("workspace-card");

        return card;
    }

    private VBox createOverviewCard(
            String title,
            String value,
            String subtitle
    ) {

        Label titleLabel = new Label(title);

        String normalized = title.toLowerCase();

        if (normalized.contains("submission")) {
            titleLabel.getStyleClass().add("overview-title-success");

        } else if (normalized.contains("pending")) {
            titleLabel.getStyleClass().add("overview-title-warning");

        } else if (normalized.contains("flagged")
                || normalized.contains("violation")) {

            titleLabel.getStyleClass().add("overview-title-danger");

        } else {
            titleLabel.getStyleClass().add("overview-card-title");
        }

        Label valueLabel = new Label(value);

        if (normalized.contains("submission")) {
            valueLabel.getStyleClass().add("overview-value-success");

        } else if (normalized.contains("pending")) {
            valueLabel.getStyleClass().add("overview-value-warning");

        } else if (normalized.contains("flagged")
                || normalized.contains("violation")) {

            valueLabel.getStyleClass().add("overview-value-danger");

        } else {
            valueLabel.getStyleClass().add("overview-card-value");
        }

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("overview-card-subtitle");

        VBox card = new VBox(
                10,
                titleLabel,
                valueLabel,
                subtitleLabel
        );

        card.getStyleClass().add("overview-card");

        return card;
    }

    private VBox createLargeOverviewCard(
            String title,
            List<String> rows
    ) {

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("workspace-section-title");

        VBox content = new VBox(10);

        for (String row : rows) {

            Label label = new Label(row);

            label.getStyleClass().add("overview-detail-row");

            content.getChildren().add(label);
        }

        VBox card = new VBox(
                18,
                titleLabel,
                content
        );

        card.getStyleClass().add("workspace-card");

        return card;
    }

    public void openWorkspaceFromDashboard(Long examId) {
        if (examId == null) {
            return;
        }

        Task<ExamResponse> task = new Task<>() {
            @Override
            protected ExamResponse call() throws Exception {
                return examApiService.getExamPreview(examId);
            }
        };

        task.setOnSucceeded(event -> {
            ExamResponse exam = task.getValue();

            if (exam == null) {
                showError("Exam not found.");
                return;
            }

            ExamRow row = mapToExamRow(exam);

            if (examRows.stream().noneMatch(e -> e.getExamId().equals(row.getExamId()))) {
                examRows.add(row);
            }

            openExamWorkspace(row.getExamId(), row.getTitle(), row.getStatus());
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();
            showError("Unable to open exam workspace.");
        });

        Thread thread = new Thread(task, "open-dashboard-exam-workspace");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void showWorkspaceStudents() {
        setActiveWorkspaceTab(studentsTabButton);

        if (selectedWorkspaceExamId == null) {
            return;
        }

        workspaceContent.getChildren().setAll(
                createLoadingBox("Loading students...")
        );

        Task<List<FacultyExamStudentDTO>> task = new Task<>() {
            @Override
            protected List<FacultyExamStudentDTO> call() throws Exception {
                return examApiService.getExamStudents(selectedWorkspaceExamId);
            }
        };

        task.setOnSucceeded(event -> {
            renderWorkspaceStudents(task.getValue());
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();

            workspaceContent.getChildren().setAll(
                    createLoadingBox("Unable to load students.")
            );
        });

        Thread thread = new Thread(task, "load-exam-students-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void makeTableFillWorkspace(
            VBox root,
            VBox card,
            TableView<?> table
    ) {
        root.setFillWidth(true);
        root.setMaxHeight(Double.MAX_VALUE);

        card.setMaxHeight(Double.MAX_VALUE);
        table.setMaxHeight(Double.MAX_VALUE);

        VBox.setVgrow(card, Priority.ALWAYS);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    private void renderWorkspaceStudents(List<FacultyExamStudentDTO> students) {
        if (students == null) {
            students = List.of();
        }

        long total = students.size();

        long notStarted = students.stream()
                .filter(s -> "NOT_STARTED".equalsIgnoreCase(safe(s.getAttemptStatus())))
                .count();

        long inProgress = students.stream()
                .filter(s ->
                        "IN_PROGRESS".equalsIgnoreCase(safe(s.getAttemptStatus())) ||
                                "STARTED".equalsIgnoreCase(safe(s.getAttemptStatus()))
                )
                .count();

        long submitted = students.stream()
                .filter(s ->
                        "SUBMITTED".equalsIgnoreCase(safe(s.getAttemptStatus())) ||
                                "AUTO_SUBMITTED".equalsIgnoreCase(safe(s.getAttemptStatus()))
                )
                .count();

        long flagged = students.stream()
                .filter(s -> defaultLong(s.getViolationCount()) > 0)
                .count();

        VBox root = new VBox(16);
        root.getStyleClass().add("workspace-overview-root");

        Label pageTitle = new Label("Students");
        pageTitle.getStyleClass().add("workspace-page-title");

        Label pageSubtitle = new Label("Track student attempt status, score, and violation count.");
        pageSubtitle.getStyleClass().add("workspace-page-subtitle");

        VBox pageHeader = new VBox(3, pageTitle, pageSubtitle);

        HBox analyticsRow = new HBox(16);
        analyticsRow.setMaxWidth(Double.MAX_VALUE);

        VBox totalCard = createOverviewCard("Total Students", String.valueOf(total), "Assigned students");
        VBox notStartedCard = createOverviewCard("Not Started", String.valueOf(notStarted), "No attempt yet");
        VBox inProgressCard = createOverviewCard("In Progress", String.valueOf(inProgress), "Currently taking");
        VBox submittedCard = createOverviewCard("Submitted", String.valueOf(submitted), "Completed attempts");
        VBox flaggedCard = createOverviewCard("Flagged", String.valueOf(flagged), "With violations");

        for (VBox card : List.of(totalCard, notStartedCard, inProgressCard, submittedCard, flaggedCard)) {
            HBox.setHgrow(card, Priority.ALWAYS);
            card.setMaxWidth(Double.MAX_VALUE);
        }

        analyticsRow.getChildren().addAll(
                totalCard,
                notStartedCard,
                inProgressCard,
                submittedCard,
                flaggedCard
        );

        ObservableList<FacultyExamStudentDTO> studentRows =
                FXCollections.observableArrayList(students);

        FilteredList<FacultyExamStudentDTO> filteredStudents =
                new FilteredList<>(studentRows, s -> true);

        TextField studentSearchField = new TextField();
        studentSearchField.setPromptText("Search student name, ID, program, or section...");
        studentSearchField.getStyleClass().add("workspace-search-field");
        studentSearchField.setPrefWidth(360);

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll(
                "All",
                "Not Started",
                "In Progress",
                "Submitted",
                "Auto Submitted",
                "Flagged"
        );
        statusFilter.setValue("All");
        statusFilter.getStyleClass().add("workspace-filter-box");

        Runnable applyStudentFilter = () -> {
            String keyword = studentSearchField.getText() == null
                    ? ""
                    : studentSearchField.getText().toLowerCase().trim();

            String selectedStatus = statusFilter.getValue();

            filteredStudents.setPredicate(student -> {
                boolean matchesSearch =
                        keyword.isBlank()
                                || contains(student.getStudentName(), keyword)
                                || contains(student.getStudentId(), keyword)
                                || contains(student.getProgramCode(), keyword)
                                || contains(student.getSectionName(), keyword);

                if (!matchesSearch) {
                    return false;
                }

                String status = safe(student.getAttemptStatus()).replace("_", " ");
                long violations = defaultLong(student.getViolationCount());

                if ("All".equalsIgnoreCase(selectedStatus)) {
                    return true;
                }

                if ("Flagged".equalsIgnoreCase(selectedStatus)) {
                    return violations > 0;
                }

                return status.equalsIgnoreCase(selectedStatus);
            });
        };

        studentSearchField.textProperty().addListener((obs, oldValue, newValue) ->
                applyStudentFilter.run()
        );

        statusFilter.valueProperty().addListener((obs, oldValue, newValue) ->
                applyStudentFilter.run()
        );

        HBox studentToolbar = new HBox(10, studentSearchField, statusFilter);
        studentToolbar.setAlignment(Pos.CENTER_LEFT);

        TableView<FacultyExamStudentDTO> table = new TableView<>();
        table.getStyleClass().add("workspace-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(filteredStudents);

        TableColumn<FacultyExamStudentDTO, String> studentColumn = new TableColumn<>("Student");
        studentColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        safe(data.getValue().getStudentName())
                )
        );

        TableColumn<FacultyExamStudentDTO, String> idColumn = new TableColumn<>("Student ID");
        idColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        safe(data.getValue().getStudentId())
                )
        );

        TableColumn<FacultyExamStudentDTO, String> programColumn = new TableColumn<>("Program");
        programColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        safe(data.getValue().getProgramCode())
                )
        );

        TableColumn<FacultyExamStudentDTO, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        formatStatus(safe(data.getValue().getAttemptStatus()))
                )
        );

        TableColumn<FacultyExamStudentDTO, String> scoreColumn = new TableColumn<>("Score");
        scoreColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getScorePercentage() == null
                                ? "—"
                                : String.format("%.0f%%", data.getValue().getScorePercentage())
                )
        );

        TableColumn<FacultyExamStudentDTO, String> violationColumn = new TableColumn<>("Violations");
        violationColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        String.valueOf(defaultLong(data.getValue().getViolationCount()))
                )
        );

        TableColumn<FacultyExamStudentDTO, String> submittedColumn = new TableColumn<>("Submitted At");
        submittedColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        formatDateTime(data.getValue().getSubmittedAt())
                )
        );

        TableColumn<FacultyExamStudentDTO, String> reviewColumn =
                new TableColumn<>("Status");

        reviewColumn.setCellValueFactory(data -> {

            FacultyExamStudentDTO row = data.getValue();

            String attemptStatus = safe(row.getAttemptStatus());
            String reviewStatus = safe(row.getReviewStatus());

            if ("NOT_STARTED".equalsIgnoreCase(attemptStatus)) {
                return new javafx.beans.property.SimpleStringProperty(
                        "NOT STARTED"
                );
            }

            if ("REVIEWED".equalsIgnoreCase(reviewStatus)) {
                return new javafx.beans.property.SimpleStringProperty(
                        "REVIEWED"
                );
            }

            if (Boolean.TRUE.equals(row.getNeedsChecking())) {
                return new javafx.beans.property.SimpleStringProperty(
                        "NEEDS REVIEW"
                );
            }

            return new javafx.beans.property.SimpleStringProperty(
                    "CLEAR"
            );
        });

        TableColumn<FacultyExamStudentDTO, Void> actionColumn =
                new TableColumn<>("Action");

        actionColumn.setCellFactory(col -> new TableCell<>() {

            private final Button reviewButton = new Button("Review");

            {
                reviewButton.getStyleClass().add("workspace-review-button");

                reviewButton.setOnAction(event -> {

                    FacultyExamStudentDTO row =
                            getTableView().getItems().get(getIndex());

                    openStudentReview(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                FacultyExamStudentDTO row =
                        getTableView().getItems().get(getIndex());

                boolean submitted =
                        "SUBMITTED".equalsIgnoreCase(
                                safe(row.getAttemptStatus())
                        ) ||
                                "AUTO_SUBMITTED".equalsIgnoreCase(
                                        safe(row.getAttemptStatus())
                                );

                boolean reviewed =
                        "REVIEWED".equalsIgnoreCase(
                                safe(row.getReviewStatus())
                        );

                if (!submitted) {

                    reviewButton.setText("Waiting");
                    reviewButton.setDisable(true);

                } else if (reviewed) {

                    reviewButton.setText("View");
                    reviewButton.setDisable(false);

                } else if (Boolean.TRUE.equals(row.getNeedsChecking())) {

                    reviewButton.setText("Review");
                    reviewButton.setDisable(false);

                } else {

                    reviewButton.setText("View");
                    reviewButton.setDisable(false);
                }

                setGraphic(reviewButton);
            }
        });

        table.getColumns().addAll(
                studentColumn,
                idColumn,
                programColumn,
                statusColumn,
                scoreColumn,
                violationColumn,
                submittedColumn,
                reviewColumn,
                actionColumn
        );

        VBox tableCard = new VBox(14, studentToolbar, table);
        tableCard.getStyleClass().add("workspace-card");
        VBox.setVgrow(table, Priority.ALWAYS);


        root.getChildren().addAll(
                pageHeader,
                analyticsRow,
                tableCard
        );

        root.setMaxHeight(Double.MAX_VALUE);
        tableCard.setMaxHeight(Double.MAX_VALUE);
        table.setMaxHeight(Double.MAX_VALUE);

        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        StackPane.setAlignment(root, Pos.TOP_LEFT);
        workspaceContent.getChildren().setAll(root);

        makeTableFillWorkspace(root, tableCard, table);
    }

    private void openStudentReview(FacultyExamStudentDTO student) {
        if (student == null || selectedWorkspaceExamId == null) {
            return;
        }
        currentReviewStudent = student;

        workspaceContent.getChildren().setAll(
                createLoadingBox("Loading student answers...")
        );

        Task<FacultyAttemptReviewResponse> task = new Task<>() {
            @Override
            protected FacultyAttemptReviewResponse call() throws Exception {
                return examApiService.getStudentAttemptReview(
                        selectedWorkspaceExamId,
                        student.getStudentId()
                );
            }
        };

        task.setOnSucceeded(event -> {
            renderStudentReview(task.getValue());
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();

            workspaceContent.getChildren().setAll(
                    createLoadingBox("Unable to load student review.")
            );
        });

        Thread thread = new Thread(task, "load-student-review-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void renderStudentReview(FacultyAttemptReviewResponse review) {
        this.currentAttemptReview = review;

        if (review == null) {
            workspaceContent.getChildren().setAll(
                    createLoadingBox("No review data found.")
            );
            return;
        }

        Button backButton = new Button("← Back to Students");
        Button markReviewedButton = new Button("Mark as Reviewed");
        markReviewedButton.getStyleClass().add("review-complete-button");

        markReviewedButton.setOnAction(e -> {
            if (review.getAttemptId() == null) {
                showError("Attempt ID not found.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Complete Review");
            confirm.setHeaderText("Mark this attempt as reviewed?");
            confirm.setContentText(
                    "This will mark the student's attempt as reviewed. " +
                            "You can still reopen or adjust scores later if needed."
            );

            Optional<ButtonType> result = confirm.showAndWait();

            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    examApiService.markAttemptReviewed(review.getAttemptId());
                    return null;
                }
            };

            task.setOnSucceeded(event -> {
                markReviewedButton.setText("✔ Reviewed");
                markReviewedButton.setDisable(true);
                showWorkspaceStudents();
            });

            task.setOnFailed(event -> {
                Throwable ex = task.getException();
                if (ex != null) ex.printStackTrace();
                showError("Unable to mark attempt as reviewed.");
            });

            Thread thread = new Thread(task, "mark-reviewed-thread");
            thread.setDaemon(true);
            thread.start();
        });
        backButton.getStyleClass().add("outline-button");
        backButton.setOnAction(e -> showWorkspaceStudents());

        Label title = new Label("Review Student Attempt");
        title.getStyleClass().add("workspace-page-title");

        Label subtitle = new Label(
                safe(review.getStudentName()) +
                        " • " +
                        safe(review.getStudentId()) +
                        " • " +
                        formatStatus(safe(review.getAttemptStatus())) +
                        " • Score: " +
                        (review.getScorePercentage() == null ? "—" : String.format("%.0f%%", review.getScorePercentage()))
        );
        subtitle.getStyleClass().add("workspace-page-subtitle");

        VBox answerList = new VBox(14);

        ComboBox<String> answerFilter = new ComboBox<>();
        answerFilter.getItems().addAll(
                "All Questions",
                "Needs Review",
                "Reviewed",
                "Correct",
                "Incorrect",
                "With Violations",
                "Essay Only"
        );
        answerFilter.setValue("All Questions");
        answerFilter.getStyleClass().add("workspace-filter-box");

        Runnable renderFilteredAnswers = () -> {
            answerList.getChildren().clear();

            List<ExamAttemptAnswerReviewDTO> answers =
                    review.getAnswers() == null ? List.of() : review.getAnswers();

            String selected = answerFilter.getValue();

            List<ExamAttemptAnswerReviewDTO> filtered = answers.stream()
                    .filter(answer -> matchesAnswerReviewFilter(answer, selected))
                    .toList();

            if (filtered.isEmpty()) {
                answerList.getChildren().add(
                        createLoadingBox("No answers found for this filter.")
                );
                return;
            }

            for (ExamAttemptAnswerReviewDTO answer : filtered) {
                answerList.getChildren().add(createAnswerReviewCard(answer));
            }
        };

        answerFilter.valueProperty().addListener((obs, oldVal, newVal) ->
                renderFilteredAnswers.run()
        );

        renderFilteredAnswers.run();

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox headerActions = new HBox(10, backButton, headerSpacer, markReviewedButton);
        headerActions.setAlignment(Pos.CENTER_LEFT);

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        VBox titleBox = new VBox(3, title, subtitle);

        HBox studentHeaderRow = new HBox(12, titleBox, titleSpacer, answerFilter);
        studentHeaderRow.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(
                16,
                headerActions,
                studentHeaderRow,
                answerList
        );

        root.getStyleClass().add("workspace-overview-root");

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("workspace-scroll");

        workspaceContent.getChildren().setAll(scrollPane);
    }

    private boolean matchesAnswerReviewFilter(
            ExamAttemptAnswerReviewDTO answer,
            String selected
    ) {
        if (answer == null || selected == null) {
            return true;
        }

        boolean reviewed = Boolean.TRUE.equals(answer.getManuallyReviewed())
                || "REVIEWED".equalsIgnoreCase(safe(answer.getReviewStatus()));

        boolean hasViolations = answer.getViolations() != null
                && !answer.getViolations().isEmpty();

        boolean needsReview = !reviewed &&
                (
                        Boolean.TRUE.equals(answer.getNeedsChecking())
                                || Boolean.TRUE.equals(answer.getNeedsManualCheck())
                                || "PENDING".equalsIgnoreCase(safe(answer.getReviewStatus()))
                                || "FLAGGED".equalsIgnoreCase(safe(answer.getReviewStatus()))
                                || hasViolations
                );

        return switch (selected) {
            case "Needs Review" -> needsReview;
            case "Reviewed" -> reviewed;
            case "Correct" -> Boolean.TRUE.equals(answer.getCorrect());
            case "Incorrect" -> Boolean.FALSE.equals(answer.getCorrect());
            case "With Violations" -> hasViolations;
            case "Essay Only" -> "ESSAY".equalsIgnoreCase(safe(answer.getQuestionType()));
            default -> true;
        };
    }

    private VBox    createAnswerReviewCard(ExamAttemptAnswerReviewDTO answer) {
        Label questionHeader = new Label();
        questionHeader.setText(
                "Question " + defaultInt(answer.getQuestionNumber()) +
                        " • " + formatStatus(safe(answer.getQuestionType())) +
                        " • " + formatPoints(answer.getEarnedPoints()) +
                        " / " + formatPoints(answer.getPoints()) + " pts"
        );
        questionHeader.getStyleClass().add("review-question-header");

        Label resultBadge = createAnswerResultBadge(answer);
        Label reviewBadge = createReviewStatusBadge(answer);

        HBox badgeBox = new HBox(8, resultBadge, reviewBadge);
        badgeBox.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox(12, questionHeader, spacer, badgeBox);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label questionText = new Label(safe(answer.getQuestionText()));
        questionText.setWrapText(true);
        questionText.getStyleClass().add("review-question-text");

        VBox questionBox = new VBox(10, questionText);
        questionBox.getStyleClass().add("review-question-panel");
        questionBox.setMaxWidth(Double.MAX_VALUE);

        if (answer.getQuestionImageUrl() != null && !answer.getQuestionImageUrl().isBlank()) {
            ImageView img = new ImageView(
                    new Image(FacultyApiService.BASE_URL + answer.getQuestionImageUrl(), true)
            );
            img.setFitWidth(320);
            img.setPreserveRatio(true);
            img.getStyleClass().add("review-question-image");
            questionBox.getChildren().add(img);
        }

        VBox correctAnswerBlock = createAnswerBlock(
                "Correct Answer",
                safe(answer.getCorrectAnswer()),
                "review-correct-answer"
        );

        if (answer.getCorrectAnswerImageUrl() != null && !answer.getCorrectAnswerImageUrl().isBlank()) {
            ImageView img = new ImageView(
                    new Image(FacultyApiService.BASE_URL + answer.getCorrectAnswerImageUrl(), true)
            );
            img.setFitWidth(240);
            img.setPreserveRatio(true);
            img.getStyleClass().add("review-image");
            correctAnswerBlock.getChildren().add(img);
        }

        boolean essayQuestion =
                "ESSAY".equalsIgnoreCase(
                        safe(answer.getQuestionType())
                );

        String studentAnswerStyle;

        if (essayQuestion) {

            studentAnswerStyle = "review-answer-neutral";

        } else {

            studentAnswerStyle =
                    Boolean.TRUE.equals(answer.getCorrect())
                            ? "review-answer-correct"
                            : "review-answer-wrong";
        }

        VBox studentAnswerBlock = createAnswerBlock(
                "Student Answer",
                safe(answer.getStudentAnswer()),
                studentAnswerStyle
        );

        if (answer.getStudentAnswerImageUrl() != null && !answer.getStudentAnswerImageUrl().isBlank()) {
            ImageView img = new ImageView(
                    new Image(FacultyApiService.BASE_URL + answer.getStudentAnswerImageUrl(), true)
            );
            img.setFitWidth(240);
            img.setPreserveRatio(true);
            img.getStyleClass().add("review-image");
            studentAnswerBlock.getChildren().add(img);
        }

        HBox contentRow;

        if (essayQuestion) {

            contentRow = new HBox(
                    16,
                    questionBox,
                    studentAnswerBlock
            );

            questionBox.setPrefWidth(420);
            studentAnswerBlock.setPrefWidth(620);

            HBox.setHgrow(questionBox, Priority.ALWAYS);
            HBox.setHgrow(studentAnswerBlock, Priority.ALWAYS);

        } else {

            contentRow = new HBox(
                    16,
                    questionBox,
                    correctAnswerBlock,
                    studentAnswerBlock
            );

            questionBox.setPrefWidth(420);
            correctAnswerBlock.setPrefWidth(300);
            studentAnswerBlock.setPrefWidth(300);

            HBox.setHgrow(questionBox, Priority.ALWAYS);
            HBox.setHgrow(correctAnswerBlock, Priority.ALWAYS);
            HBox.setHgrow(studentAnswerBlock, Priority.ALWAYS);
        }

        contentRow.setAlignment(Pos.TOP_LEFT);
        contentRow.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(questionBox, Priority.ALWAYS);
        HBox.setHgrow(correctAnswerBlock, Priority.ALWAYS);
        HBox.setHgrow(studentAnswerBlock, Priority.ALWAYS);

        questionBox.setPrefWidth(420);
        correctAnswerBlock.setPrefWidth(300);
        studentAnswerBlock.setPrefWidth(300);

        VBox violationBox = createViolationSummaryBox(answer);
        VBox essayRubricReviewBox = createEssayRubricReviewBox(answer);

        HBox actionBar = new HBox(10);
        actionBar.setAlignment(Pos.CENTER_RIGHT);
        actionBar.setFocusTraversable(true);

        boolean identificationQuestion =
                "IDENTIFICATION".equalsIgnoreCase(
                        safe(answer.getQuestionType())
                );

        boolean canSaveScore =
                identificationQuestion &&
                        !Boolean.TRUE.equals(answer.getCorrect());

        if (canSaveScore) {

            Label pointsLabel = new Label("Points Earned");
            pointsLabel.getStyleClass().add("review-answer-label");

            TextField scoreField = new TextField(formatPoints(answer.getEarnedPoints()));
            scoreField.getStyleClass().add("review-score-field");
            scoreField.setPrefWidth(90);

            Label outOfLabel = new Label("/ " + formatPoints(answer.getPoints()) + " pts");
            outOfLabel.getStyleClass().add("review-score-total");

            Button saveScoreButton = new Button("✔ Update Score");
            saveScoreButton.getStyleClass().add("review-save-score-button");

            saveScoreButton.setOnAction(event -> {

                double enteredScore;

                try {

                    enteredScore =
                            Double.parseDouble(scoreField.getText().trim());

                    double maxScore =
                            answer.getPoints() == null
                                    ? 0
                                    : answer.getPoints().doubleValue();

                    if (enteredScore < 0) {
                        showError("Score cannot be negative.");
                        return;
                    }

                    if (enteredScore > maxScore) {
                        showError(
                                "Score cannot exceed " +
                                        formatPoints(answer.getPoints()) +
                                        " points."
                        );
                        return;
                    }

                } catch (NumberFormatException ex) {

                    showError("Please enter a valid numeric score.");
                    return;
                }

                actionBar.requestFocus();
                saveScoreButton.setDisable(true);
                saveScoreButton.setText("Updating...");

                double finalEnteredScore = enteredScore;

                Task<Void> saveTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {

                        examApiService.updateAnswerScore(
                                answer.getAnswerId(),
                                finalEnteredScore
                        );

                        return null;
                    }
                };

                saveTask.setOnSucceeded(e -> {

                    answer.setEarnedPoints( BigDecimal.valueOf(finalEnteredScore) );
                    questionHeader.setText(
                            "Question " + defaultInt(answer.getQuestionNumber()) +
                                    " • " + formatStatus(safe(answer.getQuestionType())) +
                                    " • " + formatPoints(answer.getEarnedPoints()) +
                                    " / " + formatPoints(answer.getPoints()) + " pts"
                    );
                    answer.setManuallyReviewed(true);
                    answer.setNeedsChecking(false);
                    answer.setReviewStatus("REVIEWED");

                    saveScoreButton.setText("✔ Updated");
                    saveScoreButton.setDisable(false);

                    reviewBadge.setText("✔ Reviewed");

                    reviewBadge.getStyleClass().clear();
                    reviewBadge.getStyleClass().add("review-status-correct");

                    resultBadge.setText(
                            finalEnteredScore > 0
                                    ? "✔ Correct"
                                    : "✖ Incorrect"
                    );

                    resultBadge.getStyleClass().clear();

                    resultBadge.getStyleClass().add(
                            finalEnteredScore > 0
                                    ? "review-status-correct"
                                    : "review-status-wrong"
                    );

                    Platform.runLater(() -> {
                        scoreField.requestFocus();
                        scoreField.positionCaret(scoreField.getText().length());
                    });
                });

                saveTask.setOnFailed(e -> {

                    Throwable ex = saveTask.getException();

                    if (ex != null) {
                        ex.printStackTrace();
                    }

                    saveScoreButton.setDisable(false);
                    saveScoreButton.setText("✔ Update Score");
                    saveScoreButton.setFocusTraversable(false);

                    showError("Unable to save score.");
                });

                Thread thread = new Thread(saveTask);
                thread.setDaemon(true);
                thread.start();
            });

            HBox scoreBox = new HBox(
                    8,
                    pointsLabel,
                    scoreField,
                    outOfLabel,
                    saveScoreButton
            );
            scoreBox.getStyleClass().add("review-score-box");
            scoreBox.setAlignment(Pos.CENTER_RIGHT);

            actionBar.getChildren().add(scoreBox);
        }

        VBox card = new VBox(
                14,
                topRow,
                contentRow,
                violationBox
        );

        if (essayRubricReviewBox != null) {
            card.getChildren().add(essayRubricReviewBox);
        }

        if (!actionBar.getChildren().isEmpty()) {
            card.getChildren().add(actionBar);
        }

        card.getStyleClass().add("workspace-card");
        card.setMaxWidth(Double.MAX_VALUE);

        return card;
    }

    private VBox createAnswerBlock(String label, String value, String valueStyleClass) {
        Label title = new Label(label);
        title.getStyleClass().add("review-answer-label");

        Label content = new Label(value);
        content.setWrapText(true);
        content.getStyleClass().add(valueStyleClass);
        content.setMaxWidth(Double.MAX_VALUE);

        VBox box = new VBox(5, title, content);
        box.setMaxWidth(Double.MAX_VALUE);

        return box;
    }

    private Label createAnswerResultBadge(ExamAttemptAnswerReviewDTO answer) {
        Label badge = new Label();

        boolean essayQuestion =
                "ESSAY".equalsIgnoreCase(
                        safe(answer.getQuestionType())
                );

        if (essayQuestion) {
            badge.setText("Manual Review");
            badge.getStyleClass().add("review-status-neutral");
            return badge;
        }

        double earned = answer.getEarnedPoints() == null
                ? 0
                : answer.getEarnedPoints().doubleValue();

        double max = answer.getPoints() == null
                ? 0
                : answer.getPoints().doubleValue();

        if (max > 0 && earned >= max) {
            badge.setText("✔ Correct");
            badge.getStyleClass().add("review-status-correct");

        } else if (earned <= 0) {
            badge.setText("✖ Incorrect");
            badge.getStyleClass().add("review-status-wrong");

        } else {
            badge.setText("◐ Partial");
            badge.getStyleClass().add("review-status-partial");
        }

        return badge;
    }

    private Label createReviewStatusBadge(ExamAttemptAnswerReviewDTO answer) {
        Label badge = new Label();

        boolean reviewed = Boolean.TRUE.equals(answer.getManuallyReviewed())
                || "REVIEWED".equalsIgnoreCase(safe(answer.getReviewStatus()));

        boolean hasViolations = answer.getViolations() != null
                && !answer.getViolations().isEmpty();

        String status = safe(answer.getReviewStatus());

        if (reviewed) {
            badge.setText("✔ Reviewed");
            badge.getStyleClass().add("review-status-correct");

        } else if ("PENDING".equalsIgnoreCase(status)
                || Boolean.TRUE.equals(answer.getNeedsManualCheck())) {
            badge.setText("⚠ Pending Review");
            badge.getStyleClass().add("review-status-warning");

        } else if ("FLAGGED".equalsIgnoreCase(status) || hasViolations) {
            badge.setText("⚠ Flagged");
            badge.getStyleClass().add("review-status-warning");

        } else if ("AUTO_CHECKED".equalsIgnoreCase(status)) {
            badge.setText("Auto Checked");
            badge.getStyleClass().add("review-status-neutral");

        } else {
            badge.setText("N/A");
            badge.getStyleClass().add("review-status-neutral");
        }

        return badge;
    }

    private VBox createViolationSummaryBox(ExamAttemptAnswerReviewDTO answer) {
        VBox box = new VBox(8);

        List<ExamAttemptViolationDTO> violations = answer.getViolations();

        if (violations == null || violations.isEmpty()) {
            Label clear = new Label("No question-level violations.");
            clear.getStyleClass().add("review-clear-text");
            box.getChildren().add(clear);
            return box;
        }

        Label title = new Label("⚠ " + violations.size() + " Violation(s) Detected");
        title.getStyleClass().add("review-violation-title");

        String reviewStatus = violations.stream()
                .map(ExamAttemptViolationDTO::getReviewStatus)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("PENDING_REVIEW");

        Button reviewButton = new Button();

        boolean reviewed =
                "IGNORED".equalsIgnoreCase(reviewStatus) ||
                        "PENALIZED".equalsIgnoreCase(reviewStatus) ||
                        "REVIEWED".equalsIgnoreCase(reviewStatus);

        if (reviewed) {
            String label = switch (reviewStatus.toUpperCase()) {
                case "IGNORED" -> "Ignored";
                case "PENALIZED" -> "Penalized";
                default -> "Reviewed";
            };

            reviewButton.setText(label);
            reviewButton.setDisable(true);
            reviewButton.getStyleClass().add("workspace-status-button");

        } else {
            reviewButton.setText("Review Evidence");
            reviewButton.setDisable(false);
            reviewButton.getStyleClass().add("workspace-review-button");
            reviewButton.setOnAction(e -> openViolationReviewModal(answer));
        }

        HBox row = new HBox(10, title, reviewButton);
        row.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().add(row);
        box.getStyleClass().add("review-violation-summary");

        return box;
    }

    private void openViolationReviewModal(ExamAttemptAnswerReviewDTO answer) {
        List<ExamAttemptViolationDTO> violations = answer.getViolations();

        if (violations == null || violations.isEmpty()) {
            showError("No violations found for this question.");
            return;
        }

        final int[] index = {0};

        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Violation Evidence");

        Label title = new Label(
                "Question " + defaultInt(answer.getQuestionNumber()) +
                        " • " + formatStatus(safe(answer.getQuestionType()))
        );
        title.getStyleClass().add("workspace-page-title");

        StackPane previewPane = new StackPane();
        previewPane.getStyleClass().add("violation-preview-pane");
        VBox.setVgrow(previewPane, Priority.ALWAYS);

        previewPane.setMinHeight(320);
        previewPane.setMaxHeight(Double.MAX_VALUE);

        Label metadataLabel = new Label();
        metadataLabel.getStyleClass().add("review-violation-row");

        Label counterLabel = new Label();
        counterLabel.getStyleClass().add("review-score-total");

        Button previousButton = new Button("← Previous");
        previousButton.getStyleClass().add("outline-button");

        Button nextButton = new Button("Next →");
        nextButton.getStyleClass().add("outline-button");

        Runnable renderViolation = () -> {
            ExamAttemptViolationDTO violation = violations.get(index[0]);

            previewPane.getChildren().clear();

            Label placeholder = new Label("Screenshot / camera evidence preview");
            placeholder.getStyleClass().add("workspace-empty-text");
            previewPane.getChildren().add(placeholder);

            metadataLabel.setText(
                    safe(violation.getSeverity()) +
                            " • " +
                            safe(violation.getViolationType()) +
                            " • " +
                            formatDateTime(violation.getOccurredAt())
            );

            counterLabel.setText((index[0] + 1) + " / " + violations.size());

            previousButton.setDisable(index[0] == 0);
            nextButton.setDisable(index[0] == violations.size() - 1);
        };

        previousButton.setOnAction(e -> {
            if (index[0] > 0) {
                index[0]--;
                renderViolation.run();
            }
        });

        nextButton.setOnAction(e -> {
            if (index[0] < violations.size() - 1) {
                index[0]++;
                renderViolation.run();
            }
        });

        HBox navigation = new HBox(12, previousButton, counterLabel, nextButton);
        navigation.setAlignment(Pos.CENTER);

        TextArea feedbackArea = new TextArea();
        feedbackArea.setPromptText("Enter feedback about this violation...");
        feedbackArea.setPrefRowCount(4);
        feedbackArea.getStyleClass().add("essay-feedback-area");

        Label currentScoreLabel = new Label(
                "Current Question Score: " +
                        formatPoints(answer.getEarnedPoints()) +
                        " / " +
                        formatPoints(answer.getPoints()) +
                        " pts"
        );
        currentScoreLabel.getStyleClass().add("review-score-total");

        TextField deductionField = new TextField("0");
        deductionField.setPrefWidth(90);
        deductionField.getStyleClass().add("review-score-field");

        Button ignoreButton = new Button("Ignore Violations");
        ignoreButton.getStyleClass().add("outline-button");

        Button applyButton = new Button("Apply Deduction");
        applyButton.getStyleClass().add("danger-button");

        ignoreButton.setOnAction(e -> {

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Ignore Violations");
            confirm.setHeaderText("Ignore all reviewed violations?");
            confirm.setContentText(
                    "This will keep the student's current score unchanged."
            );

            Optional<ButtonType> result = confirm.showAndWait();

            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }

            try {
                String decision = "IGNORED";

                examApiService.applyViolationDecision(
                        answer.getAnswerId(),
                        answer.getQuestionId(),
                        currentAttemptReview.getAttemptId(),
                        decision,
                        BigDecimal.ZERO,
                        feedbackArea.getText()
                );

                for (ExamAttemptViolationDTO violation : answer.getViolations()) {
                    violation.setReviewStatus(decision);
                    violation.setReviewedAt(OffsetDateTime.now());
                }

                answer.setManuallyReviewed(true);
                answer.setNeedsChecking(false);
                answer.setReviewStatus("REVIEWED");

                stage.close();

                if (currentAttemptReview != null) {
                    renderStudentReview(currentAttemptReview);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Unable to ignore violations.");
            }
        });

        applyButton.setOnAction(e -> {
            try {
                double deduction = Double.parseDouble(deductionField.getText().trim());

                double currentScore = answer.getEarnedPoints() == null
                        ? 0
                        : answer.getEarnedPoints().doubleValue();

                if (deduction < 0) {
                    showError("Deduction cannot be negative.");
                    return;
                }

                if (deduction > currentScore) {
                    showError("Deduction cannot be greater than current score.");
                    return;
                }

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);

                confirm.setTitle("Apply Deduction");
                confirm.setHeaderText("Apply score deduction?");
                confirm.setContentText(
                        "Current Score: " + formatPoints(answer.getEarnedPoints()) +
                                "\nDeduction: " + deduction +
                                "\nFinal Score: " + formatPoints(
                                BigDecimal.valueOf(currentScore - deduction)
                        )
                );

                Optional<ButtonType> result = confirm.showAndWait();

                if (result.isEmpty() || result.get() != ButtonType.OK) {
                    return;
                }

                double finalScore = currentScore - deduction;

                String decision = "PENALIZED";

                examApiService.applyViolationDecision(
                        answer.getAnswerId(),
                        answer.getQuestionId(),
                        currentAttemptReview.getAttemptId(),
                        decision,
                        BigDecimal.valueOf(deduction),
                        feedbackArea.getText()
                );

                answer.setEarnedPoints(BigDecimal.valueOf(finalScore));
                answer.setManuallyReviewed(true);
                answer.setNeedsChecking(false);
                answer.setReviewStatus("REVIEWED");

                for (ExamAttemptViolationDTO violation : answer.getViolations()) {
                    violation.setReviewStatus(decision);
                    violation.setReviewedAt(OffsetDateTime.now());
                }

                stage.close();

                if (currentAttemptReview != null) {
                    renderStudentReview(currentAttemptReview);
                }

            } catch (NumberFormatException ex) {
                showError("Please enter a valid deduction.");
            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Unable to apply deduction.");
            }
        });

        HBox decisionButtons = new HBox(10, ignoreButton, applyButton);
        decisionButtons.setAlignment(Pos.CENTER_RIGHT);

        Label decisionTitle = new Label("Final Decision");
        decisionTitle.getStyleClass().add("workspace-section-title");

        VBox decisionBox = new VBox(
                10,
                decisionTitle,
                feedbackArea,
                currentScoreLabel,
                new HBox(8, new Label("Deduction:"), deductionField),
                decisionButtons
        );
        decisionBox.getStyleClass().add("violation-decision-box");

        VBox root = new VBox(
                14,
                title,
                previewPane,
                metadataLabel,
                navigation,
                decisionBox
        );
        root.setMinHeight(850);
        root.setPrefHeight(Region.USE_COMPUTED_SIZE);
        root.setMaxHeight(Double.MAX_VALUE);

        root.setFillWidth(true);

        VBox.setVgrow(previewPane, Priority.ALWAYS);
        root.setPadding(new Insets(22));
        root.getStyleClass().add("exam-workspace-page");

        renderViolation.run();

        Scene scene = new Scene(root, 1050, 850);
        scene.getStylesheets().add(
                getClass().getResource("/styles/exam-management.css").toExternalForm()
        );

        stage.setScene(scene);

        if (workspaceContent.getScene() != null) {
            stage.initOwner(
                    workspaceContent.getScene().getWindow()
            );
        }

        stage.show();
    }

    @FXML
    private void showWorkspaceProctor() {
        setActiveWorkspaceTab(proctorTabButton);
        renderWorkspaceProctor();
    }

    private void renderWorkspaceProctor() {

        VBox root = new VBox(16);
        root.getStyleClass().add("workspace-overview-root");

        Label pageTitle = new Label("Live Proctoring");
        pageTitle.getStyleClass().add("workspace-page-title");

        Label pageSubtitle = new Label(
                "Zoom-style monitoring wall for active takers, live camera streams, and risk indicators."
        );
        pageSubtitle.getStyleClass().add("workspace-page-subtitle");

        VBox pageHeader = new VBox(3, pageTitle, pageSubtitle);

        TextField searchField = new TextField();
        searchField.setPromptText("Search student, ID, section...");
        searchField.getStyleClass().add("workspace-search-field");
        searchField.setPrefWidth(320);

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All Status", "Live", "Warning", "Critical", "Disconnected", "Submitted");
        statusFilter.setValue("All Status");
        statusFilter.getStyleClass().add("workspace-filter-box");

        ComboBox<String> sortFilter = new ComboBox<>();
        sortFilter.getItems().addAll("Sort by Risk", "Sort by Name", "Sort by Latest Activity");
        sortFilter.setValue("Sort by Risk");
        sortFilter.getStyleClass().add("workspace-filter-box");

        HBox filterBar = new HBox(10, searchField, statusFilter, sortFilter);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        TilePane streamGrid = new TilePane();

        streamGrid.setPrefColumns(4);

        streamGrid.setHgap(14);
        streamGrid.setVgap(14);

        streamGrid.setTileAlignment(Pos.TOP_CENTER);
        streamGrid.setAlignment(Pos.TOP_CENTER);

        streamGrid.getStyleClass().add("proctor-stream-grid");

        streamGrid.getChildren().addAll(
                createProctorStreamTile(
                        "Juan Dela Cruz",
                        "2024-14887-MN-0",
                        "BSITOUMN 2-2",
                        "Q5",
                        "42:18",
                        "Connected",
                        "Look Away • Minor",
                        "WARNING"
                ),
                createProctorStreamTile(
                        "Maria Santos",
                        "2024-20491-MN-0",
                        "BSITOUMN 2-2",
                        "Q3",
                        "45:09",
                        "Connected",
                        "No recent violations",
                        "NORMAL"
                ),
                createProctorStreamTile(
                        "Carlo Reyes",
                        "2024-11220-MN-0",
                        "BSITOUMN 2-3",
                        "Q7",
                        "31:44",
                        "Disconnected",
                        "Camera disconnected",
                        "CRITICAL"
                ),
                createProctorStreamTile(
                        "Ana Lopez",
                        "2024-11920-MN-0",
                        "BSITOUMN 2-3",
                        "Done",
                        "00:00",
                        "Connected",
                        "Submitted",
                        "SUBMITTED"
                )
        );

        VBox card = new VBox(14, pageHeader, filterBar, streamGrid);
        card.getStyleClass().add("workspace-card");

        root.getChildren().add(card);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("workspace-scroll");

        workspaceContent.getChildren().setAll(scrollPane);
    }

    private VBox createProctorStreamTile(
            String studentName,
            String studentId,
            String section,
            String currentQuestion,
            String timeRemaining,
            String cameraStatus,
            String latestViolation,
            String riskLevel
    ) {
        Label nameLabel = new Label(studentName);
        nameLabel.getStyleClass().add("proctor-header-name");

        Label metaLabel = new Label(studentId + " • " + section);
        metaLabel.getStyleClass().add("proctor-header-meta");

        VBox studentInfo = new VBox(2, nameLabel, metaLabel);

        Label riskBadge = new Label(formatStatus(riskLevel));
        riskBadge.getStyleClass().add(getProctorRiskClass(riskLevel));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, studentInfo, spacer, riskBadge);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("proctor-tile-header");

        StackPane videoPane = new StackPane();
        videoPane.getStyleClass().add("proctor-video-area");
        videoPane.setPrefHeight(165);

        Label liveBadge = new Label(
                "Disconnected".equalsIgnoreCase(cameraStatus) ? "OFFLINE" : "LIVE"
        );
        liveBadge.getStyleClass().add(
                "Disconnected".equalsIgnoreCase(cameraStatus)
                        ? "proctor-offline-badge"
                        : "proctor-live-badge"
        );

        Label videoText = new Label("Student camera stream");
        videoText.getStyleClass().add("proctor-stream-placeholder");

        StackPane.setAlignment(liveBadge, Pos.TOP_LEFT);
        StackPane.setMargin(liveBadge, new Insets(10));

        videoPane.getChildren().addAll(videoText, liveBadge);

        Label statusLabel = new Label(
                "Camera " + cameraStatus + " • " + latestViolation
        );
        statusLabel.setWrapText(true);
        statusLabel.getStyleClass().add("proctor-tile-footer");

        VBox tile = new VBox(0, header, videoPane, statusLabel);
        tile.getStyleClass().addAll("proctor-stream-tile", getProctorStreamTileClass(riskLevel));
        tile.setPrefWidth(330);
        tile.setMaxWidth(320);

        return tile;
    }

    private String getProctorStreamTileClass(String riskLevel) {
        if ("CRITICAL".equalsIgnoreCase(riskLevel)) {
            return "proctor-stream-critical";
        }

        if ("WARNING".equalsIgnoreCase(riskLevel)) {
            return "proctor-stream-warning";
        }

        if ("SUBMITTED".equalsIgnoreCase(riskLevel)) {
            return "proctor-stream-submitted";
        }

        return "proctor-stream-normal";
    }

    private String getProctorRiskClass(String riskLevel) {
        if ("CRITICAL".equalsIgnoreCase(riskLevel)) {
            return "proctor-risk-critical";
        }

        if ("WARNING".equalsIgnoreCase(riskLevel)) {
            return "proctor-risk-warning";
        }

        if ("SUBMITTED".equalsIgnoreCase(riskLevel)) {
            return "proctor-risk-submitted";
        }

        return "proctor-risk-normal";
    }


    @FXML
    private void showWorkspaceActivityLog() {
        setActiveWorkspaceTab(activityLogTabButton);

        if (selectedWorkspaceExamId == null) {
            return;
        }

        workspaceContent.getChildren().setAll(
                createLoadingBox("Loading activity logs...")
        );

        Task<List<ExamActivityLogDTO>> task = new Task<>() {
            @Override
            protected List<ExamActivityLogDTO> call() throws Exception {
                return examApiService.getExamActivityLogs(selectedWorkspaceExamId);
            }
        };

        task.setOnSucceeded(event -> {
            renderWorkspaceActivityLogs(task.getValue());
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();

            workspaceContent.getChildren().setAll(
                    createLoadingBox("Unable to load activity logs.")
            );
        });

        Thread thread = new Thread(task, "load-activity-logs-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void renderWorkspaceActivityLogs(List<ExamActivityLogDTO> logs) {
        if (logs == null) {
            logs = List.of();
        }

        VBox root = new VBox(16);
        root.getStyleClass().add("workspace-overview-root");

        Label pageTitle = new Label("Activity Log");
        pageTitle.getStyleClass().add("workspace-page-title");

        Label pageSubtitle = new Label("Search and filter exam activity, faculty actions, and violation logs.");
        pageSubtitle.getStyleClass().add("workspace-page-subtitle");

        VBox pageHeader = new VBox(3, pageTitle, pageSubtitle);

        ObservableList<ExamActivityLogDTO> rows =
                FXCollections.observableArrayList(logs);

        FilteredList<ExamActivityLogDTO> filteredRows =
                new FilteredList<>(rows, item -> true);

        TextField searchField = new TextField();
        searchField.setPromptText("Search student, action, message, question...");
        searchField.getStyleClass().add("workspace-search-field");
        searchField.setPrefWidth(360);

        ComboBox<String> typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("All Types", "Activity", "Violation");
        typeFilter.setValue("All Types");
        typeFilter.getStyleClass().add("workspace-filter-box");

        ComboBox<String> severityFilter = new ComboBox<>();
        severityFilter.getItems().addAll("All Severity", "MINOR", "MAJOR", "CRITICAL");
        severityFilter.setValue("All Severity");
        severityFilter.getStyleClass().add("workspace-filter-box");

        ComboBox<String> questionFilter = new ComboBox<>();
        questionFilter.getItems().add("All Questions");

        logs.stream()
                .map(ExamActivityLogDTO::getQuestionNumber)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .forEach(q -> questionFilter.getItems().add("Question " + q));

        questionFilter.setValue("All Questions");
        questionFilter.getStyleClass().add("workspace-filter-box");

        Runnable applyFilter = () -> {
            String keyword = searchField.getText() == null
                    ? ""
                    : searchField.getText().toLowerCase().trim();

            String selectedType = typeFilter.getValue();
            String selectedSeverity = severityFilter.getValue();
            String selectedQuestion = questionFilter.getValue();

            filteredRows.setPredicate(log -> {
                boolean matchesKeyword =
                        keyword.isBlank()
                                || contains(log.getStudentName(), keyword)
                                || contains(log.getStudentId(), keyword)
                                || contains(log.getAction(), keyword)
                                || contains(log.getModule(), keyword)
                                || contains(log.getMessage(), keyword)
                                || contains(log.getSeverity(), keyword)
                                || contains(log.getLogType(), keyword);

                if (!matchesKeyword) return false;

                if (!"All Types".equalsIgnoreCase(selectedType)) {
                    if (!safe(log.getLogType()).equalsIgnoreCase(selectedType)) {
                        return false;
                    }
                }

                if (!"All Severity".equalsIgnoreCase(selectedSeverity)) {
                    if (!safe(log.getSeverity()).equalsIgnoreCase(selectedSeverity)) {
                        return false;
                    }
                }

                if (!"All Questions".equalsIgnoreCase(selectedQuestion)) {
                    String q = log.getQuestionNumber() == null
                            ? "General"
                            : "Question " + log.getQuestionNumber();

                    if (!q.equalsIgnoreCase(selectedQuestion)) {
                        return false;
                    }
                }

                return true;
            });
        };

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter.run());
        typeFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter.run());
        severityFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter.run());
        questionFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter.run());

        HBox toolbar = new HBox(10, searchField, typeFilter, severityFilter, questionFilter);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        TableView<ExamActivityLogDTO> table = new TableView<>();
        table.getStyleClass().add("workspace-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(filteredRows);

        TableColumn<ExamActivityLogDTO, String> timeColumn = new TableColumn<>("Time");
        timeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatDateTime(data.getValue().getOccurredAt()))
        );

        TableColumn<ExamActivityLogDTO, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(safe(data.getValue().getLogType()))
        );

        TableColumn<ExamActivityLogDTO, String> studentIdColumn =
                new TableColumn<>("Student ID");

        studentIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safe(data.getValue().getStudentId())
                )
        );

        TableColumn<ExamActivityLogDTO, String> studentNameColumn =
                new TableColumn<>("Student Name");

        studentNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safe(data.getValue().getStudentName())
                )
        );

        TableColumn<ExamActivityLogDTO, String> questionColumn = new TableColumn<>("Question");
        questionColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getQuestionNumber() == null
                                ? "General"
                                : "Q" + data.getValue().getQuestionNumber()
                )
        );

        TableColumn<ExamActivityLogDTO, String> actionColumn = new TableColumn<>("Action");
        actionColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatStatus(safe(data.getValue().getAction())))
        );

        TableColumn<ExamActivityLogDTO, String> severityColumn = new TableColumn<>("Severity");
        severityColumn.setCellValueFactory(data ->
                new SimpleStringProperty(safe(data.getValue().getSeverity()))
        );

        TableColumn<ExamActivityLogDTO, String> messageColumn = new TableColumn<>("Message");
        messageColumn.setCellValueFactory(data ->
                new SimpleStringProperty(safe(data.getValue().getMessage()))
        );

        TableColumn<ExamActivityLogDTO, String> durationColumn = new TableColumn<>("Duration");
        durationColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getDurationMs() == null
                                ? "—"
                                : data.getValue().getDurationMs() + " ms"
                )
        );

        table.getColumns().addAll(
                timeColumn,
                typeColumn,
                studentIdColumn,
                studentNameColumn,
                questionColumn,
                actionColumn,
                severityColumn,
                messageColumn,
                durationColumn
        );

        studentIdColumn.setSortType(TableColumn.SortType.ASCENDING);
        timeColumn.setSortType(TableColumn.SortType.ASCENDING);

        table.getSortOrder().addAll(
                studentIdColumn,
                timeColumn
        );

        table.sort();

        VBox card = new VBox(14, pageHeader, toolbar, table);
        card.getStyleClass().add("workspace-card");

        VBox.setVgrow(table, Priority.ALWAYS);


        VBox.setVgrow(card, Priority.ALWAYS);

        table.setMaxHeight(Double.MAX_VALUE);

        card.setMaxHeight(Double.MAX_VALUE);

        root.getChildren().add(card);

        VBox.setVgrow(root, Priority.ALWAYS);
        makeTableFillWorkspace(root, card, table);

        workspaceContent.getChildren().setAll(root);
    }

    private HBox createActivityLogRow(ExamActivityLogDTO log) {
        Label time = new Label(formatDateTime(log.getOccurredAt()));
        time.getStyleClass().add("activity-log-time");

        Label type = new Label(safe(log.getLogType()));
        type.getStyleClass().add(
                "VIOLATION".equalsIgnoreCase(safe(log.getLogType()))
                        ? "activity-type-violation"
                        : "activity-type-normal"
        );

        String questionText = log.getQuestionNumber() == null
                ? "General"
                : "Question " + log.getQuestionNumber();

        Label title = new Label(
                questionText + " • " + formatStatus(safe(log.getAction()))
        );
        title.getStyleClass().add("activity-log-title");

        String subtitleText =
                safe(log.getStudentName()).isBlank()
                        ? safe(log.getMessage())
                        : safe(log.getStudentName()) + " • " + safe(log.getMessage());

        Label subtitle = new Label(subtitleText);
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("activity-log-subtitle");

        VBox info = new VBox(4, title, subtitle);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label severity = new Label(
                log.getSeverity() == null || log.getSeverity().isBlank()
                        ? ""
                        : safe(log.getSeverity())
        );

        if (!severity.getText().isBlank()) {
            severity.getStyleClass().add("activity-severity-pill");
        }

        VBox left = new VBox(4, time, type);
        left.setMinWidth(150);

        HBox row = new HBox(14, left, info, severity);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("activity-log-row");

        return row;
    }

    @FXML
    private void showWorkspaceLeaderboard() {

        setActiveWorkspaceTab(leaderboardTabButton);

        if (selectedWorkspaceExamId == null) {
            return;
        }

        workspaceContent.getChildren().setAll(
                createLoadingBox("Loading leaderboard...")
        );

        Task<List<ExamLeaderboardDTO>> task = new Task<>() {

            @Override
            protected List<ExamLeaderboardDTO> call() throws Exception {

                return examApiService.getExamLeaderboard(
                        selectedWorkspaceExamId
                );
            }
        };

        task.setOnSucceeded(event -> {

            List<ExamLeaderboardDTO> leaderboard =
                    task.getValue();

            renderWorkspaceLeaderboard(leaderboard);
        });

        task.setOnFailed(event -> {

            Throwable ex = task.getException();

            if (ex != null) {
                ex.printStackTrace();
            }

            workspaceContent.getChildren().setAll(
                    createLoadingBox("Unable to load leaderboard.")
            );
        });

        Thread thread = new Thread(task, "load-leaderboard-thread");

        thread.setDaemon(true);
        thread.start();
    }

    private void renderWorkspaceLeaderboard(List<ExamLeaderboardDTO> leaderboard) {
        if (leaderboard == null) {
            leaderboard = List.of();
        }

        VBox root = new VBox(16);
        root.setFillWidth(true);
        root.getStyleClass().add("workspace-overview-root");

        Label pageTitle = new Label("Leaderboard");
        pageTitle.getStyleClass().add("workspace-page-title");

        Label pageSubtitle = new Label(
                "Student ranking based on submitted scores. Final standing may change until results are released."
        );
        pageSubtitle.getStyleClass().add("workspace-page-subtitle");

        VBox pageHeader = new VBox(3, pageTitle, pageSubtitle);

        TableView<ExamLeaderboardDTO> table = new TableView<>();
        table.getStyleClass().add("workspace-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(leaderboard));

        TableColumn<ExamLeaderboardDTO, String> rankColumn =
                new TableColumn<>("Rank");

        rankColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getRank() == null
                                ? "—"
                                : "#" + data.getValue().getRank()
                )
        );

        TableColumn<ExamLeaderboardDTO, String> studentColumn =
                new TableColumn<>("Student");

        studentColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safe(data.getValue().getStudentName())
                )
        );

        TableColumn<ExamLeaderboardDTO, String> studentIdColumn =
                new TableColumn<>("Student ID");

        studentIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safe(data.getValue().getStudentId())
                )
        );

        TableColumn<ExamLeaderboardDTO, String> sectionColumn =
                new TableColumn<>("Section");

        sectionColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safe(data.getValue().getSectionName())
                )
        );

        TableColumn<ExamLeaderboardDTO, String> scoreColumn =
                new TableColumn<>("Score Obtained");

        scoreColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatScore(
                                data.getValue().getTotalScore(),
                                data.getValue().getTotalPossibleScore()
                        )
                )
        );

        TableColumn<ExamLeaderboardDTO, String> percentColumn =
                new TableColumn<>("Percentage");

        percentColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatPercent(data.getValue().getScorePercentage())
                )
        );

        TableColumn<ExamLeaderboardDTO, String> violationColumn =
                new TableColumn<>("Violations");

        violationColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        String.valueOf(
                                data.getValue().getViolationCount() == null
                                        ? 0
                                        : data.getValue().getViolationCount()
                        )
                )
        );

        TableColumn<ExamLeaderboardDTO, String> reviewColumn =
                new TableColumn<>("Review Status");

        reviewColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safe(data.getValue().getIntegrityStatus()).isBlank()
                                ? "—"
                                : formatStatus(data.getValue().getIntegrityStatus())
                )
        );

        table.getColumns().addAll(
                rankColumn,
                studentColumn,
                studentIdColumn,
                sectionColumn,
                scoreColumn,
                percentColumn,
                violationColumn,
                reviewColumn
        );

        VBox card = new VBox(14, pageHeader, table);
        card.getStyleClass().add("workspace-card");

        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().add(card);

        makeTableFillWorkspace(root, card, table);

        workspaceContent.getChildren().setAll(root);
    }

    @FXML
    private void showWorkspaceReports() {
        setActiveWorkspaceTab(reportsTabButton);

        if (selectedWorkspaceExamId == null) {
            return;
        }

        VBox root = new VBox(16);
        root.getStyleClass().add("workspace-overview-root");

        Label pageTitle = new Label("Reports");
        pageTitle.getStyleClass().add("workspace-page-title");

        Label pageSubtitle = new Label("Generate printable PDF reports for this exam.");
        pageSubtitle.getStyleClass().add("workspace-page-subtitle");

        VBox pageHeader = new VBox(3, pageTitle, pageSubtitle);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setMaxWidth(Double.MAX_VALUE);

        grid.add(createReportCard(
                "Exam Portfolio Report",
                "Complete printable portfolio containing exam overview, assigned classes, submissions, student answer sheets, violations, leaderboard, and review summaries.",
                "Generate Portfolio",
                true,
                mode -> {
                    try {
                        return examApiService.downloadExamPortfolioReport(
                                selectedWorkspaceExamId,
                                mode
                        );
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
        ), 0, 0);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);

        root.getChildren().addAll(pageHeader, grid);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("workspace-scroll");

        workspaceContent.getChildren().setAll(scrollPane);
    }

    private File generatePortfolioReport() throws Exception {

        if (selectedWorkspaceExamId == null) {
            throw new RuntimeException("No exam selected.");
        }

        int assignedClassCount =
                currentWorkspaceDetail == null ||
                        currentWorkspaceDetail.getAssignedClasses() == null
                        ? 0
                        : currentWorkspaceDetail.getAssignedClasses().size();

        if (assignedClassCount <= 1) {
            return examApiService.downloadExamPortfolioReport(
                    selectedWorkspaceExamId,
                    "MERGE"
            );
        }

        final String[] selectedMode = {"MERGE"};

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Generate Portfolio Report");
            alert.setHeaderText("This exam has multiple assigned classes.");
            alert.setContentText("How do you want to generate the portfolio report?");

            ButtonType mergeButton = new ButtonType("Merge into one PDF");
            ButtonType separateButton = new ButtonType("Separate by class");
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(
                    mergeButton,
                    separateButton,
                    cancelButton
            );

            Optional<ButtonType> result = alert.showAndWait();

            if (result.isEmpty() || result.get() == cancelButton) {
                selectedMode[0] = null;
                return;
            }

            selectedMode[0] =
                    result.get() == separateButton
                            ? "SEPARATE"
                            : "MERGE";
        });

        // Simpler alternative: avoid prompt here and use MERGE for now.
        if (selectedMode[0] == null) {
            throw new RuntimeException("Report generation cancelled.");
        }

        return examApiService.downloadExamPortfolioReport(
                selectedWorkspaceExamId,
                selectedMode[0]
        );
    }

    private void downloadPortfolioReport(String mode) {

        try {

            File file =
                    examApiService.downloadExamPortfolioReport(
                            selectedWorkspaceExamId,
                            mode
                    );

            showSuccess(
                    "Report saved to:\n" +
                            file.getAbsolutePath()
            );

        } catch (Exception e) {
            e.printStackTrace();
            showError("Unable to generate portfolio report.");
        }
    }

    private VBox createReportCard(
            String title,
            String description,
            String buttonText,
            boolean askPortfolioMode,
            java.util.function.Function<String, File> action
    ) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("workspace-section-title");

        Label descriptionLabel = new Label(description);
        descriptionLabel.setWrapText(true);
        descriptionLabel.getStyleClass().add("overview-detail-row");

        Button button = new Button(buttonText);
        button.getStyleClass().add("workspace-review-button");

        button.setOnAction(e -> {

            String mode = null;

            if (askPortfolioMode) {

                mode = askPortfolioMode();

                if (mode == null) {
                    return;
                }
            }

            final String finalMode = mode;

            LoadingSpinner.setLoading(
                    button,
                    true,
                    "Generating...",
                    buttonText
            );

            Task<File> task = new Task<>() {
                @Override
                protected File call() throws Exception {
                    return action.apply(finalMode);
                }
            };

            task.setOnSucceeded(event -> {

                LoadingSpinner.setLoading(
                        button,
                        false,
                        "Generating...",
                        buttonText
                );

                File file = task.getValue();

                showSuccess(
                        "Report saved to:\n" +
                                file.getAbsolutePath()
                );
            });

            task.setOnFailed(event -> {

                LoadingSpinner.setLoading(
                        button,
                        false,
                        "Generating...",
                        buttonText
                );

                Throwable ex = task.getException();

                if (ex != null) {
                    ex.printStackTrace();
                }

                showError("Unable to generate report.");
            });

            Thread thread = new Thread(task, "generate-report-thread");
            thread.setDaemon(true);
            thread.start();
        });

        VBox card = new VBox(14, titleLabel, descriptionLabel, button);
        card.getStyleClass().add("workspace-card");
        card.setMaxWidth(Double.MAX_VALUE);

        return card;
    }

    private void generateReport(String reportType) {
        if (selectedWorkspaceExamId == null) {
            showError("No exam selected.");
            return;
        }

        showSuccess("Report generation for " + reportType + " will be connected to the PDF endpoint.");
    }


    @FXML
    private void handleReleaseResults() {
        if (selectedWorkspaceExamId == null) {
            showError("No exam selected.");
            return;
        }

        try {
            List<FacultyExamStudentDTO> students =
                    examApiService.getExamStudents(selectedWorkspaceExamId);

            long submitted = students.stream()
                    .filter(s ->
                            "SUBMITTED".equalsIgnoreCase(safe(s.getAttemptStatus())) ||
                                    "AUTO_SUBMITTED".equalsIgnoreCase(safe(s.getAttemptStatus()))
                    )
                    .count();

            long remainingToReview = students.stream()
                    .filter(s ->
                            "SUBMITTED".equalsIgnoreCase(safe(s.getAttemptStatus())) ||
                                    "AUTO_SUBMITTED".equalsIgnoreCase(safe(s.getAttemptStatus()))
                    )
                    .filter(s -> Boolean.TRUE.equals(s.getNeedsChecking()))
                    .filter(s -> !"REVIEWED".equalsIgnoreCase(safe(s.getReviewStatus())))
                    .count();

            long readyForRelease = submitted - remainingToReview;

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Release Results");
            alert.setHeaderText("Release results to students?");
            alert.setContentText(
                    "Ready for Release: " + readyForRelease + "/" + submitted + "\n" +
                            "Remaining to Review: " + remainingToReview + "\n\n" +
                            "Students will be able to view their scores once results are released."
            );

            Optional<ButtonType> result = alert.showAndWait();

            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }

            releaseResultsNow();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Unable to check review status before releasing results.");
        }
    }

    private void releaseResultsNow() {
        releaseResultsButton.setDisable(true);
        releaseResultsButton.setText("Releasing...");

        Task<SimpleMessageResponse> task = new Task<>() {
            @Override
            protected SimpleMessageResponse call() throws Exception {
                return examApiService.releaseResults(selectedWorkspaceExamId);
            }
        };

        task.setOnSucceeded(event -> {
            SimpleMessageResponse response = task.getValue();

            if (response != null && response.isSuccess()) {
                releaseResultsButton.setText("Results Released");
                releaseResultsButton.setDisable(true);
                showSuccess(response.getMessage());
                showWorkspaceOverview();
            } else {
                releaseResultsButton.setText("Release Results");
                releaseResultsButton.setDisable(false);
                showError(response == null ? "Unable to release results." : response.getMessage());
            }
        });

        task.setOnFailed(event -> {
            releaseResultsButton.setText("Release Results");
            releaseResultsButton.setDisable(false);
            showError("Unable to release results.");
        });

        Thread thread = new Thread(task, "release-results-thread");
        thread.setDaemon(true);
        thread.start();
    }


    private void setupTableColumns() {
        dateCreatedColumn.setCellValueFactory(data -> data.getValue().dateCreatedProperty());
        validityColumn.setCellValueFactory(data -> data.getValue().validityProperty());
        titleColumn.setCellValueFactory(data -> data.getValue().titleProperty());
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());
        durationColumn.setCellValueFactory(data -> data.getValue().durationProperty());
        assignedColumn.setCellValueFactory(data -> data.getValue().assignedProperty());
        createdByColumn.setCellValueFactory(data -> data.getValue().createdByProperty());
        updatedByColumn.setCellValueFactory(data -> data.getValue().updatedByProperty());
        takersColumn.setCellValueFactory(data -> data.getValue().takersProperty());
    }

    private void setupColumnWidths() {
        bindColumnWidth(dateCreatedColumn, 0.07);
        bindColumnWidth(validityColumn, 0.15);
        bindColumnWidth(titleColumn, 0.20);
        bindColumnWidth(statusColumn, 0.08);
        bindColumnWidth(durationColumn, 0.05);
        bindColumnWidth(takersColumn, 0.06);
        bindColumnWidth(assignedColumn, 0.12);
        bindColumnWidth(createdByColumn, 0.09);
        bindColumnWidth(updatedByColumn, 0.09);
        bindColumnWidth(actionsColumn, 0.15);
    }

    private void bindColumnWidth(TableColumn<?, ?> column, double percent) {
        column.prefWidthProperty().bind(examTable.widthProperty().multiply(percent));
    }

    private void setupSearch() {
        if (searchField == null) return;

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            searchDebounce.setOnFinished(e -> applySearchFilter(newValue));
            searchDebounce.playFromStart();
        });
    }

    private void applySearchFilter(String value) {
        String keyword = value == null ? "" : value.toLowerCase().trim();

        filteredRows.setPredicate(row -> {
            if (keyword.isBlank()) return true;

            return contains(row.getTitle(), keyword)
                    || contains(row.getStatus(), keyword)
                    || contains(row.getAssigned(), keyword)
                    || contains(row.getDuration(), keyword)
                    || contains(row.getTakers(), keyword)
                    || contains(row.getDateCreated(), keyword)
                    || contains(row.getCreatedBy(), keyword)
                    || contains(row.getUpdatedBy(), keyword);
        });

        updateItemCount();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private void setupTitleStyle() {
        titleColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String title, boolean empty) {
                super.updateItem(title, empty);

                getStyleClass().remove("title-link");

                if (empty || title == null || title.isBlank()) {
                    setText(null);
                } else {
                    setText(title);
                    getStyleClass().add("title-link");
                }
            }
        });
    }

    private void setupStatusStyle() {
        statusColumn.setCellFactory(column -> new TableCell<>() {
            private final Label badge = new Label();

            {
                badge.getStyleClass().add("status-badge");
            }

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }

                badge.setText(status);

                badge.getStyleClass().removeAll(
                        "status-draft",
                        "status-scheduled",
                        "status-ongoing",
                        "status-expired",
                        "status-completed",
                        "status-cancelled"
                );

                switch (status) {
                    case "DRAFT" -> badge.getStyleClass().add("status-draft");
                    case "SCHEDULED" -> badge.getStyleClass().add("status-scheduled");
                    case "ONGOING" -> badge.getStyleClass().add("status-ongoing");
                    case "EXPIRED" -> badge.getStyleClass().add("status-expired");
                    case "COMPLETED" -> badge.getStyleClass().add("status-completed");
                    case "CANCELLED" -> badge.getStyleClass().add("status-cancelled");
                }

                setGraphic(badge);
                setText(null);
            }
        });
    }

    private void setupTakersStyle() {
        takersColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);

                getStyleClass().remove("takers-text");

                if (empty || val == null || val.isBlank()) {
                    setText(null);
                } else {
                    setText(val);
                    getStyleClass().add("takers-text");
                }
            }
        });
    }

    private void setupValidityStyle() {
        validityColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null || val.isBlank() ? null : val);
            }
        });
    }

    private String formatSchedule(String start, String end) {
        if (start == null || start.isBlank() || end == null || end.isBlank()) {
            return "";
        }

        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            LocalDateTime startDateTime = LocalDateTime.parse(start, inputFormatter);
            LocalDateTime endDateTime = LocalDateTime.parse(end, inputFormatter);

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            if (startDateTime.toLocalDate().equals(endDateTime.toLocalDate())) {
                return startDateTime.format(dateFormatter)
                        + " "
                        + startDateTime.format(timeFormatter)
                        + " - "
                        + endDateTime.format(timeFormatter);
            }

            return startDateTime.format(fullFormatter)
                    + " - "
                    + endDateTime.format(fullFormatter);

        } catch (Exception e) {
            return start + " - " + end;
        }
    }

    private void setupActionButtons() {

        actionsColumn.setCellFactory(col -> new TableCell<>() {

            private final Button viewBtn = new Button("View");
            private final Button publishBtn = new Button("Publish");
            private final Button cancelBtn = new Button("Cancel");
            private final Button restoreBtn = new Button("Restore");

            private final HBox actionBox = new HBox(
                    8,
                    viewBtn,
                    publishBtn,
                    cancelBtn,
                    restoreBtn
            );

            {
                actionBox.setAlignment(Pos.CENTER);

                styleButton(viewBtn, "#302C29", "white");
                styleButton(publishBtn, "#15803D", "white");
                styleButton(cancelBtn, "#B91C1C", "white");
                styleButton(restoreBtn, "#1D4ED8", "white");

                viewBtn.setOnAction(event -> {
                    ExamRow row = getRowData();
                    if (row == null) return;

                    if ("DRAFT".equalsIgnoreCase(row.getStatus())){
                        openExamWizard(row.getExamId(), true);
                    }else{
                        openExamWorkspace(row.getExamId(), row.getTitle(), row.getStatus());
                    }

                });

                publishBtn.setOnAction(event -> {
                    ExamRow row = getRowData();
                    if (row == null) return;

                    try {
                        runExamActionAsync("publish-exam-thread", () -> {
                            try {
                                examApiService.publishExamById(row.getExamId());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                cancelBtn.setOnAction(event -> {
                    ExamRow row = getRowData();
                    if (row == null) return;

                    ButtonType yesBtn = new ButtonType("Yes, Cancel");
                    ButtonType noBtn = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);

                    Alert alert = new Alert(
                            Alert.AlertType.CONFIRMATION,
                            "Are you sure you want to cancel this exam?\n\nExam: " + row.getTitle(),
                            yesBtn,
                            noBtn
                    );

                    alert.setTitle("Confirm Cancel");
                    alert.setHeaderText("Cancel Exam");

                    Optional<ButtonType> result = alert.showAndWait();

                    if (result.isPresent() && result.get() == yesBtn) {
                        try {
                            runExamActionAsync("cancel-exam-thread", () -> {
                                try {
                                    examApiService.cancelExam(row.getExamId());
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                restoreBtn.setOnAction(event -> {
                    ExamRow row = getRowData();
                    if (row == null) return;

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Restore Exam");
                    confirm.setHeaderText("Restore Cancelled Exam");
                    confirm.setContentText("Restore this exam back to DRAFT?\n\n" + row.getTitle());

                    Optional<ButtonType> result = confirm.showAndWait();

                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        try {
                            runExamActionAsync("restore-exam-thread", () -> {
                                try {
                                    examApiService.restoreExam(row.getExamId());
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                ExamRow row = getRowData();

                if (row == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                updateButtonVisibility(row.getStatus());

                setGraphic(actionBox);
                setText(null);
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                refreshHoverGraphic();
            }

            private void refreshHoverGraphic() {
                if (isEmpty()) {
                    setGraphic(null);
                    return;
                }

                TableRow<ExamRow> tableRow = getTableRow();

                if (tableRow != null && tableRow.isHover()) {
                    setGraphic(actionBox);
                } else {
                    setGraphic(null);
                }
            }

            private ExamRow getRowData() {
                if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    return null;
                }

                return getTableView().getItems().get(getIndex());
            }

            private void updateButtonVisibility(String status) {

                boolean isDraft = "DRAFT".equalsIgnoreCase(status);
                boolean isScheduled = "SCHEDULED".equalsIgnoreCase(status);
                boolean isOngoing = "ONGOING".equalsIgnoreCase(status);
                boolean isCompleted = "COMPLETED".equalsIgnoreCase(status);
                boolean isCancelled = "CANCELLED".equalsIgnoreCase(status);

                setButtonVisible(viewBtn, !isCancelled);
                setButtonVisible(publishBtn, isDraft);
                setButtonVisible(cancelBtn, isDraft || isScheduled);
                setButtonVisible(restoreBtn, isCancelled);
                viewBtn.setText(isOngoing || isCompleted ? "View" : "Edit");
            }

            private void setButtonVisible(Button button, boolean visible) {
                button.setVisible(visible);
                button.setManaged(visible);
            }
        });
    }

    private void styleButton(Button button, String backgroundColor, String textColor) {
        button.setStyle(
                "-fx-background-color: " + backgroundColor + ";" +
                        "-fx-text-fill: " + textColor + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
    }

    private void openExamWizard(Long examId, boolean viewMode) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/exam/create-exam-wizard.fxml")
            );

            Parent root = loader.load();

            CreateExamController controller = loader.getController();

            Stage modal = new Stage();
            modal.initOwner(examTable.getScene().getWindow());
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle(viewMode ? "View Exam" : "Edit Exam");

            Scene scene = new Scene(root, 1200, 900);
            modal.setScene(scene);
            modal.setResizable(false);

            modal.setOnShown(event -> {
                if (viewMode) {
                    controller.initViewMode(examId);
                } else {
                    controller.initEditMode(examId);
                }
            });

            modal.showAndWait();

            loadExamsFromBackend();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Unable to open exam wizard.");
        }
    }

    private void loadExamsFromBackend() {

        if (loading) return;

        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            currentLoadTask.cancel();
        }

        setLoading(true);

        currentLoadTask = new Task<>() {
            @Override
            protected List<ExamRow> call() throws Exception {
                List<ExamResponse> responses = examApiService.fetchExams();

                return responses.stream()
                        .map(ExamManagementController.this::mapToExamRow)
                        .toList();
            }
        };

        currentLoadTask.setOnSucceeded(event -> {
            examRows.setAll(currentLoadTask.getValue());
            updateItemCount();
            setLoading(false);
        });

        currentLoadTask.setOnFailed(event -> {
            Throwable ex = currentLoadTask.getException();
            if (ex != null) ex.printStackTrace();

            updateItemCount();
            setLoading(false);
            showError("Unable to load exams. Please check your backend connection.");
        });

        Thread thread = new Thread(currentLoadTask, "load-exams-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private ExamRow mapToExamRow(ExamResponse exam) {
        return new ExamRow(
                exam.getExamId(),
                safe(exam.getDateCreated()),
                safe(exam.getTitle()),
                computeDisplayStatus(exam),
                safe(exam.getDuration()),
                safe(exam.getAssigned()),
                formatTakers(exam.getTakers()),
                formatSchedule(exam.getStartDateTime(), exam.getEndDateTime()),
                safe(exam.getCreatedBy()),
                safe(exam.getUpdatedBy())
        );
    }

    private String computeDisplayStatus(ExamResponse exam) {
        String status = safe(exam.getStatus());

        if ("CANCELLED".equalsIgnoreCase(status)) {
            return "CANCELLED";
        }

        if ("DRAFT".equalsIgnoreCase(status)) {
            return "DRAFT";
        }

        if (isFullyTaken(exam.getTakers())) {
            return "COMPLETED";
        }

        try {
            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            LocalDateTime start =
                    LocalDateTime.parse(exam.getStartDateTime(), formatter);

            LocalDateTime end =
                    LocalDateTime.parse(exam.getEndDateTime(), formatter);

            LocalDateTime now = LocalDateTime.now();

            if (now.isBefore(start)) {
                return "SCHEDULED";
            }

            if (now.isAfter(end)) {
                return "EXPIRED";
            }

            return "ONGOING";

        } catch (Exception e) {
            return status;
        }
    }

    private boolean isFullyTaken(String takers) {
        if (takers == null || takers.isBlank()) {
            return false;
        }

        try {
            int open = takers.indexOf("(");
            int close = takers.indexOf(")");

            String ratio = takers;

            if (open >= 0 && close > open) {
                ratio = takers.substring(open + 1, close);
            }

            if (!ratio.contains("/")) {
                return false;
            }

            String[] parts = ratio.trim().split("/");

            if (parts.length != 2) {
                return false;
            }

            int submitted = Integer.parseInt(parts[0].trim());
            int total = Integer.parseInt(parts[1].trim());

            return total > 0 && submitted == total;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String formatTakers(String value) {
        if (value == null || value.isBlank()) {
            return "0% [0/0]";
        }

        try {
            String cleaned = value.trim();

            if (!cleaned.contains("/")) {
                return cleaned;
            }

            String[] parts = cleaned.split("/");

            int submitted = Integer.parseInt(parts[0].trim());
            int total = Integer.parseInt(parts[1].trim());

            int percent = total == 0
                    ? 0
                    : (int) Math.round((submitted * 100.0) / total);

            return percent + "% [" + submitted + "/" + total + "]";

        } catch (Exception e) {
            return value;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void setLoading(boolean loading) {
        this.loading = loading;

        if (loadingOverlay != null) {
            loadingOverlay.setVisible(loading);
            loadingOverlay.setManaged(loading);
        }

        if (reloadButton != null) {
            reloadButton.setDisable(loading);
        }

        if (searchField != null) {
            searchField.setDisable(loading);
        }

        if (itemCountLabel != null && loading) {
            itemCountLabel.setText("Loading...");
        }
    }


    private void updateItemCount() {
        if (itemCountLabel == null) return;

        int count = filteredRows == null ? examRows.size() : filteredRows.size();

        itemCountLabel.setText(count + (count == 1 ? " Item" : " Items"));
    }

    @FXML
    private void handleCreateExam() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/exam/create-exam-wizard.fxml")
            );

            Parent root = loader.load();

            Stage modal = new Stage();
            modal.initOwner(examTable.getScene().getWindow());
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle("Create Exam");

            Scene scene = new Scene(root, 1200, 900);
            modal.setScene(scene);
            modal.setResizable(false);

            modal.showAndWait();

            loadExamsFromBackend();

        } catch (IOException e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Create Exam Failed");
            alert.setHeaderText("Unable to open Create Exam");
            alert.setContentText("Please check if create-exam-wizard.fxml path is correct.");
            alert.show();
        }
    }

    private void handlePublishFromTable(Long examId) {
        try {
            ExamResult result = examApiService.publishExamById(examId);

            if (!result.isSuccess()) {
                showError(result.getMessage());
                return;
            }

            showSuccess("Exam published successfully.");
            loadExamsFromBackend();

        } catch (Exception e) {
            e.printStackTrace();
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleReload() {
        loadExamsFromBackend();
    }


    private void setupCenteredColumns() {
        centerColumn(statusColumn);
        centerColumn(durationColumn);
        centerColumn(takersColumn);
        centerColumn(actionsColumn);
    }

    private void centerColumn(TableColumn<?, ?> column) {
        if (column != null) {
            column.setStyle("-fx-alignment: CENTER;");
        }
    }

    @Override
    public void setShellController(DashboardShellController shellController) {
        this.shellController = shellController;
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Something went wrong");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void openExamWorkspace(Long examId, String examTitle, String status) {
        if (shellController != null) {
            shellController.hideHeroCards();
            shellController.hideHeroSection();
        }

        selectedWorkspaceExamId = examId;

        setupWorkspaceTabs(status);
        listModeContainer.setVisible(false);
        listModeContainer.setManaged(false);

        workspaceModeContainer.setVisible(true);
        workspaceModeContainer.setManaged(true);

        workspaceTitleLabel.setText(safe(examTitle));
        workspaceSubtitleLabel.setText("Review students, submissions, violations, and results.");

        showWorkspaceOverview();
    }

    private void setupWorkspaceTabs(String examStatus) {

        boolean showProctor = "ONGOING".equalsIgnoreCase(safe(examStatus));

        proctorTabButton.setVisible(showProctor);
        proctorTabButton.setManaged(showProctor);

        if (!showProctor && proctorTabButton.getStyleClass().contains("workspace-tab-active")) {
            showWorkspaceOverview();
        }
    }

    @FXML
    private void handleBackToExamList() {
        if (shellController != null) {
            shellController.showHeroCards();
            shellController.showHeroSection();
        }

        selectedWorkspaceExamId = null;

        workspaceModeContainer.setVisible(false);
        workspaceModeContainer.setManaged(false);

        listModeContainer.setVisible(true);
        listModeContainer.setManaged(true);

        workspaceContent.getChildren().clear();

        loadExamsFromBackend();
    }

    private VBox createEssayRubricReviewBox(ExamAttemptAnswerReviewDTO answer) {

        if (!"ESSAY".equalsIgnoreCase(safe(answer.getQuestionType()))) {
            return null;
        }

        if (answer.getRubrics() == null || answer.getRubrics().isEmpty()) {
            return createEssayFallbackReviewBox(answer);
        }

        VBox box = new VBox(12);
        box.getStyleClass().add("essay-review-panel");

        Label title = new Label("Essay Rubric Review");
        title.getStyleClass().add("essay-review-title");

        Label subtitle = new Label(
                "Enter score percentage per criterion. Points are calculated automatically."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("essay-review-subtitle");

        VBox rowsBox = new VBox(8);

        Map<Long, EssayRubricScoreResponse> savedScoreMap = new HashMap<>();

        if (answer.getRubricScores() != null) {
            for (EssayRubricScoreResponse score : answer.getRubricScores()) {
                if (score.getRubricId() != null) {
                    savedScoreMap.put(score.getRubricId(), score);
                }
            }
        }

        Label totalLabel = new Label();
        totalLabel.getStyleClass().add("essay-review-total");

        TextArea generalFeedbackArea = new TextArea();
        generalFeedbackArea.setPromptText("Optional overall essay feedback for the student...");
        generalFeedbackArea.setWrapText(true);
        generalFeedbackArea.setPrefRowCount(3);
        generalFeedbackArea.getStyleClass().add("essay-feedback-area");
        generalFeedbackArea.setText(safe(answer.getFacultyFeedback()));

        Button saveButton = new Button("✔ Save Essay Review");
        saveButton.getStyleClass().add("review-save-score-button");
        saveButton.setFocusTraversable(false);

        Runnable updateTotal = () -> {
            BigDecimal total = calculateEssayRubricTotal(rowsBox, answer);

            totalLabel.setText(
                    "Calculated Score: " +
                            formatPoints(total) +
                            " / " +
                            formatPoints(answer.getPoints()) +
                            " pts"
            );
        };

        for (EssayRubricRequest rubric : answer.getRubrics()) {

            EssayRubricScoreResponse savedScore =
                    savedScoreMap.get(rubric.getRubricId());

            rowsBox.getChildren().add(
                    createEssayRubricScoreRow(
                            answer,
                            rubric,
                            savedScore,
                            updateTotal
                    )
            );
        }

        updateTotal.run();

        saveButton.setOnAction(event -> {
            EssayReviewRequest request = buildEssayReviewRequest(
                    answer,
                    rowsBox,
                    generalFeedbackArea.getText()
            );

            if (request == null) {
                return;
            }

            saveButton.setDisable(true);
            saveButton.setText("Saving...");

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    examApiService.saveEssayReview(request);
                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                saveButton.setText("✔ Saved");
                openStudentReview(currentReviewStudent);
            });

            task.setOnFailed(e -> {
                Throwable ex = task.getException();

                if (ex != null) {
                    ex.printStackTrace();
                }

                saveButton.setDisable(false);
                saveButton.setText("✔ Save Essay Review");

                showError("Unable to save essay review.");
            });

            Thread thread = new Thread(task, "save-essay-review-thread");
            thread.setDaemon(true);
            thread.start();
        });

        HBox saveRow = new HBox(12, totalLabel, saveButton);
        saveRow.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        saveRow.getChildren().add(1, spacer);

        box.getChildren().addAll(
                title,
                subtitle,
                rowsBox,
                createFeedbackBlock("Overall Essay Feedback", generalFeedbackArea),
                saveRow
        );

        return box;
    }

    private HBox createEssayRubricScoreRow(
            ExamAttemptAnswerReviewDTO answer,
            EssayRubricRequest rubric,
            EssayRubricScoreResponse savedScore,
            Runnable updateTotal
    ) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("essay-rubric-score-row");

        Label criterionLabel = new Label(safe(rubric.getCriterionName()));
        criterionLabel.getStyleClass().add("essay-rubric-criterion");
        criterionLabel.setWrapText(true);
        criterionLabel.setMinWidth(160);
        criterionLabel.setPrefWidth(180);

        BigDecimal maxPoints = calculateCriterionMaxPoints(answer, rubric);

        Label weightLabel = new Label(
                formatPoints(rubric.getWeightPercentage()) +
                        "% • max " +
                        formatPoints(maxPoints) +
                        " pts"
        );
        weightLabel.getStyleClass().add("essay-rubric-weight");
        weightLabel.setMinWidth(150);

        TextField percentField = new TextField();

        percentField.setPromptText("0-100");
        percentField.setPrefWidth(80);
        percentField.getStyleClass().add("review-score-field");

        if (savedScore != null && savedScore.getScorePercentage() != null) {
            percentField.setText(
                    savedScore == null || savedScore.getScorePercentage() == null
                            ? ""
                            : formatPoints(savedScore.getScorePercentage())
            );
        }

        Label percentSymbol = new Label("%");
        percentSymbol.getStyleClass().add("essay-rubric-weight");

        Label pointsPreview = new Label("0 pts");
        pointsPreview.getStyleClass().add("essay-rubric-points");
        pointsPreview.setMinWidth(90);

        TextField feedbackField = new TextField();
        feedbackField.setPromptText("Optional criterion feedback");
        feedbackField.getStyleClass().add("essay-rubric-feedback-field");

        if (savedScore != null) {
            feedbackField.setText(safe(savedScore.getFeedback()));
        }

        HBox.setHgrow(feedbackField, Priority.ALWAYS);

        Runnable updatePreview = () -> {
            BigDecimal percentage = parsePercentage(percentField.getText());

            BigDecimal awarded = maxPoints
                    .multiply(percentage)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            pointsPreview.setText(formatPoints(awarded) + " pts");

            updateTotal.run();
        };

        percentField.textProperty().addListener((obs, oldVal, newVal) -> updatePreview.run());

        row.setUserData(new EssayRubricRowData(
                rubric.getRubricId(),
                percentField,
                feedbackField
        ));

        updatePreview.run();

        row.getChildren().addAll(
                criterionLabel,
                weightLabel,
                percentField,
                percentSymbol,
                pointsPreview,
                feedbackField
        );

        return row;
    }

    private EssayReviewRequest buildEssayReviewRequest(
            ExamAttemptAnswerReviewDTO answer,
            VBox rowsBox,
            String generalFeedback
    ) {
        EssayReviewRequest request = new EssayReviewRequest();

        request.setAnswerId(answer.getAnswerId());
        request.setFacultyFeedback(generalFeedback);

        for (javafx.scene.Node node : rowsBox.getChildren()) {

            if (!(node instanceof HBox row)) {
                continue;
            }

            if (!(row.getUserData() instanceof EssayRubricRowData rowData)) {
                continue;
            }

            String rawScore = rowData.percentField.getText() == null
                    ? ""
                    : rowData.percentField.getText().trim();

            if (rawScore.isBlank()) {
                showError("Please enter a score percentage for all rubric criteria.");
                rowData.percentField.requestFocus();
                return null;
            }

            BigDecimal percentage;

            try {
                percentage = new BigDecimal(rawScore);
            } catch (Exception e) {
                showError("Score percentage must be a valid number.");
                rowData.percentField.requestFocus();
                return null;
            }

            if (percentage.compareTo(BigDecimal.ZERO) < 0 ||
                    percentage.compareTo(new BigDecimal("100")) > 0) {
                showError("Score percentage must be between 0 and 100.");
                rowData.percentField.requestFocus();
                return null;
            }

            if (percentage.compareTo(BigDecimal.ZERO) < 0 ||
                    percentage.compareTo(new BigDecimal("100")) > 0) {

                showError("Rubric score percentage must be between 0 and 100.");
                return null;
            }

            EssayRubricScoreRequest score = new EssayRubricScoreRequest();
            score.setRubricId(rowData.rubricId);
            score.setScorePercentage(percentage);
            score.setFeedback(rowData.feedbackField.getText());

            request.getRubricScores().add(score);
        }

        if (request.getRubricScores().isEmpty()) {
            showError("Essay rubric score is required.");
            return null;
        }

        return request;
    }

    private BigDecimal calculateEssayRubricTotal(
            VBox rowsBox,
            ExamAttemptAnswerReviewDTO answer
    ) {
        BigDecimal total = BigDecimal.ZERO;

        if (answer.getRubrics() == null) {
            return total;
        }

        Map<Long, EssayRubricRequest> rubricMap = new HashMap<>();

        for (EssayRubricRequest rubric : answer.getRubrics()) {
            rubricMap.put(rubric.getRubricId(), rubric);
        }

        for (javafx.scene.Node node : rowsBox.getChildren()) {

            if (!(node instanceof HBox row)) {
                continue;
            }

            if (!(row.getUserData() instanceof EssayRubricRowData rowData)) {
                continue;
            }

            EssayRubricRequest rubric = rubricMap.get(rowData.rubricId);

            if (rubric == null) {
                continue;
            }

            BigDecimal percentage = parsePercentage(rowData.percentField.getText());
            BigDecimal maxPoints = calculateCriterionMaxPoints(answer, rubric);

            BigDecimal awarded = maxPoints
                    .multiply(percentage)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            total = total.add(awarded);
        }

        return total;
    }

    private BigDecimal calculateCriterionMaxPoints(
            ExamAttemptAnswerReviewDTO answer,
            EssayRubricRequest rubric
    ) {
        BigDecimal questionPoints =
                answer.getPoints() == null ? BigDecimal.ZERO : answer.getPoints();

        BigDecimal weight =
                rubric.getWeightPercentage() == null ? BigDecimal.ZERO : rubric.getWeightPercentage();

        return questionPoints
                .multiply(weight)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal parsePercentage(String value) {
        if (value == null || value.trim().isBlank()) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private VBox createFeedbackBlock(String title, TextArea area) {
        Label label = new Label(title);
        label.getStyleClass().add("review-answer-label");

        VBox box = new VBox(6, label, area);
        box.setMaxWidth(Double.MAX_VALUE);

        return box;
    }

    private VBox createEssayFallbackReviewBox(ExamAttemptAnswerReviewDTO answer) {
        VBox box = new VBox(10);
        box.getStyleClass().add("essay-review-panel");

        Label title = new Label("Essay Manual Review");
        title.getStyleClass().add("essay-review-title");

        Label note = new Label("No rubric was configured for this essay. Use manual score editing.");
        note.setWrapText(true);
        note.getStyleClass().add("essay-review-subtitle");

        box.getChildren().addAll(title, note);

        return box;
    }

    private static class EssayRubricRowData {
        private final Long rubricId;
        private final TextField percentField;
        private final TextField feedbackField;

        private EssayRubricRowData(
                Long rubricId,
                TextField percentField,
                TextField feedbackField
        ) {
            this.rubricId = rubricId;
            this.percentField = percentField;
            this.feedbackField = feedbackField;
        }
    }



    private void runExamActionAsync(String loadingText, Runnable action) {
        setLoading(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                action.run();
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            loadExamsFromBackend();
        });

        task.setOnFailed(e -> {
            setLoading(false);
            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();
            showError(ex == null ? "Action failed." : ex.getMessage());
        });

        Thread thread = new Thread(task, loadingText);
        thread.setDaemon(true);
        thread.start();
    }

    private VBox createLoadingBox(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("workspace-section-subtitle");

        VBox box = new VBox(label);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("workspace-card");

        return box;
    }

    private void setActiveWorkspaceTab(Button activeButton) {
        List<Button> buttons = List.of(
                overviewTabButton,
                studentsTabButton,
                proctorTabButton,
                activityLogTabButton,
                leaderboardTabButton,
                reportsTabButton
        );

        for (Button button : buttons) {
            button.getStyleClass().remove("workspace-tab-active");
        }

        if (activeButton != null&&
                !activeButton.getStyleClass().contains("workspace-tab-active")) {
            activeButton.getStyleClass().add("workspace-tab-active");
        }
    }

    private long defaultLong(Long value) {
        return value == null ? 0 : value;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String yesNo(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Yes" : "No";
    }

    private String formatStatus(String status) {
        if (status == null || status.isBlank()) {
            return "—";
        }

        return status.replace("_", " ");
    }

    private String formatDateTime(OffsetDateTime value) {
        if (value == null) {
            return "—";
        }

        return value.atZoneSameInstant(ZoneId.of("Asia/Manila"))
                .format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"));
    }

    private String formatPoints(java.math.BigDecimal value) {
        if (value == null) {
            return "0";
        }

        return value.stripTrailingZeros().toPlainString();
    }

    private String formatScore(Double score, Double total) {
        double s = score == null ? 0.0 : score;
        double t = total == null ? 0.0 : total;

        return String.format("%.1f / %.1f", s, t);
    }

    private boolean answerNeedsReview(ExamAttemptAnswerReviewDTO answer) {
        String status = safe(answer.getReviewStatus());

        return "PENDING".equalsIgnoreCase(status)
                || "FLAGGED".equalsIgnoreCase(status)
                || Boolean.TRUE.equals(answer.getNeedsChecking());
    }

    private String formatPercent(Double value) {
        if (value == null) {
            return "0%";
        }

        return String.format("%.0f%%", value);
    }

    private String askPortfolioMode() {

        int assignedClassCount =
                currentWorkspaceDetail == null ||
                        currentWorkspaceDetail.getAssignedClasses() == null
                        ? 0
                        : currentWorkspaceDetail.getAssignedClasses().size();

        if (assignedClassCount <= 1) {
            return "MERGE";
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Generate Portfolio Report");
        alert.setHeaderText("This exam has multiple assigned classes.");
        alert.setContentText("How do you want to generate the portfolio report?");

        ButtonType mergeButton = new ButtonType("Merge into one PDF");
        ButtonType separateButton = new ButtonType("Separate by class");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(
                mergeButton,
                separateButton,
                cancelButton
        );

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isEmpty() || result.get() == cancelButton) {
            return null;
        }

        return result.get() == separateButton ? "SEPARATE" : "MERGE";
    }

}