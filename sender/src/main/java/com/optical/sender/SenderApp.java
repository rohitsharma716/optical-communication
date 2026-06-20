package com.optical.sender;

import com.optical.sender.ui.SenderController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JavaFX Application for the Sender.
 * Displays the sender UI for file selection and QR code display.
 */
public class SenderApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(SenderApp.class);
    private SenderController controller;

    @Override
    public void start(Stage primaryStage) {
        log.info("Starting Optical File Transfer — Sender");

        controller = new SenderController(primaryStage);
        Scene scene = new Scene(controller.getRoot(), 950, 850);

        // Load CSS stylesheet
        String css = getClass().getResource("/css/sender-styles.css").toExternalForm();
        scene.getStylesheets().add(css);

        primaryStage.setTitle("Optical File Transfer — Sender");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(800);
        primaryStage.setOnCloseRequest(e -> {
            log.info("Sender application closing");
            if (controller != null) {
                controller.shutdown();
            }
        });

        primaryStage.show();
        log.info("Sender UI ready");
    }

    @Override
    public void stop() {
        log.info("Sender application stopped");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
