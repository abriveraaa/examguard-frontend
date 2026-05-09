package com.example.examguard.controller.student;

import com.example.examguard.model.exam.take.StudentExamRow;
import com.example.examguard.model.student.response.StudentDashboardResponse;
import com.example.examguard.model.student.StudentUpcomingExam;
import com.example.examguard.service.StudentApiService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class StudentExamListController {

    @FXML private TableView<StudentExamRow> examTable;

    @FXML private TableColumn<StudentExamRow, String> titleColumn;
    @FXML private TableColumn<StudentExamRow, String> courseColumn;
    @FXML private TableColumn<StudentExamRow, String> scheduleColumn;
    @FXML private TableColumn<StudentExamRow, String> statusColumn;
    @FXML private TableColumn<StudentExamRow, Long> questionsColumn;
    @FXML private TableColumn<StudentExamRow, Integer> timeColumn;
    @FXML private TableColumn<StudentExamRow, StudentExamRow> actionColumn;

    private final StudentApiService studentApiService = new StudentApiService();

    private static final ZoneId MANILA_ZONE = ZoneId.of("Asia/Manila");

    private static final DateTimeFormatter SAME_DAY_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

    private static final DateTimeFormatter TIME_ONLY_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a");

    @FXML
    private void initialize() {
        setupTable();
        setupColumns();
        loadExamsFromBackend();
    }

    private void setupTable() {
        examTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        examTable.setPlaceholder(new Label("No exams available."));

        titleColumn.setMaxWidth(1f * Integer.MAX_VALUE * 30);
        courseColumn.setMaxWidth(1f * Integer.MAX_VALUE * 12);
        scheduleColumn.setMaxWidth(1f * Integer.MAX_VALUE * 28);
        statusColumn.setMaxWidth(1f * Integer.MAX_VALUE * 10);
        questionsColumn.setMaxWidth(1f * Integer.MAX_VALUE * 8);
        timeColumn.setMaxWidth(1f * Integer.MAX_VALUE * 8);
        actionColumn.setMaxWidth(1f * Integer.MAX_VALUE * 12);
    }

    private void setupColumns() {
        titleColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getTitle())));

        courseColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getCourseCode())));

        scheduleColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getSchedule())));

        statusColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(nullSafe(data.getValue().getStatus())));

        questionsColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(safeLong(data.getValue().getQuestionCount())));

        timeColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(safeInteger(data.getValue().getTimeLimitMinutes())));

        actionColumn.setCellValueFactory(data ->
                new ReadOnlyObjectWrapper<>(data.getValue()));

        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button startButton = new Button();
            private final HBox box = new HBox(startButton);

            {
                box.setSpacing(8);
                startButton.setMaxWidth(Double.MAX_VALUE);

                startButton.setOnAction(event -> {
                    StudentExamRow row = getTableView().getItems().get(getIndex());
                    openExamPage(row);
                });
            }

            @Override
            protected void updateItem(StudentExamRow row, boolean empty) {
                super.updateItem(row, empty);

                if (empty || row == null) {
                    setGraphic(null);
                    return;
                }

                boolean canStart = "AVAILABLE".equalsIgnoreCase(row.getStatus());

                startButton.setDisable(!canStart);
                startButton.setText(canStart ? "Start Exam" : row.getStatus());

                startButton.getStyleClass().removeAll(
                        "start-button",
                        "disabled-button"
                );

                startButton.getStyleClass().add(
                        canStart ? "start-button" : "disabled-button"
                );

                setGraphic(box);
            }
        });
    }

    private void loadExamsFromBackend() {
        try {
            StudentDashboardResponse response = studentApiService.fetchDashboard();

            if (response == null || response.getUpcomingExams() == null) {
                examTable.setItems(FXCollections.observableArrayList());
                return;
            }

            List<StudentExamRow> rows = response.getUpcomingExams()
                    .stream()
                    .sorted(Comparator.comparing(
                            StudentUpcomingExam::getStartDateTime,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ))
                    .map(this::mapToRow)
                    .collect(Collectors.toList());

            examTable.setItems(FXCollections.observableArrayList(rows));

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load exams from backend.");
        }
    }

    private StudentExamRow mapToRow(StudentUpcomingExam exam) {
        StudentExamRow row = new StudentExamRow();

        row.setExamId(exam.getExamId());
        row.setTitle(exam.getTitle());
        row.setCourseCode(exam.getCourseCode());

        row.setSchedule(formatDateRange(
                exam.getStartDateTime(),
                exam.getEndDateTime()
        ));

        row.setStatus(computeStatus(exam));

        row.setQuestionCount(
                exam.getQuestionCount() != null ? exam.getQuestionCount() : 0
        );

        row.setTimeLimitMinutes(
                exam.getTimeLimitMinutes() != null ? exam.getTimeLimitMinutes() : 0
        );

        return row;
    }

    private String computeStatus(StudentUpcomingExam exam) {
        OffsetDateTime start = exam.getStartDateTime();
        OffsetDateTime end = exam.getEndDateTime();

        if (start == null || end == null) {
            return "UNAVAILABLE";
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (now.isBefore(start)) {
            return "UPCOMING";
        }

        if (!now.isBefore(start) && !now.isAfter(end)) {
            return "AVAILABLE";
        }

        return "EXPIRED";
    }

    private String formatDateRange(OffsetDateTime start, OffsetDateTime end) {
        if (start == null || end == null) {
            return "";
        }

        try {
            OffsetDateTime manilaStart = start
                    .atZoneSameInstant(MANILA_ZONE)
                    .toOffsetDateTime();

            OffsetDateTime manilaEnd = end
                    .atZoneSameInstant(MANILA_ZONE)
                    .toOffsetDateTime();

            boolean sameDate = manilaStart.toLocalDate()
                    .equals(manilaEnd.toLocalDate());

            if (sameDate) {
                return manilaStart.format(SAME_DAY_FORMAT)
                        + " - "
                        + manilaEnd.format(TIME_ONLY_FORMAT);
            }

            return manilaStart.format(SAME_DAY_FORMAT)
                    + " - "
                    + manilaEnd.format(SAME_DAY_FORMAT);

        } catch (Exception e) {
            return start.toString() + " - " + end.toString();
        }
    }

    private void openExamPage(StudentExamRow row) {
        if (row == null || row.getExamId() == null) {
            showError("Invalid exam selected.");
            return;
        }

        if (!"AVAILABLE".equalsIgnoreCase(row.getStatus())) {
            showError("This exam is not available.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/student/exam-taking.fxml")
            );

            Parent root = loader.load();

            ExamTakingController controller = loader.getController();
            controller.startExam(
                    row.getExamId(),
                    safeInteger(row.getTimeLimitMinutes())
            );

            Stage currentStage = (Stage) examTable.getScene().getWindow();
            currentStage.close();

            Stage examStage = new Stage();
            examStage.setTitle("ExamGuard - Taking Exam");

            Scene scene = new Scene(root);
            examStage.setScene(scene);

            examStage.setMaximized(true);
            examStage.setFullScreen(true);
            examStage.setFullScreenExitHint("");
            examStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open exam-taking page.");
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private Integer safeInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private Long safeLong(Long value) {
        return value == null ? 0 : value;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("ExamGuard");
        alert.setHeaderText("Something went wrong");
        alert.setContentText(message);
        alert.showAndWait();
    }
}