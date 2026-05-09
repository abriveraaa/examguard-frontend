package com.example.examguard.utility;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;

public class LoadingSpinner {

    public static void setLoading(
            Button button,
            boolean loading,
            String loadingText,
            String defaultText
    ) {
        if (button == null) return;

        if (loading) {
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setPrefSize(16, 16);
            spinner.setMaxSize(16, 16);

            Label text = new Label(loadingText);

            String style = button.getStyle();

            String bgColor = "#650000";

            if (style != null && style.contains("-fx-background-color")) {
                int start = style.indexOf("#");
                if (start != -1 && style.length() >= start + 7) {
                    bgColor = style.substring(start, start + 7);
                }
            }

            text.setStyle(getContrastTextColor(bgColor));

            HBox content = new HBox(8, spinner, text);
            content.setAlignment(Pos.CENTER);

            button.setGraphic(content);
            button.setText("");


            button.setDisable(false);
            button.setMouseTransparent(true);

        } else {
            button.setGraphic(null);
            button.setText(defaultText);

            button.setMouseTransparent(false);
        }
    }

    private static String getContrastTextColor(String hexColor) {
        if (hexColor == null || !hexColor.startsWith("#")) {
            return "-fx-text-fill: white;";
        }

        try {
            int r = Integer.parseInt(hexColor.substring(1, 3), 16);
            int g = Integer.parseInt(hexColor.substring(3, 5), 16);
            int b = Integer.parseInt(hexColor.substring(5, 7), 16);

            // brightness formula
            double brightness = (0.299 * r + 0.587 * g + 0.114 * b);

            return brightness > 186
                    ? "-fx-text-fill: black;"
                    : "-fx-text-fill: white;";

        } catch (Exception e) {
            return "-fx-text-fill: white;";
        }
    }
}