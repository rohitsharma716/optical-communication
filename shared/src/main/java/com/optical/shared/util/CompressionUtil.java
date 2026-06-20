package com.optical.shared.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * GZIP compression and decompression utility.
 * Used to reduce file size before chunking for QR transfer.
 */
public class CompressionUtil {

    private static final int BUFFER_SIZE = 4096;

    /**
     * Compress data using GZIP.
     *
     * @param data raw bytes to compress
     * @return GZIP-compressed bytes
     */
    public static byte[] compress(byte[] data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(data);
            gzos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("GZIP compression failed", e);
        }
    }

    /**
     * Decompress GZIP-compressed data.
     *
     * @param compressedData GZIP-compressed bytes
     * @return decompressed raw bytes
     */
    public static byte[] decompress(byte[] compressedData) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzis = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = gzis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("GZIP decompression failed", e);
        }
    }
}
