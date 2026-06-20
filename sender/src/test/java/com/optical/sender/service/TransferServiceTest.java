package com.optical.sender.service;

import com.optical.shared.model.PacketType;
import com.optical.shared.protocol.PacketSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TransferService end-to-end packet generation.
 */
class TransferServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void prepareTransfer_createsCorrectPacketSequence() throws IOException {
        // Create test file
        File testFile = tempDir.resolve("test.mp3").toFile();
        byte[] testData = new byte[5000];
        new Random(42).nextBytes(testData);
        Files.write(testFile.toPath(), testData);

        TransferService service = new TransferService();
        service.prepareTransfer(testFile);

        // Should have: 1 header + N chunks + 1 end
        int totalPackets = service.getTotalPacketCount();
        assertTrue(totalPackets > 2, "Should have header + chunks + end");
        assertEquals(totalPackets, service.getChunkCount() + 2);

        // First packet should be HEADER
        String firstPacket = service.getPacketAtIndex(0);
        assertEquals(PacketType.HEADER, PacketSerializer.detectPacketType(firstPacket));

        // Last packet should be END
        String lastPacket = service.getPacketAtIndex(totalPackets - 1);
        assertEquals(PacketType.END, PacketSerializer.detectPacketType(lastPacket));

        // Middle packets should be CHUNK
        for (int i = 1; i < totalPackets - 1; i++) {
            String packet = service.getPacketAtIndex(i);
            assertEquals(PacketType.CHUNK, PacketSerializer.detectPacketType(packet),
                    "Packet at index " + i + " should be CHUNK");
        }
    }

    @Test
    void prepareTransfer_loopsCorrectly() throws IOException {
        File testFile = tempDir.resolve("small.mp3").toFile();
        Files.write(testFile.toPath(), new byte[500]);

        TransferService service = new TransferService();
        service.prepareTransfer(testFile);

        int totalPackets = service.getTotalPacketCount();

        // Accessing beyond total should loop
        String first = service.getPacketAtIndex(0);
        String looped = service.getPacketAtIndex(totalPackets);

        assertEquals(first, looped, "Should loop back to first packet");
    }

    @Test
    void prepareTransfer_setsMetadata() throws IOException {
        File testFile = tempDir.resolve("myfile.mp3").toFile();
        byte[] data = new byte[3000];
        Files.write(testFile.toPath(), data);

        TransferService service = new TransferService();
        service.prepareTransfer(testFile);

        assertEquals("myfile.mp3", service.getFileName());
        assertEquals(3000, service.getOriginalFileSize());
        assertNotNull(service.getTransferId());
        assertTrue(service.getTotalChunks() > 0);
    }

    @Test
    void prepareTransfer_notPrepared_throwsException() {
        TransferService service = new TransferService();
        assertThrows(IllegalStateException.class,
                () -> service.getPacketAtIndex(0));
    }
}
