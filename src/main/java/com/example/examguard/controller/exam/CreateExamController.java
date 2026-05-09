package com.example.examguard.controller.exam;

import com.example.examguard.controller.layout.DashboardShellController;
import com.example.examguard.controller.layout.ShellAwareController;
import com.example.examguard.model.exam.*;
import com.example.examguard.service.ExamApiService;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import static com.example.examguard.utility.LoadingSpinner.setLoading;

public class CreateExamController implements ShellAwareController {

    @FXML private StackPane wizardRoot; // make sure you have this in FXML

    @FXML private VBox stepOnePane;
    @FXML private VBox stepTwoPane;
    @FXML private VBox stepThreePane;
    @FXML private VBox stepFourPane;

    @FXML private Label stepChipLabel;
    @FXML private Label uploadStatusLabel;

    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private Spinner<Integer> durationSpinner;
    @FXML private DatePicker startDatePicker;
    @FXML private ComboBox<String> startTimeCombo;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> endTimeCombo;

    @FXML private TextField classOfferingSearchField;
    @FXML private ListView<ClassOffering> classOfferingListView;
    @FXML private Label selectedClassOfferingLabel;
    @FXML private Label reviewClassOfferingLabel;

    @FXML private Label titleErrorLabel;
    @FXML private Label durationErrorLabel;
    @FXML private Label startErrorLabel;
    @FXML private Label endErrorLabel;
    @FXML private Label classErrorLabel;
    @FXML private Label modeErrorLabel;
    @FXML private Label sourceErrorLabel;

    @FXML private CheckBox shuffleQuestionsCheck;
    @FXML private CheckBox shuffleChoicesCheck;

    @FXML private Button uploadButton;

    @FXML private RadioButton asyncModeRadio;
    @FXML private RadioButton syncModeRadio;
    @FXML private RadioButton manualSourceRadio;
    @FXML private RadioButton uploadSourceRadio;

    @FXML private Label stepOneMessageLabel;
    @FXML private Label downloadTemplateLabel;

    // Step 2: Inline Question Builder

    @FXML private ListView<QuestionDraftRow> questionListView;
    @FXML private Label questionCountLabel;
    @FXML private Label editorTitleLabel;
    @FXML private Label editorSubtitleLabel;
    @FXML private Label questionStatusBadge;
    @FXML private ComboBox<String> questionTypeComboBox;
    @FXML private TextField pointsField;
    @FXML private TextArea questionTextArea;
    @FXML private VBox dynamicAnswerContainer;
    @FXML private Label validationLabel;
    @FXML private CheckBox useImagesCheckBox;
    @FXML private VBox questionImageContainer;

    private VBox choiceAImageBox;
    private VBox choiceBImageBox;
    private VBox choiceCImageBox;
    private VBox choiceDImageBox;

    // Step 3: Violations

    @FXML private ComboBox<String> focusLostSeverityCombo;
    @FXML private ComboBox<String> fullscreenExitSeverityCombo;
    @FXML private ComboBox<String> windowMinimizeSeverityCombo;
    @FXML private ComboBox<String> restrictedKeysSeverityCombo;
    @FXML private ComboBox<String> rightClickSeverityCombo;
    @FXML private ComboBox<String> multipleMonitorsSeverityCombo;

    @FXML private Spinner<Integer> focusLostLimitSpinner;
    @FXML private Spinner<Integer> fullscreenExitLimitSpinner;
    @FXML private Spinner<Integer> windowMinimizeLimitSpinner;
    @FXML private Spinner<Integer> restrictedKeysLimitSpinner;
    @FXML private Spinner<Integer> rightClickLimitSpinner;
    @FXML private Spinner<Integer> multipleMonitorsLimitSpinner;

    @FXML private CheckBox focusLostViolationCheck;
    @FXML private CheckBox fullscreenExitViolationCheck;
    @FXML private CheckBox windowMinimizeViolationCheck;
    @FXML private CheckBox restrictedKeysViolationCheck;
    @FXML private CheckBox rightClickViolationCheck;
    @FXML private CheckBox multipleMonitorsViolationCheck;
    @FXML private Spinner<Integer> warningThresholdSpinner;
    @FXML private Spinner<Integer> majorThresholdSpinner;
    @FXML private Spinner<Integer> autoSubmitThresholdSpinner;

    // STEP4:
    @FXML private Label reviewTitleLabel;
    @FXML private Label reviewDurationLabel;
    @FXML private Label reviewScheduleLabel;
    @FXML private Label reviewQuestionCountLabel;
    @FXML private Label reviewShuffleQuestionsLabel;
    @FXML private Label reviewShuffleChoicesLabel;
    @FXML private VBox reviewQuestionContainer;
    @FXML private Label reviewValidationBadge;

    @FXML private Button backButton;
    @FXML private Button nextButton;
    @FXML private Button draftButton;
    @FXML private Button publishButton;

    private final ToggleGroup examModeGroup = new ToggleGroup();
    private final ToggleGroup questionSourceGroup = new ToggleGroup();

    private boolean updatingClassSelection = false;
    private String selectedCourseCode = null;

    private final ObservableList<QuestionDraftRow> questionRows = FXCollections.observableArrayList();
    private final ObservableList<ClassOffering> classOfferingRows = FXCollections.observableArrayList();

    private QuestionDraftRow selectedQuestion;
    private boolean updatingEditor = false;
    private boolean examEditable = true;

    private TextField questionImagePathField;

    private TextField choiceAField;
    private TextField choiceBField;
    private TextField choiceCField;
    private TextField choiceDField;

    private TextField choiceAImagePathField;
    private TextField choiceBImagePathField;
    private TextField choiceCImagePathField;
    private TextField choiceDImagePathField;

    private ComboBox<String> correctChoiceComboBox;
    private ComboBox<String> trueFalseComboBox;
    private TextField identificationAnswerField;
    private TextArea essayGuideArea;

    private WizardMode wizardMode = WizardMode.CREATE;
    private Long editingExamId;

    private int currentStep = 1;
    private Runnable onCancel;
    private DashboardShellController shellController;

    private static final ZoneId MANILA_ZONE = ZoneId.of("Asia/Manila");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    private final ExamApiService examApiService = new ExamApiService();

    private enum WizardMode {
        CREATE,
        VIEW,
        EDIT
    }

    @FXML
    public void initialize() {
        setupDefaults();
        setupToggleGroups();
        setupClassOfferingList();
        setupQuestionBuilder();
        setupAutoClearErrors();
        setupViolationDefaults();

        showStep(1);
    }

    public void initViewMode(Long examId) {
        this.wizardMode = WizardMode.VIEW;
        this.editingExamId = examId;
        this.currentStep = 4;

        showStep(4);
        updateFooterButtons();
        setWizardReadOnly(true);

        setWizardLoading(true, "Loading exam preview...");

        loadExamIntoWizard(examId, true);
    }

    public void initEditMode(Long examId) {
        this.wizardMode = WizardMode.EDIT;
        this.editingExamId = examId;

        loadExamIntoWizard(examId, false);
    }

    private void setWizardLoading(boolean loading, String message) {
        if (wizardRoot == null) return;

        if (loading) {
            if (wizardRoot.lookup("#wizard-loading-overlay") != null) return;

            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setMaxSize(60, 60);

            Label label = new Label(message == null ? "Loading..." : message);
            label.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

            VBox box = new VBox(12, spinner, label);
            box.setAlignment(Pos.CENTER);

            StackPane overlay = new StackPane(box);
            overlay.setId("wizard-loading-overlay");
            overlay.setStyle("-fx-background-color: rgba(0,0,0,0.35);");

            wizardRoot.getChildren().add(overlay);
        } else {
            wizardRoot.getChildren().removeIf(node ->
                    "wizard-loading-overlay".equals(node.getId())
            );
        }
    }

    private void loadExamIntoWizard(Long examId, boolean viewOnly) {
        Task<ExamResponse> task = new Task<>() {
            @Override
            protected ExamResponse call() throws Exception {
                return examApiService.getExamPreview(examId);
            }
        };

        task.setOnSucceeded(e -> {
            setWizardLoading(false, null);

            ExamResponse exam = task.getValue();

            populateWizardFromExam(exam);

            if (viewOnly) {
                prepareFinalReview();
                currentStep = 4;
                showStep(4);
                setWizardReadOnly(true);
            } else {
                currentStep = 1;
                showStep(1);
                setWizardReadOnly(false);
            }

            updateFooterButtons();
        });

        task.setOnFailed(e -> {
            setWizardLoading(false, null);

            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();

            showError(ex == null ? "Failed to load exam." : ex.getMessage());
        });

        Thread thread = new Thread(task, "load-exam-preview-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void populateWizardFromExam(ExamResponse exam) {
        if (exam == null) return;

        titleField.setText(exam.getTitle());
        descriptionArea.setText(exam.getDescription());

        if (exam.getTimeLimitMinutes() != null) {
            durationSpinner.getValueFactory().setValue(exam.getTimeLimitMinutes());
        }

        shuffleQuestionsCheck.setSelected(Boolean.TRUE.equals(exam.getShuffleQuestions()));
        shuffleChoicesCheck.setSelected(Boolean.TRUE.equals(exam.getShuffleChoices()));

        if (exam.getRawStartDateTime() != null) {
            OffsetDateTime start = exam.getRawStartDateTime().atZoneSameInstant(MANILA_ZONE).toOffsetDateTime();
            startDatePicker.setValue(start.toLocalDate());
            startTimeCombo.setValue(start.format(TIME_FORMATTER));
        }

        if (exam.getRawEndDateTime() != null) {
            OffsetDateTime end = exam.getRawEndDateTime().atZoneSameInstant(MANILA_ZONE).toOffsetDateTime();
            endDatePicker.setValue(end.toLocalDate());
            endTimeCombo.setValue(end.format(TIME_FORMATTER));
        }

        if ("SYNCHRONOUS".equalsIgnoreCase(exam.getExamMode())) {
            syncModeRadio.setSelected(true);
        } else {
            asyncModeRadio.setSelected(true);
        }

        examEditable = isExamEditableBySchedule(exam);
        selectClassOfferingsById(exam.getClassOfferingIds());
        populateQuestionsFromPreview(exam.getQuestions());

        updateSelectedClassOfferingLabel();
        updateQuestionCount();
    }

    private boolean isExamEditableBySchedule(ExamResponse exam) {
        if (exam == null || exam.getRawStartDateTime() == null || exam.getRawEndDateTime() == null) {
            return true;
        }

        OffsetDateTime now = OffsetDateTime.now(MANILA_ZONE);

        OffsetDateTime start = exam.getRawStartDateTime()
                .atZoneSameInstant(MANILA_ZONE)
                .toOffsetDateTime();

        OffsetDateTime end = exam.getRawEndDateTime()
                .atZoneSameInstant(MANILA_ZONE)
                .toOffsetDateTime();

        // Editable only before exam starts
        return now.isBefore(start);
    }

    private void selectClassOfferingsById(List<String> classOfferingIds) {
        if (classOfferingIds == null || classOfferingIds.isEmpty()) return;

        updatingClassSelection = true;

        classOfferingListView.getSelectionModel().clearSelection();

        for (int i = 0; i < classOfferingListView.getItems().size(); i++) {
            ClassOffering item = classOfferingListView.getItems().get(i);

            if (classOfferingIds.contains(item.getClassOfferingId())) {
                classOfferingListView.getSelectionModel().select(i);
            }
        }

        updatingClassSelection = false;

        validateSelectedClassOfferings();
        updateSelectedClassOfferingLabel();
    }

    private void populateQuestionsFromPreview(List<ExamResponse.QuestionPreview> previews) {
        questionRows.clear();

        if (previews == null || previews.isEmpty()) {
            updateQuestionCount();
            return;
        }

        int questionNo = 1;

        for (ExamResponse.QuestionPreview preview : previews) {
            QuestionDraftRow row = new QuestionDraftRow();

            row.setQuestionNo(questionNo++);
            row.setQuestionType(preview.getQuestionType());
            row.setQuestionText(preview.getQuestionText());
            row.setQuestionImagePath(preview.getQuestionImageUrl());
            row.setPoints(preview.getPoints() == null ? 1 : preview.getPoints().intValue());
            row.setCorrectAnswer(preview.getCorrectAnswer());
            row.setUsesImages(!isBlank(preview.getQuestionImageUrl()));
            row.setImageStatus(hasImage(row.getQuestionImagePath()) ? "Has image" : "No image");
            row.setViolationStatus("Default");

            if ("MULTIPLE_CHOICE".equalsIgnoreCase(preview.getQuestionType())) {
                List<ExamResponse.ChoicePreview> choices = preview.getChoices();

                if (choices != null) {
                    for (int i = 0; i < choices.size(); i++) {
                        ExamResponse.ChoicePreview choice = choices.get(i);

                        if (i == 0) {
                            row.setChoiceA(choice.getChoiceText());
                            row.setChoiceAImagePath(choice.getChoiceImageUrl());
                        } else if (i == 1) {
                            row.setChoiceB(choice.getChoiceText());
                            row.setChoiceBImagePath(choice.getChoiceImageUrl());
                        } else if (i == 2) {
                            row.setChoiceC(choice.getChoiceText());
                            row.setChoiceCImagePath(choice.getChoiceImageUrl());
                        } else if (i == 3) {
                            row.setChoiceD(choice.getChoiceText());
                            row.setChoiceDImagePath(choice.getChoiceImageUrl());
                        }

                        if (choice.isCorrect()) {
                            row.setCorrectChoiceIndex(i);
                            row.setCorrectAnswer(choice.getChoiceText());
                        }

                        if (!isBlank(choice.getChoiceImageUrl())) {
                            row.setUsesImages(true);
                        }
                    }
                }
            }

            questionRows.add(row);
        }

        renumberQuestions();
        questionListView.refresh();

        if (!questionRows.isEmpty()) {
            questionListView.getSelectionModel().selectFirst();
            selectedQuestion = questionRows.get(0);
            loadQuestionToEditor(selectedQuestion);
        }

        updateQuestionCount();
    }

    private void setWizardReadOnly(boolean readOnly) {
        stepOnePane.setDisable(readOnly);
        stepTwoPane.setDisable(readOnly);
        stepThreePane.setDisable(readOnly);

        uploadButton.setDisable(readOnly);
        backButton.setDisable(false);

        if (readOnly) {
            nextButton.setText("Edit");
            nextButton.setVisible(examEditable);
            nextButton.setManaged(examEditable);

            backButton.setText("Close");
            backButton.setVisible(true);
            backButton.setManaged(true);

            draftButton.setVisible(false);
            draftButton.setManaged(false);
            publishButton.setVisible(false);
            publishButton.setManaged(false);
        }
    }

    private void setupDefaults() {
        populateTimeComboBox(startTimeCombo);
        populateTimeComboBox(endTimeCombo);

        durationSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 300, 60)
        );

        OffsetDateTime defaultStart = getNextAllowedStartTime();
        OffsetDateTime defaultEnd = defaultStart.plusHours(12);

        startDatePicker.setValue(defaultStart.toLocalDate());
        endDatePicker.setValue(defaultEnd.toLocalDate());

        startTimeCombo.setValue(defaultStart.format(TIME_FORMATTER));
        endTimeCombo.setValue(defaultEnd.format(TIME_FORMATTER));

        asyncModeRadio.setSelected(true);

        if (manualSourceRadio != null) {
            manualSourceRadio.setSelected(true);
        }

        if (stepOneMessageLabel != null) {
            stepOneMessageLabel.setText("");
        }
    }

    private OffsetDateTime getNextAllowedStartTime() {
        OffsetDateTime now = OffsetDateTime.now(MANILA_ZONE);

        OffsetDateTime next = now
                .plusHours(1)
                .truncatedTo(ChronoUnit.HOURS);

        if (!now.equals(now.truncatedTo(ChronoUnit.HOURS))) {
            next = next.plusHours(1);
        }

        return next;
    }

    private void setupToggleGroups() {
        asyncModeRadio.setToggleGroup(examModeGroup);
        syncModeRadio.setToggleGroup(examModeGroup);

        if (manualSourceRadio != null) {
            manualSourceRadio.setToggleGroup(questionSourceGroup);
        }

        if (uploadSourceRadio != null) {
            uploadSourceRadio.setToggleGroup(questionSourceGroup);
        }
    }

    private void setupClassOfferingList() {
        FilteredList<ClassOffering> filteredList =
                new FilteredList<>(classOfferingRows, b -> true);

        classOfferingSearchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String keyword = newValue == null ? "" : newValue.toLowerCase();

            filteredList.setPredicate(item -> {
                if (keyword.isEmpty()) return true;
                return item.getDisplayName().toLowerCase().contains(keyword);
            });
        });

        SortedList<ClassOffering> sortedList = new SortedList<>(filteredList);

        classOfferingListView.setItems(sortedList);
        classOfferingListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        classOfferingListView.getSelectionModel()
                .getSelectedItems()
                .addListener((ListChangeListener<ClassOffering>) change -> {
                    if (updatingClassSelection) return;

                    validateSelectedClassOfferings();
                    updateSelectedClassOfferingLabel();
                });

        Task<List<ClassOffering>> task = new Task<>() {
            @Override
            protected List<ClassOffering> call() throws Exception {
                return examApiService.fetchClassOfferings();
            }
        };

        task.setOnSucceeded(e -> {
            classOfferingRows.setAll(task.getValue());
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();
            showError("Failed to load class offerings.");
        });

        Thread thread = new Thread(task, "load-class-offerings-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void validateSelectedClassOfferings() {
        ObservableList<ClassOffering> selectedItems =
                classOfferingListView.getSelectionModel().getSelectedItems();

        if (selectedItems == null || selectedItems.isEmpty()) {
            selectedCourseCode = null;
            clearError(classOfferingListView, classErrorLabel);
            return;
        }

        selectedCourseCode = selectedItems.get(0).getCourseCode();

        List<ClassOffering> invalidItems = new ArrayList<>();

        for (ClassOffering item : selectedItems) {
            if (item.getCourseCode() == null ||
                    !item.getCourseCode().equalsIgnoreCase(selectedCourseCode)) {
                invalidItems.add(item);
            }
        }

        if (!invalidItems.isEmpty()) {
            updatingClassSelection = true;

            for (ClassOffering invalid : invalidItems) {
                classOfferingListView.getSelectionModel().clearSelection(
                        classOfferingListView.getItems().indexOf(invalid)
                );
            }

            updatingClassSelection = false;

            setError(
                    classOfferingListView,
                    classErrorLabel,
                    "You can only select class offerings with the same course code: " + selectedCourseCode
            );
        } else {
            clearError(classOfferingListView, classErrorLabel);
        }
    }

    private void updateSelectedClassOfferingLabel() {
        int count = classOfferingListView.getSelectionModel()
                .getSelectedItems()
                .size();

        if (count == 0) {
            selectedClassOfferingLabel.setText("No class selected");
            clearError(classOfferingListView, classErrorLabel);
        } else {
            String courseCode = classOfferingListView.getSelectionModel()
                    .getSelectedItems()
                    .get(0)
                    .getCourseCode();

            selectedClassOfferingLabel.setText(
                    count + " class(es) selected • " + courseCode
            );
        }
    }

    private void setupQuestionBuilder() {
        useImagesCheckBox.selectedProperty().addListener((obs, oldVal, selected) -> {
            questionImageContainer.setVisible(selected);
            questionImageContainer.setManaged(selected);

            toggleChoiceImages(selected);

            if (!selected) {
                if (questionImagePathField != null) questionImagePathField.clear();

                if (choiceAImagePathField != null) choiceAImagePathField.clear();
                if (choiceBImagePathField != null) choiceBImagePathField.clear();
                if (choiceCImagePathField != null) choiceCImagePathField.clear();
                if (choiceDImagePathField != null) choiceDImagePathField.clear();
            }

            updateCorrectAnswerChoices();

            if (!updatingEditor) {
                saveEditorToSelectedQuestion(false);
            }
        });

        questionTypeComboBox.setItems(FXCollections.observableArrayList(
                "MULTIPLE_CHOICE",
                "TRUE_FALSE",
                "IDENTIFICATION",
                "ESSAY"
        ));

        questionListView.setItems(questionRows);

        questionListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(QuestionDraftRow item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                VBox box = new VBox(3);

                Label title = new Label("Q" + item.getQuestionNo() + " • " + item.getQuestionType());
                title.getStyleClass().add("question-list-title");

                Label preview = new Label(isBlank(item.getQuestionText())
                        ? "No question text yet"
                        : item.getQuestionText());
                preview.getStyleClass().add("question-list-preview");
                preview.setWrapText(true);

                String imageNote = hasImage(item.getQuestionImagePath()) ? " • Has question image" : "";
                String status = isQuestionComplete(item) ? "Complete" : "Incomplete";

                Label meta = new Label(item.getPoints() + " point(s) • " + status + imageNote);
                meta.getStyleClass().add(isQuestionComplete(item)
                        ? "question-complete-text"
                        : "question-incomplete-text");

                box.getChildren().addAll(title, preview, meta);
                setGraphic(box);
            }
        });

        questionListView.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldQuestion, newQuestion) -> {
                    if (updatingEditor) return;

                    saveEditorToSelectedQuestion(false);
                    selectedQuestion = newQuestion;
                    loadQuestionToEditor(newQuestion);
                });

        questionTypeComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || updatingEditor) return;

            selectedQuestion.setQuestionType(newValue);
            selectedQuestion.setCorrectAnswer("");
            selectedQuestion.setCorrectChoiceIndex(-1);

            renderAnswerFields(newValue);
            updateQuestionStatus();
            questionListView.refresh();
        });

        questionTextArea.textProperty().addListener((obs, oldValue, newValue) ->
                updateSelectedQuestionLive());

        pointsField.textProperty().addListener((obs, oldValue, newValue) ->
                updateSelectedQuestionLive());

        updateQuestionCount();
        setEditorDisabled(true);
    }

    private void setupAutoClearErrors() {
        titleField.textProperty().addListener((obs, oldValue, newValue) ->
                clearError(titleField, titleErrorLabel));

        durationSpinner.valueProperty().addListener((obs, oldValue, newValue) ->
                clearError(durationSpinner, durationErrorLabel));

        startDatePicker.valueProperty().addListener((obs, oldValue, newValue) ->
                clearError(startDatePicker, startErrorLabel));

        startTimeCombo.valueProperty().addListener((obs, oldValue, newValue) ->
                clearError(startTimeCombo, startErrorLabel));

        endDatePicker.valueProperty().addListener((obs, oldValue, newValue) ->
                clearError(endDatePicker, endErrorLabel));

        endTimeCombo.valueProperty().addListener((obs, oldValue, newValue) ->
                clearError(endTimeCombo, endErrorLabel));

        classOfferingListView.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldValue, newValue) ->
                        clearError(classOfferingListView, classErrorLabel));

        examModeGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) ->
                clearTextError(modeErrorLabel));

        questionSourceGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) ->
                clearTextError(sourceErrorLabel));
    }

    private ExamRequest buildExamRequest(boolean saveCurrentEditor) {

        if (saveCurrentEditor) {
            saveEditorToSelectedQuestion(false);
        }

        ExamRequest request = new ExamRequest();

        request.setTitle(titleField.getText().trim());
        request.setDescription(descriptionArea.getText() == null ? "" : descriptionArea.getText().trim());
        request.setTimeLimitMinutes(durationSpinner.getValue());

        request.setShuffleQuestions(shuffleQuestionsCheck.isSelected());
        request.setShuffleChoices(shuffleChoicesCheck.isSelected());

        request.setStartDateTime(getStartDateTime());
        request.setEndDateTime(getEndDateTime());

        request.setExamMode(asyncModeRadio.isSelected() ? "ASYNCHRONOUS" : "SYNCHRONOUS");

        List<String> selectedIds = classOfferingListView.getSelectionModel()
                .getSelectedItems()
                .stream()
                .map(ClassOffering::getClassOfferingId)
                .toList();

        request.setClassOfferingIds(selectedIds);

        request.setQuestions(mapQuestions());

        request.setViolationSettings(buildViolationSettings());

        return request;
    }

    // BUTTONS

    @FXML
    private void handleNext() {
        if (wizardMode == WizardMode.VIEW) {
            if (!examEditable) {
                showError("This exam can no longer be edited because it has already started.");
                return;
            }

            switchFromViewToEdit();
            return;
        }

        if (currentStep == 1) {
            if (!validateStepOne()) return;
            showStep(2);
            return;
        }

        if (currentStep == 2) {
            saveEditorToSelectedQuestion(false);

            if (questionRows.isEmpty()) {
                validationLabel.setText("Please add at least one question.");
                return;
            }

            for (QuestionDraftRow question : questionRows) {
                if (!isQuestionComplete(question)) {
                    questionListView.getSelectionModel().select(question);
                    validationLabel.setText("Please complete Question " + question.getQuestionNo() + " before continuing.");
                    return;
                }
            }

            showStep(3);
            return;
        }

        if (currentStep == 3) {
            // Placeholder for violation validation later
            prepareFinalReview();
            showStep(4);
        }
    }

    @FXML
    private void handleBack() {
        if (wizardMode == WizardMode.VIEW){
            handleCancel();
            return;
        }

        if (currentStep > 1) {
            saveEditorToSelectedQuestion(false);
            showStep(currentStep - 1);
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();

        if (onCancel != null) {
            onCancel.run();
        }
    }

    @FXML
    private void handleSaveDraft() {

        setLoading(draftButton, true, "Saving...", "Save as Draft");

        saveEditorToSelectedQuestion(false);
        Task<ExamResult> task = new Task<>() {
            @Override
            protected ExamResult call() throws Exception {

                uploadPendingImages();

                ExamRequest request = buildExamRequest(false);

                if (wizardMode == WizardMode.EDIT && editingExamId != null) {
                    return examApiService.updateExam(editingExamId, request);
                }

                return examApiService.examDraft(request);
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(draftButton, false, "Saving...", "Save as Draft");

            ExamResult result = task.getValue();

            if (result == null || !result.isSuccess()) {
                showError(result == null ? "Failed to save draft." : result.getMessage());
                return;
            }

            showSuccess("Exam saved as draft.");
            closeWizardAndReturnToTable();
        });

        task.setOnFailed(e -> {
            setLoading(draftButton, false, "Saving...", "Save as Draft");

            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();

            showError(ex == null ? "Failed to save draft." : ex.getMessage());
        });

        Thread thread = new Thread(task, "save-draft-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void switchFromViewToEdit() {
        wizardMode = WizardMode.EDIT;

        setWizardReadOnly(false);

        nextButton.setText("Next");
        backButton.setText("Back");

        showStep(1);
        updateFooterButtons();
    }

    @FXML
    private void handlePublish() {

        setLoading(publishButton, true, "Publishing...", "Publish");

        saveEditorToSelectedQuestion(false);
        Task<ExamResult> task = new Task<>() {
            @Override
            protected ExamResult call() throws Exception {

                uploadPendingImages();

                ExamRequest request = buildExamRequest(false);

                if (wizardMode == WizardMode.EDIT && editingExamId != null) {
                    ExamResult updateResult = examApiService.updateExam(editingExamId, request);

                    if (updateResult == null || !updateResult.isSuccess()) {
                        return updateResult;
                    }

                    return examApiService.publishExamById(editingExamId);
                }

                return examApiService.examPublish(request);
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(publishButton, false, "Publishing...", "Publish");

            ExamResult result = task.getValue();

            if (result == null || !result.isSuccess()) {
                showError(result == null ? "Failed to publish exam." : result.getMessage());
                return;
            }

            showSuccess("Exam published successfully.");
            closeWizardAndReturnToTable();
        });

        task.setOnFailed(e -> {
            setLoading(publishButton, false, "Publishing...", "Publish");

            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();

            showError(ex == null ? "Failed to publish exam." : ex.getMessage());
        });

        Thread thread = new Thread(task, "publish-exam-thread");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleUploadTemplate() {

        if (!validateStepOne()) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Exam Template");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );

        File file = fileChooser.showOpenDialog(uploadButton.getScene().getWindow());

        if (file == null) return;

        setLoading(uploadButton, true, "Uploading...", "Upload Template");

        Task<UploadExamTemplateResponse> task = new Task<>() {
            @Override
            protected UploadExamTemplateResponse call() throws Exception {
                return examApiService.previewExamTemplate(file);
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(uploadButton, false, "Uploading...", "Upload Template");

            UploadExamTemplateResponse response = task.getValue();

            if (response == null || !response.isSuccess()) {
                showError(response == null ? "Upload failed." : response.getMessage());
                return;
            }

            addUploadedQuestionsToStepTwo(response.getQuestions());

            if (uploadStatusLabel != null) {
                setUploadSuccess("Uploaded " + response.getQuestions().size() + " question(s).");
            }

            showSuccess("Uploaded " + response.getQuestions().size() + " question(s). Please review them in Step 2.");

        });

        task.setOnFailed(e -> {
            setLoading(uploadButton, false, "Uploading...", "Upload Template");

            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();

            String message = ex == null ? "Upload failed." : ex.getMessage();
            setUploadError(message);
            showError(message);
        });

        Thread thread = new Thread(task, "preview-exam-template-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void uploadPendingImages() throws Exception {

        for (QuestionDraftRow question : questionRows) {

            // Question Image
            if (isLocalFile(question.getQuestionImagePath())) {

                ImageUploadResponse res =
                        examApiService.uploadExamImage(
                                compressImage(new File(question.getQuestionImagePath()))
                        );

                if (!res.isSuccess()) {
                    throw new RuntimeException(res.getMessage());
                }

                question.setQuestionImagePath(res.getImageUrl());
            }

            // Choice A
            if (isLocalFile(question.getChoiceAImagePath())) {

                ImageUploadResponse res =
                        examApiService.uploadExamImage(
                                compressImage(new File(question.getChoiceAImagePath()))
                        );

                if (!res.isSuccess()) {
                    throw new RuntimeException(res.getMessage());
                }

                question.setChoiceAImagePath(res.getImageUrl());
            }

            // Choice B
            if (isLocalFile(question.getChoiceBImagePath())) {

                ImageUploadResponse res =
                        examApiService.uploadExamImage(
                                compressImage(new File(question.getChoiceBImagePath()))
                        );

                if (!res.isSuccess()) {
                    throw new RuntimeException(res.getMessage());
                }

                question.setChoiceBImagePath(res.getImageUrl());
            }

            // Choice C
            if (isLocalFile(question.getChoiceCImagePath())) {

                ImageUploadResponse res =
                        examApiService.uploadExamImage(
                                compressImage(new File(question.getChoiceCImagePath()))
                        );

                if (!res.isSuccess()) {
                    throw new RuntimeException(res.getMessage());
                }

                question.setChoiceCImagePath(res.getImageUrl());
            }

            // Choice D
            if (isLocalFile(question.getChoiceDImagePath())) {

                ImageUploadResponse res =
                        examApiService.uploadExamImage(
                                compressImage(new File(question.getChoiceDImagePath()))
                        );

                if (!res.isSuccess()) {
                    throw new RuntimeException(res.getMessage());
                }

                question.setChoiceDImagePath(res.getImageUrl());
            }
        }
    }

    private boolean isLocalFile(String path) {

        if (path == null || path.isBlank()) {
            return false;
        }

        return !path.startsWith("/uploads/");
    }

    private void setUploadSuccess(String message) {
        if (uploadStatusLabel == null) return;

        uploadStatusLabel.setText(message);

        uploadStatusLabel.getStyleClass().removeAll("error-text", "info-text");
        uploadStatusLabel.getStyleClass().add("success-text");
    }

    private void setUploadError(String message) {
        if (uploadStatusLabel == null) return;

        uploadStatusLabel.setText(message);

        uploadStatusLabel.getStyleClass().removeAll("success-text", "info-text");
        uploadStatusLabel.getStyleClass().add("error-text");
    }

    private void setUploadInfo(String message) {
        if (uploadStatusLabel == null) return;

        uploadStatusLabel.setText(message);

        uploadStatusLabel.getStyleClass().removeAll("success-text", "error-text");
        uploadStatusLabel.getStyleClass().add("info-text");
    }

    private void addUploadedQuestionsToStepTwo(List<QuestionRequest> uploadedQuestions) {
        if (uploadedQuestions == null || uploadedQuestions.isEmpty()) {
            validationLabel.setText("No valid questions found in the uploaded template.");
            return;
        }

        saveEditorToSelectedQuestion(false);

        questionRows.clear();

        int questionNo = 1;

        for (QuestionRequest request : uploadedQuestions) {
            QuestionDraftRow row = new QuestionDraftRow();

            row.setQuestionNo(questionNo++);
            row.setQuestionText(request.getQuestionText());
            row.setQuestionType(request.getQuestionType());
            row.setPoints(request.getPoints() <= 0 ? 1 : request.getPoints());
            row.setViolationStatus("Default");

            if ("MULTIPLE_CHOICE".equalsIgnoreCase(request.getQuestionType())) {
                List<ChoiceRequest> choices = request.getChoices();

                if (choices != null) {
                    if (choices.size() > 0) row.setChoiceA(choices.get(0).getChoiceText());
                    if (choices.size() > 1) row.setChoiceB(choices.get(1).getChoiceText());
                    if (choices.size() > 2) row.setChoiceC(choices.get(2).getChoiceText());
                    if (choices.size() > 3) row.setChoiceD(choices.get(3).getChoiceText());

                    for (int i = 0; i < choices.size(); i++) {
                        if (choices.get(i).isCorrect()) {
                            row.setCorrectChoiceIndex(i);
                            row.setCorrectAnswer(choices.get(i).getChoiceText());
                            break;
                        }
                    }
                }

            } else {
                row.setCorrectAnswer(request.getCorrectAnswer());
            }

            row.setImageStatus(hasImage(row.getQuestionImagePath()) ? "Has image" : "No image");

            questionRows.add(row);
        }

        renumberQuestions();
        updateQuestionCount();

        questionListView.getSelectionModel().clearSelection();

        if (!questionRows.isEmpty()) {
            questionListView.getSelectionModel().selectFirst();
            selectedQuestion = questionRows.get(0);
            loadQuestionToEditor(selectedQuestion);
        }

        questionListView.refresh();
        validationLabel.setText("");
    }

    @FXML
    private void handleDownloadTemplate(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Question Template");
        fileChooser.setInitialFileName("exam_template.xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );

        Node source = (Node) event.getSource();
        File file = fileChooser.showSaveDialog(source.getScene().getWindow());

        if (file == null) return;

        try {
            examApiService.downloadTemplate(file);
            showSuccess("Template downloaded successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to download template.");
        }
    }

    @FXML
    private void handleAddQuestion() {
        saveEditorToSelectedQuestion(false);

        QuestionDraftRow question = new QuestionDraftRow();
        question.setQuestionNo(questionRows.size() + 1);
        question.setQuestionType("MULTIPLE_CHOICE");
        question.setPoints(1);
        question.setImageStatus("No image");
        question.setViolationStatus("Default");

        questionRows.add(question);

        questionListView.getSelectionModel().select(question);
        questionListView.scrollTo(question);
        questionListView.refresh();

        updateQuestionCount();
        validationLabel.setText("");
    }

    @FXML
    private void handleSaveQuestion() {
        if (selectedQuestion == null) {
            validationLabel.setText("Please select or add a question first.");
            return;
        }

        if (!saveEditorToSelectedQuestion(true)) {
            return;
        }

        questionListView.refresh();
        updateQuestionStatus();
        validationLabel.setText("Question saved.");
    }

    @FXML
    private void handleClearQuestionEditor() {
        if (selectedQuestion == null) return;

        questionTextArea.clear();
        pointsField.setText("1");

        if (questionImagePathField != null) questionImagePathField.clear();

        if (choiceAField != null) choiceAField.clear();
        if (choiceBField != null) choiceBField.clear();
        if (choiceCField != null) choiceCField.clear();
        if (choiceDField != null) choiceDField.clear();

        if (choiceAImagePathField != null) choiceAImagePathField.clear();
        if (choiceBImagePathField != null) choiceBImagePathField.clear();
        if (choiceCImagePathField != null) choiceCImagePathField.clear();
        if (choiceDImagePathField != null) choiceDImagePathField.clear();

        if (correctChoiceComboBox != null) correctChoiceComboBox.getSelectionModel().clearSelection();
        if (trueFalseComboBox != null) trueFalseComboBox.getSelectionModel().clearSelection();
        if (identificationAnswerField != null) identificationAnswerField.clear();
        if (essayGuideArea != null) essayGuideArea.clear();

        selectedQuestion.setCorrectAnswer("");
        selectedQuestion.setCorrectChoiceIndex(-1);

        validationLabel.setText("");
        updateQuestionStatus();
        questionListView.refresh();
    }

    @FXML
    private void handleDuplicateQuestion() {
        if (selectedQuestion == null) {
            validationLabel.setText("Select a question to duplicate.");
            return;
        }

        saveEditorToSelectedQuestion(false);

        QuestionDraftRow source = selectedQuestion;

        QuestionDraftRow copy = new QuestionDraftRow();
        copy.setQuestionNo(questionRows.size() + 1);
        copy.setQuestionText(source.getQuestionText());
        copy.setQuestionImagePath(source.getQuestionImagePath());
        copy.setQuestionType(source.getQuestionType());
        copy.setImageStatus(source.getImageStatus());
        copy.setViolationStatus(source.getViolationStatus());
        copy.setPoints(source.getPoints());
        copy.setUsesImages(source.isUsesImages());

        copy.setChoiceA(source.getChoiceA());
        copy.setChoiceB(source.getChoiceB());
        copy.setChoiceC(source.getChoiceC());
        copy.setChoiceD(source.getChoiceD());

        copy.setChoiceAImagePath(source.getChoiceAImagePath());
        copy.setChoiceBImagePath(source.getChoiceBImagePath());
        copy.setChoiceCImagePath(source.getChoiceCImagePath());
        copy.setChoiceDImagePath(source.getChoiceDImagePath());

        copy.setCorrectAnswer(source.getCorrectAnswer());
        copy.setCorrectChoiceIndex(source.getCorrectChoiceIndex());

        questionRows.add(copy);
        renumberQuestions();

        updatingEditor = true;
        questionListView.getSelectionModel().select(copy);
        selectedQuestion = copy;
        updatingEditor = false;

        loadQuestionToEditor(copy);

        updateQuestionCount();
        questionListView.scrollTo(copy);
        questionListView.refresh();

        validationLabel.setText("Question duplicated.");
    }

    @FXML
    private void handleDeleteQuestion() {
        int selectedIndex = questionListView.getSelectionModel().getSelectedIndex();

        if (selectedQuestion == null || selectedIndex < 0) {
            validationLabel.setText("Select a question to delete.");
            return;
        }

        updatingEditor = true;

        questionRows.remove(selectedIndex);
        renumberQuestions();
        updateQuestionCount();

        if (questionRows.isEmpty()) {
            selectedQuestion = null;
            questionListView.getSelectionModel().clearSelection();
            clearEditor();
            setEditorDisabled(true);
            validationLabel.setText("Question deleted.");
            updatingEditor = false;
            return;
        }

        int nextIndex = Math.min(selectedIndex, questionRows.size() - 1);
        QuestionDraftRow nextQuestion = questionRows.get(nextIndex);

        questionListView.getSelectionModel().select(nextIndex);
        selectedQuestion = nextQuestion;

        updatingEditor = false;

        loadQuestionToEditor(nextQuestion);

        questionListView.refresh();
        validationLabel.setText("Question deleted.");
    }

    @FXML
    private void handleMoveQuestionUp() {
        int index = questionListView.getSelectionModel().getSelectedIndex();

        if (index <= 0) return;

        saveEditorToSelectedQuestion(false);

        QuestionDraftRow temp = questionRows.get(index);
        questionRows.set(index, questionRows.get(index - 1));
        questionRows.set(index - 1, temp);

        renumberQuestions();
        questionListView.getSelectionModel().select(index - 1);
        questionListView.refresh();
    }

    @FXML
    private void handleMoveQuestionDown() {
        int index = questionListView.getSelectionModel().getSelectedIndex();

        if (index < 0 || index >= questionRows.size() - 1) return;

        saveEditorToSelectedQuestion(false);

        QuestionDraftRow temp = questionRows.get(index);
        questionRows.set(index, questionRows.get(index + 1));
        questionRows.set(index + 1, temp);

        renumberQuestions();
        questionListView.getSelectionModel().select(index + 1);
        questionListView.refresh();
    }

    private void renderAnswerFields(String type) {
        dynamicAnswerContainer.getChildren().clear();
        questionImageContainer.getChildren().clear();
        resetDynamicEditorFields();

        questionImagePathField = new TextField();

        questionImageContainer.getChildren().add(
                createImagePickerBox("Question Image Optional", questionImagePathField)
        );

        boolean showImages = useImagesCheckBox.isSelected();
        questionImageContainer.setVisible(showImages);
        questionImageContainer.setManaged(showImages);

        if ("MULTIPLE_CHOICE".equals(type)) {
            renderMultipleChoiceFields();

        } else if ("TRUE_FALSE".equals(type)) {
            trueFalseComboBox = new ComboBox<>();
            trueFalseComboBox.setItems(FXCollections.observableArrayList("TRUE", "FALSE"));
            trueFalseComboBox.setPromptText("Select correct answer");
            trueFalseComboBox.setMaxWidth(Double.MAX_VALUE);
            trueFalseComboBox.getStyleClass().add("modern-combo");

            dynamicAnswerContainer.getChildren().add(
                    createFieldBox("Correct Answer", trueFalseComboBox)
            );

        } else if ("IDENTIFICATION".equals(type)) {
            identificationAnswerField = createAnswerInput("Enter correct answer");

            dynamicAnswerContainer.getChildren().add(
                    createFieldBox("Correct Answer", identificationAnswerField)
            );

        } else if ("ESSAY".equals(type)) {
            essayGuideArea = new TextArea();
            essayGuideArea.setPromptText("Optional rubric/guide");
            essayGuideArea.setWrapText(true);
            essayGuideArea.setPrefRowCount(4);
            essayGuideArea.getStyleClass().add("modern-textarea");

            dynamicAnswerContainer.getChildren().add(
                    createFieldBox("Guide Answer", essayGuideArea)
            );
        }

        toggleChoiceImages(showImages);
    }

    private void renderMultipleChoiceFields() {
        choiceAField = createAnswerInput("Choice 1");
        choiceBField = createAnswerInput("Choice 2");
        choiceCField = createAnswerInput("Choice 3");
        choiceDField = createAnswerInput("Choice 4");

        choiceAImagePathField = new TextField();
        choiceBImagePathField = new TextField();
        choiceCImagePathField = new TextField();
        choiceDImagePathField = new TextField();

        correctChoiceComboBox = new ComboBox<>();
        correctChoiceComboBox.setPromptText("Select correct choice");
        correctChoiceComboBox.getStyleClass().add("modern-combo");
        correctChoiceComboBox.setMaxWidth(Double.MAX_VALUE);

        choiceAField.textProperty().addListener((obs, oldVal, newVal) -> updateCorrectAnswerChoices());
        choiceBField.textProperty().addListener((obs, oldVal, newVal) -> updateCorrectAnswerChoices());
        choiceCField.textProperty().addListener((obs, oldVal, newVal) -> updateCorrectAnswerChoices());
        choiceDField.textProperty().addListener((obs, oldVal, newVal) -> updateCorrectAnswerChoices());

        choiceAImageBox = createImagePickerBox("Choice 1 Image Optional", choiceAImagePathField);
        choiceBImageBox = createImagePickerBox("Choice 2 Image Optional", choiceBImagePathField);
        choiceCImageBox = createImagePickerBox("Choice 3 Image Optional", choiceCImagePathField);
        choiceDImageBox = createImagePickerBox("Choice 4 Image Optional", choiceDImagePathField);

        dynamicAnswerContainer.getChildren().addAll(
                createChoiceBox("Choice 1", choiceAField, choiceAImageBox),
                createChoiceBox("Choice 2", choiceBField, choiceBImageBox),
                createChoiceBox("Choice 3", choiceCField, choiceCImageBox),
                createChoiceBox("Choice 4", choiceDField, choiceDImageBox),
                createFieldBox("Correct Answer", correctChoiceComboBox)
        );

        updateCorrectAnswerChoices();
        toggleChoiceImages(useImagesCheckBox != null && useImagesCheckBox.isSelected());
    }

    private VBox createChoiceBox(String labelText, TextField choiceTextField, VBox imageBox) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");

        VBox box = new VBox(8, label, choiceTextField, imageBox);
        box.getStyleClass().add("choice-card");

        return box;
    }

    private void resetDynamicEditorFields() {
        questionImagePathField = null;

        choiceAField = null;
        choiceBField = null;
        choiceCField = null;
        choiceDField = null;

        choiceAImageBox = null;
        choiceBImageBox = null;
        choiceCImageBox = null;
        choiceDImageBox = null;

        choiceAImagePathField = null;
        choiceBImagePathField = null;
        choiceCImagePathField = null;
        choiceDImagePathField = null;

        correctChoiceComboBox = null;
        trueFalseComboBox = null;
        identificationAnswerField = null;
        essayGuideArea = null;
    }

    private TextField createAnswerInput(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("modern-input");
        return field;
    }

    private VBox createFieldBox(String labelText, Control field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");

        return new VBox(6, label, field);
    }

    private VBox createImagePickerBox(String labelText, TextField pathField) {

        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");

        pathField.setEditable(false);
        pathField.setPromptText("No image selected");
        pathField.getStyleClass().add("modern-input");

        ImageView preview = new ImageView();
        preview.setFitHeight(120);
        preview.setFitWidth(220);
        preview.setPreserveRatio(true);
        preview.setSmooth(true);
        preview.setVisible(false);
        preview.setManaged(false);

        Runnable refreshPreview = () -> {
            String path = pathField.getText();

            if (path == null || path.isBlank()) {
                preview.setImage(null);
                preview.setVisible(false);
                preview.setManaged(false);
                return;
            }

            if (path.startsWith("/uploads/")) {

                String fullUrl = ExamApiService.BASE_URL + path;
                preview.setImage(new Image(fullUrl, true));

            } else {

                File localFile = new File(path);

                preview.setImage(new Image(
                        localFile.toURI().toString(),
                        true
                ));
            }
            preview.setVisible(true);
            preview.setManaged(true);
        };

        pathField.textProperty().addListener((obs, oldVal, newVal) -> refreshPreview.run());

        Button chooseButton = new Button("Choose Image");
        chooseButton.getStyleClass().add("outline-small-button");

        Button removeButton = new Button("Remove");
        removeButton.getStyleClass().add("danger-small-button");

        chooseButton.setOnAction(e -> {
            File file = chooseImageFile();
            if (file == null) return;

            pathField.setText(file.getAbsolutePath());

            updateCorrectAnswerChoices();
            saveEditorToSelectedQuestion(false);
            questionListView.refresh();
        });

        removeButton.setOnAction(e -> {
            pathField.clear();

            updateCorrectAnswerChoices();
            saveEditorToSelectedQuestion(false);
            questionListView.refresh();
        });

        HBox actions = new HBox(8, pathField, chooseButton, removeButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(pathField, Priority.ALWAYS);

        return new VBox(6, label, actions, preview);
    }

    private File chooseImageFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Image");

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Image Files",
                        "*.png", "*.jpg", "*.jpeg", "*.gif"
                )
        );

        return fileChooser.showOpenDialog(questionTextArea.getScene().getWindow());
    }

    private File compressImage(File originalFile) throws Exception {

        BufferedImage originalImage = ImageIO.read(originalFile);

        if (originalImage == null) {
            throw new RuntimeException("Invalid image file.");
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        int maxSize = 1000;

        double scale = Math.min(
                (double) maxSize / originalWidth,
                (double) maxSize / originalHeight
        );

        if (scale > 1) {
            scale = 1;
        }

        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        BufferedImage resizedImage = new BufferedImage(
                newWidth,
                newHeight,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics = resizedImage.createGraphics();

        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );

        graphics.drawImage(
                originalImage,
                0,
                0,
                newWidth,
                newHeight,
                null
        );

        graphics.dispose();

        File compressedFile = File.createTempFile(
                "examguard-compressed-",
                ".jpg"
        );

        Iterator<ImageWriter> writers =
                ImageIO.getImageWritersByFormatName("jpg");

        if (!writers.hasNext()) {
            throw new RuntimeException("No JPG writer found.");
        }

        ImageWriter writer = writers.next();

        FileOutputStream fos = new FileOutputStream(compressedFile);

        ImageOutputStream ios = ImageIO.createImageOutputStream(fos);

        writer.setOutput(ios);

        ImageWriteParam params = writer.getDefaultWriteParam();

        if (params.canWriteCompressed()) {
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(0.7f);
        }

        writer.write(
                null,
                new IIOImage(resizedImage, null, null),
                params
        );

        ios.close();
        fos.close();
        writer.dispose();

        return compressedFile;
    }

    private void loadQuestionToEditor(QuestionDraftRow question) {
        updatingEditor = true;

        if (question == null) {
            clearEditor();
            setEditorDisabled(true);
            updatingEditor = false;
            return;
        }

        setEditorDisabled(false);

        editorTitleLabel.setText("Question " + question.getQuestionNo());
        editorSubtitleLabel.setText("Edit the selected question.");

        questionTypeComboBox.setValue(question.getQuestionType());
        pointsField.setText(String.valueOf(question.getPoints()));
        questionTextArea.setText(question.getQuestionText());

        renderAnswerFields(question.getQuestionType());

        if (useImagesCheckBox != null) {
            useImagesCheckBox.setSelected(question.isUsesImages());
        }

        if (questionImagePathField != null) {
            questionImagePathField.setText(question.getQuestionImagePath());
        }

        if ("MULTIPLE_CHOICE".equals(question.getQuestionType())) {
            choiceAField.setText(question.getChoiceA());
            choiceBField.setText(question.getChoiceB());
            choiceCField.setText(question.getChoiceC());
            choiceDField.setText(question.getChoiceD());

            choiceAImagePathField.setText(question.getChoiceAImagePath());
            choiceBImagePathField.setText(question.getChoiceBImagePath());
            choiceCImagePathField.setText(question.getChoiceCImagePath());
            choiceDImagePathField.setText(question.getChoiceDImagePath());

            updateCorrectAnswerChoices();

            if (question.getCorrectChoiceIndex() >= 0) {
                correctChoiceComboBox.getSelectionModel().select(question.getCorrectChoiceIndex());
            } else {
                correctChoiceComboBox.getSelectionModel().clearSelection();
            }

        } else if ("TRUE_FALSE".equals(question.getQuestionType())) {
            trueFalseComboBox.setValue(question.getCorrectAnswer());

        } else if ("IDENTIFICATION".equals(question.getQuestionType())) {
            identificationAnswerField.setText(question.getCorrectAnswer());

        } else if ("ESSAY".equals(question.getQuestionType())) {
            essayGuideArea.setText(question.getCorrectAnswer());
        }

        updateQuestionStatus();
        validationLabel.setText("");

        updatingEditor = false;
    }

    private boolean saveEditorToSelectedQuestion(boolean showValidation) {
        if (selectedQuestion == null) return true;

        boolean usesImages = useImagesCheckBox != null && useImagesCheckBox.isSelected();
        selectedQuestion.setUsesImages(usesImages);

        if (!usesImages) {
            selectedQuestion.setQuestionImagePath("");
            selectedQuestion.setChoiceAImagePath("");
            selectedQuestion.setChoiceBImagePath("");
            selectedQuestion.setChoiceCImagePath("");
            selectedQuestion.setChoiceDImagePath("");
        }

        String type = questionTypeComboBox.getValue();

        if (isBlank(type)) {
            if (showValidation) validationLabel.setText("Question type is required.");
            return false;
        }

        int points;

        try {
            points = Integer.parseInt(pointsField.getText().trim());

            if (points <= 0) {
                if (showValidation) validationLabel.setText("Points must be greater than 0.");
                return false;
            }

        } catch (Exception e) {
            if (showValidation) validationLabel.setText("Points must be a valid number.");
            return false;
        }

        selectedQuestion.setQuestionType(type);
        selectedQuestion.setQuestionText(questionTextArea.getText() == null ? "" : questionTextArea.getText().trim());
        selectedQuestion.setQuestionImagePath(questionImagePathField == null ? "" : safeText(questionImagePathField));
        selectedQuestion.setPoints(points);

        selectedQuestion.setImageStatus(hasImage(selectedQuestion.getQuestionImagePath()) ? "Has image" : "No image");

        if ("MULTIPLE_CHOICE".equals(type)) {

            if (choiceAField != null) selectedQuestion.setChoiceA(safeText(choiceAField));
            if (choiceBField != null) selectedQuestion.setChoiceB(safeText(choiceBField));
            if (choiceCField != null) selectedQuestion.setChoiceC(safeText(choiceCField));
            if (choiceDField != null) selectedQuestion.setChoiceD(safeText(choiceDField));

            if (choiceAImagePathField != null) selectedQuestion.setChoiceAImagePath(safeText(choiceAImagePathField));
            if (choiceBImagePathField != null) selectedQuestion.setChoiceBImagePath(safeText(choiceBImagePathField));
            if (choiceCImagePathField != null) selectedQuestion.setChoiceCImagePath(safeText(choiceCImagePathField));
            if (choiceDImagePathField != null) selectedQuestion.setChoiceDImagePath(safeText(choiceDImagePathField));

            if (correctChoiceComboBox != null) {
                updateCorrectAnswerChoices();

                int selectedIndex = correctChoiceComboBox.getSelectionModel().getSelectedIndex();

                selectedQuestion.setCorrectChoiceIndex(selectedIndex);
                selectedQuestion.setCorrectAnswer(getChoiceAnswerBackup(selectedIndex));
            }

        } else if ("TRUE_FALSE".equals(type)) {

            if (trueFalseComboBox != null) {
                selectedQuestion.setCorrectAnswer(
                        trueFalseComboBox.getValue() == null ? "" : trueFalseComboBox.getValue()
                );
            }

        } else if ("IDENTIFICATION".equals(type)) {

            if (identificationAnswerField != null) {
                selectedQuestion.setCorrectAnswer(safeText(identificationAnswerField));
            }

        } else if ("ESSAY".equals(type)) {

            if (essayGuideArea != null) {
                selectedQuestion.setCorrectAnswer(essayGuideArea.getText() == null ? "" : essayGuideArea.getText().trim());
            }
        }

        if (showValidation && !isQuestionComplete(selectedQuestion)) {
            validationLabel.setText("Please complete the required question details.");
            updateQuestionStatus();
            questionListView.refresh();
            return false;
        }

        updateQuestionStatus();
        questionListView.refresh();
        return true;
    }

    private void updateSelectedQuestionLive() {
        if (updatingEditor || selectedQuestion == null) return;

        selectedQuestion.setQuestionText(questionTextArea.getText());

        try {
            selectedQuestion.setPoints(Integer.parseInt(pointsField.getText().trim()));
        } catch (Exception ignored) {
            // Keep the old valid points while the user is still typing.
        }

        questionListView.refresh();
        updateQuestionStatus();
    }

    private boolean isQuestionComplete(QuestionDraftRow question) {
        if (question == null) return false;

        if (isBlank(question.getQuestionText())) return false;
        if (question.getPoints() <= 0) return false;

        String type = question.getQuestionType();

        if ("MULTIPLE_CHOICE".equals(type)) {
            return hasChoiceContent(question.getChoiceA(), question.getChoiceAImagePath())
                    && hasChoiceContent(question.getChoiceB(), question.getChoiceBImagePath())
                    && hasChoiceContent(question.getChoiceC(), question.getChoiceCImagePath())
                    && hasChoiceContent(question.getChoiceD(), question.getChoiceDImagePath())
                    && question.getCorrectChoiceIndex() >= 0;
        }

        if ("TRUE_FALSE".equals(type)) {
            return !isBlank(question.getCorrectAnswer());
        }

        if ("IDENTIFICATION".equals(type)) {
            return !isBlank(question.getCorrectAnswer());
        }

        if ("ESSAY".equals(type)) {
            return true;
        }

        return false;
    }

    private boolean hasChoiceContent(String text, String imagePath) {
        return !isBlank(text) || !isBlank(imagePath);
    }

    private boolean hasImage(String imagePath) {
        return !isBlank(imagePath);
    }

    private void updateQuestionStatus() {
        if (selectedQuestion == null) {
            questionStatusBadge.setText("Not Started");
            questionStatusBadge.getStyleClass().removeAll("success-badge", "warning-badge");
            questionStatusBadge.getStyleClass().add("warning-badge");
            return;
        }

        boolean complete = isQuestionComplete(selectedQuestion);

        questionStatusBadge.setText(complete ? "Complete" : "Incomplete");
        questionStatusBadge.getStyleClass().removeAll("success-badge", "warning-badge");
        questionStatusBadge.getStyleClass().add(complete ? "success-badge" : "warning-badge");
    }

    private void updateQuestionCount() {
        if (questionCountLabel != null) {
            questionCountLabel.setText(questionRows.size() + " question(s) added");
        }
    }

    private void clearEditor() {
        editorTitleLabel.setText("Question Editor");
        editorSubtitleLabel.setText("Select a question or add a new one.");
        questionTextArea.clear();
        pointsField.clear();
        questionTypeComboBox.setValue(null);
        dynamicAnswerContainer.getChildren().clear();
        validationLabel.setText("");
        updateQuestionStatus();
    }

    private void setEditorDisabled(boolean disabled) {
        questionTypeComboBox.setDisable(disabled);
        pointsField.setDisable(disabled);
        questionTextArea.setDisable(disabled);
        dynamicAnswerContainer.setDisable(disabled);
    }

    private boolean validateStepOne() {
        clearAllErrors();

        boolean valid = true;

        if (isBlank(titleField.getText())) {
            setError(titleField, titleErrorLabel, "Exam title is required.");
            valid = false;
        }

        if (durationSpinner.getValue() == null || durationSpinner.getValue() <= 0) {
            setError(durationSpinner, durationErrorLabel, "Enter a valid duration.");
            valid = false;
        }

        if (startDatePicker.getValue() == null || startTimeCombo.getValue() == null) {
            setError(startDatePicker, startErrorLabel, "Start date and time are required.");
            setError(startTimeCombo, startErrorLabel, "Start date and time are required.");
            valid = false;
        }

        if (endDatePicker.getValue() == null || endTimeCombo.getValue() == null) {
            setError(endDatePicker, endErrorLabel, "End date and time are required.");
            setError(endTimeCombo, endErrorLabel, "End date and time are required.");
            valid = false;
        }

        if (startDatePicker.getValue() != null &&
                startTimeCombo.getValue() != null &&
                endDatePicker.getValue() != null &&
                endTimeCombo.getValue() != null &&
                durationSpinner.getValue() != null) {

            OffsetDateTime start = getStartDateTime();
            OffsetDateTime end = getEndDateTime();

            OffsetDateTime minimumStart = getNextAllowedStartTime();

            if (start.isBefore(minimumStart)) {
                setError(startDatePicker, startErrorLabel,
                        "Start schedule must be at least the next allowed hour.");
                setError(startTimeCombo, startErrorLabel,
                        "Start schedule must be at least the next allowed hour.");
                valid = false;
            }

            int durationMinutes = durationSpinner.getValue();
            OffsetDateTime minimumEnd = start.plusMinutes(durationMinutes);

            if (end.isBefore(minimumEnd)) {
                setError(endDatePicker, endErrorLabel,
                        "End schedule must be at least start time plus duration.");
                setError(endTimeCombo, endErrorLabel,
                        "End schedule must be at least start time plus duration.");
                valid = false;
            }
        }

        if (classOfferingListView.getSelectionModel().getSelectedItem() == null) {
            setError(classOfferingListView, classErrorLabel, "Please select a class offering.");
            valid = false;
        }

        if (examModeGroup.getSelectedToggle() == null) {
            setTextError(modeErrorLabel, "Please select an exam mode.");
            valid = false;
        }

        return valid;
    }

    private OffsetDateTime getStartDateTime() {
        return ZonedDateTime.of(
                startDatePicker.getValue(),
                LocalTime.parse(startTimeCombo.getValue()),
                ZoneId.systemDefault()
        ).toOffsetDateTime();
    }

    private OffsetDateTime getEndDateTime() {
        return ZonedDateTime.of(
                endDatePicker.getValue(),
                LocalTime.parse(endTimeCombo.getValue()),
                ZoneId.systemDefault()
        ).toOffsetDateTime();
    }

    private void prepareFinalReview() {
        saveEditorToSelectedQuestion(false);

        OffsetDateTime startDateTime = getStartDateTime();
        OffsetDateTime endDateTime = getEndDateTime();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        reviewTitleLabel.setText(titleField.getText().trim());
        reviewDurationLabel.setText(durationSpinner.getValue() + " minutes");

        reviewScheduleLabel.setText(
                startDateTime.format(formatter) + " - " + endDateTime.format(formatter)
        );

        List<ClassOffering> selectedClasses =
                classOfferingListView.getSelectionModel().getSelectedItems();

        if (selectedClasses == null || selectedClasses.isEmpty()) {
            reviewClassOfferingLabel.setText("No class selected");
        } else {
            String text = selectedClasses.stream()
                    .map(ClassOffering::getDisplayName)
                    .collect(Collectors.joining("\n"));

            reviewClassOfferingLabel.setText(text);
        }

        reviewQuestionCountLabel.setText(String.valueOf(questionRows.size()));
        reviewShuffleQuestionsLabel.setText(shuffleQuestionsCheck.isSelected() ? "Yes" : "No");
        reviewShuffleChoicesLabel.setText(shuffleChoicesCheck.isSelected() ? "Yes" : "No");

        renderReviewQuestions();
    }

    private ViolationRuleRequest buildViolationRule(
            CheckBox checkBox,
            ComboBox<String> severityCombo,
            Spinner<Integer> allowedSpinner
    ) {
        return new ViolationRuleRequest(
                checkBox != null && checkBox.isSelected(),
                severityCombo == null || severityCombo.getValue() == null
                        ? "MINOR"
                        : severityCombo.getValue(),
                allowedSpinner == null || allowedSpinner.getValue() == null
                        ? 0
                        : allowedSpinner.getValue()
        );
    }

    private ViolationSettingRequest buildViolationSetting(
            String violationType,
            CheckBox checkBox,
            ComboBox<String> severityCombo,
            Spinner<Integer> allowedSpinner
    ) {
        ViolationSettingRequest setting = new ViolationSettingRequest();

        setting.setViolationType(violationType);
        setting.setEnabled(checkBox != null && checkBox.isSelected());
        setting.setSeverity(
                severityCombo == null || severityCombo.getValue() == null
                        ? "MINOR"
                        : severityCombo.getValue()
        );
        setting.setMaxAllowedCount(
                allowedSpinner == null || allowedSpinner.getValue() == null
                        ? 0
                        : allowedSpinner.getValue()
        );

        return setting;
    }

    private List<ViolationSettingRequest> buildViolationSettings() {
        List<ViolationSettingRequest> settings = new ArrayList<>();

        settings.add(buildViolationSetting(
                "FOCUS_LOST",
                focusLostViolationCheck,
                focusLostSeverityCombo,
                focusLostLimitSpinner
        ));

        settings.add(buildViolationSetting(
                "FULLSCREEN_EXIT",
                fullscreenExitViolationCheck,
                fullscreenExitSeverityCombo,
                fullscreenExitLimitSpinner
        ));

        settings.add(buildViolationSetting(
                "WINDOW_MINIMIZED",
                windowMinimizeViolationCheck,
                windowMinimizeSeverityCombo,
                windowMinimizeLimitSpinner
        ));

        settings.add(buildViolationSetting(
                "RESTRICTED_KEY",
                restrictedKeysViolationCheck,
                restrictedKeysSeverityCombo,
                restrictedKeysLimitSpinner
        ));

        settings.add(buildViolationSetting(
                "RIGHT_CLICK",
                rightClickViolationCheck,
                rightClickSeverityCombo,
                rightClickLimitSpinner
        ));

        settings.add(buildViolationSetting(
                "MULTIPLE_MONITORS",
                multipleMonitorsViolationCheck,
                multipleMonitorsSeverityCombo,
                multipleMonitorsLimitSpinner
        ));

        return settings;
    }

    private void renderReviewQuestions() {
        reviewQuestionContainer.getChildren().clear();

        for (QuestionDraftRow question : questionRows) {
            VBox card = new VBox(10);
            card.getStyleClass().add("review-question-card");

            Label header = new Label(
                    "Q" + question.getQuestionNo()
                            + " • " + question.getQuestionType()
                            + " • " + question.getPoints() + " point(s)"
            );
            header.getStyleClass().add("review-question-header");

            Label questionText = new Label(question.getQuestionText());
            questionText.setWrapText(true);
            questionText.getStyleClass().add("review-question-text");

            card.getChildren().addAll(header, questionText);

            if (!isBlank(question.getQuestionImagePath())) {
                Label imageLabel = new Label("Question Image: " + getFileName(question.getQuestionImagePath()));
                imageLabel.getStyleClass().add("review-image-label");
                card.getChildren().add(imageLabel);
            }

            if ("MULTIPLE_CHOICE".equals(question.getQuestionType())) {
                card.getChildren().add(createReviewChoiceRow(question, 0, question.getChoiceA(), question.getChoiceAImagePath()));
                card.getChildren().add(createReviewChoiceRow(question, 1, question.getChoiceB(), question.getChoiceBImagePath()));
                card.getChildren().add(createReviewChoiceRow(question, 2, question.getChoiceC(), question.getChoiceCImagePath()));
                card.getChildren().add(createReviewChoiceRow(question, 3, question.getChoiceD(), question.getChoiceDImagePath()));
            } else {
                card.getChildren().add(createReviewAnswerLabel("Answer: " + question.getCorrectAnswer()));
            }

            reviewQuestionContainer.getChildren().add(card);
        }
    }

    private HBox createReviewChoiceRow(QuestionDraftRow question, int index, String text, String imagePath) {
        boolean isCorrect = question.getCorrectChoiceIndex() == index;

        Label marker = new Label(isCorrect ? "✓" : "");
        marker.getStyleClass().add(isCorrect ? "choice-correct-marker" : "choice-empty-marker");

        String displayText;

        if (!isBlank(text)) {
            displayText = "Choice " + (index + 1) + ": " + text;
        } else if (!isBlank(imagePath)) {
            displayText = "Choice " + (index + 1) + ": Image - " + getFileName(imagePath);
        } else {
            displayText = "Choice " + (index + 1) + ": No content";
        }

        Label choiceLabel = new Label(displayText);
        choiceLabel.setWrapText(true);
        choiceLabel.getStyleClass().add(isCorrect ? "review-choice-correct" : "review-choice");

        return new HBox(8, marker, choiceLabel);
    }

    private Label createReviewAnswerLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("review-answer-label");
        return label;
    }

    private String getFileName(String path) {
        if (isBlank(path)) return "";

        try {
            return new File(path).getName();
        } catch (Exception e) {
            return path;
        }
    }

    private void showStep(int step) {
        currentStep = step;

        stepOnePane.setVisible(step == 1);
        stepOnePane.setManaged(step == 1);

        stepTwoPane.setVisible(step == 2);
        stepTwoPane.setManaged(step == 2);

        stepThreePane.setVisible(step == 3);
        stepThreePane.setManaged(step == 3);

        stepFourPane.setVisible(step == 4);
        stepFourPane.setManaged(step == 4);

        if (stepChipLabel != null) {
            stepChipLabel.setText("Step " + step + " of 4");
        }

        backButton.setDisable(step == 1);

        nextButton.setVisible(step < 4);
        nextButton.setManaged(step < 4);

        draftButton.setVisible(step == 4);
        draftButton.setManaged(step == 4);

        publishButton.setVisible(step == 4);
        publishButton.setManaged(step == 4);

        updateFooterButtons();
    }

    private void renumberQuestions() {
        for (int i = 0; i < questionRows.size(); i++) {
            questionRows.get(i).setQuestionNo(i + 1);
        }

        questionListView.refresh();
    }

    private void updateFooterButtons() {
        if (wizardMode == WizardMode.VIEW) {
            backButton.setText("Close");

            backButton.setVisible(true);
            backButton.setManaged(true);

            nextButton.setText("Edit");
            nextButton.setVisible(examEditable);
            nextButton.setManaged(examEditable);

            draftButton.setVisible(false);
            draftButton.setManaged(false);

            publishButton.setVisible(false);
            publishButton.setManaged(false);
            return;
        }

        backButton.setText("Back");
        nextButton.setText("Next");

        backButton.setDisable(currentStep == 1);

        nextButton.setVisible(currentStep < 4);
        nextButton.setManaged(currentStep < 4);

        draftButton.setVisible(currentStep == 4);
        draftButton.setManaged(currentStep == 4);

        publishButton.setVisible(currentStep == 4);
        publishButton.setManaged(currentStep == 4);

        if (wizardMode == WizardMode.EDIT) {
            draftButton.setText("Save Changes");
            publishButton.setText("Publish Changes");
        } else {
            draftButton.setText("Save as Draft");
            publishButton.setText("Publish");
        }
    }

    private void populateTimeComboBox(ComboBox<String> comboBox) {
        List<String> times = new ArrayList<>();

        for (int hour = 0; hour < 24; hour++) {
            for (int min : new int[]{0, 30}) {
                times.add(String.format("%02d:%02d", hour, min));
            }
        }

        comboBox.setItems(FXCollections.observableArrayList(times));
    }

    private void clearAllErrors() {
        clearError(titleField, titleErrorLabel);
        clearError(durationSpinner, durationErrorLabel);

        clearError(startDatePicker, startErrorLabel);
        clearError(startTimeCombo, startErrorLabel);

        clearError(endDatePicker, endErrorLabel);
        clearError(endTimeCombo, endErrorLabel);

        clearError(classOfferingListView, classErrorLabel);

        clearTextError(modeErrorLabel);
        clearTextError(sourceErrorLabel);

        if (stepOneMessageLabel != null) {
            stepOneMessageLabel.setText("");
        }
    }

    private void setError(Control field, Label errorLabel, String message) {
        if (!field.getStyleClass().contains("input-error")) {
            field.getStyleClass().add("input-error");
        }

        if (errorLabel != null) {
            errorLabel.setText(message);
        }
    }

    private void clearError(Control field, Label errorLabel) {
        field.getStyleClass().remove("input-error");

        if (errorLabel != null) {
            errorLabel.setText("");
        }
    }

    private void setTextError(Label label, String message) {
        if (label != null) {
            label.setText(message);
        }
    }

    private void clearTextError(Label label) {
        if (label != null) {
            label.setText("");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safeText(TextInputControl field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    @Override
    public void setShellController(DashboardShellController shellController) {
        this.shellController = shellController;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    public static class QuestionDraftRow {
        private int questionNo;
        private String questionText = "";
        private String questionImagePath = "";
        private String questionType = "MULTIPLE_CHOICE";
        private String imageStatus = "No image";
        private String violationStatus = "Default";
        private int points = 1;

        private String choiceA = "";
        private String choiceB = "";
        private String choiceC = "";
        private String choiceD = "";

        private String choiceAImagePath = "";
        private String choiceBImagePath = "";
        private String choiceCImagePath = "";
        private String choiceDImagePath = "";

        private String correctAnswer = "";
        private int correctChoiceIndex = -1;
        private boolean usesImages;

        public QuestionDraftRow() {
        }

        public QuestionDraftRow(int questionNo, String questionText, String questionType,
                                String imageStatus, String violationStatus) {
            this.questionNo = questionNo;
            this.questionText = questionText;
            this.questionType = questionType;
            this.imageStatus = imageStatus;
            this.violationStatus = violationStatus;
        }

        public int getQuestionNo() {
            return questionNo;
        }

        public void setQuestionNo(int questionNo) {
            this.questionNo = questionNo;
        }

        public boolean isUsesImages() {
            return usesImages;
        }

        public void setUsesImages(boolean usesImages) {
            this.usesImages = usesImages;
        }

        public String getQuestionText() {
            return questionText;
        }

        public void setQuestionText(String questionText) {
            this.questionText = questionText == null ? "" : questionText;
        }

        public String getQuestionImagePath() {
            return questionImagePath;
        }

        public void setQuestionImagePath(String questionImagePath) {
            this.questionImagePath = questionImagePath == null ? "" : questionImagePath;
        }

        public String getQuestionType() {
            return questionType;
        }

        public void setQuestionType(String questionType) {
            this.questionType = questionType == null ? "MULTIPLE_CHOICE" : questionType;
        }

        public String getImageStatus() {
            return imageStatus;
        }

        public void setImageStatus(String imageStatus) {
            this.imageStatus = imageStatus == null ? "No image" : imageStatus;
        }

        public String getViolationStatus() {
            return violationStatus;
        }

        public void setViolationStatus(String violationStatus) {
            this.violationStatus = violationStatus == null ? "Default" : violationStatus;
        }

        public int getPoints() {
            return points;
        }

        public void setPoints(int points) {
            this.points = points;
        }

        public String getChoiceA() {
            return choiceA;
        }

        public void setChoiceA(String choiceA) {
            this.choiceA = choiceA == null ? "" : choiceA;
        }

        public String getChoiceB() {
            return choiceB;
        }

        public void setChoiceB(String choiceB) {
            this.choiceB = choiceB == null ? "" : choiceB;
        }

        public String getChoiceC() {
            return choiceC;
        }

        public void setChoiceC(String choiceC) {
            this.choiceC = choiceC == null ? "" : choiceC;
        }

        public String getChoiceD() {
            return choiceD;
        }

        public void setChoiceD(String choiceD) {
            this.choiceD = choiceD == null ? "" : choiceD;
        }

        public String getChoiceAImagePath() {
            return choiceAImagePath;
        }

        public void setChoiceAImagePath(String choiceAImagePath) {
            this.choiceAImagePath = choiceAImagePath == null ? "" : choiceAImagePath;
        }

        public String getChoiceBImagePath() {
            return choiceBImagePath;
        }

        public void setChoiceBImagePath(String choiceBImagePath) {
            this.choiceBImagePath = choiceBImagePath == null ? "" : choiceBImagePath;
        }

        public String getChoiceCImagePath() {
            return choiceCImagePath;
        }

        public void setChoiceCImagePath(String choiceCImagePath) {
            this.choiceCImagePath = choiceCImagePath == null ? "" : choiceCImagePath;
        }

        public String getChoiceDImagePath() {
            return choiceDImagePath;
        }

        public void setChoiceDImagePath(String choiceDImagePath) {
            this.choiceDImagePath = choiceDImagePath == null ? "" : choiceDImagePath;
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        public void setCorrectAnswer(String correctAnswer) {
            this.correctAnswer = correctAnswer == null ? "" : correctAnswer;
        }

        public int getCorrectChoiceIndex() {
            return correctChoiceIndex;
        }

        public void setCorrectChoiceIndex(int correctChoiceIndex) {
            this.correctChoiceIndex = correctChoiceIndex;
        }
    }

    private void toggleChoiceImages(boolean show) {
        if (choiceAImageBox != null) {
            choiceAImageBox.setVisible(show);
            choiceAImageBox.setManaged(show);
        }

        if (choiceBImageBox != null) {
            choiceBImageBox.setVisible(show);
            choiceBImageBox.setManaged(show);
        }

        if (choiceCImageBox != null) {
            choiceCImageBox.setVisible(show);
            choiceCImageBox.setManaged(show);
        }

        if (choiceDImageBox != null) {
            choiceDImageBox.setVisible(show);
            choiceDImageBox.setManaged(show);
        }
    }

    private void setParentVisibility(TextField field, boolean show) {
        if (field == null || field.getParent() == null) return;

        field.getParent().setVisible(show);
        field.getParent().setManaged(show);
    }

    private String getChoiceDisplayLabel(int index, String text, String imagePath) {
        String choiceNumber = "Choice " + (index + 1);

        if (!isBlank(text)) {
            return choiceNumber + " • " + text;
        }

        if (!isBlank(imagePath)) {
            File file = new File(imagePath);
            return choiceNumber + " • Image: " + file.getName();
        }

        return choiceNumber + " • No content yet";
    }

    private void updateCorrectAnswerChoices() {
        if (correctChoiceComboBox == null) return;

        int previousIndex = correctChoiceComboBox.getSelectionModel().getSelectedIndex();

        correctChoiceComboBox.setItems(FXCollections.observableArrayList(
                getChoiceDisplayLabel(0, safeText(choiceAField), safeText(choiceAImagePathField)),
                getChoiceDisplayLabel(1, safeText(choiceBField), safeText(choiceBImagePathField)),
                getChoiceDisplayLabel(2, safeText(choiceCField), safeText(choiceCImagePathField)),
                getChoiceDisplayLabel(3, safeText(choiceDField), safeText(choiceDImagePathField))
        ));

        if (previousIndex >= 0 && previousIndex < 4) {
            correctChoiceComboBox.getSelectionModel().select(previousIndex);
        }
    }

    private String getChoiceAnswerBackup(int index) {
        if (index == 0) return getChoiceBackupValue(choiceAField, choiceAImagePathField);
        if (index == 1) return getChoiceBackupValue(choiceBField, choiceBImagePathField);
        if (index == 2) return getChoiceBackupValue(choiceCField, choiceCImagePathField);
        if (index == 3) return getChoiceBackupValue(choiceDField, choiceDImagePathField);

        return "";
    }

    private String getChoiceBackupValue(TextField textField, TextField imagePathField) {
        String text = safeText(textField);

        if (!isBlank(text)) {
            return text;
        }

        String imagePath = safeText(imagePathField);

        if (!isBlank(imagePath)) {
            return imagePath;
        }

        return "";
    }

    private void setupViolationDefaults() {
        setSelectedIfPresent(focusLostViolationCheck, true);
        setSelectedIfPresent(fullscreenExitViolationCheck, true);
        setSelectedIfPresent(windowMinimizeViolationCheck, true);
        setSelectedIfPresent(restrictedKeysViolationCheck, true);
        setSelectedIfPresent(rightClickViolationCheck, true);
        setSelectedIfPresent(multipleMonitorsViolationCheck, true);

        setupSeverityCombo(focusLostSeverityCombo, "MAJOR");
        setupSeverityCombo(fullscreenExitSeverityCombo, "MAJOR");
        setupSeverityCombo(windowMinimizeSeverityCombo, "MAJOR");
        setupSeverityCombo(restrictedKeysSeverityCombo, "MINOR");
        setupSeverityCombo(rightClickSeverityCombo, "MINOR");
        setupSeverityCombo(multipleMonitorsSeverityCombo, "MAJOR");

        setupAllowedSpinner(focusLostLimitSpinner, 0);
        setupAllowedSpinner(fullscreenExitLimitSpinner, 0);
        setupAllowedSpinner(windowMinimizeLimitSpinner, 0);
        setupAllowedSpinner(restrictedKeysLimitSpinner, 0);
        setupAllowedSpinner(rightClickLimitSpinner, 0);
        setupAllowedSpinner(multipleMonitorsLimitSpinner, 0);

        setupAllowedSpinner(warningThresholdSpinner, 0);
        setupAllowedSpinner(majorThresholdSpinner, 0);

        if (autoSubmitThresholdSpinner != null) {
            autoSubmitThresholdSpinner.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 20, 0)
            );
        }
    }

    private List<QuestionRequest> mapQuestions() {

        List<QuestionRequest> requests = new ArrayList<>();

        for (QuestionDraftRow q : questionRows) {

            QuestionRequest req = new QuestionRequest();

            req.setQuestionText(q.getQuestionText());
            req.setQuestionImageUrl(q.getQuestionImagePath());
            req.setQuestionType(q.getQuestionType());
            req.setPoints(q.getPoints());

            if ("MULTIPLE_CHOICE".equalsIgnoreCase(q.getQuestionType())) {

                List<ChoiceRequest> choices = new ArrayList<>();

                ChoiceRequest choiceA = new ChoiceRequest(q.getChoiceA(), q.getCorrectChoiceIndex() == 0);
                choiceA.setChoiceImageUrl(q.getChoiceAImagePath());

                ChoiceRequest choiceB = new ChoiceRequest(q.getChoiceB(), q.getCorrectChoiceIndex() == 1);
                choiceB.setChoiceImageUrl(q.getChoiceBImagePath());

                ChoiceRequest choiceC = new ChoiceRequest(q.getChoiceC(), q.getCorrectChoiceIndex() == 2);
                choiceC.setChoiceImageUrl(q.getChoiceCImagePath());

                ChoiceRequest choiceD = new ChoiceRequest(q.getChoiceD(), q.getCorrectChoiceIndex() == 3);
                choiceD.setChoiceImageUrl(q.getChoiceDImagePath());

                choices.add(choiceA);
                choices.add(choiceB);
                choices.add(choiceC);
                choices.add(choiceD);

                req.setChoices(choices);

            } else {
                req.setCorrectAnswer(q.getCorrectAnswer());
            }

            requests.add(req);
        }

        return requests;
    }

    private void setSelectedIfPresent(CheckBox checkBox, boolean selected) {
        if (checkBox != null) {
            checkBox.setSelected(selected);
        }
    }

    private void setupSeverityCombo(ComboBox<String> comboBox, String defaultValue) {
        if (comboBox == null) return;

        comboBox.setItems(FXCollections.observableArrayList(
                "INFO",
                "MINOR",
                "MAJOR",
                "CRITICAL"
        ));

        comboBox.setValue(defaultValue);
    }

    private void setupAllowedSpinner(Spinner<Integer> spinner, int defaultValue) {
        if (spinner == null) return;

        spinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        0,
                        20,
                        defaultValue
                )
        );
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Something went wrong");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeWizardAndReturnToTable() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();

        if (shellController != null) {
            shellController.loadContent("/fxml/exam/exam-management.fxml");
        }

        if (onCancel != null) {
            onCancel.run();
        }
    }
}
