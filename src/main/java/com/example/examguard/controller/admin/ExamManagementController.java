package com.example.examguard.controller.admin;

import com.example.examguard.cache.ExamLocalCacheKeys;
import com.example.examguard.cache.LocalCacheService;
import com.example.examguard.utility.Session;
import com.example.examguard.config.AppConfig;
import com.example.examguard.controller.exam.CreateExamController;
import com.example.examguard.controller.layout.DashboardShellController;
import com.example.examguard.controller.layout.ShellAwareController;
import com.example.examguard.model.exam.dto.*;
import com.example.examguard.model.exam.request.EssayReviewRequest;
import com.example.examguard.model.exam.request.EssayRubricRequest;
import com.example.examguard.model.exam.request.EssayRubricScoreRequest;
import com.example.examguard.model.exam.response.EssayRubricScoreResponse;
import com.example.examguard.model.exam.response.ExamResponse;
import com.example.examguard.model.exam.result.ExamResult;
import com.example.examguard.model.exam.result.ExamRow;
import com.example.examguard.model.faculty.dto.FacultyClassDTO;
import com.example.examguard.model.faculty.dto.FacultyExamStudentDTO;
import com.example.examguard.model.faculty.response.FacultyAttemptReviewResponse;
import com.example.examguard.model.faculty.response.FacultyExamDetailResponse;
import com.example.examguard.model.faculty.response.SimpleMessageResponse;
import com.example.examguard.service.ExamApiService;
import com.example.examguard.service.FacultyApiService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class ExamManagementController implements ShellAwareController {

    private final ExamApiService examApiService = new ExamApiService();
    private final FacultyApiService facultyApiService = new FacultyApiService();
    private final LocalCacheService localCacheService = new LocalCacheService();

    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(250));
    private final ObservableList<ExamRow> examRows = FXCollections.observableArrayList();
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
    @FXML private ComboBox<String> termFilterComboBox;
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
    @FXML private Button activityLogTabButton;
    @FXML private Button leaderboardTabButton;
    private FacultyExamStudentDTO currentReviewStudent;
    private FacultyAttemptReviewResponse currentAttemptReview;
    private FilteredList<ExamRow> filteredRows;
    private DashboardShellController shellController;
    private Task<List<ExamResponse>> currentLoadTask;
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

        termFilterComboBox.valueProperty().addListener((
                obs, oldVal, newVal) -> applyFilters());

        reportsTabButton.setOnAction(
                e -> showWorkspaceReports()
        );
    }

    @FXML
    private void showWorkspaceOverview() {
        setActiveWorkspaceTab(overviewTabButton);

        if (selectedWorkspaceExamId == null) {
            return;
        }

        boolean hasCache =
                loadWorkspaceOverviewFromCache(selectedWorkspaceExamId);

        if (!hasCache) {
            workspaceContent.getChildren().setAll(
                    createLoadingBox("Loading exam overview...")
            );
        }

        Task<FacultyExamDetailResponse> task = new Task<>() {
            @Override
            protected FacultyExamDetailResponse call() throws Exception {
                return examApiService.getExamDetail(selectedWorkspaceExamId);
            }
        };

        task.setOnSucceeded(event -> {
            FacultyExamDetailResponse detail = task.getValue();

            localCacheService.save(
                    workspaceOverviewCacheKey(selectedWorkspaceExamId),
                    ExamLocalCacheKeys.VERSION,
                    detail
            );

            renderWorkspaceOverview(detail);
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();

            if (ex != null) {
                ex.printStackTrace();
            }

            if (!hasCache) {
                workspaceContent.getChildren().setAll(
                        createLoadingBox("Unable to load exam overview.")
                );
            }
        });

        Thread thread = new Thread(task, "load-exam-overview-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private boolean loadWorkspaceOverviewFromCache(Long examId) {
        try {
            FacultyExamDetailResponse cached =
                    localCacheService.loadData(
                            workspaceOverviewCacheKey(examId),
                            FacultyExamDetailResponse.class
                    );

            if (cached == null) {
                return false;
            }

            renderWorkspaceOverview(cached);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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

        boolean hasCache =
                loadWorkspaceStudentsFromCache(selectedWorkspaceExamId);

        if (!hasCache) {
            workspaceContent.getChildren().setAll(
                    createLoadingBox("Loading students...")
            );
        }

        Task<List<FacultyExamStudentDTO>> task = new Task<>() {
            @Override
            protected List<FacultyExamStudentDTO> call() throws Exception {
                return examApiService.getExamStudents(selectedWorkspaceExamId);
            }
        };

        task.setOnSucceeded(event -> {
            List<FacultyExamStudentDTO> students = task.getValue();

            if (students == null) {
                students = List.of();
            }

            localCacheService.save(
                    workspaceStudentsCacheKey(selectedWorkspaceExamId),
                    ExamLocalCacheKeys.VERSION,
                    students
            );

            renderWorkspaceStudents(students);
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();

            if (ex != null) {
                ex.printStackTrace();
            }

            if (!hasCache) {
                workspaceContent.getChildren().setAll(
                        createLoadingBox("Unable to load students.")
                );
            }
        });

        Thread thread = new Thread(task, "load-exam-students-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private boolean loadWorkspaceStudentsFromCache(Long examId) {
        try {
            List<FacultyExamStudentDTO> cached =
                    localCacheService.loadList(
                            workspaceStudentsCacheKey(examId),
                            FacultyExamStudentDTO.class
                    );

            if (cached == null) {
                return false;
            }

            renderWorkspaceStudents(cached);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void deleteExamManagementCache(Long examId) {
        localCacheService.delete(examsCacheKey());

        if (examId != null) {
            localCacheService.delete(workspaceOverviewCacheKey(examId));
            localCacheService.delete(workspaceStudentsCacheKey(examId));
        }
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
                deleteExamManagementCache(selectedWorkspaceExamId);

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
                answerList.getChildren().add(createLoadingBox("No answers found for this filter."));
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

    private VBox createAnswerReviewCard(ExamAttemptAnswerReviewDTO answer) {
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
                    new Image(AppConfig.BASE_URL + answer.getQuestionImageUrl(), true)
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
                    new Image(AppConfig.BASE_URL + answer.getCorrectAnswerImageUrl(), true)
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
                    new Image(AppConfig.BASE_URL + answer.getStudentAnswerImageUrl(), true)
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

        Button timelineButton = new Button("Review Timeline");
        timelineButton.getStyleClass().add("outline-button");

        timelineButton.setOnAction(e ->
                openAnswerReviewTimelineModal(answer)
        );

        actionBar.getChildren().add(timelineButton);

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
                    deleteExamManagementCache(selectedWorkspaceExamId);

                    answer.setEarnedPoints(BigDecimal.valueOf(finalEnteredScore));
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

    private void openAnswerReviewTimelineModal(
            ExamAttemptAnswerReviewDTO answer
    ) {
        if (answer == null || answer.getAnswerId() == null) {
            showError("Answer ID not found.");
            return;
        }

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Review Timeline");

        Label title = new Label(
                "Question " + defaultInt(answer.getQuestionNumber()) +
                        " • Review Timeline"
        );
        title.getStyleClass().add("workspace-page-title");

        Label subtitle = new Label(
                "Historical changes for scores, violation decisions, and review actions."
        );
        subtitle.getStyleClass().add("workspace-page-subtitle");

        VBox timelineBox = new VBox(12);
        timelineBox.getChildren().add(createLoadingBox("Loading timeline..."));

        VBox root = new VBox(16, title, subtitle, timelineBox);
        root.getStyleClass().add("workspace-card");
        root.setPadding(new Insets(24));
        root.setPrefWidth(780);
        root.setPrefHeight(620);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("workspace-scroll");

        Scene scene = new Scene(scrollPane);
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/styles/exam-management.css")
                ).toExternalForm()
        );
        stage.setScene(scene);
        stage.show();

        Task<List<AnswerReviewTimelineDTO>> task = new Task<>() {
            @Override
            protected List<AnswerReviewTimelineDTO> call() throws Exception {
                return examApiService.getAnswerReviewTimeline(
                        answer.getAnswerId()
                );
            }
        };

        task.setOnSucceeded(event -> {
            List<AnswerReviewTimelineDTO> logs = task.getValue();

            timelineBox.getChildren().clear();

            if (logs == null || logs.isEmpty()) {
                Label empty = new Label("No review timeline yet.");
                empty.getStyleClass().add("workspace-section-subtitle");
                timelineBox.getChildren().add(empty);
                return;
            }

            for (AnswerReviewTimelineDTO log : logs) {
                timelineBox.getChildren().add(
                        createTimelineLogCard(log)
                );
            }
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            if (ex != null) {
                ex.printStackTrace();
            }

            timelineBox.getChildren().clear();

            Label error = new Label("Unable to load review timeline.");
            error.getStyleClass().add("workspace-section-subtitle");
            timelineBox.getChildren().add(error);
        });

        Thread thread = new Thread(task, "load-answer-review-timeline");
        thread.setDaemon(true);
        thread.start();
    }

    private VBox createTimelineLogCard(AnswerReviewTimelineDTO log) {

        Label actionLabel = new Label(formatTimelineAction(log.getActionType()));
        actionLabel.getStyleClass().add("timeline-action-title");

        Label timeLabel = new Label(
                formatDateTime(log.getCreatedAt()) +
                        " • " +
                        safe(log.getCreatedBy()) +
                        " (" +
                        safe(log.getCreatedByRole()) +
                        ")"
        );
        timeLabel.getStyleClass().add("timeline-meta-text");

        String scoreText =
                "Score: " +
                        formatPoints(log.getScoreBefore()) +
                        " → " +
                        formatPoints(log.getScoreAfter());

        if (log.getDeduction() != null
                && log.getDeduction().compareTo(BigDecimal.ZERO) > 0) {
            scoreText += " • Deduction: " + formatPoints(log.getDeduction());
        }

        Label scoreLabel = new Label(scoreText);
        scoreLabel.getStyleClass().add("timeline-score-text");

        Label statusLabel = new Label(
                "Status: " +
                        safe(log.getPreviousValue()) +
                        " → " +
                        safe(log.getNewValue())
        );
        statusLabel.getStyleClass().add("timeline-status-text");

        Label notesLabel = new Label(
                log.getNotes() == null || log.getNotes().isBlank()
                        ? "No notes provided."
                        : log.getNotes()
        );
        notesLabel.setWrapText(true);
        notesLabel.getStyleClass().add("timeline-notes-text");

        VBox card = new VBox(
                6,
                actionLabel,
                timeLabel,
                statusLabel,
                scoreLabel,
                notesLabel
        );

        card.getStyleClass().add("timeline-log-card");
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
                case "IGNORED" -> "View Evidence • Ignored";
                case "PENALIZED" -> "View Evidence • Penalized";
                default -> "View Evidence • Reviewed";
            };

            reviewButton.setText(label);
            reviewButton.setDisable(false);
            reviewButton.getStyleClass().add("workspace-status-button");
            reviewButton.setOnAction(e -> openViolationReviewModal(answer));

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

        Button reviseButton = new Button("Revise Decision");
        reviseButton.getStyleClass().add("review-revise-button");

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

            List<EvidenceFrame> frames = getEvidenceFrames(violation);

            if (frames.isEmpty()) {

                Label placeholder = new Label("No evidence image available.");
                placeholder.getStyleClass().add("workspace-empty-text");
                previewPane.getChildren().add(placeholder);

            } else {

                HBox frameRow = new HBox(14);
                frameRow.setAlignment(Pos.CENTER);
                frameRow.setFillHeight(true);

                for (int i = 0; i < frames.size(); i++) {
                    final int frameIndex = i;
                    EvidenceFrame frame = frames.get(i);

                    VBox frameCard = new VBox(8);
                    frameCard.setAlignment(Pos.CENTER);
                    frameCard.getStyleClass().add("evidence-frame-card");

                    Label frameLabel = new Label(
                            formatEvidenceLabel(frame.label(), i)
                    );
                    frameLabel.getStyleClass().add("evidence-frame-label");

                    ImageView imageView = new ImageView(
                            new Image(buildImageUrl(frame.url()), true)
                    );

                    imageView.setPreserveRatio(true);
                    imageView.setSmooth(true);

                    if (frames.size() == 1) {
                        imageView.setFitWidth(760);
                        imageView.setFitHeight(360);
                    } else {
                        imageView.setFitWidth(250);
                        imageView.setFitHeight(190);
                    }

                    imageView.getStyleClass().add("evidence-frame-image");

                    imageView.setOnMouseClicked(e ->
                            openEvidenceImagePreview(
                                    buildImageUrl(frame.url()),
                                    formatEvidenceLabel(frame.label(), frameIndex)
                            )
                    );

                    Label offsetLabel = new Label(
                            frame.offsetMs() == null
                                    ? ""
                                    : frame.offsetMs() + " ms"
                    );
                    offsetLabel.getStyleClass().add("evidence-frame-offset");

                    frameCard.getChildren().addAll(
                            frameLabel,
                            imageView,
                            offsetLabel
                    );

                    frameRow.getChildren().add(frameCard);
                }

                ScrollPane frameScroll = new ScrollPane(frameRow);
                frameScroll.setFitToHeight(true);
                frameScroll.setFitToWidth(true);
                frameScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                frameScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                frameScroll.getStyleClass().add("evidence-frame-scroll");

                previewPane.getChildren().add(frameScroll);
            }

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

                deleteExamManagementCache(selectedWorkspaceExamId);

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

                double currentScore = answer.getEarnedPoints() == null ? 0 : answer.getEarnedPoints().doubleValue();

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

                deleteExamManagementCache(selectedWorkspaceExamId);

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

        reviseButton.setOnAction(e -> {

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);

            confirm.setTitle("Revise Decision");

            confirm.setHeaderText(
                    "Allow changing this violation decision?"
            );

            confirm.setContentText(
                    "The previous deduction will be recalculated automatically."
            );

            Optional<ButtonType> result =
                    confirm.showAndWait();

            if (result.isEmpty()
                    || result.get() != ButtonType.OK) {
                return;
            }

            feedbackArea.setEditable(true);
            feedbackArea.requestFocus();

            deductionField.setDisable(false);

            ignoreButton.setDisable(false);
            applyButton.setDisable(false);

            reviseButton.setDisable(true);
            reviseButton.setText("Revision Mode");
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

        boolean alreadyFinalized = violations.stream()
                .anyMatch(this::isViolationFinalized);

        reviseButton.setVisible(alreadyFinalized);
        reviseButton.setManaged(alreadyFinalized);

        if (alreadyFinalized) {
            feedbackArea.setEditable(false);
            deductionField.setDisable(true);
            ignoreButton.setDisable(true);
            applyButton.setDisable(true);

            reviseButton.setVisible(true);
            reviseButton.setManaged(true);

        } else {
            reviseButton.setVisible(false);
            reviseButton.setManaged(false);
        }

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        HBox headerRow = new HBox(
                12,
                title,
                titleSpacer,
                reviseButton
        );

        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(
                16,
                headerRow,
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

                    return q.equalsIgnoreCase(selectedQuestion);
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

        TableColumn<ExamActivityLogDTO, String> actorIdColumn =
                new TableColumn<>("Actor ID");

        actorIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safe(data.getValue().getActorId())
                )
        );

        TableColumn<ExamActivityLogDTO, String> actorNameColumn =
                new TableColumn<>("Actor Name");

        actorNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safe(data.getValue().getActorName())
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
                actorIdColumn,
                actorNameColumn,
                studentIdColumn,
                studentNameColumn,
                questionColumn,
                actionColumn,
                severityColumn,
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

        renderWorkspaceReports();
    }

    private void renderWorkspaceReports() {

        VBox root = new VBox(16);

        root.getStyleClass().add(
                "workspace-overview-root"
        );

        Label pageTitle =
                new Label(
                        "Reports"
                );

        pageTitle.getStyleClass().add(
                "workspace-page-title"
        );

        Label pageSubtitle =
                new Label(
                        "Generate exam reports and analytics."
                );

        pageSubtitle.getStyleClass().add(
                "workspace-page-subtitle"
        );

        VBox pageHeader =
                new VBox(
                        3,
                        pageTitle,
                        pageSubtitle
                );

        VBox portfolioCard =
                createReportCard(
                        "Exam Portfolio",
                        "Questions, answers, summaries and exam content.",
                        "Export PDF",
                        e -> handleExportPortfolio()
                );

        VBox summaryCard =
                createReportCard(
                        "Exam Result Summary",
                        "Performance analytics, score distribution, question analysis and violations.",
                        "Export PDF",
                        e -> handleExportExamResultSummary()
                );

        VBox classRecordCard =
                createReportCard(
                        "Class Record",
                        "Student scores per exam, average percentage, and section grade record.",
                        "Export PDF",
                        e -> handleExportClassRecord()
                );

        HBox reportRow = new HBox(16);

        reportRow.setAlignment(Pos.TOP_LEFT);

        HBox.setHgrow(portfolioCard, Priority.ALWAYS);
        HBox.setHgrow(summaryCard, Priority.ALWAYS);
        HBox.setHgrow(classRecordCard, Priority.ALWAYS);

        portfolioCard.setMaxWidth(Double.MAX_VALUE);
        summaryCard.setMaxWidth(Double.MAX_VALUE);
        classRecordCard.setMaxWidth(Double.MAX_VALUE);

        reportRow.getChildren().addAll(
                portfolioCard,
                summaryCard,
                classRecordCard
        );


        root.getChildren().addAll(pageHeader, reportRow);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("workspace-scroll");
        workspaceContent.getChildren().setAll(scrollPane);
    }

    private VBox createReportCard(
            String title,
            String description,
            String buttonText,
            javafx.event.EventHandler<javafx.event.ActionEvent> action
    ) {

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("workspace-section-title");
        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.getStyleClass().add("workspace-page-subtitle");

        Button button = new Button(buttonText);
        button.getStyleClass().add("workspace-review-button");
        button.setOnAction(action);
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox card = new VBox(14, titleLabel, descLabel, spacer, button);

        card.setMinHeight(180);
        card.setPrefWidth(420);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("workspace-card");

        return card;
    }

    private void handleExportPortfolio() {

        if (selectedWorkspaceExamId == null) {
            showError("No exam selected.");
            return;
        }

        Task<FacultyExamDetailResponse> task = new Task<>() {
            @Override
            protected FacultyExamDetailResponse call() throws Exception {
                return examApiService.getExamDetail(selectedWorkspaceExamId);
            }
        };

        task.setOnSucceeded(event -> {
            FacultyExamDetailResponse detail = task.getValue();

            if (detail == null) {
                showError("Exam not found.");
                return;
            }

            List<FacultyClassDTO> classes = detail.getAssignedClasses();

            if (classes == null || classes.isEmpty()) {
                showError("No assigned classes found.");
                return;
            }

            showPortfolioModePicker(classes);
        });

        task.setOnFailed(event -> showError("Unable to load exam details."));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void handleExportClassRecord() {

        if (selectedWorkspaceExamId == null) {
            showError("No exam selected.");
            return;
        }

        Task<FacultyExamDetailResponse> task =
                new Task<>() {

                    @Override
                    protected FacultyExamDetailResponse call()
                            throws Exception {

                        return examApiService.getExamDetail(
                                selectedWorkspaceExamId
                        );
                    }
                };

        task.setOnSucceeded(event -> {

            FacultyExamDetailResponse detail =
                    task.getValue();

            if (detail == null) {
                showError("Exam not found.");
                return;
            }

            List<FacultyClassDTO> classes =
                    detail.getAssignedClasses();

            if (classes == null || classes.isEmpty()) {
                showError("No assigned classes found.");
                return;
            }

            if (classes.size() == 1) {
                exportWorkspaceClassRecord(
                        classes.get(0).getClassOfferingId()
                );
                return;
            }

            showClassRecordSectionPicker(classes);
        });

        task.setOnFailed(event -> {
            showError("Unable to load exam details.");
        });

        Thread thread =
                new Thread(task);

        thread.setDaemon(true);
        thread.start();
    }

    private void showClassRecordSectionPicker(
            List<FacultyClassDTO> classes
    ) {
        List<String> sectionOptions =
                classes.stream()
                        .map(this::formatClassOption)
                        .toList();

        ChoiceDialog<String> dialog =
                new ChoiceDialog<>(
                        sectionOptions.get(0),
                        sectionOptions
                );

        dialog.setTitle("Export Class Record");
        dialog.setHeaderText("Choose section for class record");
        dialog.setContentText("Section:");

        dialog.showAndWait()
                .ifPresent(selectedLabel -> {

                    FacultyClassDTO selectedClass =
                            classes.stream()
                                    .filter(c ->
                                            formatClassOption(c)
                                                    .equals(selectedLabel)
                                    )
                                    .findFirst()
                                    .orElse(null);

                    if (selectedClass == null) {
                        showError("Selected class not found.");
                        return;
                    }

                    exportWorkspaceClassRecord(
                            selectedClass.getClassOfferingId()
                    );
                });
    }

    private void exportWorkspaceClassRecord(
            String classOfferingId
    ) {
        if (classOfferingId == null || classOfferingId.isBlank()) {
            showError("Class offering ID not found.");
            return;
        }

        try {
            byte[] bytes =
                    facultyApiService.exportClassRecordPdf(
                            classOfferingId
                    );

            boolean saved =
                    saveBytesToFile(
                            bytes,
                            "class-record.pdf",
                            "PDF Files",
                            "*.pdf"
                    );

            if (!saved) {
                return;
            }

            showInfo(
                    "Export Successful",
                    "Class record exported successfully."
            );

        } catch (Exception e) {
            e.printStackTrace();
            showError("Unable to export class record.");
        }
    }

    private void showPortfolioModePicker(List<FacultyClassDTO> classes) {

        ChoiceDialog<String> dialog =
                new ChoiceDialog<>(
                        "MERGED",
                        "MERGED",
                        "SEPARATE"
                );

        dialog.setTitle("Export Exam Portfolio");
        dialog.setHeaderText("Select export mode");
        dialog.setContentText("Mode:");

        dialog.showAndWait().ifPresent(mode -> {

            if ("MERGED".equalsIgnoreCase(mode)) {
                exportWorkspaceExamPortfolio("MERGED", null);
                return;
            }

            showPortfolioSectionPicker(classes);
        });
    }

    private void showPortfolioSectionPicker(List<FacultyClassDTO> classes) {

        List<String> sectionOptions = classes.stream()
                .map(this::formatClassOption)
                .toList();

        ChoiceDialog<String> dialog =
                new ChoiceDialog<>(
                        sectionOptions.get(0),
                        sectionOptions
                );

        dialog.setTitle("Export Exam Portfolio");
        dialog.setHeaderText("Choose section");
        dialog.setContentText("Section:");

        dialog.showAndWait().ifPresent(selectedLabel -> {

            FacultyClassDTO selectedClass =
                    classes.stream()
                            .filter(c -> formatClassOption(c).equals(selectedLabel))
                            .findFirst()
                            .orElse(null);

            if (selectedClass == null) {
                showError("Selected class not found.");
                return;
            }

            exportWorkspaceExamPortfolio(
                    "SEPARATE",
                    selectedClass.getClassOfferingId()
            );
        });
    }

    private void exportWorkspaceExamPortfolio(
            String mode,
            String classOfferingId
    ) {

        try {
            byte[] bytes =
                    facultyApiService.exportExamPortfolio(
                            selectedWorkspaceExamId,
                            mode,
                            classOfferingId
                    );

            boolean saved =
                    saveBytesToFile(
                            bytes,
                            "exam-portfolio.pdf",
                            "PDF Files",
                            "*.pdf"
                    );

            if (!saved) {
                return;
            }

            showInfo("Export Successful", "Exam portfolio exported successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            showError("Unable to export exam portfolio.");
        }
    }

    private void handleExportExamResultSummary() {

        if (selectedWorkspaceExamId == null) {
            showError("No exam selected.");
            return;
        }

        Task<FacultyExamDetailResponse> task =
                new Task<>() {

                    @Override
                    protected FacultyExamDetailResponse call()
                            throws Exception {

                        return examApiService.getExamDetail(
                                selectedWorkspaceExamId
                        );
                    }
                };

        task.setOnSucceeded(event -> {

            FacultyExamDetailResponse detail =
                    task.getValue();

            if (detail == null) {
                showError("Exam not found.");
                return;
            }

            List<FacultyClassDTO> classes =
                    detail.getAssignedClasses();

            if (classes == null || classes.isEmpty()) {
                showError("No assigned classes found.");
                return;
            }

            if (classes.size() == 1) {

                exportWorkspaceExamResultSummary(
                        null
                );

                return;
            }

            showSummaryModePicker(classes);

        });

        task.setOnFailed(event -> {
            showError(
                    "Unable to load exam details."
            );
        });

        Thread thread =
                new Thread(task);

        thread.setDaemon(true);
        thread.start();
    }

    private void showSummaryModePicker(
            List<FacultyClassDTO> classes
    ) {

        ChoiceDialog<String> dialog =
                new ChoiceDialog<>(
                        "MERGED",
                        "MERGED",
                        "SEPARATE"
                );

        dialog.setTitle(
                "Export Exam Result Summary"
        );

        dialog.setHeaderText(
                "Select export mode"
        );

        dialog.setContentText(
                "Mode:"
        );

        dialog.showAndWait()
                .ifPresent(mode -> {

                    if ("MERGED".equals(mode)) {

                        exportWorkspaceExamResultSummary(
                                null
                        );

                        return;
                    }

                    showSectionPicker(classes);

                });
    }

    private void showSectionPicker(
            List<FacultyClassDTO> classes
    ) {
        List<String> sectionOptions = classes.stream()
                .map(this::formatClassOption)
                .toList();

        ChoiceDialog<String> dialog =
                new ChoiceDialog<>(
                        sectionOptions.get(0),
                        sectionOptions
                );

        dialog.setTitle("Select Class");
        dialog.setHeaderText("Choose section");
        dialog.setContentText("Section:");

        dialog.showAndWait()
                .ifPresent(selectedLabel -> {

                    FacultyClassDTO selectedClass =
                            classes.stream()
                                    .filter(c -> formatClassOption(c).equals(selectedLabel))
                                    .findFirst()
                                    .orElse(null);

                    if (selectedClass == null) {
                        showError("Selected class not found.");
                        return;
                    }

                    exportWorkspaceExamResultSummary(
                            selectedClass.getClassOfferingId()
                    );
                });
    }

    private String formatClassOption(FacultyClassDTO item) {
        if (item == null) {
            return "Unknown Section";
        }

        return safe(item.getCourseCode()) +
                " • " +
                safe(item.getProgramCode()) +
                " " +
                item.getYearLevel() +
                "-" +
                safe(item.getSectionName());
    }

    private void exportWorkspaceExamResultSummary(
            String classOfferingId
    ) {

        try {

            byte[] bytes =
                    facultyApiService
                            .exportExamResultSummary(
                                    selectedWorkspaceExamId,
                                    classOfferingId
                            );

            boolean saved =
                    saveBytesToFile(
                            bytes,
                            "exam-result-summary.pdf",
                            "PDF Files",
                            "*.pdf"
                    );

            if (!saved) {
                return;
            }

            showInfo(
                    "Export Successful",
                    "Exam result summary exported."
            );

        } catch (Exception e) {

            e.printStackTrace();

            showError(
                    "Unable to export report."
            );
        }
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
                deleteExamManagementCache(selectedWorkspaceExamId);
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

            private final Button workspaceBtn = new Button("Workspace");
            private final Button editBtn = new Button("Edit");
            private final Button publishBtn = new Button("Publish");
            private final Button cancelBtn = new Button("Cancel");
            private final Button restoreBtn = new Button("Restore");

            private final HBox actionBox = new HBox(
                    8,
                    workspaceBtn,
                    editBtn,
                    publishBtn,
                    cancelBtn,
                    restoreBtn
            );

            {
                actionBox.setAlignment(Pos.CENTER);

                styleButton(workspaceBtn, "#302C29", "white");
                styleButton(editBtn, "#800000", "white");
                styleButton(publishBtn, "#15803D", "white");
                styleButton(cancelBtn, "#B91C1C", "white");
                styleButton(restoreBtn, "#D4AF37", "white");

                workspaceBtn.setOnAction(event -> {
                    ExamRow row = getRowData();
                    if (row == null) return;

                    openExamWorkspace(
                            row.getExamId(),
                            row.getTitle(),
                            row.getStatus()
                    );
                });

                editBtn.setOnAction(event -> {
                    ExamRow row = getRowData();
                    if (row == null) return;

                    openExamWizard(row.getExamId(), false);
                });

                publishBtn.setOnAction(event -> {
                    ExamRow row = getRowData();
                    if (row == null) return;

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Publish Exam");
                    confirm.setHeaderText("Publish this exam?");
                    confirm.setContentText(
                            "Students assigned to this exam will be able to see it.\n\n" +
                                    row.getTitle()
                    );

                    Optional<ButtonType> result = confirm.showAndWait();

                    if (result.isEmpty() || result.get() != ButtonType.OK) {
                        return;
                    }

                    try {
                        runExamActionAsync("publish-exam-thread", row.getExamId(), () -> {
                            try {
                                examApiService.publishExamById(row.getExamId());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        showError("Unable to publish exam.");
                    }
                });

                cancelBtn.setOnAction(event -> {
                    ExamRow row = getRowData();
                    if (row == null) return;

                    ButtonType yesBtn = new ButtonType("Yes, Cancel");
                    ButtonType noBtn = new ButtonType(
                            "No",
                            ButtonBar.ButtonData.CANCEL_CLOSE
                    );

                    Alert alert = new Alert(
                            Alert.AlertType.CONFIRMATION,
                            "Are you sure you want to cancel this exam?\n\nExam: " +
                                    row.getTitle(),
                            yesBtn,
                            noBtn
                    );

                    alert.setTitle("Confirm Cancel");
                    alert.setHeaderText("Cancel Exam");

                    Optional<ButtonType> result = alert.showAndWait();

                    if (result.isEmpty() || result.get() != yesBtn) {
                        return;
                    }

                    try {
                        runExamActionAsync("cancel-exam-thread", row.getExamId(), () -> {
                            try {
                                examApiService.cancelExam(row.getExamId());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        showError("Unable to cancel exam.");
                    }
                });

                restoreBtn.setOnAction(event -> {
                    ExamRow row = getRowData();
                    if (row == null) return;

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Restore Exam");
                    confirm.setHeaderText("Restore cancelled exam?");
                    confirm.setContentText(
                            "This will restore the exam back to draft.\n\n" +
                                    row.getTitle()
                    );

                    Optional<ButtonType> result = confirm.showAndWait();

                    if (result.isEmpty() || result.get() != ButtonType.OK) {
                        return;
                    }

                    try {
                        runExamActionAsync("cancel-exam-thread", row.getExamId(), () -> {
                            try {
                                examApiService.cancelExam(row.getExamId());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        showError("Unable to restore exam.");
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
                if (getIndex() < 0 ||
                        getIndex() >= getTableView().getItems().size()) {
                    return null;
                }

                return getTableView().getItems().get(getIndex());
            }

            private void updateButtonVisibility(String status) {

                String normalized = safe(status).toUpperCase();

                boolean isDraft = "DRAFT".equals(normalized);
                boolean isScheduled = "SCHEDULED".equals(normalized);
                boolean isAssigned = "ASSIGNED".equals(normalized);
                boolean isPublished = "PUBLISHED".equals(normalized);
                boolean isOngoing = "ONGOING".equals(normalized);
                boolean isExpired = "EXPIRED".equals(normalized);
                boolean isCompleted = "COMPLETED".equals(normalized);
                boolean isCancelled = "CANCELLED".equals(normalized);

                setButtonVisible(editBtn, isDraft);
                setButtonVisible(publishBtn, isDraft);

                setButtonVisible(
                        workspaceBtn,
                        isScheduled ||
                                isAssigned ||
                                isPublished ||
                                isOngoing ||
                                isExpired ||
                                isCompleted
                );

                setButtonVisible(
                        cancelBtn,
                        isScheduled ||
                                isAssigned ||
                                isPublished
                );

                setButtonVisible(restoreBtn, isCancelled);
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

            deleteExamManagementCache(examId);
            loadExamsFromBackend();


        } catch (IOException e) {
            e.printStackTrace();
            showError("Unable to open exam wizard.");
        }
    }

    private void loadExamsFromBackend() {

        if (loading) return;

        boolean hasCache = loadExamsFromCache();

        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            currentLoadTask.cancel();
        }

        if (!hasCache) {
            setLoading(true);
        } else {
            setRefreshingInBackground(true);
        }

        currentLoadTask = new Task<>() {
            @Override
            protected List<ExamResponse> call() throws Exception {
                return examApiService.fetchExams();
            }
        };

        currentLoadTask.setOnSucceeded(event -> {
            List<ExamResponse> responses = currentLoadTask.getValue();

            if (responses == null) {
                responses = List.of();
            }

            localCacheService.save(
                    examsCacheKey(),
                    ExamLocalCacheKeys.VERSION,
                    responses
            );

            populateFilterDropdowns(responses);

            List<ExamRow> rows =
                    responses.stream()
                            .map(ExamManagementController.this::mapToExamRow)
                            .toList();

            examRows.setAll(rows);
            applyFilters();
            updateItemCount();
            updateShellExamCards();

            setLoading(false);
            setRefreshingInBackground(false);
        });

        currentLoadTask.setOnFailed(event -> {
            Throwable ex = currentLoadTask.getException();

            if (ex != null) {
                ex.printStackTrace();
            }

            updateItemCount();

            setLoading(false);
            setRefreshingInBackground(false);

            if (!hasCache) {
                showError("Unable to load exams. Please check your backend connection.");
            }
        });

        Thread thread = new Thread(currentLoadTask, "load-exams-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void setRefreshingInBackground(boolean refreshing) {
        if (reloadButton != null) {
            reloadButton.setDisable(refreshing);
        }

        if (itemCountLabel != null && refreshing) {
            itemCountLabel.setText(
                    filteredRows == null
                            ? examRows.size() + " Items • Refreshing..."
                            : filteredRows.size() + " Items • Refreshing..."
            );
        }
    }

    private boolean loadExamsFromCache() {
        try {
            List<ExamResponse> cachedResponses =
                    localCacheService.loadList(
                            examsCacheKey(),
                            ExamResponse.class
                    );

            if (cachedResponses == null) {
                return false;
            }

            populateFilterDropdowns(cachedResponses);

            List<ExamRow> rows =
                    cachedResponses.stream()
                            .map(this::mapToExamRow)
                            .toList();

            examRows.setAll(rows);
            applyFilters();
            updateItemCount();
            updateShellExamCards();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    private String currentRole() {
        String role = Session.getRole();

        if (role == null || role.isBlank()) {
            return "UNKNOWN_ROLE";
        }

        return role;
    }

    private String currentUserId() {
        String id = Session.getSchoolId();

        if (id == null || id.isBlank()) {
            return "UNKNOWN_USER";
        }

        return id;
    }

    private String examsCacheKey() {
        return ExamLocalCacheKeys.exams(
                currentRole(),
                currentUserId()
        );
    }

    private String workspaceOverviewCacheKey(Long examId) {
        return ExamLocalCacheKeys.workspaceOverview(
                currentRole(),
                currentUserId(),
                examId
        );
    }

    private String workspaceStudentsCacheKey(Long examId) {
        return ExamLocalCacheKeys.workspaceStudents(
                currentRole(),
                currentUserId(),
                examId
        );
    }

    private void populateFilterDropdowns(List<ExamResponse> exams) {

        List<ExamResponse> sortedExams = exams.stream()
                .sorted((a, b) -> {

                    int ayCompare = academicYearStart(b.getAcademicYear())
                            .compareTo(academicYearStart(a.getAcademicYear()));

                    if (ayCompare != 0) {
                        return ayCompare;
                    }

                    return Integer.compare(
                            termRank(a.getTerm()),
                            termRank(b.getTerm())
                    );
                })
                .toList();

        List<String> terms = sortedExams.stream()
                .map(exam -> safe(exam.getTerm()) + ", AY " + safe(exam.getAcademicYear()))
                .filter(value -> !value.equals(", AY "))
                .distinct()
                .toList();

        termFilterComboBox.getItems().clear();
        termFilterComboBox.getItems().add("All Terms");
        termFilterComboBox.getItems().addAll(terms);

        if (!terms.isEmpty()) {
            termFilterComboBox.getSelectionModel().select(1);
        } else {
            termFilterComboBox.getSelectionModel().selectFirst();
        }

        applyFilters();
    }

    private Integer academicYearStart(String academicYear) {
        if (academicYear == null || academicYear.isBlank()) {
            return 0;
        }

        try {
            return Integer.parseInt(academicYear.substring(0, 4));
        } catch (Exception e) {
            return 0;
        }
    }

    private int termRank(String term) {
        if (term == null) {
            return 99;
        }

        String normalized = term.trim().toUpperCase();

        return switch (normalized) {
            case "SUMMER", "SUMMER TERM" -> 1;

            case "SECOND SEMESTER",
                 "2ND SEMESTER",
                 "SECOND" -> 2;

            case "FIRST SEMESTER",
                 "1ST SEMESTER",
                 "FIRST" -> 3;

            case "FOURTH QUARTER",
                 "4TH QUARTER",
                 "Q4" -> 1;

            case "THIRD QUARTER",
                 "3RD QUARTER",
                 "Q3" -> 2;

            case "SECOND QUARTER",
                 "2ND QUARTER",
                 "Q2" -> 3;

            case "FIRST QUARTER",
                 "1ST QUARTER",
                 "Q1" -> 4;

            default -> 99;
        };
    }

    private void applyFilters() {
        if (filteredRows == null) {
            return;
        }

        String keyword = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().toLowerCase().trim();

        String selectedTerm = termFilterComboBox == null
                ? "All Terms"
                : termFilterComboBox.getValue();

        filteredRows.setPredicate(row -> {
            boolean matchesSearch =
                    keyword.isBlank()
                            || contains(row.getTitle(), keyword)
                            || contains(row.getStatus(), keyword)
                            || contains(row.getAssigned(), keyword)
                            || contains(row.getDuration(), keyword)
                            || contains(row.getTakers(), keyword)
                            || contains(row.getDateCreated(), keyword)
                            || contains(row.getValidity(), keyword)
                            || contains(row.getCreatedBy(), keyword)
                            || contains(row.getUpdatedBy(), keyword)
                            || contains(row.getTerm(), keyword)
                            || contains(row.getAcademicYear(), keyword);

            boolean matchesTerm;

            if (selectedTerm == null || selectedTerm.equals("All Terms")) {
                matchesTerm = true;
            } else {
                String rowTerm = safe(row.getTerm()) + ", AY " + safe(row.getAcademicYear());
                matchesTerm = selectedTerm.equalsIgnoreCase(rowTerm);
            }


            return matchesSearch && matchesTerm;
        });

        updateShellExamCards();
        updateItemCount();
    }

    private ExamRow mapToExamRow(ExamResponse exam) {
        return new ExamRow(
                exam.getExamId(),
                safe(exam.getDateCreated()),
                safe(exam.getTitle()),
                safe(exam.getStatus()),
                safe(exam.getDuration()),
                safe(exam.getAssigned()),
                safe(exam.getTerm()),
                safe(exam.getAcademicYear()),
                formatTakers(exam.getTakers()),
                formatSchedule(exam.getStartDateTime(), exam.getEndDateTime()),
                safe(exam.getCreatedBy()),
                safe(exam.getUpdatedBy())
        );
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

    private List<EvidenceFrame> getEvidenceFrames(ExamAttemptViolationDTO violation) {

        List<EvidenceFrame> frames = new ArrayList<>();

        if (violation == null) {
            return frames;
        }

        JsonObject metadata = violation.getEvidenceMetadata();

        if (metadata != null
                && metadata.has("frames")
                && metadata.get("frames").isJsonArray()) {

            JsonArray frameArray = metadata.getAsJsonArray("frames");

            for (JsonElement element : frameArray) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }

                JsonObject frameObject = element.getAsJsonObject();

                String url = getJsonString(frameObject, "url");
                String label = getJsonString(frameObject, "label");
                Integer offsetMs = getJsonInt(frameObject, "offsetMs");

                if (url != null && !url.isBlank()) {
                    frames.add(new EvidenceFrame(
                            url,
                            label == null || label.isBlank() ? "evidence" : label,
                            offsetMs
                    ));
                }
            }
        }

        if (frames.isEmpty()
                && violation.getEvidenceUrl() != null
                && !violation.getEvidenceUrl().isBlank()) {

            frames.add(new EvidenceFrame(
                    violation.getEvidenceUrl(),
                    "evidence",
                    0
            ));
        }

        return frames;
    }

    private String getJsonString(JsonObject object, String key) {
        if (object == null || key == null) {
            return null;
        }

        if (!object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }

        return object.get(key).getAsString();
    }

    private Integer getJsonInt(JsonObject object, String key) {
        if (object == null || key == null) {
            return null;
        }

        if (!object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }

        try {
            return object.get(key).getAsInt();
        } catch (Exception e) {
            return null;
        }
    }

    private String buildImageUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return "";
        }

        if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            return rawUrl;
        }

        if (rawUrl.startsWith("/")) {
            return AppConfig.BASE_URL + rawUrl;
        }

        return AppConfig.BASE_URL + "/" + rawUrl;
    }

    private String formatEvidenceLabel(String label, int index) {
        if (label == null || label.isBlank()) {
            return "Frame " + (index + 1);
        }

        return switch (label.toLowerCase()) {
            case "before" -> "Before";
            case "during" -> "During";
            case "after" -> "After";
            case "screenshot" -> "Screenshot";
            default -> formatStatus(label);
        };
    }

    private void openEvidenceImagePreview(String imageUrl, String titleText) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(titleText == null || titleText.isBlank()
                ? "Evidence Preview"
                : titleText
        );

        ImageView imageView = new ImageView(
                new Image(imageUrl, true)
        );

        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setFitWidth(1100);
        imageView.setFitHeight(760);

        Label hint = new Label("Click anywhere to close");
        hint.getStyleClass().add("evidence-preview-hint");

        VBox box = new VBox(12, imageView, hint);
        box.setAlignment(Pos.CENTER);

        StackPane root = new StackPane(box);
        root.getStyleClass().add("evidence-preview-root");
        root.setOnMouseClicked(e -> stage.close());

        Scene scene = new Scene(root, 1200, 820);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private boolean isViolationFinalized(ExamAttemptViolationDTO violation) {
        if (violation == null) {
            return false;
        }

        String status = safe(violation.getReviewStatus());

        return "IGNORED".equalsIgnoreCase(status)
                || "PENALIZED".equalsIgnoreCase(status)
                || "REVIEWED".equalsIgnoreCase(status)
                || "UPHELD".equalsIgnoreCase(status);
    }

    private void updateShellExamCards() {
        if (shellController == null) {
            return;
        }

        long drafts = filteredRows.stream()
                .filter(row -> "DRAFT".equalsIgnoreCase(safe(row.getStatus())))
                .count();

        long published = filteredRows.stream()
                .filter(row ->
                        "PUBLISHED".equalsIgnoreCase(safe(row.getStatus())) ||
                                "SCHEDULED".equalsIgnoreCase(safe(row.getStatus())) ||
                                "ASSIGNED".equalsIgnoreCase(safe(row.getStatus()))
                )
                .count();

        long ongoing = filteredRows.stream()
                .filter(row -> "ONGOING".equalsIgnoreCase(safe(row.getStatus())))
                .count();

        long cancelled = filteredRows.stream()
                .filter(row -> "CANCELLED".equalsIgnoreCase(safe(row.getStatus())))
                .count();

        long expired = filteredRows.stream()
                .filter(row -> "EXPIRED".equalsIgnoreCase(safe(row.getStatus())))
                .count();

        long completed = filteredRows.stream()
                .filter(row -> "COMPLETED".equalsIgnoreCase(safe(row.getStatus())))
                .count();

        shellController.setHeroCards(
                new DashboardShellController.HeroCardData("Drafts", String.valueOf(drafts)),
                new DashboardShellController.HeroCardData("Scheduled", String.valueOf(published)),
                new DashboardShellController.HeroCardData("Ongoing", String.valueOf(ongoing)),
                new DashboardShellController.HeroCardData("Cancelled", String.valueOf(cancelled)),
                new DashboardShellController.HeroCardData("Expired", String.valueOf(expired)),
                new DashboardShellController.HeroCardData("Completed", String.valueOf(completed))

        );
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
        updateShellExamCards();
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

        if (!showProctor) {
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
            BigDecimal rubricTotal = calculateEssayRubricTotal(rowsBox, answer);

            BigDecimal actualEarned = answer.getEarnedPoints() == null
                    ? BigDecimal.ZERO
                    : answer.getEarnedPoints();

            totalLabel.setText(
                    "Rubric Score: " +
                            formatPoints(rubricTotal) +
                            " / " +
                            formatPoints(answer.getPoints()) +
                            " pts" +
                            "  • Final Score: " +
                            formatPoints(actualEarned) +
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

    private void runExamActionAsync(
            String threadName,
            Runnable action
    ) {
        runExamActionAsync(threadName, null, action);
    }

    private void runExamActionAsync(
            String threadName,
            Long affectedExamId,
            Runnable action
    ) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                action.run();
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            deleteExamManagementCache(affectedExamId);
            loadExamsFromBackend();
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();

            if (ex != null) {
                ex.printStackTrace();
            }

            showError("Unable to complete action.");
        });

        Thread thread = new Thread(task, threadName);
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
                activityLogTabButton,
                leaderboardTabButton,
                reportsTabButton
        );

        for (Button button : buttons) {
            button.getStyleClass().remove("workspace-tab-active");
        }

        if (activeButton != null &&
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

    private void showInfo(
            String title,
            String message
    ) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean saveBytesToFile(
            byte[] bytes,
            String fileName,
            String description,
            String extension
    ) {

        try {

            FileChooser chooser =
                    new FileChooser();

            chooser.setTitle(
                    "Save Report"
            );

            chooser.setInitialFileName(
                    fileName
            );

            chooser.getExtensionFilters()
                    .add(
                            new FileChooser.ExtensionFilter(
                                    description,
                                    extension
                            )
                    );

            File file =
                    chooser.showSaveDialog(
                            workspaceContent
                                    .getScene()
                                    .getWindow()
                    );

            if (file == null) {
                return false;
            }

            Files.write(
                    file.toPath(),
                    bytes
            );

            Desktop.getDesktop()
                    .open(file);

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
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

    private String formatTimelineAction(String actionType) {
        if (actionType == null || actionType.isBlank()) {
            return "Review Update";
        }

        return switch (actionType.toUpperCase()) {
            case "VIOLATION_PENALIZED" -> "Violation Penalized";
            case "VIOLATION_IGNORED" -> "Violation Ignored";
            case "VIOLATION_REVISED" -> "Violation Revised";
            case "SCORE_UPDATED" -> "Score Updated";
            case "ESSAY_REVIEWED" -> "Essay Reviewed";
            case "ATTEMPT_MARKED_REVIEWED" -> "Attempt Marked Reviewed";
            default -> formatStatus(actionType);
        };
    }

    private record EvidenceFrame(
            String url,
            String label,
            Integer offsetMs
    ) {
    }

    private record EssayRubricRowData(Long rubricId, TextField percentField, TextField feedbackField) {
    }

}