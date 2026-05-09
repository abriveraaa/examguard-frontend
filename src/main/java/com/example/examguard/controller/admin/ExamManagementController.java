package com.example.examguard.controller.admin;

import com.example.examguard.controller.layout.DashboardShellController;
import com.example.examguard.controller.layout.ShellAwareController;
import com.example.examguard.controller.exam.CreateExamController;
import com.example.examguard.model.faculty.*;
import com.example.examguard.model.faculty.response.FacultyAttemptReviewResponse;
import com.example.examguard.model.faculty.response.FacultyExamDetailResponse;
import com.example.examguard.model.faculty.response.SimpleMessageResponse;
import com.example.examguard.service.FacultyApiService;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import com.example.examguard.model.exam.ExamResponse;
import com.example.examguard.model.exam.ExamResult;
import com.example.examguard.model.exam.ExamRow;
import com.example.examguard.service.ExamApiService;
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
import java.util.Objects;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
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
    private FilteredList<ExamRow> filteredRows;
    private DashboardShellController shellController;
    private Task<List<ExamRow>> currentLoadTask;

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
                return facultyApiService.getExamDetail(selectedWorkspaceExamId);
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

            openExamWorkspace(row.getExamId(), row.getTitle());
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
                return facultyApiService.getExamStudents(selectedWorkspaceExamId);
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

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("workspace-scroll");

        workspaceContent.getChildren().setAll(scrollPane);
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
                return facultyApiService.getStudentAttemptReview(
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
                    facultyApiService.markAttemptReviewed(review.getAttemptId());
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

        if (review.getAnswers() == null || review.getAnswers().isEmpty()) {
            answerList.getChildren().add(
                    createLoadingBox("No answers found for this attempt.")
            );
        } else {
            for (FacultyAttemptAnswerReviewDTO answer : review.getAnswers()) {
                answerList.getChildren().add(createAnswerReviewCard(answer));
            }
        }

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox headerActions = new HBox(10, backButton, headerSpacer, markReviewedButton);
        headerActions.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(
                16,
                headerActions,
                new VBox(3, title, subtitle),
                answerList
        );

        root.getStyleClass().add("workspace-overview-root");

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("workspace-scroll");

        workspaceContent.getChildren().setAll(scrollPane);
    }

    private VBox    createAnswerReviewCard(FacultyAttemptAnswerReviewDTO answer) {
        Label questionHeader = new Label(
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

        VBox studentAnswerBlock = createAnswerBlock(
                "Student Answer",
                safe(answer.getStudentAnswer()),
                Boolean.TRUE.equals(answer.getCorrect())
                        ? "review-answer-correct"
                        : "review-answer-wrong"
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

        HBox contentRow = new HBox(
                16,
                questionBox,
                correctAnswerBlock,
                studentAnswerBlock
        );
        contentRow.setAlignment(Pos.TOP_LEFT);
        contentRow.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(questionBox, Priority.ALWAYS);
        HBox.setHgrow(correctAnswerBlock, Priority.ALWAYS);
        HBox.setHgrow(studentAnswerBlock, Priority.ALWAYS);

        questionBox.setPrefWidth(420);
        correctAnswerBlock.setPrefWidth(300);
        studentAnswerBlock.setPrefWidth(300);

        VBox violationBox = createViolationBox(answer.getViolations());

        HBox actionBar = new HBox(10);
        actionBar.setAlignment(Pos.CENTER_RIGHT);

        if (Boolean.TRUE.equals(answer.getNeedsManualCheck())
                || Boolean.TRUE.equals(answer.getManuallyReviewed())) {
            Label pointsLabel = new Label("Points Earned");
            pointsLabel.getStyleClass().add("review-answer-label");

            TextField scoreField = new TextField(formatPoints(answer.getEarnedPoints()));
            scoreField.getStyleClass().add("review-score-field");
            scoreField.setPrefWidth(90);

            Label outOfLabel = new Label("/ " + formatPoints(answer.getPoints()) + " pts");
            outOfLabel.getStyleClass().add("review-score-total");

            Button saveScoreButton = new Button("✔ Save Score");
            saveScoreButton.getStyleClass().add("review-accept-button");

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

                saveScoreButton.setDisable(true);
                saveScoreButton.setText("Saving...");

                double finalEnteredScore = enteredScore;

                Task<Void> saveTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {

                        facultyApiService.updateAnswerScore(
                                answer.getAnswerId(),
                                finalEnteredScore
                        );

                        return null;
                    }
                };

                saveTask.setOnSucceeded(e -> {

                    saveScoreButton.setText("✔ Saved");

                    openStudentReview(currentReviewStudent);
                });

                saveTask.setOnFailed(e -> {

                    Throwable ex = saveTask.getException();

                    if (ex != null) {
                        ex.printStackTrace();
                    }

                    saveScoreButton.setDisable(false);
                    saveScoreButton.setText("✔ Save Score");

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
            scoreBox.setAlignment(Pos.CENTER_RIGHT);

            actionBar.getChildren().add(scoreBox);
        }

        VBox card = new VBox(
                14,
                topRow,
                contentRow,
                violationBox
        );

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

    private Label createAnswerResultBadge(FacultyAttemptAnswerReviewDTO answer) {
        Label badge = new Label();

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

    private Label createReviewStatusBadge(FacultyAttemptAnswerReviewDTO answer) {

        Label badge = new Label();

        if (Boolean.TRUE.equals(answer.getNeedsManualCheck())) {

            badge.setText("⚠ Needs Review");
            badge.getStyleClass().add("review-status-warning");

        } else if (Boolean.TRUE.equals(answer.getManuallyReviewed())) {

            badge.setText("✔ Reviewed");
            badge.getStyleClass().add("review-status-correct");

        } else {

            badge.setText("N/A");
            badge.getStyleClass().add("review-status-neutral");
        }

        return badge;
    }

    private VBox createViolationBox(List<FacultyAttemptViolationDTO> violations) {
        VBox box = new VBox(6);

        if (violations == null || violations.isEmpty()) {
            Label clear = new Label("No question-level violations.");
            clear.getStyleClass().add("review-clear-text");
            box.getChildren().add(clear);
            return box;
        }

        Label title = new Label("Violations");
        title.getStyleClass().add("review-violation-title");
        box.getChildren().add(title);

        for (FacultyAttemptViolationDTO violation : violations) {
            Label row = new Label(
                    safe(violation.getSeverity()) +
                            " • " +
                            safe(violation.getViolationType()) +
                            " • " +
                            safe(violation.getViolationMessage()) +
                            " • " +
                            formatDateTime(violation.getOccurredAt())
            );
            row.setWrapText(true);
            row.getStyleClass().add("review-violation-row");

            box.getChildren().add(row);
        }

        return box;
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

        Task<List<FacultyActivityLogDTO>> task = new Task<>() {
            @Override
            protected List<FacultyActivityLogDTO> call() throws Exception {
                return facultyApiService.getExamActivityLogs(selectedWorkspaceExamId);
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

    private void renderWorkspaceActivityLogs(List<FacultyActivityLogDTO> logs) {
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

        ObservableList<FacultyActivityLogDTO> rows =
                FXCollections.observableArrayList(logs);

        FilteredList<FacultyActivityLogDTO> filteredRows =
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
                .map(FacultyActivityLogDTO::getQuestionNumber)
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

        TableView<FacultyActivityLogDTO> table = new TableView<>();
        table.getStyleClass().add("workspace-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(filteredRows);

        TableColumn<FacultyActivityLogDTO, String> timeColumn = new TableColumn<>("Time");
        timeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatDateTime(data.getValue().getOccurredAt()))
        );

        TableColumn<FacultyActivityLogDTO, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(safe(data.getValue().getLogType()))
        );

        TableColumn<FacultyActivityLogDTO, String> studentIdColumn =
                new TableColumn<>("Student ID");

        studentIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safe(data.getValue().getStudentId())
                )
        );

        TableColumn<FacultyActivityLogDTO, String> studentNameColumn =
                new TableColumn<>("Student Name");

        studentNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safe(data.getValue().getStudentName())
                )
        );

        TableColumn<FacultyActivityLogDTO, String> questionColumn = new TableColumn<>("Question");
        questionColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getQuestionNumber() == null
                                ? "General"
                                : "Q" + data.getValue().getQuestionNumber()
                )
        );

        TableColumn<FacultyActivityLogDTO, String> actionColumn = new TableColumn<>("Action");
        actionColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatStatus(safe(data.getValue().getAction())))
        );

        TableColumn<FacultyActivityLogDTO, String> severityColumn = new TableColumn<>("Severity");
        severityColumn.setCellValueFactory(data ->
                new SimpleStringProperty(safe(data.getValue().getSeverity()))
        );

        TableColumn<FacultyActivityLogDTO, String> messageColumn = new TableColumn<>("Message");
        messageColumn.setCellValueFactory(data ->
                new SimpleStringProperty(safe(data.getValue().getMessage()))
        );

        TableColumn<FacultyActivityLogDTO, String> durationColumn = new TableColumn<>("Duration");
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

        root.getChildren().add(card);

        workspaceContent.getChildren().setAll(root);
    }

    private HBox createActivityLogRow(FacultyActivityLogDTO log) {
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

        Task<List<FacultyLeaderboardDTO>> task = new Task<>() {

            @Override
            protected List<FacultyLeaderboardDTO> call() throws Exception {

                return facultyApiService.getExamLeaderboard(
                        selectedWorkspaceExamId
                );
            }
        };

        task.setOnSucceeded(event -> {

            List<FacultyLeaderboardDTO> leaderboard =
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

    private void renderWorkspaceLeaderboard(List<FacultyLeaderboardDTO> leaderboard) {
        if (leaderboard == null) {
            leaderboard = List.of();
        }

        VBox root = new VBox(16);
        root.getStyleClass().add("workspace-overview-root");

        Label pageTitle = new Label("Leaderboard");
        pageTitle.getStyleClass().add("workspace-page-title");

        Label pageSubtitle = new Label(
                "Student ranking based on submitted scores. Final standing may change until results are released."
        );
        pageSubtitle.getStyleClass().add("workspace-page-subtitle");

        VBox pageHeader = new VBox(3, pageTitle, pageSubtitle);

        TableView<FacultyLeaderboardDTO> table = new TableView<>();
        table.getStyleClass().add("workspace-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(leaderboard));

        TableColumn<FacultyLeaderboardDTO, String> rankColumn =
                new TableColumn<>("Rank");

        rankColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getRank() == null
                                ? "—"
                                : "#" + data.getValue().getRank()
                )
        );

        TableColumn<FacultyLeaderboardDTO, String> studentColumn =
                new TableColumn<>("Student");

        studentColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safe(data.getValue().getStudentName())
                )
        );

        TableColumn<FacultyLeaderboardDTO, String> studentIdColumn =
                new TableColumn<>("Student ID");

        studentIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safe(data.getValue().getStudentId())
                )
        );

        TableColumn<FacultyLeaderboardDTO, String> sectionColumn =
                new TableColumn<>("Section");

        sectionColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safe(data.getValue().getSectionName())
                )
        );

        TableColumn<FacultyLeaderboardDTO, String> scoreColumn =
                new TableColumn<>("Score Obtained");

        scoreColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatScore(
                                data.getValue().getTotalScore(),
                                data.getValue().getTotalPossibleScore()
                        )
                )
        );

        TableColumn<FacultyLeaderboardDTO, String> percentColumn =
                new TableColumn<>("Percentage");

        percentColumn.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatPercent(data.getValue().getScorePercentage())
                )
        );

        TableColumn<FacultyLeaderboardDTO, String> violationColumn =
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

        TableColumn<FacultyLeaderboardDTO, String> reviewColumn =
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

        workspaceContent.getChildren().setAll(root);
    }


    @FXML
    private void handleReleaseResults() {
        if (selectedWorkspaceExamId == null) {
            showError("No exam selected.");
            return;
        }

        try {
            List<FacultyExamStudentDTO> students =
                    facultyApiService.getExamStudents(selectedWorkspaceExamId);

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
                return facultyApiService.releaseResults(selectedWorkspaceExamId);
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

            private final Button editBtn = new Button("Edit");
            private final Button proctorBtn = new Button("Proctor");
            private final Button viewBtn = new Button("View");
            private final Button publishBtn = new Button("Publish");
            private final Button cancelBtn = new Button("Cancel");
            private final Button restoreBtn = new Button("Restore");

            private final HBox actionBox = new HBox(
                    8,
                    editBtn,
                    proctorBtn,
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
                styleButton(editBtn, "#C9A227", "#302C29");
                styleButton(proctorBtn, "#800000", "white");

                viewBtn.setOnAction(event -> {
                    ExamRow row = getRowData();
                    if (row == null) return;

                    openExamWorkspace(row.getExamId(), row.getTitle());
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

                editBtn.setOnAction(event -> {
                    ExamRow row = getRowData();
                    if (row == null) return;

                    openExamWizard(row.getExamId(), false);
                });

                proctorBtn.setOnAction(event -> {
                    ExamRow row = getRowData();
                    if (row == null) return;

                    // openProctorPage(row.getExamId());
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
                boolean isCancelled = "CANCELLED".equalsIgnoreCase(status);

                setButtonVisible(editBtn, false);
                setButtonVisible(viewBtn, true);

                setButtonVisible(publishBtn, isDraft);
                setButtonVisible(cancelBtn, isDraft || isScheduled);
                setButtonVisible(proctorBtn, isOngoing);
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
        if (takers == null || takers.isBlank() || !takers.contains("/")) {
            return false;
        }

        try {
            String[] parts = takers.trim().split("/");

            int submitted = Integer.parseInt(parts[0].trim());
            int total = Integer.parseInt(parts[1].trim());

            return total > 0 && submitted >= total;

        } catch (Exception e) {
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

    private void openExamWorkspace(Long examId, String examTitle) {
        if (shellController != null) {
            shellController.hideHeroCards();
            shellController.hideHeroSection();
        }

        selectedWorkspaceExamId = examId;

        listModeContainer.setVisible(false);
        listModeContainer.setManaged(false);

        workspaceModeContainer.setVisible(true);
        workspaceModeContainer.setManaged(true);

        workspaceTitleLabel.setText(safe(examTitle));
        workspaceSubtitleLabel.setText("Review students, submissions, violations, and results.");

        showWorkspaceOverview();
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
                leaderboardTabButton
        );

        for (Button button : buttons) {
            button.getStyleClass().remove("workspace-tab-active");
        }

        if (activeButton != null) {
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

    private String formatPercent(Double value) {
        if (value == null) {
            return "0%";
        }

        return String.format("%.0f%%", value);
    }

}