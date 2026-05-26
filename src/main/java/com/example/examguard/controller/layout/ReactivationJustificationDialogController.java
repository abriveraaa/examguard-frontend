package com.example.examguard.controller.layout;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class ReactivationJustificationDialogController {

    @FXML
    private Label descriptionLabel;
    @FXML
    private TextArea justificationArea;
    @FXML
    private Label errorLabel;

    private String justification;
    private boolean confirmed = false;

    public void setDescription(String description) {
        if (descriptionLabel != null) {
            descriptionLabel.setText(description);
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getJustification() {
        return justification;
    }

    @FXML
    private void initialize() {
        // clear error when typing
        justificationArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (errorLabel != null) {
                errorLabel.setText("");
            }
        });
    }

    @FXML
    private void handleConfirm() {
        String value = justificationArea.getText() == null
                ? ""
                : justificationArea.getText().trim();

        if (value.isBlank()) {
            errorLabel.setText("Justification is required.");
            return;
        }

        if (value.length() < 5) {
            errorLabel.setText("Justification must be at least 5 characters.");
            return;
        }

        justification = value;
        confirmed = true;

        close();
    }

    @FXML
    private void handleCancel() {
        confirmed = false;
        close();
    }

    private void close() {
        Stage stage = (Stage) justificationArea.getScene().getWindow();
        stage.close();
    }
}