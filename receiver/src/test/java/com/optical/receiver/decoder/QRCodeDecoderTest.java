package com.optical.receiver.decoder;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for QR code decoding.
 * Generates QR images using ZXing and verifies they can be decoded.
 */
class QRCodeDecoderTest {

    private BufferedImage generateTestQR(String data) throws Exception {
        BitMatrix matrix = new MultiFormatWriter().encode(
                data, BarcodeFormat.QR_CODE, 400, 400);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    @Test
    void decode_simpleText() throws Exception {
        QRCodeDecoder decoder = new QRCodeDecoder();
        BufferedImage qrImage = generateTestQR("Hello, World!");

        Optional<String> result = decoder.decode(qrImage);

        assertTrue(result.isPresent());
        assertEquals("Hello, World!", result.get());
    }

    @Test
    void decode_jsonPacket() throws Exception {
        QRCodeDecoder decoder = new QRCodeDecoder();
        String json = "{\"transferId\":\"abc-123\",\"sequence\":1,\"totalChunks\":10,\"payload\":\"SGVsbG8=\",\"type\":\"CHUNK\"}";
        BufferedImage qrImage = generateTestQR(json);

        Optional<String> result = decoder.decode(qrImage);

        assertTrue(result.isPresent());
        assertEquals(json, result.get());
    }

    @Test
    void decode_noQRCode_returnsEmpty() {
        QRCodeDecoder decoder = new QRCodeDecoder();

        // Blank white image — no QR code
        BufferedImage blank = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 400; x++) {
            for (int y = 0; y < 400; y++) {
                blank.setRGB(x, y, 0xFFFFFF);
            }
        }

        Optional<String> result = decoder.decode(blank);
        assertTrue(result.isEmpty());
    }

    @Test
    void decode_nullImage_returnsEmpty() {
        QRCodeDecoder decoder = new QRCodeDecoder();
        Optional<String> result = decoder.decode(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void decode_largePayload() throws Exception {
        QRCodeDecoder decoder = new QRCodeDecoder();

        // Simulate realistic chunk packet
        StringBuilder sb = new StringBuilder();
        sb.append("{\"transferId\":\"123e4567-e89b-12d3-a456-426614174000\",");
        sb.append("\"sequence\":42,\"totalChunks\":100,\"payload\":\"");
        sb.append("A".repeat(1334)); // ~1000 bytes Base64
        sb.append("\",\"type\":\"CHUNK\"}");

        BufferedImage qrImage = generateTestQR(sb.toString());
        Optional<String> result = decoder.decode(qrImage);

        assertTrue(result.isPresent());
        assertTrue(result.get().contains("\"sequence\":42"));
    }
}
