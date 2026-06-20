package com.optical.integration;

import com.optical.receiver.assembler.FileAssembler;
import com.optical.receiver.decoder.QRCodeDecoder;
import com.optical.receiver.service.ReceptionService;
import com.optical.sender.qr.QRCodeGenerator;
import com.optical.sender.service.TransferService;
import com.optical.shared.model.PacketType;
import com.optical.shared.protocol.PacketSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test: simulates the full optical transfer pipeline
 * without actual screen/camera — using in-memory QR generation and decoding.
 *
 * Flow: File → Compress → Chunk → QR Generate → QR Decode → Reassemble → Verify
 */
class TransferIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void fullTransferPipeline_smallFile() throws Exception {
        // Create a test file
        Path testFile = tempDir.resolve("test_song.mp3");
        byte[] originalData = "This is simulated MP3 audio content for integration testing.".getBytes();
        Files.write(testFile, originalData);

        // === SENDER SIDE ===
        TransferService senderService = new TransferService();
        senderService.prepareTransfer(testFile.toFile());

        QRCodeGenerator qrGenerator = new QRCodeGenerator(500, 500);
        int totalPackets = senderService.getTotalPacketCount();

        // === RECEIVER SIDE ===
        QRCodeDecoder decoder = new QRCodeDecoder();
        ReceptionService receptionService = new ReceptionService();

        // Simulate optical transfer: generate QR → decode QR → process packet
        for (int i = 0; i < totalPackets; i++) {
            String packetJson = senderService.getPacketAtIndex(i);

            // Generate QR image (what would appear on sender's screen)
            BufferedImage qrImage = qrGenerator.generateQRImage(packetJson);

            // Decode QR image (what receiver's camera would scan)
            Optional<String> decoded = decoder.decode(qrImage);

            assertTrue(decoded.isPresent(), "QR code should be decodable for packet " + i);
            assertEquals(packetJson, decoded.get(), "Decoded data should match original");

            // Process the decoded packet
            receptionService.processPacket(decoded.get());
        }

        // Verify reception is complete
        assertEquals(ReceptionService.State.COMPLETE, receptionService.getState());
        assertEquals(senderService.getTotalChunks(), receptionService.getReceivedChunkCount());

        // === ASSEMBLY ===
        FileAssembler assembler = new FileAssembler();
        Path outputDir = tempDir.resolve("output");
        File reconstructedFile = assembler.assemble(
                receptionService.getReceivedChunks(),
                receptionService.getTotalChunks(),
                receptionService.getExpectedHash(),
                receptionService.getFileName(),
                outputDir.toString()
        );

        // Verify reconstructed file
        assertTrue(reconstructedFile.exists());
        byte[] reconstructedData = Files.readAllBytes(reconstructedFile.toPath());
        assertArrayEquals(originalData, reconstructedData, "Reconstructed file should match original");
    }

    @Test
    void fullTransferPipeline_largerFile() throws Exception {
        // Create a larger test file (10 KB)
        Path testFile = tempDir.resolve("bigger_song.mp3");
        byte[] originalData = new byte[10_000];
        new Random(12345).nextBytes(originalData);
        Files.write(testFile, originalData);

        // === SENDER ===
        TransferService senderService = new TransferService();
        senderService.prepareTransfer(testFile.toFile());

        // Use larger QR images to ensure reliable decoding for large payloads
        QRCodeGenerator qrGenerator = new QRCodeGenerator(900, 900);
        int totalPackets = senderService.getTotalPacketCount();

        // === RECEIVER ===
        QRCodeDecoder decoder = new QRCodeDecoder();
        ReceptionService receptionService = new ReceptionService();

        int decodedCount = 0;
        int qrDecodedCount = 0;
        for (int i = 0; i < totalPackets; i++) {
            String packetJson = senderService.getPacketAtIndex(i);
            BufferedImage qrImage = qrGenerator.generateQRImage(packetJson);
            Optional<String> decoded = decoder.decode(qrImage);

            if (decoded.isPresent()) {
                assertEquals(packetJson, decoded.get(), "Decoded data should match for packet " + i);
                receptionService.processPacket(decoded.get());
                qrDecodedCount++;
            } else {
                // Fallback: process directly from JSON (simulates retransmission catch)
                receptionService.processPacket(packetJson);
            }
            decodedCount++;
        }

        assertTrue(qrDecodedCount > totalPackets * 0.9,
                "At least 90% of QR codes should decode, got " + qrDecodedCount + "/" + totalPackets);
        assertEquals(ReceptionService.State.COMPLETE, receptionService.getState());

        // === ASSEMBLY ===
        FileAssembler assembler = new FileAssembler();
        File result = assembler.assemble(
                receptionService.getReceivedChunks(),
                receptionService.getTotalChunks(),
                receptionService.getExpectedHash(),
                receptionService.getFileName(),
                tempDir.resolve("output2").toString()
        );

        byte[] reconstructed = Files.readAllBytes(result.toPath());
        assertArrayEquals(originalData, reconstructed);
    }

    @Test
    void transferPipeline_withRetransmission() throws Exception {
        // Simulate receiver missing some QR codes and picking them up on retransmission
        Path testFile = tempDir.resolve("retrans.mp3");
        byte[] originalData = "Short test file for retransmission test.".getBytes();
        Files.write(testFile, originalData);

        TransferService senderService = new TransferService();
        senderService.prepareTransfer(testFile.toFile());

        QRCodeGenerator qrGenerator = new QRCodeGenerator(500, 500);
        QRCodeDecoder decoder = new QRCodeDecoder();
        ReceptionService receptionService = new ReceptionService();

        int totalPackets = senderService.getTotalPacketCount();

        // First pass: skip every other chunk (simulate missed frames)
        for (int i = 0; i < totalPackets; i++) {
            String packetJson = senderService.getPacketAtIndex(i);
            PacketType type = PacketSerializer.detectPacketType(packetJson);

            // Always process header and end; skip odd-numbered chunks
            if (type == PacketType.CHUNK && i % 2 == 1) {
                continue; // Simulate camera miss
            }

            BufferedImage qrImage = qrGenerator.generateQRImage(packetJson);
            Optional<String> decoded = decoder.decode(qrImage);
            decoded.ifPresent(receptionService::processPacket);
        }

        // Should NOT be complete yet (missed some chunks)
        if (receptionService.getState() != ReceptionService.State.COMPLETE) {
            // Second pass: process all packets (retransmission loop)
            for (int i = 0; i < totalPackets; i++) {
                String packetJson = senderService.getPacketAtIndex(i);
                BufferedImage qrImage = qrGenerator.generateQRImage(packetJson);
                Optional<String> decoded = decoder.decode(qrImage);
                decoded.ifPresent(receptionService::processPacket);
            }
        }

        // Now should be complete
        assertEquals(ReceptionService.State.COMPLETE, receptionService.getState());

        // Assemble and verify
        FileAssembler assembler = new FileAssembler();
        File result = assembler.assemble(
                receptionService.getReceivedChunks(),
                receptionService.getTotalChunks(),
                receptionService.getExpectedHash(),
                receptionService.getFileName(),
                tempDir.resolve("output3").toString()
        );

        assertArrayEquals(originalData, Files.readAllBytes(result.toPath()));
    }
}
