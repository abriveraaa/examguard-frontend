package com.example.examguard.utility;

import com.example.examguard.controller.layout.DashboardShellController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    private static Stage stage;

    public static void setStage(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void switchScene(String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(
                    SceneManager.class.getResource("/fxml/" + fxmlFile)
            );

            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void homePagebyRole(String role) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneManager.class.getResource("/fxml/layout/dashboard-shell.fxml")
            );

            Parent root = loader.load();
            DashboardShellController controller = loader.getController();

            switch (role.toUpperCase()) {
                case "ADMIN":
                    controller.loadAdminView();
                    break;
                case "FACULTY":
                    controller.loadFacultyView();
                    break;
                case "STUDENT":
                    controller.loadStudentView();
                    break;
                default:
                    controller.loadStudentView();
                    break;
            }

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}