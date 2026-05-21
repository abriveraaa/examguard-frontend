package com.example.examguard.controller.student;

import com.example.examguard.controller.layout.DashboardShellController;
import com.example.examguard.controller.layout.ShellAwareController;
import com.example.examguard.model.student.StudentExamResultResponse;
import com.example.examguard.utility.LoadingSpinner;
import com.example.examguard.model.student.result.*;
import com.example.examguard.service.StudentApiService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StudentResultsWorkspaceController implements ShellAwareController {

    @FXML private Button backButton;
    @FXML private Label examTitleLabel;
    @FXML private Label courseLabel;
    @FXML private Label metaLabel;
    @FXML private Label scoreLabel;
    @FXML private Label percentageLabel;
    @FXML private VBox questionContainer;
    @FXML private Button printReportButton;

    private DashboardShellController shellController;
    private final StudentApiService studentApiService = new StudentApiService();
    private Long currentExamId;

    @Override
    public void setShellController(DashboardShellController shellController) {
        this.shellController = shellController;
    }

    public void loadResult(Long examId) {
        this.currentExamId = examId;

        questionContainer.getChildren().setAll(new Label("Loading released result..."));

        printReportButton.setVisible(false);
        printReportButton.setManaged(false);

        Task<StudentExamResultResponse> task = new Task<>() {
            @Override
            protected StudentExamResultResponse call() throws Exception {
                return studentApiService.getStudentExamResult(examId);
            }
        };

        task.setOnSucceeded(event -> {
            renderResult(task.getValue());

            printReportButton.setVisible(true);
            printReportButton.setManaged(true);
            printReportButton.setOnAction(e -> downloadAnswerSheetReport());
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            showError("Failed to load released result.");
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void renderResult(StudentExamResultResponse result) {
        examTitleLabel.setText(nullSafe(result.getTitle()));

        courseLabel.setText(
                nullSafe(result.getCourseCode()) + " - " +
                        nullSafe(result.getCourseDescription())
        );

        metaLabel.setText(
                nullSafe(result.getTerm()) +
                        " • AY " + nullSafe(result.getAcademicYear()) +
                        " • Faculty: " + nullSafe(result.getFaculty()) +
                        " • Submitted: " + formatDateTime(result.getSubmittedAt())
        );

        scoreLabel.setText(
                formatNumber(result.getTotalScore()) +
                        " / " +
                        formatNumber(result.getTotalPoints())
        );

        percentageLabel.setText(
                formatNumber(result.getScorePercentage()) + "%"
        );

        questionContainer.getChildren().clear();

        List<StudentExamResultQuestionResponse> questions = result.getQuestions();

        if (questions == null || questions.isEmpty()) {
            questionContainer.getChildren().add(new Label("No answers found."));
            return;
        }

        renderGroupedQuestions(questions);
    }

    private void renderGroupedQuestions(List<StudentExamResultQuestionResponse> questions) {
        questionContainer.getChildren().clear();

        addQuestionGroup("Multiple Choice", questions, "MULTIPLE_CHOICE");
        addQuestionGroup("True or False", questions, "TRUE_FALSE");
        addQuestionGroup("Identification", questions, "IDENTIFICATION");
        addQuestionGroup("Essay", questions, "ESSAY");
    }

    private void addQuestionGroup(
            String title,
            List<StudentExamResultQuestionResponse> questions,
            String type
    ) {
        List<StudentExamResultQuestionResponse> group = questions.stream()
                .filter(q -> type.equalsIgnoreCase(q.getQuestionType()))
                .toList();

        if (group.isEmpty()) {
            return;
        }

        Label groupTitle = new Label(title);
        groupTitle.getStyleClass().add("results-group-title");

        VBox groupBox = new VBox(14);
        groupBox.getStyleClass().add("results-question-group");
        groupBox.getChildren().add(groupTitle);

        for (StudentExamResultQuestionResponse q : group) {
            groupBox.getChildren().add(createQuestionCard(q));
        }

        questionContainer.getChildren().add(groupBox);
    }

    private VBox createQuestionCard(StudentExamResultQuestionResponse q) {
        VBox card = new VBox(12);
        card.getStyleClass().add("results-question-card");

        HBox topRow = new HBox(10);
        topRow.setFillHeight(true);

        Label numberLabel = new Label("Question " + q.getQuestionNumber());
        numberLabel.getStyleClass().add("results-question-number");

        Label typeLabel = new Label(formatType(q.getQuestionType()));
        typeLabel.getStyleClass().add("results-type-pill");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label resultPill = new Label(resolveResultPillText(q));
        resultPill.getStyleClass().add(resolveResultPillClass(q));

        Label pointsLabel = new Label(
                formatNumber(q.getEarnedPoints()) + " / " + formatNumber(q.getPoints()) + " pts"
        );
        pointsLabel.getStyleClass().add(resolvePointsPillClass(q));

        topRow.getChildren().addAll(numberLabel, typeLabel, spacer, resultPill, pointsLabel);

        Label questionText = new Label(nullSafe(q.getQuestionText()));
        questionText.setWrapText(true);
        questionText.getStyleClass().add("results-question-text");

        card.getChildren().addAll(topRow, questionText);

        if ("TRUE_FALSE".equalsIgnoreCase(q.getQuestionType())) {
            card.getChildren().add(createTrueFalseGrid(q));
        } else if (q.getChoices() != null && !q.getChoices().isEmpty()) {
            card.getChildren().add(createChoicesGrid(q.getChoices(), 2));
        }

        if (shouldShowAnswerSummary(q)) {
            card.getChildren().add(createAnswerSummary(q));
        }

        if (q.getRubrics() != null && !q.getRubrics().isEmpty()) {
            card.getChildren().add(createRubricBox(q.getRubrics()));
        }

        if (q.getViolations() != null && !q.getViolations().isEmpty()) {
            card.getChildren().add(createViolationBox(q.getViolations()));
        }

        if (q.getFeedback() != null && !q.getFeedback().isBlank()) {
            card.getChildren().add(createFeedbackBox(q.getFeedback()));
        }

        return card;
    }

    private GridPane createChoicesGrid(
            List<StudentExamResultChoiceResponse> choices,
            int columns
    ) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("results-choice-grid");
        grid.setHgap(10);
        grid.setVgap(10);

        for (int i = 0; i < choices.size(); i++) {
            StudentExamResultChoiceResponse choice = choices.get(i);

            Label label = new Label(buildChoiceTextWithIcon(choice));
            label.setMinHeight(44);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setWrapText(true);
            label.setMaxWidth(Double.MAX_VALUE);

            if (Boolean.TRUE.equals(choice.getCorrect())) {
                label.getStyleClass().add("results-choice-correct");
            } else if (Boolean.TRUE.equals(choice.getSelected())) {
                label.getStyleClass().add("results-choice-selected-wrong");
            } else {
                label.getStyleClass().add("results-choice-normal");
            }

            int col = i % columns;
            int row = i / columns;

            grid.add(label, col, row);
            GridPane.setHgrow(label, Priority.ALWAYS);
            GridPane.setFillWidth(label, true);
            GridPane.setHgrow(label, Priority.ALWAYS);
            GridPane.setFillWidth(label, true);
        }

        for (int i = 0; i < columns; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / columns);
            grid.getColumnConstraints().add(cc);
        }

        return grid;
    }

    private VBox createAnswerSummary(StudentExamResultQuestionResponse q) {
        VBox box = new VBox(8);
        box.getStyleClass().add("results-answer-box");

        Label yourAnswer = new Label(buildAnswerText(q));
        yourAnswer.setWrapText(true);

        if (Boolean.TRUE.equals(q.getCorrect())) {
            yourAnswer.getStyleClass().add("results-answer-correct");
        } else if (Boolean.FALSE.equals(q.getCorrect())) {
            yourAnswer.getStyleClass().add("results-answer-incorrect");
        } else {
            yourAnswer.getStyleClass().add("results-answer-neutral");
        }

        box.getChildren().add(yourAnswer);

        boolean showCorrectAnswer =
                !"ESSAY".equalsIgnoreCase(q.getQuestionType())
                        && q.getCorrectAnswer() != null
                        && !q.getCorrectAnswer().isBlank()
                        && !Boolean.TRUE.equals(q.getCorrect());

        if (showCorrectAnswer) {
            Label correctAnswer = new Label("Correct Answer: " + q.getCorrectAnswer());
            correctAnswer.setWrapText(true);
            correctAnswer.getStyleClass().add("results-answer-correct-reference");
            box.getChildren().add(correctAnswer);
        }

        return box;
    }

    private VBox createRubricBox(List<StudentExamResultRubricResponse> rubrics) {
        VBox box = new VBox(8);
        box.getStyleClass().add("results-rubric-box");

        Label title = new Label("Essay Rubric");
        title.getStyleClass().add("results-section-title");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("results-rubric-grid");
        grid.setHgap(10);
        grid.setVgap(8);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(32);

        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(28);

        ColumnConstraints c3 = new ColumnConstraints();
        c3.setPercentWidth(40);

        grid.getColumnConstraints().addAll(c1, c2, c3);

        Label h1 = new Label("Rubric (%)");
        Label h2 = new Label("Score");
        Label h3 = new Label("Feedback");

        h1.getStyleClass().add("results-rubric-header");
        h2.getStyleClass().add("results-rubric-header");
        h3.getStyleClass().add("results-rubric-header");

        grid.add(h1, 0, 0);
        grid.add(h2, 1, 0);
        grid.add(h3, 2, 0);

        for (int i = 0; i < rubrics.size(); i++) {
            StudentExamResultRubricResponse r = rubrics.get(i);
            int row = i + 1;

            Label rubric = new Label(
                    nullSafe(r.getCriterionName()) +
                            " (" + formatNumber(r.getWeightPercentage()) + "%)"
            );
            rubric.setWrapText(true);

            Label score = new Label(
                    formatNumber(r.getScoreAwarded()) +
                            " (" + formatNumber(r.getScorePercentage()) + "%)"
            );
            score.setWrapText(true);

            Label feedback = new Label(nullSafe(r.getFeedback()));
            feedback.setWrapText(true);

            rubric.getStyleClass().add("results-rubric-cell");
            score.getStyleClass().add("results-rubric-cell");
            feedback.getStyleClass().add("results-rubric-cell");

            grid.add(rubric, 0, row);
            grid.add(score, 1, row);
            grid.add(feedback, 2, row);
        }

        box.getChildren().addAll(title, grid);
        return box;
    }

    private VBox createViolationBox(List<StudentExamResultViolationResponse> violations) {
        VBox box = new VBox(8);
        box.getStyleClass().add("results-violation-box-danger");

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label title = new Label(
                violations.size() == 1
                        ? "Violation"
                        : "Multiple Violations"
        );
        title.getStyleClass().add("results-violation-title-danger");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button previewButton = new Button("View Evidence");
        previewButton.getStyleClass().add("results-preview-danger-button");
        previewButton.setOnAction(e -> showViolationPreviewDialog(violations));

        header.getChildren().addAll(title, spacer, previewButton);

        Label subtitle = new Label(
                violations.size() == 1
                        ? formatType(violations.get(0).getViolationType()) + " • " +
                        nullSafe(violations.get(0).getSeverity()) + " • " +
                        nullSafe(violations.get(0).getReviewStatus())
                        : violations.size() + " reviewed violations were recorded for this question."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("results-violation-subtitle-danger");

        box.getChildren().addAll(header, subtitle);

        return box;
    }

    private VBox createFeedbackBox(String feedback) {
        VBox box = new VBox(6);
        box.getStyleClass().add("results-feedback-box");

        Label title = new Label("Faculty Feedback");
        title.getStyleClass().add("results-section-title");

        Label body = new Label(feedback);
        body.setWrapText(true);

        box.getChildren().addAll(title, body);
        return box;
    }

    private void downloadAnswerSheetReport() {

        setPrintButtonLoading(true);

        Task<StudentApiService.FileDownloadResponse> task = new Task<>() {
            @Override
            protected StudentApiService.FileDownloadResponse call() throws Exception {
                return studentApiService.downloadStudentAnswerSheetReport(currentExamId);
            }
        };

        task.setOnSucceeded(e -> {
            try {

                StudentApiService.FileDownloadResponse download = task.getValue();

                byte[] pdfBytes = download.getData();
                String filename = download.getFilename();

                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save Student Answer Sheet Report");

                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
                );

                fileChooser.setInitialFileName(filename);

                File file = fileChooser.showSaveDialog(
                        printReportButton.getScene().getWindow()
                );

                if (file != null) {
                    Files.write(file.toPath(), pdfBytes);

                    showSuccess("Report downloaded successfully.");
                }

            } catch (Exception ex) {

                showError("Failed to save report: " + ex.getMessage());

            } finally {

                setPrintButtonLoading(false);
            }
        });

        task.setOnFailed(e -> {

            Throwable ex = task.getException();

            showError(
                    "Failed to download report: " +
                            (ex != null ? ex.getMessage() : "Unknown error")
            );

            setPrintButtonLoading(false);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleBack() {
        if (shellController != null) {
            shellController.showHeroSection();
            shellController.openStudentExamsPageWithFilter("RESULTS RELEASED");
        }
    }


    private void setPrintButtonLoading(boolean loading) {

        if (loading) {

            ImageView icon = new ImageView(
                    new Image(
                            getClass().getResourceAsStream("/icons/printer.png")
                    )
            );

            icon.setFitWidth(14);
            icon.setFitHeight(14);
            icon.setPreserveRatio(true);

            printReportButton.setGraphic(icon);

            LoadingSpinner.setLoading(
                    printReportButton,
                    true,
                    "Generating...",
                    ""
            );

            return;
        }

        ImageView icon = new ImageView(
                new Image(
                        getClass().getResourceAsStream("/icons/printer.png")
                )
        );

        icon.setFitWidth(14);
        icon.setFitHeight(14);
        icon.setPreserveRatio(true);

        printReportButton.setGraphic(icon);
        printReportButton.setText("Print Report");
    }

    private String formatDateTime(java.time.OffsetDateTime value) {
        if (value == null) {
            return "-";
        }

        return value.atZoneSameInstant(ZoneId.of("Asia/Manila"))
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"));
    }

    private String formatType(String value) {
        if (value == null) {
            return "-";
        }

        String cleaned = value.replace("_", " ").toLowerCase();
        String[] words = cleaned.split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isBlank()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    private String formatNumber(Double value) {
        if (value == null) {
            return "0";
        }

        if (value % 1 == 0) {
            return String.valueOf(value.intValue());
        }

        return String.format("%.2f", value);
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private String buildChoiceTextWithIcon(StudentExamResultChoiceResponse choice) {
        String icon = "";

        if (Boolean.TRUE.equals(choice.getSelected())) {
            icon = Boolean.TRUE.equals(choice.getCorrect()) ? "✓ " : "✕ ";
        } else if (Boolean.TRUE.equals(choice.getCorrect())) {
            icon = "✓ ";
        }

        return icon + nullSafe(choice.getChoiceText());
    }

    private void showViolationPreviewDialog(
            List<StudentExamResultViolationResponse> violations
    ) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Violation Evidence Preview");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("violation-preview-root");

        ImageView previewImage = new ImageView();
        previewImage.setFitWidth(760);
        previewImage.setFitHeight(430);
        previewImage.setPreserveRatio(true);
        previewImage.getStyleClass().add("violation-preview-image");

        Label emptyPreviewLabel = new Label("No screenshot/evidence available for this violation.");
        emptyPreviewLabel.getStyleClass().add("violation-preview-empty");

        StackPane previewPane = new StackPane(emptyPreviewLabel, previewImage);
        previewPane.getStyleClass().add("violation-preview-pane");

        VBox listBox = new VBox(8);
        listBox.getStyleClass().add("violation-preview-list");

        for (int i = 0; i < violations.size(); i++) {
            StudentExamResultViolationResponse violation = violations.get(i);

            Button item = new Button(
                    "Violation " + (i + 1) +
                            " • " + formatType(violation.getViolationType()) +
                            " • " + nullSafe(violation.getReviewStatus())
            );
            item.setMaxWidth(Double.MAX_VALUE);
            item.getStyleClass().add("violation-preview-list-button");

            item.setOnAction(e -> {
                String evidenceUrl = violation.getEvidenceUrl();

                if (evidenceUrl == null || evidenceUrl.isBlank()) {
                    previewImage.setImage(null);
                    emptyPreviewLabel.setVisible(true);
                    emptyPreviewLabel.setManaged(true);
                    return;
                }

                try {
                    Image image = new Image(buildImageUrl(evidenceUrl), true);
                    previewImage.setImage(image);
                    emptyPreviewLabel.setVisible(false);
                    emptyPreviewLabel.setManaged(false);
                } catch (Exception ex) {
                    previewImage.setImage(null);
                    emptyPreviewLabel.setText("Failed to load evidence image.");
                    emptyPreviewLabel.setVisible(true);
                    emptyPreviewLabel.setManaged(true);
                }
            });

            listBox.getChildren().add(item);
        }

        VBox detailsBox = new VBox(10);
        detailsBox.getStyleClass().add("violation-preview-details");

        Label title = new Label("Violation Evidence");
        title.getStyleClass().add("violation-preview-title");

        Label note = new Label("Select a violation to preview its screenshot or evidence.");
        note.setWrapText(true);
        note.getStyleClass().add("violation-preview-note");

        detailsBox.getChildren().addAll(title, note, listBox);

        root.setCenter(previewPane);
        root.setRight(detailsBox);

        Scene scene = new Scene(root, 1100, 620);
        scene.getStylesheets().add(
                getClass().getResource("/css/student-exam.css").toExternalForm()
        );

        dialog.setScene(scene);
        dialog.show();

        if (!violations.isEmpty()) {
            listBox.getChildren().get(0).fireEvent(
                    new javafx.event.ActionEvent()
            );
        }
    }

    private String buildImageUrl(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }

        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }

        String baseUrl = "http://localhost:8080";

        if (path.startsWith("/")) {
            return baseUrl + path;
        }

        return baseUrl + "/" + path;
    }

    private String resolveResultPillText(StudentExamResultQuestionResponse q) {
        if (hasPenalizedViolation(q)) {

            if (isFullScore(q)) {
                return "✓ Correct • Penalized";
            }

            return "Penalized";
        }

        if ("ESSAY".equalsIgnoreCase(q.getQuestionType())) {
            return "Reviewed";
        }

        if (isFullScore(q)) {
            return "✓ Correct";
        }

        if (isPartialScore(q)) {
            return "Partial";
        }

        return "✕ Incorrect";
    }

    private String resolveResultPillClass(StudentExamResultQuestionResponse q) {

        if (hasPenalizedViolation(q)) {

            if (isFullScore(q)) {
                return "results-status-correct-penalized";
            }

            return "results-status-penalized";
        }

        if (isFullScore(q)) {
            return "results-status-correct";
        }

        if (isPartialScore(q)) {
            return "results-status-partial";
        }

        return "results-status-incorrect";
    }

    private String resolvePointsPillClass(StudentExamResultQuestionResponse q) {

        if (hasPenalizedViolation(q)) {

            if (isFullScore(q)) {
                return "results-points-correct-penalized";
            }

            return "results-points-penalized";
        }

        if (isFullScore(q)) {
            return "results-points-correct";
        }

        if (isPartialScore(q)) {
            return "results-points-partial";
        }

        return "results-points-incorrect";
    }

    private boolean isFullScore(StudentExamResultQuestionResponse q) {
        double earned = q.getEarnedPoints() == null ? 0.0 : q.getEarnedPoints();
        double total = q.getPoints() == null ? 0.0 : q.getPoints();

        return total > 0 && earned >= total;
    }

    private boolean isPartialScore(StudentExamResultQuestionResponse q) {
        double earned = q.getEarnedPoints() == null ? 0.0 : q.getEarnedPoints();
        double total = q.getPoints() == null ? 0.0 : q.getPoints();

        return total > 0 && earned > 0 && earned < total;
    }

    private GridPane createTrueFalseGrid(StudentExamResultQuestionResponse q) {
        List<StudentExamResultChoiceResponse> tfChoices = List.of(
                createSyntheticChoice("TRUE", q.getStudentAnswer(), q.getCorrectAnswer()),
                createSyntheticChoice("FALSE", q.getStudentAnswer(), q.getCorrectAnswer())
        );

        return createChoicesGrid(tfChoices, 2);
    }

    private StudentExamResultChoiceResponse createSyntheticChoice(
            String value,
            String studentAnswer,
            String correctAnswer
    ) {
        return new StudentExamResultChoiceResponse() {
            @Override
            public String getChoiceText() {
                return value;
            }

            @Override
            public Boolean getSelected() {
                return value.equalsIgnoreCase(nullSafeRaw(studentAnswer));
            }

            @Override
            public Boolean getCorrect() {
                return value.equalsIgnoreCase(nullSafeRaw(correctAnswer));
            }
        };
    }

    private boolean hasPenalizedViolation(StudentExamResultQuestionResponse q) {
        if (q.getViolations() == null || q.getViolations().isEmpty()) {
            return false;
        }

        return q.getViolations()
                .stream()
                .anyMatch(v -> "PENALIZED".equalsIgnoreCase(v.getReviewStatus()));
    }

    private boolean shouldShowAnswerSummary(StudentExamResultQuestionResponse q) {

        if ("ESSAY".equalsIgnoreCase(q.getQuestionType())) {
            return true;
        }

        if ("IDENTIFICATION".equalsIgnoreCase(q.getQuestionType())) {
            return true;
        }

        return !Boolean.TRUE.equals(q.getCorrect());
    }

    private String buildAnswerText(StudentExamResultQuestionResponse q) {
        String icon = "";

        if (Boolean.TRUE.equals(q.getCorrect())) {
            icon = "✓ ";
        } else if (Boolean.FALSE.equals(q.getCorrect())) {
            icon = "✕ ";
        }

        return icon + "Your Answer: " + nullSafe(q.getStudentAnswer());
    }

    private String nullSafeRaw(String value) {
        return value == null ? "" : value.trim();
    }

    private void showSuccess(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

}