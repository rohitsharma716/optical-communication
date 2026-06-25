package com.optical.sender.qr;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for QR code generation.
 */
class QRCodeGeneratorTest {

    @Test
    void generateQRImage_returnsImage() {
        QRCodeGenerator generator = new QRCodeGenerator(300, 300);
        BufferedImage image = generator.generateQRImage("Hello QR");

        assertNotNull(image);
        assertEquals(300, image.getWidth());
        assertEquals(300, image.getHeight());
    }

    @Test
    void generateQRImage_withJsonPayload() {
        QRCodeGenerator generator = new QRCodeGenerator(700, 700);
        String json = """
                {"transferId":"abc","sequence":1,"totalChunks":10,"payload":"SGVsbG8=","type":"CHUNK"}
                """;

        BufferedImage image = generator.generateQRImage(json.trim());

        assertNotNull(image);
        assertEquals(700, image.getWidth());
    }

    @Test
    void generateQRImage_largePayload() {
        QRCodeGenerator generator = new QRCodeGenerator(700, 700);

        // Simulate a realistic chunk payload (~1400 chars)
        StringBuilder sb = new StringBuilder();
        sb.append("{\"transferId\":\"123e4567-e89b-12d3-a456-426614174000\",");
        sb.append("\"sequence\":1,\"totalChunks\":500,\"payload\":\"");
        // ~1334 chars of Base64 (1000 bytes → Base64)
        sb.append("A".repeat(1334));
        sb.append("\",\"type\":\"CHUNK\"}");

        String json = sb.toString();
        assertTrue(json.length() > 1400, "Payload should be >1400 chars");

        BufferedImage image = generator.generateQRImage(json);
        assertNotNull(image, "Should generate QR for large payloads");
    }

    @Test
    void generateQRImage_customDimensions() {
        QRCodeGenerator generator = new QRCodeGenerator(700, 700);
        BufferedImage image = generator.generateQRImage("test", 200, 200);

        assertNotNull(image);
        assertEquals(200, image.getWidth());
        assertEquals(200, image.getHeight());
    }
}
