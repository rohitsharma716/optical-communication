package com.optical.sender.chunk;

import com.optical.shared.model.ChunkPacket;
import com.optical.shared.util.ChunkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Splits compressed file data into chunks and creates ChunkPacket objects.
 * Each chunk is Base64-encoded and assigned a 1-indexed sequence number.
 */
public class FileChunker {

    private static final Logger log = LoggerFactory.getLogger(FileChunker.class);

    private final int chunkSize;

    public FileChunker(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    /**
     * Split compressed data into ChunkPacket objects ready for QR serialization.
     *
     * @param compressedData the GZIP-compressed file data
     * @param transferId     unique transfer session ID
     * @return ordered list of ChunkPackets (1-indexed sequence numbers)
     */
    public List<ChunkPacket> createChunkPackets(byte[] compressedData, String transferId) {
        List<byte[]> rawChunks = ChunkUtil.splitIntoChunks(compressedData, chunkSize);
        int totalChunks = rawChunks.size();

        log.info("Splitting {} bytes into {} chunks of max {} bytes each",
                compressedData.length, totalChunks, chunkSize);

        List<ChunkPacket> packets = new ArrayList<>(totalChunks);
        Base64.Encoder encoder = Base64.getEncoder();

        for (int i = 0; i < rawChunks.size(); i++) {
            String payload = encoder.encodeToString(rawChunks.get(i));
            int sequence = i + 1; // 1-indexed
            ChunkPacket packet = new ChunkPacket(transferId, sequence, totalChunks, payload);
            packets.add(packet);
        }

        log.info("Created {} chunk packets for transfer {}", packets.size(), transferId);
        return packets;
    }
}
