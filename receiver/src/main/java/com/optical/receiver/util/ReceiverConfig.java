package com.optical.receiver.util;

/**
 * Configuration constants for the receiver application.
 */
public class ReceiverConfig {

    /** Default webcam index (0 = first/default camera) */
    public static final int CAMERA_INDEX = 0;

    /** Target webcam scan frame rate */
    public static final int SCAN_FPS = 15;

    /** Camera capture width */
    public static final int CAMERA_WIDTH = 640;

    /** Camera capture height */
    public static final int CAMERA_HEIGHT = 480;

    /** Default output directory for received files */
    public static final String OUTPUT_DIR = "received_files";

    private ReceiverConfig() {
        // Utility class — no instantiation
    }
}
