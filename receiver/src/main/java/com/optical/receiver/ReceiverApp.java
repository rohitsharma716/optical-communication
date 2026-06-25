package com.optical.receiver;

import com.optical.receiver.ui.ReceiverController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JavaFX Application for the Receiver.
 * Displays live webcam preview, QR decoding status, and file reconstruction progress.
 */
public class ReceiverApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(ReceiverApp.class);
    private ReceiverController controller;

    @Override
    public void start(Stage primaryStage) {
        log.info("Starting Optical File Transfer — Receiver");

        controller = new ReceiverController(primaryStage);
        Scene scene = new Scene(controller.getRoot(), 1000, 850);

        // Load CSS stylesheet
        String css = getClass().getResource("/css/receiver-styles.css").toExternalForm();
        scene.getStylesheets().add(css);

        primaryStage.setTitle("Optical File Transfer — Receiver");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(800);
        primaryStage.setOnCloseRequest(e -> {
            log.info("Receiver application closing");
            if (controller != null) {
                controller.shutdown();
            }
        });

        primaryStage.show();
        log.info("Receiver UI ready");
    }

    @Override
    public void stop() {
        log.info("Receiver application stopped");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
