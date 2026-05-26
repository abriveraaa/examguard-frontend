package com.example.examguard.controller.faculty;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;

public class FacultyProctorController {

    @FXML
    private ComboBox<String> examSelector;
    @FXML
    private TextField searchField;
    @FXML
    private CheckBox flaggedOnlyCheck;

    @FXML
    private Label totalTakersLabel;
    @FXML
    private Label onlineLabel;
    @FXML
    private Label flaggedLabel;
    @FXML
    private Label submittedLabel;

    @FXML
    private TilePane studentGrid;

    @FXML
    private Label selectedStudentName;
    @FXML
    private Label selectedStudentInfo;
    @FXML
    private Label phoneCameraStatus;
    @FXML
    private Label connectionStatus;
    @FXML
    private Label angleStatus;

    @FXML
    private ListView<String> violationList;

    private final List<ProctorStudentRow> students = new ArrayList<>();

    @FXML
    private void initialize() {
        setupExamSelector();
        setupSearchListener();
        loadMockData();
        renderStudentGrid();
        updateSummary();
    }

    private void setupExamSelector() {
        examSelector.getItems().setAll(
                "COMP 009 - Object Oriented Programming",
                "INFO 001 - Information Management"
        );

        examSelector.getSelectionModel().selectFirst();
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> renderStudentGrid());
    }

    private void loadMockData() {
        students.clear();

        students.add(new ProctorStudentRow(
                "2022-00001",
                "Ariel Rivera",
                "BSITOUMN 2-2",
                "In Progress",
                true,
                true,
                false,
                12,
                40,
                1
        ));

        students.add(new ProctorStudentRow(
                "2022-00002",
                "Maria Santos",
                "BSITOUMN 2-2",
                "In Progress",
                true,
                true,
                true,
                18,
                40,
                3
        ));

        students.add(new ProctorStudentRow(
                "2022-00003",
                "John Cruz",
                "BSITOUMN 2-3",
                "Waiting",
                false,
                false,
                false,
                0,
                40,
                0
        ));

        students.add(new ProctorStudentRow(
                "2022-00004",
                "Kyla Reyes",
                "BSITOUMN 2-3",
                "Submitted",
                false,
                false,
                false,
                40,
                40,
                0
        ));

        students.add(new ProctorStudentRow(
                "2022-00005",
                "Mark Dela Cruz",
                "BSITOUMN 2-2",
                "Disconnected",
                false,
                false,
                false,
                9,
                40,
                2
        ));
    }

    private void renderStudentGrid() {
        studentGrid.getChildren().clear();

        String keyword = searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase();

        boolean flaggedOnly = flaggedOnlyCheck.isSelected();

        for (ProctorStudentRow student : students) {
            boolean matchesSearch =
                    student.name().toLowerCase().contains(keyword)
                            || student.studentId().toLowerCase().contains(keyword)
                            || student.section().toLowerCase().contains(keyword);

            boolean matchesFlag =
                    !flaggedOnly || student.violationCount() > 0;

            if (matchesSearch && matchesFlag) {
                studentGrid.getChildren().add(createStudentCard(student));
            }
        }
    }

    private VBox createStudentCard(ProctorStudentRow student) {
        VBox card = new VBox(10);
        card.setPrefWidth(260);
        card.setMinHeight(260);
        card.getStyleClass().add("student-card");

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Circle statusDot = new Circle(5);
        statusDot.getStyleClass().add(student.online() ? "dot-online" : "dot-offline");

        Label status = new Label(student.status());
        status.getStyleClass().add(student.violationCount() > 0 ? "status-danger" : "status-normal");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label violationBadge = new Label("Viol: " + student.violationCount());
        violationBadge.getStyleClass().add(student.violationCount() > 0 ? "violation-badge-danger" : "violation-badge");

        topRow.getChildren().addAll(statusDot, status, spacer, violationBadge);

        StackPane videoBox = new StackPane();
        videoBox.setPrefHeight(130);
        videoBox.getStyleClass().add(student.cameraConnected() ? "video-box-live" : "video-box-waiting");

        Label videoText = new Label(student.cameraConnected()
                ? "PHONE CAMERA LIVE"
                : "WAITING FOR PHONE CAMERA");
        videoText.getStyleClass().add("video-text");

        videoBox.getChildren().add(videoText);

        Label name = new Label(student.name());
        name.getStyleClass().add("card-student-name");
        name.setMaxWidth(Double.MAX_VALUE);

        Label info = new Label(student.studentId() + " • " + student.section());
        info.getStyleClass().add("card-student-info");

        ProgressBar progressBar = new ProgressBar();
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setProgress(student.progressPercent());

        Label progressText = new Label(
                "Question " + student.currentQuestion() + " of " + student.totalQuestions()
        );
        progressText.getStyleClass().add("progress-text");

        card.getChildren().addAll(
                topRow,
                videoBox,
                name,
                info,
                progressBar,
                progressText
        );

        card.setOnMouseClicked(event -> selectStudent(student));

        return card;
    }

    private void selectStudent(ProctorStudentRow student) {
        selectedStudentName.setText(student.name());

        selectedStudentInfo.setText(
                student.studentId()
                        + " • "
                        + student.section()
                        + " • Question "
                        + student.currentQuestion()
                        + "/"
                        + student.totalQuestions()
        );

        phoneCameraStatus.setText(
                student.cameraConnected()
                        ? "Phone camera: Connected"
                        : "Phone camera: Waiting"
        );

        connectionStatus.setText(
                student.online()
                        ? "Connection: Online"
                        : "Connection: Offline"
        );

        angleStatus.setText(
                student.angleVerified()
                        ? "45° angle: Verified"
                        : "45° angle: Not verified"
        );

        violationList.getItems().clear();

        if (student.violationCount() == 0) {
            violationList.setPlaceholder(new Label("No violations yet."));
        } else {
            violationList.getItems().addAll(
                    "Face not centered detected",
                    "Possible looking away",
                    "Camera angle changed"
            );
        }
    }

    private void updateSummary() {
        int total = students.size();
        int online = 0;
        int flagged = 0;
        int submitted = 0;

        for (ProctorStudentRow student : students) {
            if (student.online()) {
                online++;
            }

            if (student.violationCount() > 0) {
                flagged++;
            }

            if ("Submitted".equalsIgnoreCase(student.status())) {
                submitted++;
            }
        }

        totalTakersLabel.setText(String.valueOf(total));
        onlineLabel.setText(String.valueOf(online));
        flaggedLabel.setText(String.valueOf(flagged));
        submittedLabel.setText(String.valueOf(submitted));
    }

    @FXML
    private void handleRefresh() {
        renderStudentGrid();
        updateSummary();
    }

    @FXML
    private void handleGridView() {
        renderStudentGrid();
    }

    @FXML
    private void handleFilterChanged() {
        renderStudentGrid();
    }

    @FXML
    private void handleFocusView() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Focus View");
        alert.setHeaderText("Focus View");
        alert.setContentText("This will open the selected student's larger live camera view later.");
        alert.showAndWait();
    }

    private record ProctorStudentRow(
            String studentId,
            String name,
            String section,
            String status,
            boolean online,
            boolean cameraConnected,
            boolean angleVerified,
            int currentQuestion,
            int totalQuestions,
            int violationCount
    ) {
        double progressPercent() {
            if (totalQuestions <= 0) {
                return 0;
            }

            return Math.min(1.0, (double) currentQuestion / totalQuestions);
        }
    }
}