package com.optical.sender.ui;

import com.optical.sender.qr.QRCodeGenerator;
import com.optical.sender.service.TransferService;
import com.optical.sender.util.TransferConfig;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Controller for the Sender UI.
 * Manages file selection, transfer preparation, QR code display, and progress tracking.
 */
public class SenderController {

    private static final Logger log = LoggerFactory.getLogger(SenderController.class);

    private final Stage stage;
    private final BorderPane root;

    // UI Components
    private final Label fileNameLabel;
    private final Label fileSizeLabel;
    private final Label statusLabel;
    private final Label chunkLabel;
    private final Label estimatedTimeLabel;
    private final Label fpsLabel;
    private final ProgressBar progressBar;
    private final ImageView qrImageView;
    private final Button selectFileButton;
    private final Button startButton;
    private final Button pauseButton;
    private final Button stopButton;
    private final Slider fpsSlider;

    // State
    private TransferService transferService;
    private QRCodeGenerator qrGenerator;
    private Timeline displayTimeline;
    private int currentPacketIndex = 0;
    private int currentCycleChunk = 0;
    private boolean isPaused = false;
    private long transferStartTime;
    private File selectedFile;

    public SenderController(Stage stage) {
        this.stage = stage;
        this.qrGenerator = new QRCodeGenerator();

        // --- Header Section ---
        Label titleLabel = new Label("OPTICAL FILE TRANSFER");
        titleLabel.getStyleClass().add("app-title");

        Label subtitleLabel = new Label("S E N D E R");
        subtitleLabel.getStyleClass().add("app-subtitle");

        VBox headerBox = new VBox(4, titleLabel, subtitleLabel);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(20, 0, 10, 0));

        // --- File Selection Section ---
        selectFileButton = new Button("📁  Select MP3 File");
        selectFileButton.getStyleClass().add("primary-button");
        selectFileButton.setOnAction(e -> selectFile());

        fileNameLabel = new Label("No file selected");
        fileNameLabel.getStyleClass().add("info-label");

        fileSizeLabel = new Label("");
        fileSizeLabel.getStyleClass().add("info-label-dim");

        HBox fileInfoBox = new HBox(15, fileNameLabel, fileSizeLabel);
        fileInfoBox.setAlignment(Pos.CENTER_LEFT);

        VBox fileSection = new VBox(10, selectFileButton, fileInfoBox);
        fileSection.getStyleClass().add("section-box");

        // --- Control Section ---
        startButton = new Button("▶  Start Transfer");
        startButton.getStyleClass().addAll("primary-button", "start-button");
        startButton.setDisable(true);
        startButton.setOnAction(e -> startTransfer());

        pauseButton = new Button("⏸  Pause");
        pauseButton.getStyleClass().add("control-button");
        pauseButton.setDisable(true);
        pauseButton.setOnAction(e -> togglePause());

        stopButton = new Button("⏹  Stop");
        stopButton.getStyleClass().addAll("control-button", "stop-button");
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> stopTransfer());

        // FPS Slider
        Label speedLabel = new Label("Speed:");
        speedLabel.getStyleClass().add("info-label");

        fpsSlider = new Slider(TransferConfig.MIN_FPS, TransferConfig.MAX_FPS, TransferConfig.DEFAULT_FPS);
        fpsSlider.setShowTickLabels(true);
        fpsSlider.setShowTickMarks(true);
        fpsSlider.setMajorTickUnit(2);
        fpsSlider.setMinorTickCount(1);
        fpsSlider.setSnapToTicks(true);
        fpsSlider.setPrefWidth(200);
        fpsSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateFpsLabel(newVal.intValue());
            if (displayTimeline != null && !isPaused) {
                restartTimeline(newVal.intValue());
            }
        });

        fpsLabel = new Label(TransferConfig.DEFAULT_FPS + " QR/sec");
        fpsLabel.getStyleClass().add("fps-label");

        HBox speedBox = new HBox(10, speedLabel, fpsSlider, fpsLabel);
        speedBox.setAlignment(Pos.CENTER_LEFT);

        HBox controlBox = new HBox(12, startButton, pauseButton, stopButton);
        controlBox.setAlignment(Pos.CENTER);

        VBox controlSection = new VBox(12, controlBox, speedBox);
        controlSection.getStyleClass().add("section-box");

        // --- QR Display Section ---
        qrImageView = new ImageView();
        qrImageView.setFitWidth(TransferConfig.QR_SIZE);
        qrImageView.setFitHeight(TransferConfig.QR_SIZE);
        qrImageView.setPreserveRatio(true);
        qrImageView.getStyleClass().add("qr-image");

        StackPane qrContainer = new StackPane(qrImageView);
        qrContainer.getStyleClass().add("qr-container");
        qrContainer.setMinSize(TransferConfig.QR_SIZE + 20, TransferConfig.QR_SIZE + 20);
        qrContainer.setMaxSize(TransferConfig.QR_SIZE + 20, TransferConfig.QR_SIZE + 20);

        // --- Progress Section ---
        statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("status-label");

        chunkLabel = new Label("Chunk: — / —");
        chunkLabel.getStyleClass().add("info-label");

        estimatedTimeLabel = new Label("");
        estimatedTimeLabel.getStyleClass().add("info-label-dim");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("transfer-progress");

        VBox progressSection = new VBox(8, statusLabel, progressBar, chunkLabel, estimatedTimeLabel);
        progressSection.getStyleClass().add("section-box");

        // --- Main Layout ---
        VBox centerContent = new VBox(15, fileSection, controlSection, qrContainer, progressSection);
        centerContent.setAlignment(Pos.TOP_CENTER);
        centerContent.setPadding(new Insets(10, 30, 30, 30));

        ScrollPane scrollPane = new ScrollPane(centerContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("main-scroll");

        root = new BorderPane();
        root.setTop(headerBox);
        root.setCenter(scrollPane);
        root.getStyleClass().add("root-pane");
    }

    public BorderPane getRoot() {
        return root;
    }

    private void selectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Transfer");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("MP3 Files", "*.mp3"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            selectedFile = file;
            fileNameLabel.setText(file.getName());
            fileSizeLabel.setText(formatFileSize(file.length()));
            startButton.setDisable(false);
            statusLabel.setText("File selected — ready to transfer");
            log.info("Selected file: {} ({})", file.getName(), formatFileSize(file.length()));
        }
    }

    private void startTransfer() {
        if (selectedFile == null) return;

        statusLabel.setText("Preparing transfer...");
        startButton.setDisable(true);
        selectFileButton.setDisable(true);

        // Prepare transfer on background thread
        Thread prepThread = new Thread(() -> {
            try {
                transferService = new TransferService();
                transferService.prepareTransfer(selectedFile);

                Platform.runLater(() -> {
                    statusLabel.setText("Transmitting...");
                    pauseButton.setDisable(false);
                    stopButton.setDisable(false);
                    currentPacketIndex = 0;
                    currentCycleChunk = 0;
                    transferStartTime = System.currentTimeMillis();

                    // Start QR display timeline
                    int fps = (int) fpsSlider.getValue();
                    startTimeline(fps);

                    log.info("Transfer started: {} chunks at {} QR/sec",
                            transferService.getTotalChunks(), fps);
                });

            } catch (Exception e) {
                log.error("Failed to prepare transfer", e);
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    startButton.setDisable(false);
                    selectFileButton.setDisable(false);
                });
            }
        }, "transfer-prep");
        prepThread.setDaemon(true);
        prepThread.start();
    }

    private void startTimeline(int fps) {
        double intervalMs = 1000.0 / fps;
        displayTimeline = new Timeline(new KeyFrame(Duration.millis(intervalMs), e -> displayNextQR()));
        displayTimeline.setCycleCount(Timeline.INDEFINITE);
        displayTimeline.play();
    }

    private void restartTimeline(int fps) {
        if (displayTimeline != null) {
            displayTimeline.stop();
            startTimeline(fps);
        }
    }

    private void displayNextQR() {
        if (transferService == null) return;

        String packetJson = transferService.getPacketAtIndex(currentPacketIndex);

        try {
            // Generate QR code image
            BufferedImage qrImage = qrGenerator.generateQRImage(packetJson);
            WritableImage fxImage = SwingFXUtils.toFXImage(qrImage, null);
            qrImageView.setImage(fxImage);

            // Calculate which chunk we're displaying (for progress)
            int totalPackets = transferService.getTotalPacketCount();
            int indexInCycle = currentPacketIndex % totalPackets;
            int cycleNumber = (currentPacketIndex / totalPackets) + 1;

            // Index 0 = header, 1..totalChunks = chunks, totalChunks+1 = end
            if (indexInCycle == 0) {
                chunkLabel.setText("Sending: HEADER (Cycle " + cycleNumber + ")");
            } else if (indexInCycle <= transferService.getTotalChunks()) {
                currentCycleChunk = indexInCycle;
                chunkLabel.setText(String.format("Chunk: %d / %d (Cycle %d)",
                        indexInCycle, transferService.getTotalChunks(), cycleNumber));
            } else {
                chunkLabel.setText("Sending: END (Cycle " + cycleNumber + ")");
            }

            // Update progress bar (based on current cycle progress)
            double progress = (double) indexInCycle / totalPackets;
            progressBar.setProgress(progress);

            // Update estimated time
            updateEstimatedTime(indexInCycle, totalPackets);

            currentPacketIndex++;

        } catch (Exception e) {
            log.error("Error displaying QR code at index {}", currentPacketIndex, e);
        }
    }

    private void updateEstimatedTime(int currentIndex, int totalPackets) {
        long elapsed = System.currentTimeMillis() - transferStartTime;
        if (currentIndex > 0) {
            long estimatedTotal = (elapsed * totalPackets) / currentIndex;
            long remaining = estimatedTotal - elapsed;
            if (remaining > 0) {
                estimatedTimeLabel.setText(String.format("Estimated remaining: %d sec",
                        remaining / 1000));
            } else {
                estimatedTimeLabel.setText("Completing cycle...");
            }
        }
    }

    private void togglePause() {
        if (displayTimeline == null) return;

        if (isPaused) {
            displayTimeline.play();
            pauseButton.setText("⏸  Pause");
            statusLabel.setText("Transmitting...");
            isPaused = false;
            log.info("Transfer resumed");
        } else {
            displayTimeline.pause();
            pauseButton.setText("▶  Resume");
            statusLabel.setText("Paused");
            isPaused = true;
            log.info("Transfer paused at packet {}", currentPacketIndex);
        }
    }

    private void stopTransfer() {
        if (displayTimeline != null) {
            displayTimeline.stop();
            displayTimeline = null;
        }

        isPaused = false;
        currentPacketIndex = 0;
        currentCycleChunk = 0;
        qrImageView.setImage(null);
        progressBar.setProgress(0);
        chunkLabel.setText("Chunk: — / —");
        estimatedTimeLabel.setText("");
        statusLabel.setText("Transfer stopped");

        startButton.setDisable(false);
        selectFileButton.setDisable(false);
        pauseButton.setDisable(true);
        stopButton.setDisable(true);
        pauseButton.setText("⏸  Pause");

        log.info("Transfer stopped by user");
    }

    private void updateFpsLabel(int fps) {
        fpsLabel.setText(fps + " QR/sec");
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * Clean shutdown — stop the display timeline.
     */
    public void shutdown() {
        if (displayTimeline != null) {
            displayTimeline.stop();
        }
        log.info("Sender controller shut down");
    }
}
