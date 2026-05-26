package com.example.examguard;

import com.example.examguard.utility.SceneManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.net.URL;

public class ExamGuardApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        SceneManager.setStage(stage);
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/auth/login.fxml")
        );

        Scene scene = new Scene(loader.load());

        stage.setTitle("ExamGuard");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }


}