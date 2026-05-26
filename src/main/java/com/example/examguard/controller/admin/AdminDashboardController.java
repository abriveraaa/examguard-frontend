package com.example.examguard.controller.admin;

import com.example.examguard.controller.layout.DashboardShellController;
import com.example.examguard.model.admin.monitoring.AdminLogRowDto;
import com.example.examguard.model.admin.monitoring.ChartPointDto;
import com.example.examguard.model.admin.monitoring.MetricCardDto;
import com.example.examguard.model.admin.monitoring.MonitoringOverviewResponse;
import com.example.examguard.service.AdminApiService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminDashboardController {

    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final AdminApiService adminApiService = new AdminApiService();
    @FXML
    private ComboBox<String> concurrentTimeScaleComboBox;
    @FXML
    private ComboBox<String> rangeComboBox;
    @FXML
    private DatePicker customStartDatePicker;
    @FXML
    private DatePicker customEndDatePicker;
    @FXML
    private LineChart<String, Number> loginVolumeChart;
    @FXML
    private LineChart<String, Number> concurrentUsersChart;
    @FXML
    private BarChart<String, Number> violationsByProgramChart;
    @FXML
    private ListView<String> recentSystemEventsListView;

    @FXML
    public void initialize() {
        loginVolumeChart.setCreateSymbols(true);
        concurrentUsersChart.setCreateSymbols(true);

        setupFilters();
        clearDashboard();
        loadOverviewAsync();
    }

    private void setupFilters() {

        rangeComboBox.setItems(FXCollections.observableArrayList(
                "Today",
                "This Month",
                "Entire Period",
                "Custom Range"
        ));
        rangeComboBox.setValue("Today");
        rangeComboBox.setOnAction(e -> {
            updateCustomRangeVisibility();
            setupTimeScaleOptions();
            reloadDashboard();
        });

        customStartDatePicker.setOnAction(e -> {
            setupTimeScaleOptions();
            reloadDashboard();
        });

        customEndDatePicker.setOnAction(e -> {
            setupTimeScaleOptions();
            reloadDashboard();
        });

        setupTimeScaleOptions();
        updateCustomRangeVisibility();
    }

    private void loadOverviewAsync() {
        Task<MonitoringOverviewResponse> task = new Task<>() {
            @Override
            protected MonitoringOverviewResponse call() throws Exception {
                return adminApiService.getOverview(buildOverviewRequest());
            }
        };

        task.setOnSucceeded(e -> applyOverviewResponse(task.getValue()));

        task.setOnFailed(e -> {
            task.getException().printStackTrace();
            clearDashboard();
            recentSystemEventsListView.setItems(
                    FXCollections.observableArrayList("Failed to load dashboard overview.")
            );
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private String formatDuration(Long durationMs) {
        if (durationMs == null || durationMs <= 0) {
            return "-";
        }

        long totalSeconds = durationMs / 1000;

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        }

        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        }

        return seconds + "s";
    }

    private boolean isSlowEvent(AdminLogRowDto log) {
        return log != null
                && log.getDurationMs() != null
                && log.getDurationMs() >= 5000;
    }

    private String eventPrefix(AdminLogRowDto log) {
        if (isSlowEvent(log)) {
            return "[SLOW]";
        }

        String status = safe(log.getStatus()).toUpperCase();

        if (status.contains("FAILED") || status.contains("ERROR")) {
            return "[FAILED]";
        }

        if (status.contains("CRITICAL")) {
            return "[CRITICAL]";
        }

        return "[EVENT]";
    }

    private void reloadDashboard() {
        if (!isCustomRangeValid()) {
            return;
        }

        loadOverviewAsync();
    }

    private void updateCustomRangeVisibility() {
        boolean custom = "Custom Range".equals(valueOf(rangeComboBox, "Today"));

        customStartDatePicker.setVisible(custom);
        customStartDatePicker.setManaged(custom);

        customEndDatePicker.setVisible(custom);
        customEndDatePicker.setManaged(custom);
    }

    private boolean isCustomRangeValid() {
        if (!"Custom Range".equals(valueOf(rangeComboBox, "Today"))) {
            return true;
        }

        if (customStartDatePicker.getValue() == null || customEndDatePicker.getValue() == null) {
            return false;
        }

        if (customEndDatePicker.getValue().isBefore(customStartDatePicker.getValue())) {
            recentSystemEventsListView.setItems(
                    FXCollections.observableArrayList("End date cannot be earlier than start date.")
            );
            return false;
        }

        return true;
    }

    private void putCustomDates(Map<String, Object> body) {
        if (!"Custom Range".equals(valueOf(rangeComboBox, "Today"))) {
            return;
        }

        if (customStartDatePicker.getValue() == null || customEndDatePicker.getValue() == null) {
            return;
        }

        body.put(
                "startDate",
                customStartDatePicker.getValue()
                        .atStartOfDay(MANILA)
                        .toOffsetDateTime()
                        .toString()
        );

        body.put(
                "endDate",
                customEndDatePicker.getValue()
                        .atTime(23, 59, 59)
                        .atZone(MANILA)
                        .toOffsetDateTime()
                        .toString()
        );
    }

    private Map<String, Object> buildOverviewRequest() {
        Map<String, Object> body = new HashMap<>();

        body.put("range", valueOf(rangeComboBox, "Today"));
        body.put("programCode", "All Programs");
        body.put("collegeCode", "All Colleges");
        body.put("role", "All Roles");

        String groupBy = valueOf(concurrentTimeScaleComboBox, "Auto");

        if ("24 Hours".equals(groupBy)) {
            groupBy = "HOUR";
        }

        body.put("groupBy", groupBy);

        putCustomDates(body);

        return body;
    }

    private void setupTimeScaleOptions() {
        if (concurrentTimeScaleComboBox == null) {
            return;
        }

        String range = valueOf(rangeComboBox, "Today");
        String previous = concurrentTimeScaleComboBox.getValue();

        ObservableList<String> options;

        boolean singleDayCustomRange =
                "Custom Range".equals(range)
                        && customStartDatePicker.getValue() != null
                        && customEndDatePicker.getValue() != null
                        && customStartDatePicker.getValue()
                        .isEqual(customEndDatePicker.getValue());

        if ("Today".equals(range) || singleDayCustomRange) {

            options = FXCollections.observableArrayList(
                    "24 Hours"
            );

        } else if ("This Month".equals(range)
                || "Custom Range".equals(range)) {

            options = FXCollections.observableArrayList(
                    "Auto",
                    "Day",
                    "Hour"
            );

        } else if ("Entire Term".equals(range)) {

            options = FXCollections.observableArrayList(
                    "Auto",
                    "Month",
                    "Day"
            );

        } else {

            options = FXCollections.observableArrayList(
                    "Auto",
                    "Year",
                    "Month"
            );
        }

        concurrentTimeScaleComboBox.setItems(options);

        if (previous != null && options.contains(previous)) {
            concurrentTimeScaleComboBox.setValue(previous);
        } else {
            concurrentTimeScaleComboBox.setValue(
                    ("Today".equals(range) || singleDayCustomRange)
                            ? "24 Hours"
                            : "Auto"
            );
        }

        concurrentTimeScaleComboBox.setOnAction(
                e -> reloadDashboard()
        );
    }


    private void applyOverviewResponse(MonitoringOverviewResponse response) {
        clearDashboard();

        if (response == null) {
            return;
        }

        applySummaryCards(response.getSummaryCards());
        applyLoginVolumeTrend(response.getLoginVolume());
        applyConcurrentUsersTrend(response.getConcurrentUsers());
        applyViolationsByProgramChart(response.getViolationsByProgram());
        applyRecentSystemEvents(response.getRecentCriticalEvents());
    }

    private void applySummaryCards(List<MetricCardDto> cards) {

        long systemLogs = 0;
        long violations = 0;
        long attentionEvents = 0;
        long activeSessions = 0;
        long sessionVolume = 0;

        if (cards != null) {
            for (MetricCardDto card : cards) {

                String label = card.getLabel() == null
                        ? ""
                        : card.getLabel().toLowerCase();

                long value = card.getValue() == null
                        ? 0
                        : card.getValue();

                if (label.contains("system logs")) {
                    systemLogs = value;
                } else if (label.contains("violation")) {
                    violations = value;
                } else if (label.contains("attention")) {
                    attentionEvents = value;
                } else if (label.contains("active session")) {
                    activeSessions = value;
                } else if (label.contains("session volume")) {
                    sessionVolume = value;
                }
            }
        }

        DashboardShellController shell = DashboardShellController.getInstance();

        if (shell != null) {
            shell.setHeroCards(
                    new DashboardShellController.HeroCardData("System Logs", String.valueOf(systemLogs)),
                    new DashboardShellController.HeroCardData("Attention Events", String.valueOf(attentionEvents)),
                    new DashboardShellController.HeroCardData("Active Sessions", String.valueOf(activeSessions)),
                    new DashboardShellController.HeroCardData("Session Volume", String.valueOf(sessionVolume))
            );
        }
    }

    private void applyLoginVolumeTrend(List<ChartPointDto> points) {
        populateLineChart(loginVolumeChart, points);
    }

    private void applyConcurrentUsersTrend(List<ChartPointDto> points) {
        populateLineChart(concurrentUsersChart, points);
    }

    private void populateLineChart(
            LineChart<String, Number> chart,
            List<ChartPointDto> points
    ) {

        chart.getData().clear();

        chart.lookupAll(".chart-point-label")
                .forEach(node -> ((Pane) node.getParent()).getChildren().remove(node));

        if (points == null || points.isEmpty()) {

            XYChart.Series<String, Number> empty = new XYChart.Series<>();
            empty.setName("No Data");
            empty.getData().add(new XYChart.Data<>("No Data", 0));

            chart.getData().add(empty);

            return;
        }

        Map<String, XYChart.Series<String, Number>>
                seriesMap = new LinkedHashMap<>();

        for (ChartPointDto point : points) {

            String role =
                    safe(point.getCategory());

            String label =
                    safe(point.getLabel());

            String timeScale =
                    valueOf(
                            concurrentTimeScaleComboBox,
                            "Auto"
                    );

            if ("24 Hours".equals(timeScale)
                    || "Hour".equalsIgnoreCase(timeScale)) {

                label =
                        formatHourLabel(label);
            }

            long value =
                    point.getValue() == null
                            ? 0
                            : point.getValue();

            XYChart.Series<String, Number> series =
                    seriesMap.computeIfAbsent(
                            role,
                            key -> {
                                XYChart.Series<String, Number> s =
                                        new XYChart.Series<>();

                                s.setName(key);

                                return s;
                            }
                    );

            XYChart.Data<String, Number> data =
                    new XYChart.Data<>(
                            label,
                            value
                    );

            series.getData().add(data);

            data.nodeProperty().addListener(
                    (obs, oldNode, node) -> {

                        if (node == null) {
                            return;
                        }

                        Platform.runLater(() -> {

                            Pane plotArea =
                                    (Pane) chart.lookup(".chart-plot-background")
                                            .getParent();

                            Label valueLabel =
                                    new Label(
                                            String.valueOf(value)
                                    );

                            valueLabel.getStyleClass()
                                    .add("chart-point-label");

                            plotArea.getChildren()
                                    .add(valueLabel);

                            valueLabel.layoutXProperty()
                                    .bind(
                                            node.layoutXProperty()
                                                    .add(6)
                                    );

                            valueLabel.layoutYProperty()
                                    .bind(
                                            node.layoutYProperty()
                                                    .subtract(18)
                                    );
                        });
                    }
            );
        }

        chart.getData().addAll(
                seriesMap.values()
        );
    }

    private void applyViolationsByProgramChart(List<ChartPointDto> points) {
        violationsByProgramChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Violations");

        if (points == null || points.isEmpty()) {
            series.getData().add(new XYChart.Data<>("No Data", 0));
            violationsByProgramChart.getData().add(series);
            return;
        }

        for (ChartPointDto point : points) {
            series.getData().add(
                    new XYChart.Data<>(
                            safe(point.getLabel()),
                            point.getValue() == null ? 0 : point.getValue()
                    )
            );
        }

        violationsByProgramChart.getData().add(series);
    }

    private void applyRecentSystemEvents(List<AdminLogRowDto> logs) {
        ObservableList<String> items = FXCollections.observableArrayList();

        if (logs != null) {
            for (AdminLogRowDto log : logs) {

                if (isSlowEvent(log)) {
                    items.add(
                            "[SLOW] " +
                                    formatDateTime(log.getStartedAt()) + " • " +
                                    safe(log.getModule()) + " • " +
                                    safe(log.getAction()) + " • " +
                                    safe(log.getActorRole()) + " • " +
                                    safe(log.getActorId()) + " • " +
                                    formatDuration(log.getDurationMs()) + " • " +
                                    safe(log.getMessage())
                    );
                }
            }
        }

        if (items.isEmpty()) {
            items.add("No recent slow activities.");
        }

        recentSystemEventsListView.setItems(items);
    }

    private void clearDashboard() {

        loginVolumeChart.getData().clear();
        concurrentUsersChart.getData().clear();
        violationsByProgramChart.getData().clear();

        recentSystemEventsListView.setItems(
                FXCollections.observableArrayList(
                        "Loading system events..."
                )
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String formatDateTime(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        try {
            return OffsetDateTime.parse(value)
                    .atZoneSameInstant(MANILA)
                    .format(DATE_TIME_FORMATTER);
        } catch (Exception e) {
            return value;
        }
    }

    private String formatHourLabel(String label) {

        try {

            return java.time.LocalDateTime
                    .parse(
                            label,
                            java.time.format.DateTimeFormatter.ofPattern(
                                    "yyyy-MM-dd HH:mm"
                            )
                    )
                    .format(
                            java.time.format.DateTimeFormatter.ofPattern(
                                    "h a"
                            )
                    );

        } catch (Exception e) {
            return label;
        }
    }

    private String valueOf(ComboBox<String> comboBox, String fallback) {
        return comboBox == null || comboBox.getValue() == null
                ? fallback
                : comboBox.getValue();
    }
}