package com.optical.sender.service;

import com.optical.sender.chunk.FileChunker;
import com.optical.sender.util.TransferConfig;
import com.optical.shared.model.ChunkPacket;
import com.optical.shared.model.TransferEnd;
import com.optical.shared.model.TransferHeader;
import com.optical.shared.protocol.PacketSerializer;
import com.optical.shared.util.CompressionUtil;
import com.optical.shared.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core transfer service for the sender.
 * Reads a file, compresses it, creates the transfer protocol packets,
 * and provides them as serialized JSON strings in display order.
 */
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private String transferId;
    private String fileName;
    private long originalFileSize;
    private int totalChunks;
    private List<String> packetSequence; // All JSON packets in display order

    /**
     * Prepare a file for transfer.
     * This processes the file through: read → hash → compress → chunk → serialize.
     *
     * @param file the file to transfer
     * @throws IOException if the file cannot be read
     */
    public void prepareTransfer(File file) throws IOException {
        log.info("Preparing transfer for file: {}", file.getName());

        // Step 1: Read file bytes
        byte[] originalBytes = Files.readAllBytes(file.toPath());
        this.fileName = file.getName();
        this.originalFileSize = originalBytes.length;
        log.info("Read {} bytes from {}", originalFileSize, fileName);

        // Step 2: Compute SHA-256 hash of original file
        String hash = HashUtil.sha256(originalBytes);
        log.info("SHA-256 hash: {}", hash);

        // Step 3: GZIP compress
        byte[] compressedBytes = CompressionUtil.compress(originalBytes);
        double ratio = (compressedBytes.length * 100.0) / originalFileSize;
        log.info("Compressed: {} bytes → {} bytes (ratio: {}%)",
                originalFileSize, compressedBytes.length,
                String.format("%.1f", ratio));

        // Step 4: Generate unique transfer ID
        this.transferId = UUID.randomUUID().toString();
        log.info("Transfer ID: {}", transferId);

        // Step 5: Split into chunks and create packets
        FileChunker chunker = new FileChunker(TransferConfig.CHUNK_SIZE);
        List<ChunkPacket> chunkPackets = chunker.createChunkPackets(compressedBytes, transferId);
        this.totalChunks = chunkPackets.size();

        // Step 6: Create header packet
        TransferHeader header = new TransferHeader(transferId, fileName, originalFileSize, totalChunks, hash);

        // Step 7: Create end packet
        TransferEnd end = new TransferEnd(transferId);

        // Step 8: Build the complete packet sequence (HEADER → CHUNKS → END)
        packetSequence = new ArrayList<>();
        packetSequence.add(PacketSerializer.serialize(header));

        for (ChunkPacket chunk : chunkPackets) {
            packetSequence.add(PacketSerializer.serialize(chunk));
        }

        packetSequence.add(PacketSerializer.serialize(end));

        log.info("Transfer prepared: {} total packets (1 header + {} chunks + 1 end)",
                packetSequence.size(), totalChunks);
    }

    /**
     * Get the JSON packet at the given index in the display sequence.
     * The sequence loops: after the last packet (END), it wraps back to the header.
     *
     * @param displayIndex the absolute display index (can exceed sequence length for looping)
     * @return the JSON string for the packet at the looped index
     */
    public String getPacketAtIndex(int displayIndex) {
        if (packetSequence == null || packetSequence.isEmpty()) {
            throw new IllegalStateException("Transfer not prepared");
        }
        int index = displayIndex % packetSequence.size();
        return packetSequence.get(index);
    }

    /**
     * @return total number of packets in one complete cycle (header + chunks + end)
     */
    public int getTotalPacketCount() {
        return packetSequence != null ? packetSequence.size() : 0;
    }

    public String getTransferId() {
        return transferId;
    }

    public String getFileName() {
        return fileName;
    }

    public long getOriginalFileSize() {
        return originalFileSize;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    /**
     * @return the number of chunk packets only (excludes header and end)
     */
    public int getChunkCount() {
        return totalChunks;
    }
}
