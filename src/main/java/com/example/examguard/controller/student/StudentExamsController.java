package com.example.examguard.controller.student;

import com.example.examguard.controller.layout.DashboardShellController;
import com.example.examguard.cache.LocalCacheService;
import com.example.examguard.cache.StudentLocalCacheKeys;
import com.example.examguard.utility.Session;
import com.example.examguard.controller.layout.ShellAwareController;
import com.example.examguard.model.student.StudentExamResponse;
import com.example.examguard.model.student.dashboard.ExamCardVM;
import com.example.examguard.service.StudentApiService;
import javafx.application.Platform;
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
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StudentExamsController implements ShellAwareController {

    private DashboardShellController shellController;

    @FXML private ComboBox<String> termComboBox;
    @FXML private ScrollPane examScrollPane;
    @FXML private Label pageInfoLabel;
    @FXML private TextField searchField;
    @FXML private TilePane examCardGrid;

    @FXML private Button allButton;
    @FXML private Button upcomingButton;
    @FXML private Button inProgressButton;
    @FXML private Button submittedButton;
    @FXML private Button resultsButton;
    @FXML private Button missedButton;

    private boolean examsCacheShown = false;

    private static final int PAGE_SIZE = 10;
    private int currentPage = 0;
    final double headerWidth = 300;
    private String selectedStatusFilter = "ALL";
    private final List<ExamCardVM> allExams = new ArrayList<>();
    private List<ExamCardVM> filteredExams = new ArrayList<>();
    private final Image defaultExamImage = new Image(getClass().getResourceAsStream("/images/exam-card-default.png"));

    private final StudentApiService studentApiService = new StudentApiService();
    private final LocalCacheService localCacheService = new LocalCacheService();

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

    private boolean canEnterLobbyNow(OffsetDateTime startValue, OffsetDateTime endValue) {
        OffsetDateTime start = toManila(startValue);
        OffsetDateTime end = toManila(endValue);

        if (start == null || end == null) {
            return false;
        }

        OffsetDateTime now = nowManila();
        OffsetDateTime lobbyOpenAt = start.minusMinutes(LOBBY_OPEN_MINUTES_BEFORE_START);

        return !now.isBefore(lobbyOpenAt) && !now.isAfter(end);
    }

    @Override
    public void setShellController(DashboardShellController shellController) {
        this.shellController = shellController;
    }

    @FXML
    public void initialize() {
        termComboBox.getSelectionModel().selectFirst();

        examCardGrid.prefTileWidthProperty().set(300);
        examCardGrid.prefTileHeightProperty().set(275);
        examCardGrid.setAlignment(Pos.TOP_LEFT);
        examCardGrid.setTileAlignment(Pos.TOP_LEFT);

        examScrollPane.setFitToWidth(true);
        examScrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            examCardGrid.setPrefWidth(newBounds.getWidth());
            updateResponsiveColumns(newBounds.getWidth());
        });

        Platform.runLater(() -> {
            examsCacheShown = loadCachedStudentExamsFirst();
            loadStudentExams();
        });
        termComboBox.setOnAction(event -> applyCombinedFilters());
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            applyCombinedFilters();
        });

    }

    public void applyInitialFilter(String filter) {

        selectedStatusFilter = normalizeFilter(filter);

        switch (selectedStatusFilter) {
            case "UPCOMING" -> setActiveFilterButton(upcomingButton);
            case "PENDING REVIEW" -> setActiveFilterButton(submittedButton);
            case "RESULTS RELEASED" -> setActiveFilterButton(resultsButton);
            case "ONGOING" -> setActiveFilterButton(inProgressButton);
            case "DID NOT TAKE" -> setActiveFilterButton(missedButton);
            default -> setActiveFilterButton(allButton);
        }

        applyCombinedFilters();
    }

    private String normalizeFilter(String filter) {

        if (filter == null || filter.isBlank()) {
            return "ALL";
        }

        String value = filter.trim().toUpperCase();

        return switch (value) {
            case "UPCOMING" -> "UPCOMING";

            case "PENDING_REVIEW", "PENDING REVIEW" -> "PENDING REVIEW";

            case "RESULTS_RELEASED", "RESULTS RELEASED" -> "RESULTS RELEASED";

            case "ONGOING" -> "ONGOING";

            case "DID_NOT_TAKE", "DID NOT TAKE" -> "DID NOT TAKE";

            default -> "ALL";
        };
    }

    private boolean loadCachedStudentExamsFirst() {
        String schoolId = Session.getSchoolId();

        if (schoolId == null || schoolId.isBlank()) {
            return false;
        }

        List<StudentExamResponse> cachedExams =
                localCacheService.loadList(
                        StudentLocalCacheKeys.exams(schoolId),
                        StudentExamResponse.class
                );

        if (cachedExams == null || cachedExams.isEmpty()) {
            return false;
        }

        List<ExamCardVM> cards = cachedExams.stream()
                .map(this::mapToExamCardVM)
                .toList();

        renderExamCards(cards);
        updateHeroCards(cards);
        populateTermDropdown(cards);

        return true;
    }

    private void loadStudentExams() {

        Task<List<StudentExamResponse>> task = new Task<>() {
            @Override
            protected List<StudentExamResponse> call() throws Exception {
                return studentApiService.getStudentExams();
            }
        };

        task.setOnSucceeded(event -> {
            List<ExamCardVM> cards = task.getValue()
                    .stream()
                    .map(this::mapToExamCardVM)
                    .toList();

            renderExamCards(cards);
            updateHeroCards(cards);
            populateTermDropdown(cards);

            String schoolId = Session.getSchoolId();

            if (schoolId != null && !schoolId.isBlank()) {
                localCacheService.save(
                        StudentLocalCacheKeys.exams(schoolId),
                        String.valueOf(System.currentTimeMillis()),
                        task.getValue()
                );
            }
        });


        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            ex.printStackTrace();

            if (!examsCacheShown) {
                showError("Failed to load exams.");
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void populateTermDropdown(List<ExamCardVM> cards) {

        List<ExamCardVM> sortedCards = cards.stream()
                .sorted((a, b) -> {

                    // ACTIVE first
                    int activeCompare = Boolean.compare(
                            isActiveOffering(b.classOfferingStatus()),
                            isActiveOffering(a.classOfferingStatus())
                    );

                    if (activeCompare != 0) {
                        return activeCompare;
                    }

                    // Latest academic year first
                    int ayCompare = academicYearStart(b.academicYear())
                            .compareTo(academicYearStart(a.academicYear()));

                    if (ayCompare != 0) {
                        return ayCompare;
                    }

                    // Latest term first
                    return Integer.compare(
                            termRank(a.term()),
                            termRank(b.term())
                    );
                })
                .toList();

        List<String> terms = sortedCards.stream()
                .map(card ->
                        card.term() + ", AY " + card.academicYear()
                )
                .distinct()
                .toList();

        termComboBox.getItems().clear();
        termComboBox.getItems().add("All Terms");
        termComboBox.getItems().addAll(terms);

        // Select latest automatically
        if (!terms.isEmpty()) {
            termComboBox.getSelectionModel().select(1);
        } else {
            termComboBox.getSelectionModel().selectFirst();
        }

        applyCombinedFilters();
    }

    private ExamCardVM mapToExamCardVM(StudentExamResponse dto) {

        String schedule = formatSchedule(
                dto.getStartDateTime(),
                dto.getEndDateTime()
        );

        return new ExamCardVM(
                dto.getExamId(),
                dto.getTitle(),
                dto.getCourseCode(),
                dto.getCourseDescription(),
                dto.getTerm(),
                dto.getAcademicYear(),
                dto.getClassOfferingStatus(),
                dto.getMode(),
                dto.getFaculty(),
                dto.getDurationMinutes(),
                schedule,
                computeStudentExamStatus(dto),
                dto.getQuestionCount(),
                Boolean.TRUE.equals(dto.getActionable()),
                dto.getStartDateTime(),
                dto.getEndDateTime()
        );
    }

    private VBox createExamCard(
            Long examId,
            String title,
            String courseCode,
            String courseDescription,
            String mode,
            String faculty,
            Integer duration,
            String schedule,
            String status,
            Long questionCount,
            boolean actionable
    ) {

        final double headerWidth = 300;
        final double headerHeight = 108;

        StackPane imageBox = new StackPane();
        imageBox.getStyleClass().add("exam-card-image");
        imageBox.setMinSize(headerWidth, headerHeight);
        imageBox.setPrefSize(headerWidth, headerHeight);
        imageBox.setMaxSize(headerWidth, headerHeight);

        Rectangle rounded = new Rectangle(headerWidth, headerHeight);
        rounded.setArcWidth(30);
        rounded.setArcHeight(30);

        Rectangle bottomFix = new Rectangle(
                0,
                headerHeight - 16,
                headerWidth,
                16
        );

        Shape headerClip = Shape.union(rounded, bottomFix);
        imageBox.setClip(headerClip);

        ImageView examImage = new ImageView(defaultExamImage);
        examImage.setFitWidth(headerWidth);
        examImage.setFitHeight(headerHeight);
        examImage.setPreserveRatio(false);

        Rectangle readabilityGradient = new Rectangle(headerWidth, headerHeight);

        readabilityGradient.setFill(
                new LinearGradient(
                        0, 0, 1, 0,
                        true,
                        CycleMethod.NO_CYCLE,
                        new Stop(0.00, Color.rgb(212, 175, 55, 0.58)),
                        new Stop(0.30, Color.rgb(212, 175, 55, 0.34)),
                        new Stop(0.60, Color.rgb(212, 175, 55, 0.12)),
                        new Stop(1.00, Color.rgb(212, 175, 55, 0.00))
                )
        );

        readabilityGradient.setMouseTransparent(true);

        readabilityGradient.setMouseTransparent(true);

        Label courseCodeLabel = new Label(courseCode);
        courseCodeLabel.getStyleClass().add("exam-card-course-code");

        Label courseDescriptionLabel = new Label(courseDescription);
        courseDescriptionLabel.getStyleClass().add("exam-card-course-description");
        courseDescriptionLabel.setWrapText(true);
        courseDescriptionLabel.setMaxWidth(170);

        courseCodeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        courseDescriptionLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.92); -fx-font-size: 11.5px; -fx-font-weight: 600;");

        VBox courseTextBox = new VBox(2, courseCodeLabel, courseDescriptionLabel);
        courseTextBox.setMouseTransparent(true);

        courseTextBox.setAlignment(Pos.CENTER_LEFT);

        StackPane.setAlignment(courseTextBox, Pos.CENTER_LEFT);
        StackPane.setMargin(courseTextBox, new Insets(0, 0, 0, 18));

        Label arrowBadge = new Label("→");
        arrowBadge.getStyleClass().add("exam-card-arrow-badge");
        arrowBadge.setVisible(actionable);
        arrowBadge.setManaged(actionable);

        StackPane.setAlignment(arrowBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(arrowBadge, new Insets(8, 8, 0, 0));

        imageBox.getChildren().addAll(
                examImage,
                readabilityGradient,
                courseTextBox,
                arrowBadge
        );

        Label statusLabel = new Label(status);
        statusLabel.getStyleClass().addAll(
                "exam-status-pill",
                getStatusClass(status)
        );

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("exam-card-title");
        titleLabel.setWrapText(true);

        HBox modeDurationRow = createIconTextRow(
                "/icons/online-learning.png",
                mode + " | " + formatDuration(duration),
                "exam-card-meta"
        );

        HBox facultyRow = createIconTextRow(
                "/icons/teacher.png",
                faculty,
                "exam-card-meta"
        );

        HBox scheduleRow = createIconTextRow(
                "/icons/calendar.png",
                schedule,
                "exam-card-schedule"
        );


        VBox body = new VBox(
                7,
                statusLabel,
                titleLabel,
                modeDurationRow,
                facultyRow,
                scheduleRow
        );

        body.setPadding(new Insets(10));
        body.setFillWidth(true);

        VBox card = new VBox(imageBox, body);
        card.getStyleClass().add("exam-card");

        card.setOnMouseClicked(event -> {
            if (!actionable) {
                return;
            }

            if ("LOBBY OPEN".equalsIgnoreCase(status)
                    || "ONGOING".equalsIgnoreCase(status)) {

                openExamLobby(examId, title, duration, questionCount, null, null);

            } else if ("RESULTS RELEASED".equalsIgnoreCase(status)) {
                openStudentResultsWorkspace(examId);
            }
        });

        return card;
    }

    private String getStatusClass(String status) {
        return switch (status.toUpperCase()) {

            case "UPCOMING" -> "exam-status-upcoming";

            case "ONGOING", "LOBBY OPEN" -> "exam-status-progress";

            case "PENDING REVIEW" -> "exam-status-violation";

            case "RESULTS RELEASED" -> "exam-status-released";

            case "DID NOT TAKE" -> "exam-status-missed";

            default -> "exam-status-submitted";
        };
    }

    private void updateResponsiveColumns(double width) {

        double padding = 12;
        double gap = 14;
        double usableWidth = width - padding;

        int columns;

        if (usableWidth >= 1500) {
            columns = 5;
        } else {
            columns = 4;
        }

        double cardWidth =
                (usableWidth - (gap * (columns - 1))) / columns;

        cardWidth = Math.max(280, Math.min(cardWidth, 330));

        examCardGrid.setPrefColumns(columns);
        examCardGrid.setPrefTileWidth(cardWidth);
        examCardGrid.setHgap(gap);
        examCardGrid.setVgap(18);
    }

    private void renderPage() {
        examCardGrid.getChildren().clear();

        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, filteredExams.size());

        if (from >= filteredExams.size()) {
            currentPage = 0;
            from = 0;
            to = Math.min(PAGE_SIZE, filteredExams.size());
        }

        for (int i = from; i < to; i++) {
            examCardGrid.getChildren().add(createExamCard(filteredExams.get(i)));
        }

        int totalPages = Math.max(1, (int) Math.ceil(filteredExams.size() / (double) PAGE_SIZE));
        pageInfoLabel.setText("Page " + (currentPage + 1) + " of " + totalPages);
    }

    private void applyFilter(String filter) {
        selectedStatusFilter = filter;
        applyCombinedFilters();
    }

    private void applyCombinedFilters() {

        String selectedTerm =
                termComboBox.getSelectionModel().getSelectedItem();

        String keyword = searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase();

        filteredExams = allExams.stream()
                .filter(exam -> {

                    boolean matchesStatus =
                            "ALL".equals(selectedStatusFilter)
                                    || selectedStatusFilter.equalsIgnoreCase(exam.status());

                    boolean matchesTerm;

                    if (selectedTerm == null
                            || selectedTerm.equals("All Terms")) {
                        matchesTerm = true;
                    } else {
                        String examTerm =
                                exam.term() + ", AY " + exam.academicYear();

                        matchesTerm = selectedTerm.equalsIgnoreCase(examTerm);
                    }

                    boolean matchesSearch =
                            keyword.isBlank()
                                    || containsIgnoreCase(exam.title(), keyword)
                                    || containsIgnoreCase(exam.courseCode(), keyword)
                                    || containsIgnoreCase(exam.courseDescription(), keyword)
                                    || containsIgnoreCase(exam.faculty(), keyword);

                    return matchesStatus && matchesTerm && matchesSearch;
                })
                .toList();

        currentPage = 0;
        renderPage();
    }

    private VBox createExamCard(ExamCardVM exam) {
        VBox card = createExamCard(
                exam.examId(),
                exam.title(),
                exam.courseCode(),
                exam.courseDescription(),
                exam.mode(),
                exam.faculty(),
                exam.duration(),
                exam.schedule(),
                exam.status(),
                exam.questionCount(),
                exam.actionable()
        );

        /*
         * We attach the correct click handler here because this method has access
         * to startDateTime and endDateTime from ExamCardVM.
         */
        card.setOnMouseClicked(event -> {
            if (!exam.actionable()) {
                return;
            }

            if ("LOBBY OPEN".equalsIgnoreCase(exam.status())
                    || "ONGOING".equalsIgnoreCase(exam.status())) {

                openExamLobby(
                        exam.examId(),
                        exam.title(),
                        exam.duration(),
                        exam.questionCount(),
                        exam.startDateTime(),
                        exam.endDateTime()
                );

            } else if ("RESULTS RELEASED".equalsIgnoreCase(exam.status())) {
                openStudentResultsWorkspace(exam.examId());
            }
        });

        return card;
    }

    private void renderExamCards(List<ExamCardVM> cards) {
        allExams.clear();
        allExams.addAll(cards);

        filteredExams = new ArrayList<>(allExams);
        currentPage = 0;

        renderPage();
    }

    @FXML
    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            renderPage();
        }
    }

    @FXML
    private void nextPage() {
        int totalPages = Math.max(1, (int) Math.ceil(filteredExams.size() / (double) PAGE_SIZE));

        if (currentPage < totalPages - 1) {
            currentPage++;
            renderPage();
        }
    }

    private void setActiveFilterButton(Button activeButton) {
        List<Button> buttons = List.of(
                allButton,
                upcomingButton,
                inProgressButton,
                submittedButton,
                resultsButton,
                missedButton
        );

        for (Button button : buttons) {
            button.getStyleClass().removeAll("exam-filter-active", "exam-filter-button");

            if (button == activeButton) {
                button.getStyleClass().add("exam-filter-active");
            } else {
                button.getStyleClass().add("exam-filter-button");
            }
        }
    }

    // ============
    // FILTER
    // ============

    @FXML
    private void filterAll() {
        setActiveFilterButton(allButton);
        applyFilter("ALL");
    }

    @FXML
    private void filterUpcoming() {
        setActiveFilterButton(upcomingButton);
        applyFilter("UPCOMING");
    }

    @FXML
    private void filterOngoing() {
        setActiveFilterButton(inProgressButton);
        applyFilter("ONGOING");
    }

    @FXML
    private void filterPendingReview() {
        setActiveFilterButton(submittedButton);
        applyFilter("PENDING REVIEW");
    }

    @FXML
    private void filterResultsReleased() {
        setActiveFilterButton(resultsButton);
        applyFilter("RESULTS RELEASED");
    }

    @FXML
    private void filterMissed() {
        setActiveFilterButton(missedButton);
        applyFilter("DID NOT TAKE");
    }

    private void updateHeroCards(List<ExamCardVM> cards) {
        if (shellController == null) {
            return;
        }

        long ongoing = countByStatus(cards, "ONGOING");
        long upcoming = countByStatus(cards, "UPCOMING");
        long pendingReview = countByStatus(cards, "PENDING REVIEW");
        long resultsReleased = countByStatus(cards, "RESULTS RELEASED");
        long didNotTake = countByStatus(cards, "DID NOT TAKE");

        shellController.setHeroCards(
                new DashboardShellController.HeroCardData("Ongoing", String.valueOf(ongoing)),
                new DashboardShellController.HeroCardData("Upcoming", String.valueOf(upcoming)),
                new DashboardShellController.HeroCardData("Pending Review", String.valueOf(pendingReview)),
                new DashboardShellController.HeroCardData("Results Released", String.valueOf(resultsReleased)),
                new DashboardShellController.HeroCardData("Did Not Take", String.valueOf(didNotTake))
        );
    }

    // ============
    // HELP
    // ============
    private String formatDuration(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            return "-";
        }

        int hours = minutes / 60;
        int mins = minutes % 60;

        if (hours > 0 && mins > 0) {
            return hours + "hr " + mins + "mins";
        }

        if (hours > 0) {
            return hours + "hr";
        }

        return mins + "mins";
    }

    private String formatSchedule(OffsetDateTime start, OffsetDateTime end) {

        if (start == null || end == null) {
            return "-";
        }

        ZoneId manilaZone = ZoneId.of("Asia/Manila");

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

        String startText = start
                .atZoneSameInstant(manilaZone)
                .format(formatter);

        String endText = end
                .atZoneSameInstant(manilaZone)
                .format(formatter);

        return startText + " – " + endText;
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private long countByStatus(List<ExamCardVM> cards, String status) {
        return cards.stream()
                .filter(card -> status.equalsIgnoreCase(card.status()))
                .count();
    }

    private boolean isActiveOffering(String status) {
        return status != null && status.equalsIgnoreCase("ACTIVE");
    }

    private int termRank(String term) {
        if (term == null) {
            return 99;
        }

        String normalized = term.trim().toUpperCase();

        return switch (normalized) {

            // College / SHS style
            case "SUMMER", "SUMMER TERM" -> 1;

            case "SECOND SEMESTER",
                 "2ND SEMESTER",
                 "SECOND" -> 2;

            case "FIRST SEMESTER",
                 "1ST SEMESTER",
                 "FIRST" -> 3;

            // Elementary / quarterly style
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

    private Integer academicYearStart(String academicYear) {

        if (academicYear == null || academicYear.isBlank()) {
            return 0;
        }

        try {
            return Integer.parseInt(
                    academicYear.substring(0, 4)
            );
        } catch (Exception e) {
            return 0;
        }
    }

    private void openExamLobby(
            Long examId,
            String title,
            Integer durationMinutes,
            Long questionCount,
            OffsetDateTime startDateTime,
            OffsetDateTime endDateTime
    ) {

        if (examId == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/student/exam-taking.fxml")
            );

            Parent root = loader.load();

            ExamTakingController controller = loader.getController();

            controller.setExamLobby(
                    examId,
                    title,
                    durationMinutes == null ? 60 : durationMinutes,
                    questionCount,
                    startDateTime,
                    endDateTime
            );

            Scene scene = new Scene(root);

            Stage stage = (Stage) examCardGrid.getScene().getWindow();

            stage.setTitle("ExamGuard - Taking Exam");
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();

            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            stage.setFullScreen(true);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open exam lobby.");
        }
    }

    private void openStudentResultsWorkspace(Long examId) {

        if (examId == null) {
            return;
        }

        try {

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

            controller.loadResult(examId);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open released results.");
        }
    }

    private int parseDurationToMinutes(String durationText) {

        if (durationText == null || durationText.isBlank()) {
            return 60;
        }

        String value = durationText.toLowerCase().trim();

        int totalMinutes = 0;

        try {
            if (value.contains("hr")) {
                String hourPart = value.substring(0, value.indexOf("hr")).trim();
                totalMinutes += Integer.parseInt(hourPart) * 60;
            }

            if (value.contains("mins")) {
                String beforeMins = value.substring(0, value.indexOf("mins")).trim();

                String[] parts = beforeMins.split(" ");

                String minutePart = parts[parts.length - 1].trim();

                totalMinutes += Integer.parseInt(minutePart);
            }

            return totalMinutes > 0 ? totalMinutes : 60;

        } catch (Exception e) {
            return 60;
        }
    }

    private String computeStudentExamStatus(StudentExamResponse dto) {
        if (dto == null) {
            return "UPCOMING";
        }

        String status = dto.getStatus();

        if ("SUBMITTED".equalsIgnoreCase(status)
                || "PENDING REVIEW".equalsIgnoreCase(status)
                || "RESULTS RELEASED".equalsIgnoreCase(status)
                || "DID NOT TAKE".equalsIgnoreCase(status)) {
            return status;
        }

        OffsetDateTime start = dto.getStartDateTime();
        OffsetDateTime end = dto.getEndDateTime();

        if (start == null || end == null) {
            return status == null ? "UPCOMING" : status;
        }

        OffsetDateTime now = nowManila();
        OffsetDateTime startManila = toManila(start);
        OffsetDateTime endManila = toManila(end);
        OffsetDateTime lobbyOpenAt = startManila.minusMinutes(15);

        if (now.isAfter(endManila)) {
            return "DID NOT TAKE";
        }

        if (now.isBefore(lobbyOpenAt)) {
            return "UPCOMING";
        }

        if (now.isBefore(startManila)) {
            return "LOBBY OPEN";
        }

        return "ONGOING";
    }

    private HBox createIconTextRow(String iconPath, String text, String textStyleClass) {
        ImageView icon = new ImageView(
                new Image(getClass().getResourceAsStream(iconPath))
        );

        icon.setFitWidth(14);
        icon.setFitHeight(14);
        icon.setPreserveRatio(true);
        icon.setSmooth(true);

        Label label = new Label(text == null ? "" : text);
        label.getStyleClass().add(textStyleClass);
        label.setWrapText(true);

        HBox row = new HBox(6, icon, label);
        row.setAlignment(Pos.CENTER_LEFT);

        return row;
    }

}