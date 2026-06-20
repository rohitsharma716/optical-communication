package com.optical.shared.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Chunk packet — carries a single chunk of file data encoded as Base64.
 * Each QR code in the data portion of the transfer contains one of these.
 */
public class ChunkPacket {

    @JsonProperty("transferId")
    private String transferId;

    @JsonProperty("sequence")
    private int sequence;

    @JsonProperty("totalChunks")
    private int totalChunks;

    @JsonProperty("payload")
    private String payload;

    @JsonProperty("type")
    private final String type = "CHUNK";

    // Default constructor for Jackson
    public ChunkPacket() {
    }

    public ChunkPacket(String transferId, int sequence, int totalChunks, String payload) {
        this.transferId = transferId;
        this.sequence = sequence;
        this.totalChunks = totalChunks;
        this.payload = payload;
    }

    public String getTransferId() {
        return transferId;
    }

    public void setTransferId(String transferId) {
        this.transferId = transferId;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "ChunkPacket{" +
                "transferId='" + transferId + '\'' +
                ", sequence=" + sequence +
                ", totalChunks=" + totalChunks +
                ", payloadLength=" + (payload != null ? payload.length() : 0) +
                '}';
    }
}
