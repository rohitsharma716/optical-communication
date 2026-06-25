package com.optical.shared.util;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GZIP compression/decompression utility.
 */
class CompressionUtilTest {

    @Test
    void compressDecompress_roundTrip() {
        byte[] original = "Hello, this is test data for compression!".getBytes();

        byte[] compressed = CompressionUtil.compress(original);
        assertNotNull(compressed);
        assertTrue(compressed.length > 0);

        byte[] decompressed = CompressionUtil.decompress(compressed);
        assertArrayEquals(original, decompressed);
    }

    @Test
    void compressDecompress_largeData() {
        byte[] original = new byte[100_000]; // 100 KB
        new Random(42).nextBytes(original);

        byte[] compressed = CompressionUtil.compress(original);
        byte[] decompressed = CompressionUtil.decompress(compressed);

        assertArrayEquals(original, decompressed);
    }

    @Test
    void compressDecompress_repetitiveData_compresses() {
        // Highly repetitive data should compress well
        byte[] original = new byte[10_000];
        java.util.Arrays.fill(original, (byte) 'A');

        byte[] compressed = CompressionUtil.compress(original);

        // Compressed should be much smaller than original for repetitive data
        assertTrue(compressed.length < original.length,
                "Repetitive data should compress: " + compressed.length + " vs " + original.length);

        byte[] decompressed = CompressionUtil.decompress(compressed);
        assertArrayEquals(original, decompressed);
    }

    @Test
    void compressDecompress_singleByte() {
        byte[] original = new byte[]{0x42};

        byte[] compressed = CompressionUtil.compress(original);
        byte[] decompressed = CompressionUtil.decompress(compressed);

        assertArrayEquals(original, decompressed);
    }

    @Test
    void compress_producesGzipMagicHeader() {
        byte[] data = "test".getBytes();
        byte[] compressed = CompressionUtil.compress(data);

        // GZIP magic number: 0x1f 0x8b
        assertEquals((byte) 0x1f, compressed[0]);
        assertEquals((byte) 0x8b, compressed[1]);
    }

    @Test
    void decompress_invalidData_throwsException() {
        byte[] invalidGzip = new byte[]{0x00, 0x01, 0x02, 0x03};

        assertThrows(RuntimeException.class,
                () -> CompressionUtil.decompress(invalidGzip));
    }
}
