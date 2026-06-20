package com.optical.receiver.ui;

import com.optical.receiver.assembler.FileAssembler;
import com.optical.receiver.camera.WebcamCapture;
import com.optical.receiver.decoder.QRCodeDecoder;
import com.optical.receiver.service.ReceptionService;
import com.optical.receiver.util.ReceiverConfig;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controller for the Receiver UI.
 * Manages webcam preview, QR decoding, progress tracking, and file reconstruction.
 */
public class ReceiverController {

    private static final Logger log = LoggerFactory.getLogger(ReceiverController.class);

    private final Stage stage;
    private final BorderPane root;

    // UI Components
    private final ImageView cameraPreview;
    private final Label statusLabel;
    private final Label transferIdLabel;
    private final Label fileNameLabel;
    private final Label detectedChunksLabel;
    private final Label missingChunksLabel;
    private final Label totalChunksLabel;
    private final Label outputPathLabel;
    private final ProgressBar progressBar;
    private final Button startScanButton;
    private final Button stopButton;
    private final Button selectOutputButton;
    private final Button saveButton;

    // Services
    private final WebcamCapture webcamCapture;
    private final QRCodeDecoder qrDecoder;
    private final ReceptionService receptionService;
    private final FileAssembler fileAssembler;

    // State
    private Thread scanThread;
    private final AtomicBoolean scanning = new AtomicBoolean(false);
    private String outputDir = ReceiverConfig.OUTPUT_DIR;
    private String lastDecodedText = "";

    public ReceiverController(Stage stage) {
        this.stage = stage;
        this.webcamCapture = new WebcamCapture();
        this.qrDecoder = new QRCodeDecoder();
        this.receptionService = new ReceptionService();
        this.fileAssembler = new FileAssembler();

        // Set up reception service listeners
        receptionService.setStateChangeListener(state -> Platform.runLater(() -> onStateChanged(state)));
        receptionService.setChunkReceivedListener(() -> Platform.runLater(this::updateProgress));

        // --- Header Section ---
        Label titleLabel = new Label("OPTICAL FILE TRANSFER");
        titleLabel.getStyleClass().add("app-title");

        Label subtitleLabel = new Label("R E C E I V E R");
        subtitleLabel.getStyleClass().add("app-subtitle");

        VBox headerBox = new VBox(4, titleLabel, subtitleLabel);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(20, 0, 10, 0));

        // --- Camera Preview Section ---
        cameraPreview = new ImageView();
        cameraPreview.setFitWidth(ReceiverConfig.CAMERA_WIDTH);
        cameraPreview.setFitHeight(ReceiverConfig.CAMERA_HEIGHT);
        cameraPreview.setPreserveRatio(true);
        cameraPreview.getStyleClass().add("camera-preview");

        StackPane cameraContainer = new StackPane(cameraPreview);
        cameraContainer.getStyleClass().add("camera-container");
        cameraContainer.setMinSize(ReceiverConfig.CAMERA_WIDTH + 20, ReceiverConfig.CAMERA_HEIGHT + 20);

        // --- Control Section ---
        startScanButton = new Button("📷  Start Scanning");
        startScanButton.getStyleClass().addAll("primary-button", "start-button");
        startScanButton.setOnAction(e -> startScanning());

        stopButton = new Button("⏹  Stop");
        stopButton.getStyleClass().addAll("control-button", "stop-button");
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> stopScanning());

        selectOutputButton = new Button("📂  Output Folder");
        selectOutputButton.getStyleClass().add("control-button");
        selectOutputButton.setOnAction(e -> selectOutputDir());

        saveButton = new Button("💾  Save File");
        saveButton.getStyleClass().addAll("primary-button", "save-button");
        saveButton.setDisable(true);
        saveButton.setOnAction(e -> saveFile());

        HBox controlBox = new HBox(12, startScanButton, stopButton, selectOutputButton, saveButton);
        controlBox.setAlignment(Pos.CENTER);

        VBox controlSection = new VBox(12, controlBox);
        controlSection.getStyleClass().add("section-box");

        // --- Transfer Info Section ---
        statusLabel = new Label("Waiting for transfer...");
        statusLabel.getStyleClass().add("status-label");

        transferIdLabel = createInfoRow("Transfer ID:", "—");
        fileNameLabel = createInfoRow("File:", "—");
        detectedChunksLabel = createInfoRow("Received:", "0");
        missingChunksLabel = createInfoRow("Missing:", "0");
        totalChunksLabel = createInfoRow("Total Chunks:", "—");

        outputPathLabel = new Label("Output: " + outputDir);
        outputPathLabel.getStyleClass().add("info-label-dim");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("transfer-progress");

        VBox infoSection = new VBox(8,
                statusLabel,
                progressBar,
                transferIdLabel,
                fileNameLabel,
                detectedChunksLabel,
                missingChunksLabel,
                totalChunksLabel,
                outputPathLabel
        );
        infoSection.getStyleClass().add("section-box");

        // --- Main Layout ---
        VBox centerContent = new VBox(15, cameraContainer, controlSection, infoSection);
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

    private Label createInfoRow(String label, String value) {
        Label l = new Label(label + " " + value);
        l.getStyleClass().add("info-label");
        return l;
    }

    private void updateInfoLabel(Label label, String prefix, String value) {
        label.setText(prefix + " " + value);
    }

    // --- Scanning Control ---

    private void startScanning() {
        if (scanning.get()) return;

        statusLabel.setText("Opening camera...");
        startScanButton.setDisable(true);

        Thread openThread = new Thread(() -> {
            boolean opened = webcamCapture.open();

            Platform.runLater(() -> {
                if (opened) {
                    scanning.set(true);
                    stopButton.setDisable(false);
                    statusLabel.setText("Scanning for QR codes...");
                    receptionService.reset();
                    resetInfoLabels();

                    // Start scan loop thread
                    scanThread = new Thread(this::scanLoop, "qr-scan-loop");
                    scanThread.setDaemon(true);
                    scanThread.start();

                    log.info("Camera scanning started");
                } else {
                    statusLabel.setText("Error: Could not open webcam");
                    startScanButton.setDisable(false);
                }
            });
        }, "camera-open");
        openThread.setDaemon(true);
        openThread.start();
    }

    private void scanLoop() {
        long frameDurationMs = 1000 / ReceiverConfig.SCAN_FPS;

        while (scanning.get() && webcamCapture.isRunning()) {
            long frameStart = System.currentTimeMillis();

            try {
                // Grab frame
                Mat frame = webcamCapture.grabFrame();
                if (frame == null) continue;

                // Convert to BufferedImage
                BufferedImage bufferedImage = webcamCapture.matToBufferedImage(frame);

                // Update camera preview on FX thread
                if (bufferedImage != null) {
                    WritableImage fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
                    Platform.runLater(() -> cameraPreview.setImage(fxImage));

                    // Try to decode QR code
                    Optional<String> decoded = qrDecoder.decode(bufferedImage);
                    decoded.ifPresent(text -> {
                        // Only process if this is a new/different QR code
                        if (!text.equals(lastDecodedText)) {
                            lastDecodedText = text;
                            receptionService.processPacket(text);
                        }
                    });
                }

                // Release OpenCV Mat
                frame.release();

            } catch (Exception e) {
                log.error("Error in scan loop: {}", e.getMessage());
            }

            // Frame rate throttling
            long elapsed = System.currentTimeMillis() - frameStart;
            long sleepMs = frameDurationMs - elapsed;
            if (sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void stopScanning() {
        scanning.set(false);
        webcamCapture.release();

        startScanButton.setDisable(false);
        stopButton.setDisable(true);

        if (receptionService.getState() == ReceptionService.State.RECEIVING) {
            statusLabel.setText("Scanning stopped — transfer incomplete");
        } else if (receptionService.getState() != ReceptionService.State.COMPLETE) {
            statusLabel.setText("Scanning stopped");
        }

        log.info("Camera scanning stopped");
    }

    // --- State & Progress ---

    private void onStateChanged(ReceptionService.State state) {
        switch (state) {
            case RECEIVING -> {
                statusLabel.setText("Receiving: " + receptionService.getFileName());
                updateInfoLabel(transferIdLabel, "Transfer ID:",
                        shortenUUID(receptionService.getTransferId()));
                updateInfoLabel(fileNameLabel, "File:", receptionService.getFileName());
                updateInfoLabel(totalChunksLabel, "Total Chunks:",
                        String.valueOf(receptionService.getTotalChunks()));
            }
            case COMPLETE -> {
                statusLabel.setText("✓ All chunks received! Ready to save.");
                statusLabel.getStyleClass().add("status-complete");
                saveButton.setDisable(false);
                updateProgress();
            }
            case ERROR -> {
                statusLabel.setText("Error: " + receptionService.getErrorMessage());
                statusLabel.getStyleClass().add("status-error");
            }
            default -> { }
        }
    }

    private void updateProgress() {
        int received = receptionService.getReceivedChunkCount();
        int total = receptionService.getTotalChunks();
        int missing = receptionService.getMissingChunkCount();

        updateInfoLabel(detectedChunksLabel, "Received:", received + " / " + total);
        updateInfoLabel(missingChunksLabel, "Missing:", String.valueOf(missing));

        if (total > 0) {
            progressBar.setProgress((double) received / total);
        }
    }

    private void resetInfoLabels() {
        updateInfoLabel(transferIdLabel, "Transfer ID:", "—");
        updateInfoLabel(fileNameLabel, "File:", "—");
        updateInfoLabel(detectedChunksLabel, "Received:", "0");
        updateInfoLabel(missingChunksLabel, "Missing:", "0");
        updateInfoLabel(totalChunksLabel, "Total Chunks:", "—");
        progressBar.setProgress(0);
        lastDecodedText = "";
    }

    // --- File Save ---

    private void selectOutputDir() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Output Directory");
        File dir = chooser.showDialog(stage);
        if (dir != null) {
            outputDir = dir.getAbsolutePath();
            outputPathLabel.setText("Output: " + outputDir);
            log.info("Output directory set to: {}", outputDir);
        }
    }

    private void saveFile() {
        if (receptionService.getState() != ReceptionService.State.COMPLETE) {
            return;
        }

        saveButton.setDisable(true);
        statusLabel.setText("Assembling file...");

        Thread assemblyThread = new Thread(() -> {
            try {
                File savedFile = fileAssembler.assemble(
                        receptionService.getReceivedChunks(),
                        receptionService.getTotalChunks(),
                        receptionService.getExpectedHash(),
                        receptionService.getFileName(),
                        outputDir
                );

                Platform.runLater(() -> {
                    statusLabel.setText("✓ File saved: " + savedFile.getAbsolutePath());
                    outputPathLabel.setText("Saved: " + savedFile.getAbsolutePath());
                    log.info("File successfully saved: {}", savedFile.getAbsolutePath());
                });

            } catch (Exception e) {
                log.error("File assembly failed", e);
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    saveButton.setDisable(false);
                });
            }
        }, "file-assembly");
        assemblyThread.setDaemon(true);
        assemblyThread.start();
    }

    private String shortenUUID(String uuid) {
        if (uuid == null) return "—";
        return uuid.length() > 13 ? uuid.substring(0, 13) + "..." : uuid;
    }

    /**
     * Clean shutdown — stop scanning and release camera.
     */
    public void shutdown() {
        scanning.set(false);
        webcamCapture.release();
        log.info("Receiver controller shut down");
    }
}
