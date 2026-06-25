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

    /** Default IP camera URL (for phone camera via IP Webcam app) */
    public static final String DEFAULT_IP_CAMERA_URL = "http://192.168.1.100:8080";

    /** Default output directory for received files */
    public static final String OUTPUT_DIR = "received_files";

    private ReceiverConfig() {
        // Utility class — no instantiation
    }
}
