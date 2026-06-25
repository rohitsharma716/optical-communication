package com.optical.shared.util;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility for splitting byte arrays into fixed-size chunks and merging them back.
 * Core of the transfer protocol's data segmentation strategy.
 */
public class ChunkUtil {

    /**
     * Split data into fixed-size chunks.
     *
     * @param data      the byte array to split
     * @param chunkSize maximum number of bytes per chunk
     * @return ordered list of byte arrays, each at most chunkSize bytes
     */
    public static List<byte[]> splitIntoChunks(byte[] data, int chunkSize) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data must not be null or empty");
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }

        List<byte[]> chunks = new ArrayList<>();
        int offset = 0;

        while (offset < data.length) {
            int length = Math.min(chunkSize, data.length - offset);
            byte[] chunk = new byte[length];
            System.arraycopy(data, offset, chunk, 0, length);
            chunks.add(chunk);
            offset += length;
        }

        return chunks;
    }

    /**
     * Merge chunks back into a single byte array.
     * Chunks are sorted by their sequence number (map key) before merging.
     *
     * @param chunks      map of sequence number → chunk data (1-indexed)
     * @param totalChunks expected total number of chunks
     * @return merged byte array
     * @throws IllegalStateException if any chunks are missing
     */
    public static byte[] mergeChunks(Map<Integer, byte[]> chunks, int totalChunks) {
        if (chunks.size() != totalChunks) {
            throw new IllegalStateException(
                    "Missing chunks: expected " + totalChunks + ", got " + chunks.size());
        }

        // Use TreeMap to ensure sorted order by sequence number
        TreeMap<Integer, byte[]> sorted = new TreeMap<>(chunks);

        // Verify all sequence numbers are present (1..totalChunks)
        for (int i = 1; i <= totalChunks; i++) {
            if (!sorted.containsKey(i)) {
                throw new IllegalStateException("Missing chunk with sequence: " + i);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 1; i <= totalChunks; i++) {
            byte[] chunk = sorted.get(i);
            baos.write(chunk, 0, chunk.length);
        }

        return baos.toByteArray();
    }
}
