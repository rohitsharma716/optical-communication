package com.optical.receiver;

/**
 * Entry point for the Receiver application.
 * Loads OpenCV native library and launches the JavaFX application.
 */
public class ReceiverLauncher {

    public static void main(String[] args) {
        // Load OpenCV native library before JavaFX starts
        nu.pattern.OpenCV.loadShared();
        System.out.println("OpenCV native library loaded successfully");

        ReceiverApp.main(args);
    }
}
