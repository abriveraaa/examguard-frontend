package com.example.examguard.controller.faculty;

import com.example.examguard.model.core.response.BrandingResponse;
import com.example.examguard.model.faculty.dto.students.*;
import com.example.examguard.service.FacultyApiService;
import com.example.examguard.service.BrandingService;

import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FacultyStudentsController {

    @FXML private ComboBox<AcademicPeriodOption> academicPeriodCombo;
    @FXML private ComboBox<CourseOption> courseCombo;
    @FXML private ComboBox<SectionOption> sectionCombo;
    @FXML private TextField searchField;
    @FXML private ToggleButton listViewToggle;
    @FXML private ToggleButton cardViewToggle;
    @FXML private Label totalStudentsLabel;
    @FXML private Label selectedContextLabel;
    @FXML private VBox contentContainer;
    @FXML private Label pageInfoLabel;
    @FXML private ScrollPane studentsScrollPane;
    @FXML private HBox paginationBox;
    @FXML private MenuButton exportMenuButton;


    private static final int PAGE_SIZE = 10;
    private StackPane loadingOverlay;
    private int currentPage = 0;
    private List<StudentRow> currentFilteredStudents = new ArrayList<>();
    private final FacultyApiService facultyApiService = new FacultyApiService();
    private final ObservableList<StudentRow> masterStudents = FXCollections.observableArrayList();
    private final ObservableList<StudentRow> allPeriodStudents = FXCollections.observableArrayList();
    private boolean loadingDropdowns = false;

    private BrandingResponse branding;
    private ViewMode currentViewMode = ViewMode.LIST;

    @FXML
    public void initialize() {
        setupViewToggle();
        setupSearch();
        setupFilters();
        setupLoadingOverlay();

        loadBranding();
        loadAcademicPeriodsAsync();
    }


    @FXML
    private void handleExportExcel() {
        exportRoster("excel");
    }

    @FXML
    private void handleExportPdf() {
        exportRoster("pdf");
    }

    private void setupLoadingOverlay() {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(54, 54);

        Label label = new Label("Loading students...");
        label.getStyleClass().add("students-loading-label");

        VBox box = new VBox(12, spinner, label);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("students-loading-box");

        loadingOverlay = new StackPane(box);
        loadingOverlay.getStyleClass().add("students-loading-overlay");
        loadingOverlay.setVisible(false);
        loadingOverlay.setManaged(false);

        contentContainer.getChildren().add(loadingOverlay);
    }

    private void showLoading(boolean show) {
        if (loadingOverlay == null) return;

        loadingOverlay.setVisible(show);
        loadingOverlay.setManaged(show);

        academicPeriodCombo.setDisable(show);
        courseCombo.setDisable(show);
        sectionCombo.setDisable(show);
        searchField.setDisable(show);
        exportMenuButton.setDisable(show);
    }

    private void exportRoster(String type) {
        try {
            AcademicPeriodOption period = academicPeriodCombo.getValue();

            if (period == null) {
                showAlert("Export Failed", "Please select an academic period first.");
                return;
            }

            String academicYear = period.academicYear();
            String term = period.term();

            CourseOption selectedCourse = courseCombo.getValue();

            String courseCode = null;

            if (selectedCourse != null && !selectedCourse.all()) {
                courseCode = selectedCourse.courseCode();
            }

            String programCode = null;
            String yearLevel = null;
            String sectionName = null;

            SectionOption selectedSection = sectionCombo.getValue();

            if (selectedSection != null && !selectedSection.all()) {
                programCode = selectedSection.programCode();
                yearLevel = selectedSection.yearLevel();
                sectionName = selectedSection.sectionName();
            }

            FileChooser fileChooser = new FileChooser();

            String extension =
                    "pdf".equalsIgnoreCase(type)
                            ? "pdf"
                            : "xlsx";

            if ("pdf".equalsIgnoreCase(type)) {

                fileChooser.setTitle("Save Student Roster PDF");

                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter(
                                "PDF Files",
                                "*.pdf"
                        )
                );

            } else {

                fileChooser.setTitle("Save Student Roster Excel");

                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter(
                                "Excel Files",
                                "*.xlsx"
                        )
                );
            }

            fileChooser.setInitialFileName(
                    buildExportFileName(
                            "student-roster",
                            extension
                    )
            );

            File file = fileChooser.showSaveDialog( exportMenuButton.getScene().getWindow());

            if (file == null) { return;}

            byte[] bytes = facultyApiService.exportStudentsRoster(
                    academicYear,
                    term,
                    courseCode,
                    programCode,
                    yearLevel,
                    sectionName,
                    type
            );

            Files.write(file.toPath(), bytes);

            showAlert("Export Successful", "Roster exported successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(
                    "Export Failed",
                    "Unable to export roster: " + e.getMessage()
            );
        }
    }

    private String buildExportFileName(
            String reportName,
            String extension
    ) {

        List<String> parts = new ArrayList<>();

        parts.add(reportName);

        AcademicPeriodOption period =
                academicPeriodCombo.getValue();

        if (period != null) {

            parts.add(
                    sanitizeFileName(
                            period.term()
                    )
            );

            parts.add(
                    "AY-" +
                            sanitizeFileName(
                                    period.academicYear()
                            )
            );
        }

        CourseOption course =
                courseCombo.getValue();

        if (course != null && !course.all()) {

            parts.add(
                    sanitizeFileName(
                            course.courseCode()
                    )
            );
        }

        SectionOption section =
                sectionCombo.getValue();

        if (section != null && !section.all()) {

            parts.add(
                    sanitizeFileName(
                            section.programCode()
                                    + "-"
                                    + section.yearLevel()
                                    + "-"
                                    + section.sectionName()
                    )
            );
        }

        return String.join(
                "_",
                parts
        ) + "." + extension;
    }

    private String sanitizeFileName(String value) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .replaceAll("[\\\\/:*?\"<>|]", "")
                .replaceAll("\\s+", "-");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadBranding() {

        try {
            branding = new BrandingService().getBranding();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupViewToggle() {
        ToggleGroup group = new ToggleGroup();

        listViewToggle.setToggleGroup(group);
        cardViewToggle.setToggleGroup(group);

        listViewToggle.setSelected(true);

        group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                listViewToggle.setSelected(true);
                return;
            }

            currentViewMode = newToggle == cardViewToggle
                    ? ViewMode.CARD
                    : ViewMode.LIST;

            renderStudents();
        });
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            currentPage = 0;
            renderStudents();
        });
    }

    private void setupFilters() {
        academicPeriodCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;

            loadStudentsForPeriod(newVal);
        });

        courseCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (loadingDropdowns) return;

            populateSectionsFromLoadedStudents();
            applyLocalFilters();
        });

        sectionCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (loadingDropdowns) return;

            applyLocalFilters();
        });
    }

    private void loadAcademicPeriodsAsync() {
        showLoading(true);

        Task<List<FacultyAcademicPeriodDTO>> task = new Task<>() {
            @Override
            protected List<FacultyAcademicPeriodDTO> call() throws Exception {
                return facultyApiService.getStudentAcademicPeriods();
            }
        };

        task.setOnSucceeded(event -> {
            try {
                List<FacultyAcademicPeriodDTO> periods = task.getValue();

                List<AcademicPeriodOption> options = periods.stream()
                        .sorted((a, b) -> {
                            int activeCompare = Boolean.compare(
                                    isActiveOffering(b.classOfferingStatus()),
                                    isActiveOffering(a.classOfferingStatus())
                            );

                            if (activeCompare != 0) return activeCompare;

                            int ayCompare = academicYearStart(b.academicYear())
                                    .compareTo(academicYearStart(a.academicYear()));

                            if (ayCompare != 0) return ayCompare;

                            return Integer.compare(
                                    termRank(a.term()),
                                    termRank(b.term())
                            );
                        })
                        .map(p -> new AcademicPeriodOption(
                                p.academicYear(),
                                p.term(),
                                p.term() + ", AY " + p.academicYear(),
                                p.classOfferingStatus(),
                                false
                        ))
                        .distinct()
                        .toList();

                academicPeriodCombo.setItems(FXCollections.observableArrayList(options));

                if (!options.isEmpty()) {
                    academicPeriodCombo.getSelectionModel().selectFirst();
                } else {
                    showLoading(false);
                    renderEmptyState("No academic periods found.");
                }

            } catch (Exception e) {
                e.printStackTrace();
                showLoading(false);
                renderEmptyState("Failed to load academic periods.");
            }
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            showLoading(false);
            renderEmptyState("Failed to load academic periods.");
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void loadStudentsForPeriod(AcademicPeriodOption period) {
        showLoading(true);

        Task<List<FacultyStudentDTO>> task = new Task<>() {
            @Override
            protected List<FacultyStudentDTO> call() throws Exception {
                return facultyApiService.getStudentsByPeriod(
                        period.academicYear(),
                        period.term()
                );
            }
        };

        task.setOnSucceeded(event -> {
            try {
                allPeriodStudents.setAll(
                        task.getValue().stream()
                                .map(student -> new StudentRow(
                                        student.studentId(),
                                        student.fullName(),
                                        student.emailAddress(),
                                        student.collegeCode(),
                                        student.collegeName(),
                                        student.programCode(),
                                        student.programName(),
                                        student.yearLevel(),
                                        student.sectionName(),
                                        student.courseCode(),
                                        student.courseDescription(),
                                        student.classOfferingId()
                                ))
                                .toList()
                );

                populateCoursesFromLoadedStudents();
                populateSectionsFromLoadedStudents();
                applyLocalFilters();
                updateContextLabel();

            } catch (Exception e) {
                e.printStackTrace();
                renderEmptyState("Failed to load students.");
            } finally {
                showLoading(false);
            }
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            showLoading(false);
            renderEmptyState("Failed to load students.");
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void populateCoursesFromLoadedStudents() {
        loadingDropdowns = true;

        CourseOption current = courseCombo.getValue();

        List<CourseOption> options = new ArrayList<>();
        options.add(new CourseOption("ALL", "All Courses", true));

        allPeriodStudents.stream()
                .filter(s -> s.courseCode() != null && !s.courseCode().isBlank())
                .collect(java.util.stream.Collectors.toMap(
                        StudentRow::courseCode,
                        s -> new CourseOption(
                                s.courseCode(),
                                s.courseDescription(),
                                false
                        ),
                        (a, b) -> a,
                        java.util.TreeMap::new
                ))
                .values()
                .forEach(options::add);

        courseCombo.setItems(FXCollections.observableArrayList(options));

        if (current != null) {
            options.stream()
                    .filter(o -> o.courseCode().equals(current.courseCode()))
                    .findFirst()
                    .ifPresentOrElse(
                            o -> courseCombo.getSelectionModel().select(o),
                            () -> courseCombo.getSelectionModel().selectFirst()
                    );
        } else {
            courseCombo.getSelectionModel().selectFirst();
        }

        loadingDropdowns = false;
    }

    private void populateSectionsFromLoadedStudents() {
        loadingDropdowns = true;

        CourseOption selectedCourse = courseCombo.getValue();
        SectionOption current = sectionCombo.getValue();

        List<SectionOption> options = new ArrayList<>();

        options.add(new SectionOption(
                "ALL",
                "ALL",
                null,
                "ALL",
                "All Sections",
                true
        ));

        allPeriodStudents.stream()
                .filter(s ->
                        selectedCourse == null ||
                                selectedCourse.all() ||
                                selectedCourse.courseCode().equals(s.courseCode())
                )
                .filter(s ->
                        s.programCode() != null &&
                                s.yearLevel() != null &&
                                s.sectionName() != null
                )
                .collect(java.util.stream.Collectors.toMap(
                        s -> s.programCode() + "|" + s.yearLevel() + "|" + s.sectionName(),
                        s -> new SectionOption(
                                s.classOfferingId(),
                                s.programCode(),
                                s.yearLevel(),
                                s.sectionName(),
                                s.programCode() + " " + s.yearLevel() + "-" + s.sectionName(),
                                false
                        ),
                        (existing, duplicate) -> existing,
                        java.util.LinkedHashMap::new
                ))
                .values()
                .forEach(options::add);

        sectionCombo.setItems(FXCollections.observableArrayList(options));

        if (current != null) {
            options.stream()
                    .filter(o -> o.classOfferingId().equals(current.classOfferingId()))
                    .findFirst()
                    .ifPresentOrElse(
                            o -> sectionCombo.getSelectionModel().select(o),
                            () -> sectionCombo.getSelectionModel().selectFirst()
                    );
        } else {
            sectionCombo.getSelectionModel().selectFirst();
        }

        loadingDropdowns = false;
    }

    private void applyLocalFilters() {
        CourseOption course = courseCombo.getValue();
        SectionOption section = sectionCombo.getValue();

        List<StudentRow> filtered = allPeriodStudents.stream()
                .filter(s ->
                        course == null ||
                                course.all() ||
                                course.courseCode().equals(s.courseCode())
                )
                .filter(s ->
                        section == null ||
                                section.all() ||
                                (
                                        section.programCode().equals(s.programCode()) &&
                                                section.yearLevel().equals(s.yearLevel()) &&
                                                section.sectionName().equals(s.sectionName())
                                )
                )
                .toList();

        // If only academic year + term / All Courses / All Sections,
        // show each student once only.
        if (course == null || course.all()) {

            filtered = filtered.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            StudentRow::studentNo,
                            s -> s,
                            (first, duplicate) -> first,
                            java.util.LinkedHashMap::new
                    ))
                    .values()
                    .stream()
                    .toList();
        }

        masterStudents.setAll(filtered);

        currentPage = 0;
        updateContextLabel();
        renderStudents();
    }

    @FXML
    private void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            renderCurrentPage();
        }
    }

    @FXML
    private void nextPage() {
        int totalPages = getTotalPages();

        if (currentPage < totalPages - 1) {
            currentPage++;
            renderCurrentPage();
        }
    }

    private void renderCurrentPage() {
        if (currentViewMode == ViewMode.LIST) {
            renderListPage();
        } else {
            renderCardPage();
        }

        updatePagination();
    }

    private void renderStudents() {
        currentFilteredStudents = filterStudents();

        totalStudentsLabel.setText(
                currentFilteredStudents.size() +
                        " student" +
                        (currentFilteredStudents.size() == 1 ? "" : "s")
        );

        if (currentFilteredStudents.isEmpty()) {
            renderEmptyState("No students found.");
            return;
        }

        int totalPages = getTotalPages();

        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }

        if (currentViewMode == ViewMode.LIST) {
            renderListPage();
        } else {
            renderCardPage();
        }

        updatePagination();
    }

    private void renderListPage() {
        contentContainer.getChildren().clear();

        int fromIndex = currentPage * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, currentFilteredStudents.size());

        List<StudentRow> pageStudents =
                currentFilteredStudents.subList(fromIndex, toIndex);

        renderListView(pageStudents);
    }

    private void updatePagination() {
        int totalPages = getTotalPages();

        pageInfoLabel.setText(
                "Page " + (currentPage + 1) + " of " + totalPages
        );

        paginationBox.setVisible(true);
        paginationBox.setManaged(true);

        Button previousButton = (Button) paginationBox.getChildren().get(0);
        Button nextButton = (Button) paginationBox.getChildren().get(2);

        previousButton.setDisable(currentPage == 0);
        nextButton.setDisable(currentPage >= totalPages - 1);
    }

    private List<StudentRow> filterStudents() {
        String keyword = searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);

        if (keyword.isEmpty()) {
            return new ArrayList<>(masterStudents);
        }

        return masterStudents.stream()
                .filter(s ->
                        safeLower(s.studentNo()).contains(keyword) ||
                                safeLower(s.fullName()).contains(keyword) ||
                                safeLower(s.email()).contains(keyword) ||
                                safeLower(s.programCode()).contains(keyword) ||
                                safeLower(s.programName()).contains(keyword) ||
                                safeLower(s.sectionName()).contains(keyword)
                )
                .toList();
    }

    private void renderListView(List<StudentRow> students) {
        VBox table = new VBox();
        table.getStyleClass().add("students-table");

        HBox header = new HBox();
        header.getStyleClass().add("students-table-header");

        header.getChildren().addAll(
                createHeaderCell("Student Name", 1.7),
                createHeaderCell("Student ID", 1.3),
                createHeaderCell("Email", 1.8),
                createHeaderCell("College", 2.0),
                createHeaderCell("Program", 1.2),
                createHeaderCell("Year", 0.4),
                createHeaderCell("Section", 0.5)
        );

        table.getChildren().add(header);

        for (StudentRow student : students) {
            table.getChildren().add(createStudentRow(student));
        }

        contentContainer.getChildren().add(table);
    }

    private HBox createStudentRow(StudentRow student) {
        HBox row = new HBox();
        row.getStyleClass().add("students-table-row");

        Label avatar = new Label(getInitials(student.fullName()));
        avatar.getStyleClass().add("student-avatar-small");

        VBox nameBox = new VBox();
        nameBox.setAlignment(Pos.CENTER);
        nameBox.setFillWidth(true);

        Label name = new Label(student.fullName());
        name.getStyleClass().add("student-name");

        name.setWrapText(false);
        name.setMaxWidth(180); // reduced width
        name.setAlignment(Pos.CENTER);

        nameBox.getChildren().add(name);

        HBox studentCell = new HBox(10, avatar, nameBox);
        studentCell.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(nameBox, Priority.ALWAYS);

        Button profileButton = new Button("Profile");
        profileButton.getStyleClass().add("student-action-button");
        profileButton.setOnAction(e -> openStudentProfile(student));

        row.getChildren().addAll(
                createBodyCell(new Label(student.fullName()), 1.7),
                createBodyCell(new Label(student.studentNo()), 1.3),
                createBodyCell(new Label(student.email()), 1.8),
                createBodyCell(new Label(student.collegeName()), 2.0),
                createBodyCell(new Label(student.programCode()), 1.2),
                createBodyCell(new Label(String.valueOf(student.yearLevel())), 0.4),
                createBodyCell(new Label(student.sectionName()), 0.5)
        );

        return row;
    }

    private void renderCardView(List<StudentRow> students) {
        currentFilteredStudents = students;
        currentPage = 0;

        renderCardPage();
        updatePagination();
    }

    private void renderCardPage() {
        contentContainer.getChildren().clear();

        int fromIndex = currentPage * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, currentFilteredStudents.size());

        FlowPane grid = new FlowPane();
        grid.getStyleClass().add("student-card-grid");
        grid.setHgap(18);
        grid.setVgap(22);
        grid.setAlignment(Pos.TOP_LEFT);
        grid.setMaxWidth(Double.MAX_VALUE);

        for (StudentRow student : currentFilteredStudents.subList(fromIndex, toIndex)) {
            grid.getChildren().add(createStudentCard(student));
        }

        contentContainer.getChildren().add(grid);
    }

    private int getTotalPages() {
        if (currentFilteredStudents.isEmpty()) {
            return 1;
        }

        return (int) Math.ceil(
                currentFilteredStudents.size() / (double) PAGE_SIZE
        );
    }

    private VBox createStudentCard(StudentRow student) {
        VBox card = new VBox();
        card.getStyleClass().add("student-id-card");

        HBox topBrand = new HBox(2);
        topBrand.setMaxHeight(28);
        topBrand.setPrefHeight(28);
        topBrand.setSpacing(6);
        topBrand.setAlignment(Pos.TOP_CENTER);

        ImageView logo = new ImageView();
        logo.getStyleClass().add("student-card-logo");
        logo.setFitWidth(50);
        logo.setFitHeight(50);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);

        try {

            if (branding != null &&
                    branding.getLogoUrl() != null &&
                    !branding.getLogoUrl().isBlank()) {

                logo.setImage(
                        new Image(
                                BrandingService.BASE_URL +
                                        branding.getLogoUrl(),
                                true
                        )
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        Label section = new Label(student.yearLevel() + " - " + student.sectionName());
        section.getStyleClass().add("student-card-section-small");
        VBox.setMargin(section, new Insets(-25, 0, 10, 0));

        VBox schoolTextBox = new VBox(-6);
        schoolTextBox.setAlignment(Pos.CENTER);
        schoolTextBox.setPrefWidth(80);
        schoolTextBox.setMinWidth(80);
        schoolTextBox.setMaxWidth(80);
        schoolTextBox.setSpacing(-1);
        schoolTextBox.setTranslateY(-2);

        String schoolName = branding != null && branding.getSchoolName() != null
                ? branding.getSchoolName()
                : "POLYTECHNIC UNIVERSITY OF THE PHILIPPINES";

        List<javafx.scene.Node> schoolNameNodes =
                buildSchoolNameNodes(schoolName);

        schoolTextBox.getChildren().addAll(schoolNameNodes);


        topBrand.getChildren().addAll(logo, schoolTextBox);

        Label tagline = new Label("“The Country’s 1st PolytechnicU”");
        tagline.getStyleClass().add("student-card-tagline");

        Label photo = new Label(getInitials(student.fullName()));
        photo.getStyleClass().add("student-card-photo");

        Label firstName = new Label(extractFirstDisplayName(student.fullName()));
        firstName.getStyleClass().add("student-card-firstname");

        Label surname = new Label(extractSurname(student.fullName()));
        surname.getStyleClass().add("student-card-surname");

        Label studentNo = new Label(student.studentNo());
        studentNo.getStyleClass().add("student-card-studentno");

        Region divider = new Region();
        divider.getStyleClass().add("student-card-divider");

        double dividerWidth =
                Math.max(
                        90,
                        student.studentNo().length() * 10
                );

        divider.setPrefWidth(dividerWidth);
        divider.setMaxWidth(dividerWidth);

        Label program = new Label(student.programName());
        program.getStyleClass().add("student-card-program");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(
                topBrand,
                tagline,
                photo,
                section,
                firstName,
                surname,
                studentNo,
                divider,
                program,
                spacer
        );

        card.setOnMouseClicked(e -> openStudentProfile(student));

        card.setCursor(javafx.scene.Cursor.HAND);

        return card;
    }

    private List<javafx.scene.Node> buildSchoolNameNodes(String schoolName) {
        List<javafx.scene.Node> nodes = new ArrayList<>();

        List<String> words = normalizeSchoolWords(schoolName);

        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);

            if (word.equalsIgnoreCase("of")
                    && i + 1 < words.size()
                    && words.get(i + 1).equalsIgnoreCase("the")) {

                nodes.add(createOfTheLine());
                i++;
                continue;
            }

            nodes.add(createJustifiedSchoolWord(word.toUpperCase()));
        }

        return nodes;
    }

    private HBox createJustifiedSchoolWord(String word) {
        HBox row = new HBox();
        row.getStyleClass().add("student-card-school-word-row");
        row.setAlignment(Pos.CENTER);
        row.setMinHeight(12);
        row.setPrefHeight(12);
        row.setMaxHeight(12);
        row.setMaxWidth(Double.MAX_VALUE);

        for (int i = 0; i < word.length(); i++) {
            Label letter = new Label(String.valueOf(word.charAt(i)));
            letter.getStyleClass().add("student-card-school-letter");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            row.getChildren().add(letter);

            if (i < word.length() - 1) {
                row.getChildren().add(spacer);
            }
        }

        return row;
    }

    private List<String> normalizeSchoolWords(String schoolName) {
        if (schoolName == null || schoolName.isBlank()) {
            return List.of("POLYTECHNIC", "UNIVERSITY", "OF", "THE", "PHILIPPINES");
        }

        return List.of(
                schoolName.trim()
                        .replace("&", "AND")
                        .replaceAll("\\s+", " ")
                        .split(" ")
        );
    }

    private HBox createOfTheLine() {
        HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER);
        row.setMaxWidth(Double.MAX_VALUE);

        Region leftLine = new Region();
        leftLine.getStyleClass().add("student-card-school-line");

        Label ofThe = new Label("of the");
        ofThe.getStyleClass().add("student-card-of-the");

        HBox.setHgrow(leftLine, Priority.ALWAYS);

        row.getChildren().addAll(leftLine, ofThe);

        row.setMinHeight(7);
        row.setPrefHeight(7);
        row.setMaxHeight(7);

        return row;
    }

    private Region createHeaderCell(String text, double grow) {
        Label label = new Label(text);
        label.getStyleClass().add("students-table-header-cell");

        HBox box = new HBox(label);
        box.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setMinWidth(80);
        box.setPrefWidth(grow * 120);

        return box;
    }

    private Region createBodyCell(javafx.scene.Node content, double grow) {
        HBox box = new HBox(content);
        box.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setMinWidth(80);
        box.setPrefWidth(grow * 120);

        return box;
    }

    private void renderEmptyState(String message) {

        paginationBox.setVisible(false);
        paginationBox.setManaged(false);

        totalStudentsLabel.setText("0 students");
        contentContainer.getChildren().clear();

        VBox emptyBox = new VBox(8);
        emptyBox.getStyleClass().add("students-empty-state");
        emptyBox.setAlignment(Pos.CENTER);

        Label title = new Label(message);
        title.getStyleClass().add("students-empty-title");

        Label subtitle = new Label("Use the filters above to locate the class roster.");
        subtitle.getStyleClass().add("students-empty-subtitle");

        emptyBox.getChildren().addAll(title, subtitle);
        contentContainer.getChildren().add(emptyBox);
    }

    private void updateContextLabel() {
        AcademicPeriodOption period = academicPeriodCombo.getValue();
        CourseOption course = courseCombo.getValue();
        SectionOption section = sectionCombo.getValue();

        StringBuilder builder = new StringBuilder();

        if (period != null) builder.append(period.label());
        if (course != null) builder.append(" • ").append(course.courseCode());
        if (section != null) builder.append(" • ").append(section.label());

        selectedContextLabel.setText(builder.isEmpty() ? "No class selected" : builder.toString());
    }

    private void openStudentProfile(StudentRow student) {
        // TODO: Open student profile modal/page later.
        System.out.println("Opening profile for: " + student.fullName());
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";

        String[] parts = name.trim().split("\\s+");

        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }

        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private enum ViewMode {
        LIST,
        CARD
    }

    public record AcademicPeriodOption(
            String academicYear,
            String term,
            String label,
            String classOfferingStatus,
            boolean all
    ) {
        @Override
        public String toString() {
            return label;
        }
    }

    public record CourseOption(
            String courseCode,
            String courseDescription,
            boolean all
    ) {
        @Override
        public String toString() {
            return all ? "All Courses" : courseCode + " - " + courseDescription;
        }
    }

    public record SectionOption(
            String classOfferingId,
            String programCode,
            String yearLevel,
            String sectionName,
            String label,
            boolean all
    ) {
        @Override
        public String toString() {
            return label;
        }
    }

    private String extractSurname(String fullName) {
        if (fullName == null || fullName.isBlank()) return "";

        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1].toUpperCase();
    }

    private String extractFirstDisplayName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "";

        String[] parts = fullName.trim().split("\\s+");

        if (parts.length == 1) return parts[0].toUpperCase();

        String first = parts[0].toUpperCase();
        String middleInitial = "";

        if (parts.length > 2) {
            middleInitial = parts[1].substring(0, 1).toUpperCase() + ".";
        }

        return middleInitial.isBlank() ? first : first + " " + middleInitial;
    }

    private boolean isActiveOffering(String status) {
        return status != null && status.equalsIgnoreCase("ACTIVE");
    }

    private int termRank(String term) {
        if (term == null) {
            return 99;
        }

        String normalized = term.trim().toUpperCase();

        return switch (normalized) {
            case "SUMMER", "SUMMER TERM" -> 1;

            case "SECOND SEMESTER",
                 "2ND SEMESTER",
                 "SECOND" -> 2;

            case "FIRST SEMESTER",
                 "1ST SEMESTER",
                 "FIRST" -> 3;

            case "FOURTH QUARTER",
                 "4TH QUARTER",
                 "Q4" -> 1;

            case "THIRD QUARTER",
                 "3RD QUARTER",
                 "Q3" -> 2;

            case "SECOND QUARTER",
                 "2ND QUARTER",
                 "Q2" -> 3;

            case "FIRST QUARTER",
                 "1ST QUARTER",
                 "Q1" -> 4;

            default -> 99;
        };
    }

    private Integer academicYearStart(String academicYear) {
        if (academicYear == null || academicYear.isBlank()) {
            return 0;
        }

        try {
            return Integer.parseInt(academicYear.substring(0, 4));
        } catch (Exception e) {
            return 0;
        }
    }

    public record StudentRow(
            String studentNo,
            String fullName,
            String email,
            String collegeCode,
            String collegeName,
            String programCode,
            String programName,
            String yearLevel,
            String sectionName,
            String courseCode,
            String courseDescription,
            String classOfferingId
    ) {}
}