package com.optical.receiver.camera;

import org.opencv.core.Mat;

import java.awt.image.BufferedImage;

/**
 * Common interface for camera sources used by the receiver.
 * Implementations include local webcam (WebcamCapture) and
 * IP-based phone camera streams (IpCameraCapture).
 */
public interface CameraSource {

    /**
     * Open the camera and prepare for frame capture.
     *
     * @return true if the camera was opened successfully
     */
    boolean open();

    /**
     * Grab a single frame from the camera.
     *
     * @return the frame as an OpenCV Mat, or null if capture failed
     */
    Mat grabFrame();

    /**
     * Convert an OpenCV Mat to a BufferedImage for ZXing QR processing.
     *
     * @param mat the OpenCV Mat frame
     * @return BufferedImage representation, or null on conversion failure
     */
    BufferedImage matToBufferedImage(Mat mat);

    /**
     * Check if the camera source is currently active.
     */
    boolean isRunning();

    /**
     * Release all camera resources.
     */
    void release();

    /**
     * Get a human-readable description of this camera source.
     */
    String getDescription();
}
