package com.optical.sender.util;

/**
 * Configuration constants for the sender application.
 */
public class TransferConfig {

    /** Maximum bytes per chunk before Base64 encoding */
    public static final int CHUNK_SIZE = 1000;

    /** Default QR codes displayed per second */
    public static final int DEFAULT_FPS = 5;

    /** Minimum QR display rate */
    public static final int MIN_FPS = 1;

    /** Maximum QR display rate */
    public static final int MAX_FPS = 15;

    /** QR code image width and height in pixels */
    public static final int QR_SIZE = 700;

    /** QR code error correction level margin */
    public static final int QR_MARGIN = 1;

    private TransferConfig() {
        // Utility class — no instantiation
    }
}
