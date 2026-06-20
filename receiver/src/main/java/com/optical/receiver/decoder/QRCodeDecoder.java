package com.optical.receiver.decoder;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Decodes QR codes from BufferedImage frames using ZXing.
 * Configured with TRY_HARDER hint for maximum decode reliability.
 */
public class QRCodeDecoder {

    private static final Logger log = LoggerFactory.getLogger(QRCodeDecoder.class);

    private final MultiFormatReader reader;

    public QRCodeDecoder() {
        this.reader = new MultiFormatReader();

        Map<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, java.util.List.of(BarcodeFormat.QR_CODE));
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        reader.setHints(hints);
    }

    /**
     * Attempt to decode a QR code from the given image.
     *
     * @param image the webcam frame as a BufferedImage
     * @return Optional containing the decoded string, or empty if no QR code was found
     */
    public Optional<String> decode(BufferedImage image) {
        if (image == null) {
            return Optional.empty();
        }

        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = reader.decode(bitmap);
            return Optional.of(result.getText());
        } catch (NotFoundException e) {
            // No QR code found in this frame — this is normal and expected
            log.trace("No QR code detected in frame");
            return Optional.empty();
        } catch (Exception e) {
            log.debug("QR decode error: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
