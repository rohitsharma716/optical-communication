package com.optical.receiver.assembler;

import com.optical.shared.util.ChunkUtil;
import com.optical.shared.util.CompressionUtil;
import com.optical.shared.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Assembles received chunks back into the original file.
 * Handles: merge → decompress → verify hash → save.
 */
public class FileAssembler {

    private static final Logger log = LoggerFactory.getLogger(FileAssembler.class);

    /**
     * Assemble chunks into the original file.
     *
     * @param chunks       map of sequence number → raw chunk bytes (1-indexed)
     * @param totalChunks  expected total number of chunks
     * @param expectedHash expected SHA-256 hash of the original (decompressed) file
     * @param fileName     original file name
     * @param outputDir    directory to save the reconstructed file
     * @return the saved File
     * @throws IOException          if file writing fails
     * @throws IllegalStateException if chunks are missing or hash doesn't match
     */
    public File assemble(Map<Integer, byte[]> chunks, int totalChunks,
                         String expectedHash, String fileName, String outputDir)
            throws IOException {

        log.info("Starting file assembly: {} chunks, expected file: {}", totalChunks, fileName);

        // Step 1: Validate all chunks present
        if (chunks.size() != totalChunks) {
            int missing = totalChunks - chunks.size();
            throw new IllegalStateException(
                    "Cannot assemble: " + missing + " chunks still missing (have " +
                    chunks.size() + "/" + totalChunks + ")");
        }

        // Step 2: Merge chunks in sequence order
        log.info("Merging {} chunks...", totalChunks);
        byte[] compressedData = ChunkUtil.mergeChunks(chunks, totalChunks);
        log.info("Merged compressed data: {} bytes", compressedData.length);

        // Step 3: Decompress GZIP
        log.info("Decompressing GZIP data...");
        byte[] originalData = CompressionUtil.decompress(compressedData);
        log.info("Decompressed: {} bytes → {} bytes", compressedData.length, originalData.length);

        // Step 4: Verify SHA-256 hash
        String actualHash = HashUtil.sha256(originalData);
        log.info("Hash verification — expected: {}, actual: {}", expectedHash, actualHash);

        if (!actualHash.equalsIgnoreCase(expectedHash)) {
            throw new IllegalStateException(
                    "SHA-256 hash mismatch! Expected: " + expectedHash + ", Got: " + actualHash);
        }
        log.info("✓ SHA-256 hash verified successfully");

        // Step 5: Save file
        Path outputPath = Path.of(outputDir);
        Files.createDirectories(outputPath);

        File outputFile = outputPath.resolve(fileName).toFile();

        // Avoid overwriting — add suffix if file exists
        if (outputFile.exists()) {
            String baseName = fileName.contains(".")
                    ? fileName.substring(0, fileName.lastIndexOf('.'))
                    : fileName;
            String extension = fileName.contains(".")
                    ? fileName.substring(fileName.lastIndexOf('.'))
                    : "";
            int counter = 1;
            do {
                outputFile = outputPath.resolve(baseName + "_" + counter + extension).toFile();
                counter++;
            } while (outputFile.exists());
        }

        Files.write(outputFile.toPath(), originalData);
        log.info("✓ File saved: {} ({} bytes)", outputFile.getAbsolutePath(), originalData.length);

        return outputFile;
    }
}
