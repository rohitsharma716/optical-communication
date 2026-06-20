package com.optical.shared.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Transfer end packet — sent as the last QR code in a transfer session.
 * Signals to the receiver that all chunks have been transmitted.
 */
public class TransferEnd {

    @JsonProperty("transferId")
    private String transferId;

    @JsonProperty("type")
    private String type = "END";

    // Default constructor for Jackson
    public TransferEnd() {
    }

    public TransferEnd(String transferId) {
        this.transferId = transferId;
        this.type = "END";
    }

    public String getTransferId() {
        return transferId;
    }

    public void setTransferId(String transferId) {
        this.transferId = transferId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "TransferEnd{" +
                "transferId='" + transferId + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
