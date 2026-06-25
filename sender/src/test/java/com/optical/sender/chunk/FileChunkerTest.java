package com.optical.sender.chunk;

import com.optical.shared.model.ChunkPacket;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FileChunker packet creation.
 */
class FileChunkerTest {

    @Test
    void createChunkPackets_correctCount() {
        byte[] data = new byte[3000];
        FileChunker chunker = new FileChunker(1000);

        List<ChunkPacket> packets = chunker.createChunkPackets(data, "test-id");

        assertEquals(3, packets.size());
    }

    @Test
    void createChunkPackets_correctSequencing() {
        byte[] data = new byte[2500];
        FileChunker chunker = new FileChunker(1000);

        List<ChunkPacket> packets = chunker.createChunkPackets(data, "test-id");

        assertEquals(1, packets.get(0).getSequence());
        assertEquals(2, packets.get(1).getSequence());
        assertEquals(3, packets.get(2).getSequence());
    }

    @Test
    void createChunkPackets_correctTransferId() {
        byte[] data = new byte[1000];
        FileChunker chunker = new FileChunker(1000);
        String transferId = "my-transfer-123";

        List<ChunkPacket> packets = chunker.createChunkPackets(data, transferId);

        for (ChunkPacket p : packets) {
            assertEquals(transferId, p.getTransferId());
        }
    }

    @Test
    void createChunkPackets_correctTotalChunks() {
        byte[] data = new byte[5500];
        FileChunker chunker = new FileChunker(1000);

        List<ChunkPacket> packets = chunker.createChunkPackets(data, "id");

        for (ChunkPacket p : packets) {
            assertEquals(6, p.getTotalChunks());
        }
    }

    @Test
    void createChunkPackets_payloadIsValidBase64() {
        byte[] data = new byte[2000];
        new Random(42).nextBytes(data);
        FileChunker chunker = new FileChunker(1000);

        List<ChunkPacket> packets = chunker.createChunkPackets(data, "id");

        for (ChunkPacket p : packets) {
            assertNotNull(p.getPayload());
            // Should not throw
            byte[] decoded = Base64.getDecoder().decode(p.getPayload());
            assertTrue(decoded.length > 0);
        }
    }

    @Test
    void createChunkPackets_decodedPayloadMatchesOriginal() {
        byte[] data = new byte[2500];
        new Random(99).nextBytes(data);
        FileChunker chunker = new FileChunker(1000);

        List<ChunkPacket> packets = chunker.createChunkPackets(data, "id");

        // Decode first chunk and verify it matches first 1000 bytes
        byte[] decoded0 = Base64.getDecoder().decode(packets.get(0).getPayload());
        assertEquals(1000, decoded0.length);
        for (int i = 0; i < 1000; i++) {
            assertEquals(data[i], decoded0[i]);
        }

        // Last chunk should be 500 bytes
        byte[] decodedLast = Base64.getDecoder().decode(packets.get(2).getPayload());
        assertEquals(500, decodedLast.length);
    }
}
