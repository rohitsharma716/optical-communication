package com.optical.receiver.assembler;

import com.optical.shared.util.CompressionUtil;
import com.optical.shared.util.HashUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FileAssembler reassembly and verification.
 */
class FileAssemblerTest {

    @TempDir
    Path tempDir;

    @Test
    void assemble_correctData() throws IOException {
        // Simulate the sender pipeline
        byte[] originalData = "Hello, this is test MP3 content!".getBytes();
        byte[] compressed = CompressionUtil.compress(originalData);
        String hash = HashUtil.sha256(originalData);

        // Split into chunks
        int chunkSize = 10;
        Map<Integer, byte[]> chunks = new HashMap<>();
        int seq = 1;
        for (int i = 0; i < compressed.length; i += chunkSize) {
            int len = Math.min(chunkSize, compressed.length - i);
            byte[] chunk = new byte[len];
            System.arraycopy(compressed, i, chunk, 0, len);
            chunks.put(seq++, chunk);
        }

        int totalChunks = chunks.size();

        FileAssembler assembler = new FileAssembler();
        File result = assembler.assemble(chunks, totalChunks, hash, "test.mp3",
                tempDir.toString());

        assertTrue(result.exists());
        byte[] savedData = Files.readAllBytes(result.toPath());
        assertArrayEquals(originalData, savedData);
    }

    @Test
    void assemble_hashMismatch_throwsException() {
        byte[] originalData = "test data".getBytes();
        byte[] compressed = CompressionUtil.compress(originalData);

        Map<Integer, byte[]> chunks = new HashMap<>();
        chunks.put(1, compressed);

        FileAssembler assembler = new FileAssembler();

        assertThrows(IllegalStateException.class, () ->
                assembler.assemble(chunks, 1, "wrong_hash_value", "test.mp3",
                        tempDir.toString()));
    }

    @Test
    void assemble_missingChunks_throwsException() {
        Map<Integer, byte[]> chunks = new HashMap<>();
        chunks.put(1, new byte[10]);
        // Missing chunk 2

        FileAssembler assembler = new FileAssembler();

        assertThrows(IllegalStateException.class, () ->
                assembler.assemble(chunks, 2, "hash", "test.mp3",
                        tempDir.toString()));
    }

    @Test
    void assemble_largeFile() throws IOException {
        byte[] originalData = new byte[50_000]; // 50 KB
        new Random(42).nextBytes(originalData);

        byte[] compressed = CompressionUtil.compress(originalData);
        String hash = HashUtil.sha256(originalData);

        // Chunk at 1000 bytes
        Map<Integer, byte[]> chunks = new HashMap<>();
        int seq = 1;
        for (int i = 0; i < compressed.length; i += 1000) {
            int len = Math.min(1000, compressed.length - i);
            byte[] chunk = new byte[len];
            System.arraycopy(compressed, i, chunk, 0, len);
            chunks.put(seq++, chunk);
        }

        FileAssembler assembler = new FileAssembler();
        File result = assembler.assemble(chunks, chunks.size(), hash, "large.mp3",
                tempDir.toString());

        assertTrue(result.exists());
        byte[] savedData = Files.readAllBytes(result.toPath());
        assertArrayEquals(originalData, savedData);
    }

    @Test
    void assemble_duplicateFileName_createsSuffix() throws IOException {
        byte[] data = "test".getBytes();
        byte[] compressed = CompressionUtil.compress(data);
        String hash = HashUtil.sha256(data);

        Map<Integer, byte[]> chunks = new HashMap<>();
        chunks.put(1, compressed);

        FileAssembler assembler = new FileAssembler();

        // First save
        File first = assembler.assemble(chunks, 1, hash, "dup.mp3", tempDir.toString());
        assertTrue(first.getName().equals("dup.mp3"));

        // Second save with same name — should get suffix
        File second = assembler.assemble(chunks, 1, hash, "dup.mp3", tempDir.toString());
        assertTrue(second.getName().contains("dup_1.mp3"));
    }
}
