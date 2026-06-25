package com.optical.shared.model;

/**
 * Enum representing the type of packet detected from a QR code's JSON content.
 */
public enum PacketType {
    HEADER,
    CHUNK,
    END,
    UNKNOWN
}
