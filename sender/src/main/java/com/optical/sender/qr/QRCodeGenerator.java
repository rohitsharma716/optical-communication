package com.optical.sender.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.optical.sender.util.TransferConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates QR code images from string data using ZXing.
 * Configured for optimal balance between data capacity and scan reliability.
 */
public class QRCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(QRCodeGenerator.class);

    private final int width;
    private final int height;
    private final Map<EncodeHintType, Object> hints;

    public QRCodeGenerator() {
        this(TransferConfig.QR_SIZE, TransferConfig.QR_SIZE);
    }

    public QRCodeGenerator(int width, int height) {
        this.width = width;
        this.height = height;

        this.hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, TransferConfig.QR_MARGIN);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    }

    /**
     * Generate a QR code image from the given data string.
     *
     * @param data the string data to encode (typically JSON packet)
     * @return BufferedImage of the QR code
     * @throws RuntimeException if encoding fails
     */
    public BufferedImage generateQRImage(String data) {
        return generateQRImage(data, this.width, this.height);
    }

    /**
     * Generate a QR code image with custom dimensions.
     *
     * @param data   the string data to encode
     * @param width  image width in pixels
     * @param height image height in pixels
     * @return BufferedImage of the QR code
     * @throws RuntimeException if encoding fails
     */
    public BufferedImage generateQRImage(String data, int width, int height) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    data, BarcodeFormat.QR_CODE, width, height, hints);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (WriterException e) {
            log.error("Failed to generate QR code for data of length {}: {}", data.length(), e.getMessage());
            throw new RuntimeException("QR code generation failed", e);
        }
    }
}
