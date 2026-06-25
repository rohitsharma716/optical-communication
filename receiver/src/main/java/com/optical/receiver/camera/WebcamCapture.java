package com.optical.receiver.camera;

import com.optical.receiver.util.ReceiverConfig;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages webcam capture using OpenCV's VideoCapture.
 * Provides frame grabbing and conversion to BufferedImage for QR decoding.
 */
public class WebcamCapture {

    private static final Logger log = LoggerFactory.getLogger(WebcamCapture.class);

    private VideoCapture capture;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final int cameraIndex;

    public WebcamCapture() {
        this(ReceiverConfig.CAMERA_INDEX);
    }

    public WebcamCapture(int cameraIndex) {
        this.cameraIndex = cameraIndex;
    }

    /**
     * Open the webcam and prepare for frame capture.
     *
     * @return true if the camera was opened successfully
     */
    public boolean open() {
        capture = new VideoCapture(cameraIndex);

        if (!capture.isOpened()) {
            log.error("Failed to open webcam at index {}", cameraIndex);
            return false;
        }

        // Set capture resolution
        capture.set(Videoio.CAP_PROP_FRAME_WIDTH, ReceiverConfig.CAMERA_WIDTH);
        capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, ReceiverConfig.CAMERA_HEIGHT);

        running.set(true);
        log.info("Webcam opened at index {} ({}x{})", cameraIndex,
                (int) capture.get(Videoio.CAP_PROP_FRAME_WIDTH),
                (int) capture.get(Videoio.CAP_PROP_FRAME_HEIGHT));
        return true;
    }

    /**
     * Grab a single frame from the webcam.
     *
     * @return the frame as an OpenCV Mat, or null if capture failed
     */
    public Mat grabFrame() {
        if (capture == null || !capture.isOpened()) {
            return null;
        }

        Mat frame = new Mat();
        if (capture.read(frame) && !frame.empty()) {
            return frame;
        }
        frame.release();
        return null;
    }

    /**
     * Convert an OpenCV Mat to a BufferedImage for ZXing processing.
     *
     * @param mat the OpenCV Mat frame
     * @return BufferedImage representation, or null on conversion failure
     */
    public BufferedImage matToBufferedImage(Mat mat) {
        if (mat == null || mat.empty()) {
            return null;
        }

        try {
            MatOfByte mob = new MatOfByte();
            Imgcodecs.imencode(".bmp", mat, mob);
            byte[] byteArray = mob.toArray();
            mob.release();

            try (ByteArrayInputStream bis = new ByteArrayInputStream(byteArray)) {
                return ImageIO.read(bis);
            }
        } catch (IOException e) {
            log.error("Failed to convert Mat to BufferedImage", e);
            return null;
        }
    }

    /**
     * Check if the webcam is currently active.
     */
    public boolean isRunning() {
        return running.get() && capture != null && capture.isOpened();
    }

    /**
     * Release the webcam resources.
     */
    public void release() {
        running.set(false);
        if (capture != null && capture.isOpened()) {
            capture.release();
            log.info("Webcam released");
        }
    }
}
