package com.optical.shared.util;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ChunkUtil split/merge operations.
 */
class ChunkUtilTest {

    @Test
    void splitIntoChunks_exactDivisible() {
        byte[] data = new byte[3000]; // 3000 / 1000 = exactly 3 chunks
        Arrays.fill(data, (byte) 0xAB);

        List<byte[]> chunks = ChunkUtil.splitIntoChunks(data, 1000);

        assertEquals(3, chunks.size());
        for (byte[] chunk : chunks) {
            assertEquals(1000, chunk.length);
        }
    }

    @Test
    void splitIntoChunks_withRemainder() {
        byte[] data = new byte[2500]; // 2500 / 1000 = 2 full + 1 partial (500)
        Arrays.fill(data, (byte) 0xCD);

        List<byte[]> chunks = ChunkUtil.splitIntoChunks(data, 1000);

        assertEquals(3, chunks.size());
        assertEquals(1000, chunks.get(0).length);
        assertEquals(1000, chunks.get(1).length);
        assertEquals(500, chunks.get(2).length);
    }

    @Test
    void splitIntoChunks_singleChunk() {
        byte[] data = new byte[500]; // Less than chunk size
        Arrays.fill(data, (byte) 0xEF);

        List<byte[]> chunks = ChunkUtil.splitIntoChunks(data, 1000);

        assertEquals(1, chunks.size());
        assertEquals(500, chunks.get(0).length);
    }

    @Test
    void splitIntoChunks_exactlyOneChunk() {
        byte[] data = new byte[1000];
        Arrays.fill(data, (byte) 0x01);

        List<byte[]> chunks = ChunkUtil.splitIntoChunks(data, 1000);

        assertEquals(1, chunks.size());
        assertEquals(1000, chunks.get(0).length);
    }

    @Test
    void splitIntoChunks_nullData_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> ChunkUtil.splitIntoChunks(null, 1000));
    }

    @Test
    void splitIntoChunks_emptyData_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> ChunkUtil.splitIntoChunks(new byte[0], 1000));
    }

    @Test
    void splitIntoChunks_zeroChunkSize_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> ChunkUtil.splitIntoChunks(new byte[100], 0));
    }

    @Test
    void mergeChunks_correctOrder() {
        // Create data with identifiable patterns per chunk
        byte[] original = new byte[2500];
        for (int i = 0; i < original.length; i++) {
            original[i] = (byte) (i % 256);
        }

        List<byte[]> chunks = ChunkUtil.splitIntoChunks(original, 1000);

        // Simulate received chunks (1-indexed map)
        Map<Integer, byte[]> receivedChunks = new HashMap<>();
        for (int i = 0; i < chunks.size(); i++) {
            receivedChunks.put(i + 1, chunks.get(i));
        }

        byte[] merged = ChunkUtil.mergeChunks(receivedChunks, chunks.size());

        assertArrayEquals(original, merged);
    }

    @Test
    void mergeChunks_outOfOrder() {
        byte[] original = new byte[3000];
        new Random(42).nextBytes(original);

        List<byte[]> chunks = ChunkUtil.splitIntoChunks(original, 1000);

        // Add chunks in reverse order
        Map<Integer, byte[]> receivedChunks = new HashMap<>();
        receivedChunks.put(3, chunks.get(2));
        receivedChunks.put(1, chunks.get(0));
        receivedChunks.put(2, chunks.get(1));

        byte[] merged = ChunkUtil.mergeChunks(receivedChunks, 3);

        assertArrayEquals(original, merged);
    }

    @Test
    void mergeChunks_missingChunk_throwsException() {
        Map<Integer, byte[]> chunks = new HashMap<>();
        chunks.put(1, new byte[100]);
        chunks.put(3, new byte[100]);
        // Missing chunk 2

        assertThrows(IllegalStateException.class,
                () -> ChunkUtil.mergeChunks(chunks, 3));
    }

    @Test
    void splitAndMerge_roundTrip() {
        // Large data roundtrip
        byte[] original = new byte[10240]; // 10 KB
        new Random(123).nextBytes(original);

        List<byte[]> chunks = ChunkUtil.splitIntoChunks(original, 1000);

        Map<Integer, byte[]> chunkMap = new HashMap<>();
        for (int i = 0; i < chunks.size(); i++) {
            chunkMap.put(i + 1, chunks.get(i));
        }

        byte[] roundTripped = ChunkUtil.mergeChunks(chunkMap, chunks.size());

        assertArrayEquals(original, roundTripped);
    }
}
