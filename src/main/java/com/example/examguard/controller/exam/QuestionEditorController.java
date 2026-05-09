package com.example.examguard.controller.exam;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class QuestionEditorController {

    @FXML private Label titleLabel;

    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextArea questionTextArea;
    @FXML private TextField pointsField;

    @FXML private VBox choicesPane;
    @FXML private TextField choiceAField;
    @FXML private TextField choiceBField;
    @FXML private TextField choiceCField;
    @FXML private TextField choiceDField;
    @FXML private ComboBox<String> correctChoiceComboBox;

    @FXML private VBox shortAnswerPane;
    @FXML private TextField correctAnswerField;

    @FXML private VBox essayPane;
    @FXML private TextArea essayGuideArea;

    private CreateExamController.QuestionDraftRow question;
    private boolean saved = false;

    @FXML
    public void initialize() {
        typeComboBox.getItems().setAll(
                "MULTIPLE_CHOICE",
                "TRUE_FALSE",
                "IDENTIFICATION",
                "ESSAY"
        );

        correctChoiceComboBox.getItems().setAll("A", "B", "C", "D");
        pointsField.setText("1");

        typeComboBox.getSelectionModel().select("MULTIPLE_CHOICE");

        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateVisibleFields(newVal));

        updateVisibleFields("MULTIPLE_CHOICE");
    }

    public void setQuestion(CreateExamController.QuestionDraftRow question) {
        this.question = question;

        if (question == null) return;

        titleLabel.setText("Edit Question");

        questionTextArea.setText(question.getQuestionText());
        typeComboBox.getSelectionModel().select(question.getQuestionType());
        pointsField.setText(String.valueOf(question.getPoints()));

        choiceAField.setText(question.getChoiceA());
        choiceBField.setText(question.getChoiceB());
        choiceCField.setText(question.getChoiceC());
        choiceDField.setText(question.getChoiceD());

        correctChoiceComboBox.getSelectionModel().select(question.getCorrectAnswer());
        correctAnswerField.setText(question.getCorrectAnswer());
        essayGuideArea.setText(question.getCorrectAnswer());
    }

    public boolean isSaved() {
        return saved;
    }

    public CreateExamController.QuestionDraftRow getQuestion() {
        return question;
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) return;

        if (question == null) {
            question = new CreateExamController.QuestionDraftRow();
        }

        String type = typeComboBox.getValue();

        question.setQuestionText(questionTextArea.getText().trim());
        question.setQuestionType(type);
        question.setPoints(Integer.parseInt(pointsField.getText().trim()));

        if ("MULTIPLE_CHOICE".equals(type)) {
            question.setChoiceA(choiceAField.getText().trim());
            question.setChoiceB(choiceBField.getText().trim());
            question.setChoiceC(choiceCField.getText().trim());
            question.setChoiceD(choiceDField.getText().trim());

            String selectedLetter = correctChoiceComboBox.getValue();

            String correctAnswerValue = switch (selectedLetter) {
                case "A" -> choiceAField.getText().trim();
                case "B" -> choiceBField.getText().trim();
                case "C" -> choiceCField.getText().trim();
                case "D" -> choiceDField.getText().trim();
                default -> "";
            };

            question.setCorrectAnswer(correctAnswerValue);
        } else if ("TRUE_FALSE".equals(type)) {
            question.setChoiceA("True");
            question.setChoiceB("False");
            question.setChoiceC("");
            question.setChoiceD("");
            question.setCorrectAnswer(correctChoiceComboBox.getValue());
        } else if ("IDENTIFICATION".equals(type)) {
            question.setChoiceA("");
            question.setChoiceB("");
            question.setChoiceC("");
            question.setChoiceD("");
            question.setCorrectAnswer(correctAnswerField.getText().trim());
        } else {
            question.setChoiceA("");
            question.setChoiceB("");
            question.setChoiceC("");
            question.setChoiceD("");
            question.setCorrectAnswer(essayGuideArea.getText().trim());
        }

        saved = true;
        close();
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private boolean validateInput() {
        if (typeComboBox.getValue() == null) {
            showWarning("Please select a question type.");
            return false;
        }

        if (questionTextArea.getText() == null || questionTextArea.getText().isBlank()) {
            showWarning("Question text is required.");
            return false;
        }

        try {
            int points = Integer.parseInt(pointsField.getText().trim());

            if (points <= 0) {
                showWarning("Points must be greater than 0.");
                return false;
            }

        } catch (Exception e) {
            showWarning("Points must be a valid number.");
            return false;
        }

        String type = typeComboBox.getValue();

        if ("MULTIPLE_CHOICE".equals(type)) {
            if (choiceAField.getText().isBlank()
                    || choiceBField.getText().isBlank()
                    || choiceCField.getText().isBlank()
                    || choiceDField.getText().isBlank()) {
                showWarning("All choices are required for multiple choice.");
                return false;
            }

            if (correctChoiceComboBox.getValue() == null) {
                showWarning("Please select the correct answer.");
                return false;
            }
        }

        if ("IDENTIFICATION".equals(type)) {
            if (correctAnswerField.getText() == null || correctAnswerField.getText().isBlank()) {
                showWarning("Correct answer is required.");
                return false;
            }
        }

        return true;
    }

    private void updateVisibleFields(String type) {
        boolean multipleChoice = "MULTIPLE_CHOICE".equals(type);
        boolean trueFalse = "TRUE_FALSE".equals(type);
        boolean identification = "IDENTIFICATION".equals(type);
        boolean essay = "ESSAY".equals(type);

        choicesPane.setVisible(multipleChoice || trueFalse);
        choicesPane.setManaged(multipleChoice || trueFalse);

        shortAnswerPane.setVisible(identification);
        shortAnswerPane.setManaged(identification);

        essayPane.setVisible(essay);
        essayPane.setManaged(essay);

        if (trueFalse) {
            choiceAField.setText("True");
            choiceBField.setText("False");
            choiceCField.clear();
            choiceDField.clear();

            choiceAField.setDisable(true);
            choiceBField.setDisable(true);
            choiceCField.setDisable(true);
            choiceDField.setDisable(true);

            correctChoiceComboBox.getItems().setAll("True", "False");
        } else {
            choiceAField.setDisable(false);
            choiceBField.setDisable(false);
            choiceCField.setDisable(false);
            choiceDField.setDisable(false);

            correctChoiceComboBox.getItems().setAll("A", "B", "C", "D");
        }
    }

    private void showWarning(String message) {
        new Alert(Alert.AlertType.WARNING, message).show();
    }

    private void close() {
        Stage stage = (Stage) questionTextArea.getScene().getWindow();
        stage.close();
    }
}