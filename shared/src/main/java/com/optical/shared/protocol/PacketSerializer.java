package com.optical.shared.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optical.shared.model.ChunkPacket;
import com.optical.shared.model.PacketType;
import com.optical.shared.model.TransferEnd;
import com.optical.shared.model.TransferHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serializes and deserializes transfer protocol packets to/from JSON strings.
 * Uses Jackson ObjectMapper configured for fault-tolerant deserialization.
 */
public class PacketSerializer {

    private static final Logger log = LoggerFactory.getLogger(PacketSerializer.class);
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Serialize any packet object to a JSON string.
     */
    public static String serialize(Object packet) {
        try {
            return mapper.writeValueAsString(packet);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize packet: {}", packet, e);
            throw new RuntimeException("Serialization failed", e);
        }
    }

    /**
     * Deserialize a JSON string into a TransferHeader.
     */
    public static TransferHeader deserializeHeader(String json) {
        try {
            return mapper.readValue(json, TransferHeader.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize header from: {}", json, e);
            throw new RuntimeException("Header deserialization failed", e);
        }
    }

    /**
     * Deserialize a JSON string into a ChunkPacket.
     */
    public static ChunkPacket deserializeChunk(String json) {
        try {
            return mapper.readValue(json, ChunkPacket.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize chunk from: {}", json, e);
            throw new RuntimeException("Chunk deserialization failed", e);
        }
    }

    /**
     * Deserialize a JSON string into a TransferEnd.
     */
    public static TransferEnd deserializeEnd(String json) {
        try {
            return mapper.readValue(json, TransferEnd.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize end packet from: {}", json, e);
            throw new RuntimeException("End deserialization failed", e);
        }
    }

    /**
     * Detect the type of packet from a JSON string by inspecting key fields.
     *
     * Detection logic:
     * - Has "type":"END"  → END
     * - Has "type":"HEADER" (and "hash") → HEADER
     * - Has "type":"CHUNK" (and "payload") → CHUNK
     * - Otherwise → UNKNOWN
     */
    public static PacketType detectPacketType(String json) {
        try {
            JsonNode node = mapper.readTree(json);

            if (node.has("type")) {
                String type = node.get("type").asText();
                return switch (type) {
                    case "END" -> PacketType.END;
                    case "HEADER" -> PacketType.HEADER;
                    case "CHUNK" -> PacketType.CHUNK;
                    default -> PacketType.UNKNOWN;
                };
            }

            return PacketType.UNKNOWN;
        } catch (JsonProcessingException e) {
            log.warn("Failed to detect packet type from JSON: {}", json, e);
            return PacketType.UNKNOWN;
        }
    }
}
