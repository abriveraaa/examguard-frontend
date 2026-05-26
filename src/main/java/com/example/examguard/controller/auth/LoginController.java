package com.example.examguard.controller.auth;

import com.example.examguard.ai.MediaPipeFaceRuntime;
import com.example.examguard.model.core.response.LoginApiResponse;
import com.example.examguard.model.enums.UserType;
import com.example.examguard.service.AuthApiService;
import com.example.examguard.utility.LoadingSpinner;
import com.example.examguard.utility.SceneManager;
import com.example.examguard.utility.Session;
import com.google.gson.Gson;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;


public class LoginController implements Initializable {

    // UI
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button loginButton;
    @FXML
    private ProgressIndicator loginSpinner;

    // DATA
    private final AuthApiService authApiService = new AuthApiService();
    private final Gson gson = new Gson();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        startAiRuntimeForTesting();
    }

    @FXML
    public void login() {

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            setLoadingState(false);
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText("Please fill in all fields.");
            return;
        }

        setLoadingState(true);
        errorLabel.setStyle("-fx-text-fill: white;");
        errorLabel.setText("Logging in...");

        Task<String> loginTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return authApiService.login(username, password);
            }
        };

        loginTask.setOnSucceeded(event -> {
            setLoadingState(false);

            try {
                String response = loginTask.getValue();

                LoginApiResponse apiResponse = gson.fromJson(response, LoginApiResponse.class);

                if (apiResponse == null) {
                    errorLabel.setStyle("-fx-text-fill: red;");
                    errorLabel.setText("Invalid server response.");
                    return;
                }

                if (apiResponse.isSuccess()) {

                    Session.setUsername(apiResponse.getUsername());
                    Session.setFirstName(apiResponse.getFirstName());
                    Session.setLastName(apiResponse.getLastName());
                    Session.setSchoolId(apiResponse.getSchoolId());
                    Session.setRole(apiResponse.getRole());
                    Session.setSessionToken(apiResponse.getSessionToken());
                    Session.setMustChangePassword(apiResponse.isMustChangePassword());

                    if (apiResponse.isMustChangePassword()) {
                        SceneManager.homePagebyRole(apiResponse.getRole());
                        return;
                    }

                    String roleFromApi = apiResponse.getRole();

                    UserType userType = UserType.fromString(roleFromApi);

                    if (userType != null) {
                        SceneManager.homePagebyRole(userType.name());
                    } else {
                        errorLabel.setStyle("-fx-text-fill: red;");
                        errorLabel.setText("Invalid role.");
                    }

                } else {
                    errorLabel.setStyle("-fx-text-fill: red;");
                    errorLabel.setText(apiResponse.getMessage());
                }

            } catch (Exception e) {
                errorLabel.setStyle("-fx-text-fill: red;");
                errorLabel.setText("Failed to process server response.");
                e.printStackTrace();
            }
        });

        loginTask.setOnFailed(event -> {
            setLoadingState(false);
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setMinHeight(40);
            errorLabel.setText("Cannot connect to backend.");

            Throwable ex = loginTask.getException();
            if (ex != null) {
                ex.printStackTrace();
            }
        });

        Thread thread = new Thread(loginTask);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void switchPage(Event event) {

        Node source = (Node) event.getSource();
        String action = (String) source.getUserData();

        if (action == null) return;

        switch (action) {
            case "activate":
                SceneManager.switchScene("auth/activate.fxml");
                break;

            case "forgot":
                SceneManager.switchScene("auth/forgot-password.fxml");
                break;
        }
    }

    private void startAiRuntimeForTesting() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                MediaPipeFaceRuntime.startIfNeeded();
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            System.out.println("MediaPipe AI service started.");
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex != null) {
                ex.printStackTrace();
            }

            // Do not block login while testing
            System.out.println("MediaPipe AI service failed to start.");
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void setLoadingState(boolean loading) {
        LoadingSpinner.setLoading(
                loginButton,
                loading,
                "Logging in...",
                "LOGIN"
        );
    }
}