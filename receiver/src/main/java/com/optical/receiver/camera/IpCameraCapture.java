package com.optical.receiver.camera;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Captures frames from a phone camera via HTTP snapshot requests.
 * <p>
 * Uses the IP Webcam app's {@code /shot.jpg} endpoint to grab individual JPEG
 * frames over HTTP. This approach does NOT require FFmpeg or GStreamer backends
 * in OpenCV — it works with the plain {@code openpnp} OpenCV package.
 * </p>
 *
 * <h3>How it works:</h3>
 * <ol>
 *   <li>Each call to {@link #grabFrame()} makes an HTTP GET to {@code <baseUrl>/shot.jpg}</li>
 *   <li>The returned JPEG bytes are decoded into an OpenCV Mat via {@code Imgcodecs.imdecode}</li>
 *   <li>The Mat is then available for QR code scanning, same as a webcam frame</li>
 * </ol>
 *
 * <h3>Compatible apps:</h3>
 * <ul>
 *   <li><b>IP Webcam</b> (Android) — {@code http://<ip>:8080/shot.jpg}</li>
 *   <li>Any app exposing a JPEG snapshot URL</li>
 * </ul>
 */
public class IpCameraCapture implements CameraSource {

    private static final Logger log = LoggerFactory.getLogger(IpCameraCapture.class);

    /** Timeout for HTTP connections (milliseconds) */
    private static final int CONNECT_TIMEOUT_MS = 5000;
    /** Timeout for reading data (milliseconds) */
    private static final int READ_TIMEOUT_MS = 5000;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private String baseUrl;
    private String snapshotUrl;

    /**
     * Create an IP camera capture with the given base URL.
     *
     * @param baseUrl the base URL of the IP camera (e.g., "http://10.110.15.146:8080")
     */
    public IpCameraCapture(String baseUrl) {
        setUrl(baseUrl);
    }

    /**
     * Update the IP camera URL.
     *
     * @param baseUrl the base URL (trailing slash is stripped; "/shot.jpg" appended automatically)
     */
    public void setUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            this.baseUrl = "";
            this.snapshotUrl = "";
            return;
        }

        // Normalize: remove trailing slash
        this.baseUrl = baseUrl.replaceAll("/+$", "");

        // Build the snapshot URL
        if (this.baseUrl.endsWith("/shot.jpg")) {
            this.snapshotUrl = this.baseUrl;
        } else {
            this.snapshotUrl = this.baseUrl + "/shot.jpg";
        }
    }

    @Override
    public boolean open() {
        if (snapshotUrl == null || snapshotUrl.isBlank()) {
            log.error("IP camera URL is empty");
            return false;
        }

        log.info("Testing IP camera connection at: {}", snapshotUrl);

        // Validate by fetching a single frame
        byte[] testFrame = fetchJpegBytes();
        if (testFrame == null || testFrame.length == 0) {
            log.error("Failed to connect to IP camera at: {}", snapshotUrl);
            return false;
        }

        running.set(true);
        log.info("IP camera connected: {} (snapshot size: {} bytes)", snapshotUrl, testFrame.length);
        return true;
    }

    @Override
    public Mat grabFrame() {
        if (!running.get()) {
            return null;
        }

        byte[] jpegBytes = fetchJpegBytes();
        if (jpegBytes == null || jpegBytes.length == 0) {
            return null;
        }

        // Decode JPEG bytes into an OpenCV Mat
        MatOfByte mob = new MatOfByte(jpegBytes);
        Mat frame = Imgcodecs.imdecode(mob, Imgcodecs.IMREAD_COLOR);
        mob.release();

        if (frame.empty()) {
            frame.release();
            return null;
        }

        return frame;
    }

    @Override
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

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void release() {
        running.set(false);
        log.info("IP camera released");
    }

    @Override
    public String getDescription() {
        return "IP Camera (" + baseUrl + ")";
    }

    /**
     * Test if the IP camera URL is reachable and can provide frames.
     *
     * @return true if a JPEG snapshot was successfully fetched
     */
    public boolean testConnection() {
        try {
            byte[] jpeg = fetchJpegBytes();
            if (jpeg != null && jpeg.length > 0) {
                log.info("IP camera test passed: {} ({} bytes)", snapshotUrl, jpeg.length);
                return true;
            }
        } catch (Exception e) {
            log.error("IP camera connection test failed: {}", e.getMessage());
        }
        return false;
    }

    // ---- Internal ----

    /**
     * Fetch a single JPEG snapshot from the IP camera via HTTP GET.
     *
     * @return the JPEG bytes, or null on failure
     */
    private byte[] fetchJpegBytes() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(snapshotUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("Accept", "image/jpeg");

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.warn("IP camera returned HTTP {}: {}", responseCode, snapshotUrl);
                return null;
            }

            try (InputStream is = conn.getInputStream();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream(65536)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                return baos.toByteArray();
            }

        } catch (IOException e) {
            log.debug("Failed to fetch snapshot from {}: {}", snapshotUrl, e.getMessage());
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
