package com.transcriber;

import com.transcriber.audio.AudioRecorder;
import com.transcriber.cloud.GCloudTranscriber;
import com.transcriber.file.FileManager;
import com.transcriber.template.TemplateManager;
import com.transcriber.text.TranscriptionCleaner;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main JavaFX GUI application for the Medical Transcriber.
 */
public class TranscriberApp extends javafx.application.Application {
    
    private AudioRecorder recorder;
    private Path currentRecording;
    private Path currentTranscriptionFile;
    private Map<String, Path> templates;
    private Thread transcribeThread;
    
    // UI Components
    private TextField patientField;
    private TextField dobField;
    private ComboBox<String> templateCombo;
    private Label statusLabel;
    private Button recordButton;
    private Button stopButton;
    private Button sendToGoogleButton;
    private Button deleteRecordingButton;
    private Button saveButton;
    private Button cleanButton;
    private Button deleteTranscriptionButton;
    private ListView<String> fileListView;
    private TextArea textEditor;
    
    @Override
    public void start(Stage primaryStage) {
        recorder = new AudioRecorder();
        templates = TemplateManager.loadTemplates();
        
        primaryStage.setTitle("Medical Transcriber");
        primaryStage.setWidth(1100);
        primaryStage.setHeight(700);
        

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
    
        // Top panel with controls
        GridPane topPanel = createTopPanel();
        root.setTop(topPanel);
        BorderPane.setMargin(topPanel, new Insets(0, 0, 10, 0));
    
        // Body with file list and editor
        Pane bodyPanel = createBodyPanel();
        root.setCenter(bodyPanel); // Removed unnecessary setPrefSize
    
        Scene scene = new Scene(root, 1200, 800); // Added initial size for better layout
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Transcription Manager");
        primaryStage.show();
    
        // Refresh file list after window is shown
        Platform.runLater(this::refreshFileList);
    }
    
    private GridPane createTopPanel() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
    
        // Define column constraints for predictable resizing
        ColumnConstraints colLabel = new ColumnConstraints();
        colLabel.setHgrow(Priority.NEVER); // Labels stay fixed
    
        ColumnConstraints colPatientField = new ColumnConstraints();
        colPatientField.setHgrow(Priority.ALWAYS); // Patient Name expands
        colPatientField.setMinWidth(150); // Prevent collapsing too much
    
        ColumnConstraints colFixed = new ColumnConstraints();
        colFixed.setHgrow(Priority.NEVER); // DOB, Template, Status fixed
    
        // Apply constraints: 7 columns total
        grid.getColumnConstraints().addAll(
            colLabel, colPatientField, colFixed, colFixed, colFixed, colFixed, colFixed
        );
    
        // Patient Name
        grid.add(new Label("Patient Name:"), 0, 0);
        patientField = new TextField();
        patientField.setPrefWidth(200);
        grid.add(patientField, 1, 0);
    
        // DOB
        grid.add(new Label("DOB (MM/DD/YYYY):"), 2, 0);
        dobField = new TextField();
        dobField.setPrefWidth(120);
        grid.add(dobField, 3, 0);
    
        // Template
        grid.add(new Label("Template:"), 4, 0);
        templateCombo = new ComboBox<>();
        templateCombo.getItems().addAll(templates.keySet());
        templateCombo.getSelectionModel().selectFirst();
        templateCombo.setPrefWidth(150);
        grid.add(templateCombo, 5, 0);
    
        // Status
        statusLabel = new Label("Idle");
        statusLabel.setMinWidth(80); // Reserve space for status text
        statusLabel.getStyleClass().add("status");
        grid.add(statusLabel, 6, 0);
    
        // Buttons row
        HBox buttonBox = new HBox(5);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
    
        recordButton = new Button("Record");
        recordButton.setOnAction(e -> startRecord());
    
        stopButton = new Button("Stop");
        stopButton.setOnAction(e -> stopRecord());
    
        sendToGoogleButton = new Button("Send to Google");
        sendToGoogleButton.setOnAction(e -> triggerTranscription());
    
        deleteRecordingButton = new Button("Delete Recording");
        deleteRecordingButton.setOnAction(e -> deleteRecording());
    
        saveButton = new Button("Save");
        saveButton.setOnAction(e -> saveCurrentTranscription());
    
        cleanButton = new Button("Clean Transcription");
        cleanButton.setOnAction(e -> cleanTranscription());
    
        deleteTranscriptionButton = new Button("Delete Transcription");
        deleteTranscriptionButton.setOnAction(e -> deleteTranscription());
    
        buttonBox.getChildren().addAll(recordButton, stopButton, sendToGoogleButton,
                deleteRecordingButton, saveButton, cleanButton, deleteTranscriptionButton);
    
        // Make button row span all columns
        grid.add(buttonBox, 0, 1, 7, 1);
    
        return grid;
    }    
    private Pane createBodyPanel() {
        BorderPane body = new BorderPane();
    
        // Left panel - File list
        VBox leftPanel = new VBox(8);
        leftPanel.setPadding(new Insets(8));
        leftPanel.setSpacing(8);
        leftPanel.setMinWidth(300);
        leftPanel.setPrefWidth(350);
        leftPanel.setMaxWidth(350);
        
        Label leftTitle = new Label("Transcriptions");
    
        fileListView = new ListView<>();
        VBox.setVgrow(fileListView, Priority.ALWAYS);
    
        fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadSelectedFile(newVal);
            }
        });
    
        leftPanel.getChildren().addAll(leftTitle, fileListView);
    
        // Center panel - Text editor
        VBox centerPanel = new VBox(8);
        centerPanel.setPadding(new Insets(8));
        centerPanel.setSpacing(8);
        centerPanel.setMinSize(400, 200);
    
        Label editorLabel = new Label("Transcription Editor");
    
        textEditor = new TextArea();
        textEditor.setWrapText(true);
        textEditor.setPrefRowCount(28);
        textEditor.setPrefColumnCount(100);
        VBox.setVgrow(textEditor, Priority.ALWAYS);
    
        centerPanel.getChildren().addAll(editorLabel, textEditor);
    
        // Wrap center panel in ScrollPane for better responsiveness
        ScrollPane scrollCenter = new ScrollPane(centerPanel);
        scrollCenter.setFitToWidth(true);
        scrollCenter.setFitToHeight(true);
    
        // Add panels to BorderPane
        body.setLeft(leftPanel);
        body.setCenter(scrollCenter);
        BorderPane.setMargin(leftPanel, new Insets(0, 10, 0, 0));
    
        return body;
    }
            
    private void setStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }
    
    private void startRecord() {
        try {
            currentRecording = recorder.start();
            setStatus("Recording → " + currentRecording.getFileName());
        } catch (Exception e) {
            showError("Recording Error", e.getMessage());
        }
    }
    
    private void stopRecord() {
        Path recording = recorder.stop();
        if (recording != null) {
            setStatus("Recording stopped");
        }
    }
    
    private void deleteRecording() {
        if (currentRecording == null) {
            return;
        }
        FileManager.secureDelete(currentRecording, patientField.getText());
        currentRecording = null;
        setStatus("Recording deleted");
    }
    
    private void triggerTranscription() {
        if (transcribeThread != null && transcribeThread.isAlive()) {
            showInfo("In Progress", "Transcription already running.");
            return;
        }
        if (currentRecording == null || !currentRecording.toFile().exists()) {
            showError("No recording", "Please record audio first.");
            return;
        }
        String patient = patientField.getText().trim();
        if (patient.isEmpty()) {
            showError("Missing info", "Patient name is required before transcription.");
            return;
        }
        String dob = dobField.getText().trim();
        if (dob.isEmpty()) {
            showError("Missing info", "DOB is required before transcription.");
            return;
        }
        currentTranscriptionFile = null; // prepare for new transcription result
        
        Path recordingToProcess = currentRecording;
        transcribeThread = new Thread(() -> runTranscription(recordingToProcess, patient, dob), "TranscriptionThread");
        transcribeThread.setDaemon(true);
        transcribeThread.start();
    }
    
    private void runTranscription(Path recording, String patient, String dob) {
        try {
            String transcript = GCloudTranscriber.uploadAndTranscribe(recording, patient, this::setStatus);
            
            String templateName = templateCombo.getSelectionModel().getSelectedItem();
            Path templatePath = templates.get(templateName);
            
            Map<String, String> context = new HashMap<>();
            context.put("PATIENT", patient);
            context.put("DOB", dob);
            
            String finalText;
            if (templatePath != null) {
                finalText = TemplateManager.applyTemplate(templatePath, transcript, context);
            } else {
                finalText = transcript;
            }
            
            Path savedPath = saveNewTranscription(finalText, patient, dob);
            if (recording != null) {
                FileManager.secureDelete(recording, patient);
            }
            currentRecording = null;
            
            Platform.runLater(() -> {
                textEditor.clear();
                textEditor.appendText(finalText);
                currentTranscriptionFile = savedPath;
                refreshFileList(savedPath.getFileName().toString());
                setStatus("Transcription saved to " + savedPath.getFileName());
            });
            
        } catch (Exception e) {
            Platform.runLater(() -> {
                showError("Transcription Error", e.getMessage());
                setStatus("Transcription failed");
            });
        }
    }
    
    private void saveCurrentTranscription() {
        String content = textEditor.getText().trim();
        if (content.isEmpty()) {
            showError("Empty", "Transcription text is empty.");
            return;
        }
        
        // If a file is currently loaded, overwrite it
        if (currentTranscriptionFile != null) {
            try {
                FileManager.saveTranscription(currentTranscriptionFile, content);
                refreshFileList(currentTranscriptionFile.getFileName().toString());
                setStatus("Saved " + currentTranscriptionFile.getFileName());
            } catch (Exception e) {
                showError("Save Error", e.getMessage());
            }
            return;
        }
        
        String patient = patientField.getText().trim();
        String dob = dobField.getText().trim();
        if (patient.isEmpty() || dob.isEmpty()) {
            showError("Missing info", "Patient name and DOB are required.");
            return;
        }
        
        try {
            Path path = saveNewTranscription(content, patient, dob);
            currentTranscriptionFile = path;
            refreshFileList(path.getFileName().toString());
            setStatus("Saved " + path.getFileName());
            
            // Securely delete recording post-save (if any remains)
            if (currentRecording != null) {
                FileManager.secureDelete(currentRecording, patient);
                currentRecording = null;
            }
        } catch (Exception e) {
            showError("Save Error", e.getMessage());
        }
    }

    private Path saveNewTranscription(String content, String patient, String dob) throws IOException {
        Path path = FileManager.generateFilename(patient, dob);
        FileManager.saveTranscription(path, content);
        return path;
    }
    
    private void refreshFileList() {
        refreshFileList(null);
    }

    private void refreshFileList(String fileToSelect) {
        Platform.runLater(() -> {
            fileListView.getItems().clear();
            List<Path> files = FileManager.listTranscriptions();
            for (Path file : files) {
                fileListView.getItems().add(file.getFileName().toString());
            }
            if (fileToSelect != null) {
                selectFileInListInternal(fileToSelect);
            }
        });
    }

    private void selectFileInListInternal(String fileName) {
        ObservableList<String> items = fileListView.getItems();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).equals(fileName)) {
                fileListView.getSelectionModel().select(i);
                fileListView.scrollTo(i);
                break;
            }
        }
    }
    
    private void loadSelectedFile(String fileName) {
        List<Path> files = FileManager.listTranscriptions();
        for (Path file : files) {
            if (file.getFileName().toString().equals(fileName)) {
                try {
                    String content = FileManager.loadTranscription(file);
                    textEditor.clear();
                    textEditor.appendText(content);
                    currentTranscriptionFile = file;
                    setStatus("Loaded " + fileName);
                } catch (Exception e) {
                    showError("Load Error", e.getMessage());
                }
                break;
            }
        }
    }
    
    private void cleanTranscription() {
        String content = textEditor.getText();
        if (content.trim().isEmpty()) {
            showInfo("Empty", "No transcription text to clean.");
            return;
        }
        
        String cleaned = TranscriptionCleaner.removeFillerWords(content);
        textEditor.clear();
        textEditor.appendText(cleaned);
        setStatus("Transcription cleaned");
    }
    
    private void deleteTranscription() {
        if (currentTranscriptionFile == null) {
            showInfo("No file", "No transcription file is currently loaded.");
            return;
        }
        
        String filename = currentTranscriptionFile.getFileName().toString();
        if (!showConfirm("Confirm Deletion", 
                "Are you sure you want to permanently delete '" + filename + "'?\n\n" +
                "This action cannot be undone.")) {
            return;
        }
        
        String patient = patientField.getText().trim();
        if (patient.isEmpty()) {
            patient = "unknown";
        }
        FileManager.secureDelete(currentTranscriptionFile, patient);
        currentTranscriptionFile = null;
        
        textEditor.clear();
        refreshFileList();
        setStatus("Deleted " + filename);
    }
    
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    private boolean showConfirm(String title, String message) {
        // This method is called from JavaFX event handlers, so we're already on the JavaFX thread
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}

