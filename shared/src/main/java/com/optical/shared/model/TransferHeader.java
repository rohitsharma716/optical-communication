package com.optical.shared.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Transfer header packet — sent as the first QR code in a transfer session.
 * Contains metadata about the file being transferred.
 */
public class TransferHeader {

    @JsonProperty("transferId")
    private String transferId;

    @JsonProperty("fileName")
    private String fileName;

    @JsonProperty("fileSize")
    private long fileSize;

    @JsonProperty("totalChunks")
    private int totalChunks;

    @JsonProperty("hash")
    private String hash;

    @JsonProperty("type")
    private final String type = "HEADER";

    // Default constructor for Jackson
    public TransferHeader() {
    }

    public TransferHeader(String transferId, String fileName, long fileSize, int totalChunks, String hash) {
        this.transferId = transferId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.totalChunks = totalChunks;
        this.hash = hash;
    }

    public String getTransferId() {
        return transferId;
    }

    public void setTransferId(String transferId) {
        this.transferId = transferId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "TransferHeader{" +
                "transferId='" + transferId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +
                ", totalChunks=" + totalChunks +
                ", hash='" + hash + '\'' +
                '}';
    }
}
