package com.optical.receiver.service;

import com.optical.shared.model.ChunkPacket;
import com.optical.shared.model.PacketType;
import com.optical.shared.model.TransferEnd;
import com.optical.shared.model.TransferHeader;
import com.optical.shared.protocol.PacketSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages the file reception state machine.
 * Thread-safe: the camera thread writes packets, the UI thread reads state.
 *
 * States: IDLE → RECEIVING → COMPLETE / ERROR
 */
public class ReceptionService {

    private static final Logger log = LoggerFactory.getLogger(ReceptionService.class);

    public enum State {
        IDLE,
        RECEIVING,
        COMPLETE,
        ERROR
    }

    // Transfer metadata (set on HEADER)
    private volatile String transferId;
    private volatile String fileName;
    private volatile long fileSize;
    private volatile int totalChunks;
    private volatile String expectedHash;

    // Chunk storage
    private final ConcurrentHashMap<Integer, byte[]> receivedChunks = new ConcurrentHashMap<>();
    private final Set<Integer> receivedSequences = ConcurrentHashMap.newKeySet();

    // State
    private volatile State state = State.IDLE;
    private volatile String errorMessage;
    private volatile boolean endReceived = false;

    // Callbacks
    private Consumer<State> stateChangeListener;
    private Runnable chunkReceivedListener;

    /**
     * Process a decoded QR code JSON string.
     * Routes to appropriate handler based on packet type.
     *
     * @param json the decoded JSON from a QR code
     */
    public void processPacket(String json) {
        if (json == null || json.isBlank()) {
            return;
        }

        try {
            PacketType type = PacketSerializer.detectPacketType(json);

            switch (type) {
                case HEADER -> processHeader(json);
                case CHUNK -> processChunk(json);
                case END -> processEnd(json);
                case UNKNOWN -> log.debug("Ignoring unknown packet type");
            }
        } catch (Exception e) {
            log.warn("Error processing packet: {}", e.getMessage());
        }
    }

    private void processHeader(String json) {
        TransferHeader header = PacketSerializer.deserializeHeader(json);

        if (state == State.IDLE || !header.getTransferId().equals(transferId)) {
            // New transfer session
            this.transferId = header.getTransferId();
            this.fileName = header.getFileName();
            this.fileSize = header.getFileSize();
            this.totalChunks = header.getTotalChunks();
            this.expectedHash = header.getHash();
            this.endReceived = false;

            // Don't clear chunks if this is a retransmission of the same transfer
            if (state == State.IDLE) {
                receivedChunks.clear();
                receivedSequences.clear();
            }

            setState(State.RECEIVING);
            log.info("Transfer header received — ID: {}, File: {}, Size: {}, Chunks: {}",
                    transferId, fileName, fileSize, totalChunks);
        }
    }

    private void processChunk(String json) {
        if (state != State.RECEIVING) {
            return;
        }

        ChunkPacket chunk = PacketSerializer.deserializeChunk(json);

        // Validate transferId
        if (!chunk.getTransferId().equals(transferId)) {
            log.debug("Ignoring chunk with mismatched transferId: {}", chunk.getTransferId());
            return;
        }

        int seq = chunk.getSequence();

        // Ignore duplicates
        if (receivedSequences.contains(seq)) {
            return;
        }

        try {
            // Decode Base64 payload
            byte[] chunkData = Base64.getDecoder().decode(chunk.getPayload());
            receivedChunks.put(seq, chunkData);
            receivedSequences.add(seq);

            log.debug("Received chunk {}/{}", seq, totalChunks);

            // Notify listener
            if (chunkReceivedListener != null) {
                chunkReceivedListener.run();
            }

            // Check if all chunks received
            if (receivedChunks.size() == totalChunks) {
                setState(State.COMPLETE);
                log.info("All {} chunks received! Ready for assembly.", totalChunks);
            }

        } catch (IllegalArgumentException e) {
            log.warn("Invalid Base64 payload in chunk {}: {}", seq, e.getMessage());
        }
    }

    private void processEnd(String json) {
        TransferEnd end = PacketSerializer.deserializeEnd(json);

        if (end.getTransferId().equals(transferId)) {
            endReceived = true;
            log.info("Transfer END packet received. Chunks: {}/{}",
                    receivedChunks.size(), totalChunks);

            if (receivedChunks.size() == totalChunks) {
                setState(State.COMPLETE);
            }
        }
    }

    private void setState(State newState) {
        this.state = newState;
        log.info("State changed to: {}", newState);
        if (stateChangeListener != null) {
            stateChangeListener.accept(newState);
        }
    }

    /**
     * Reset the service for a new transfer.
     */
    public void reset() {
        state = State.IDLE;
        transferId = null;
        fileName = null;
        fileSize = 0;
        totalChunks = 0;
        expectedHash = null;
        endReceived = false;
        receivedChunks.clear();
        receivedSequences.clear();
        log.info("Reception service reset");
    }

    // --- Getters ---

    public State getState() {
        return state;
    }

    public String getTransferId() {
        return transferId;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public String getExpectedHash() {
        return expectedHash;
    }

    public int getReceivedChunkCount() {
        return receivedChunks.size();
    }

    public int getMissingChunkCount() {
        if (totalChunks == 0) return 0;
        return totalChunks - receivedChunks.size();
    }

    public Map<Integer, byte[]> getReceivedChunks() {
        return Collections.unmodifiableMap(receivedChunks);
    }

    public boolean isEndReceived() {
        return endReceived;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    // --- Listeners ---

    public void setStateChangeListener(Consumer<State> listener) {
        this.stateChangeListener = listener;
    }

    public void setChunkReceivedListener(Runnable listener) {
        this.chunkReceivedListener = listener;
    }
}
