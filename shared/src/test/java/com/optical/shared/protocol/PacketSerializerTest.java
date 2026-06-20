package com.optical.shared.protocol;

import com.optical.shared.model.ChunkPacket;
import com.optical.shared.model.PacketType;
import com.optical.shared.model.TransferEnd;
import com.optical.shared.model.TransferHeader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PacketSerializer serialization/deserialization and type detection.
 */
class PacketSerializerTest {

    @Test
    void serializeAndDeserialize_header() {
        TransferHeader header = new TransferHeader(
                "test-uuid-1234", "song.mp3", 5242880, 2500,
                "abc123def456");

        String json = PacketSerializer.serialize(header);
        assertNotNull(json);
        assertTrue(json.contains("test-uuid-1234"));
        assertTrue(json.contains("song.mp3"));

        TransferHeader deserialized = PacketSerializer.deserializeHeader(json);
        assertEquals("test-uuid-1234", deserialized.getTransferId());
        assertEquals("song.mp3", deserialized.getFileName());
        assertEquals(5242880, deserialized.getFileSize());
        assertEquals(2500, deserialized.getTotalChunks());
        assertEquals("abc123def456", deserialized.getHash());
    }

    @Test
    void serializeAndDeserialize_chunk() {
        ChunkPacket chunk = new ChunkPacket(
                "test-uuid-1234", 15, 2500, "SGVsbG8gV29ybGQ=");

        String json = PacketSerializer.serialize(chunk);
        assertNotNull(json);

        ChunkPacket deserialized = PacketSerializer.deserializeChunk(json);
        assertEquals("test-uuid-1234", deserialized.getTransferId());
        assertEquals(15, deserialized.getSequence());
        assertEquals(2500, deserialized.getTotalChunks());
        assertEquals("SGVsbG8gV29ybGQ=", deserialized.getPayload());
    }

    @Test
    void serializeAndDeserialize_end() {
        TransferEnd end = new TransferEnd("test-uuid-1234");

        String json = PacketSerializer.serialize(end);
        assertNotNull(json);

        TransferEnd deserialized = PacketSerializer.deserializeEnd(json);
        assertEquals("test-uuid-1234", deserialized.getTransferId());
        assertEquals("END", deserialized.getType());
    }

    @Test
    void detectPacketType_header() {
        TransferHeader header = new TransferHeader(
                "id", "file.mp3", 1000, 10, "hash");
        String json = PacketSerializer.serialize(header);

        assertEquals(PacketType.HEADER, PacketSerializer.detectPacketType(json));
    }

    @Test
    void detectPacketType_chunk() {
        ChunkPacket chunk = new ChunkPacket("id", 1, 10, "data");
        String json = PacketSerializer.serialize(chunk);

        assertEquals(PacketType.CHUNK, PacketSerializer.detectPacketType(json));
    }

    @Test
    void detectPacketType_end() {
        TransferEnd end = new TransferEnd("id");
        String json = PacketSerializer.serialize(end);

        assertEquals(PacketType.END, PacketSerializer.detectPacketType(json));
    }

    @Test
    void detectPacketType_invalidJson() {
        assertEquals(PacketType.UNKNOWN, PacketSerializer.detectPacketType("not json"));
    }

    @Test
    void detectPacketType_emptyJson() {
        assertEquals(PacketType.UNKNOWN, PacketSerializer.detectPacketType("{}"));
    }

    @Test
    void deserialize_unknownFields_ignored() {
        // JSON with extra fields should not cause deserialization failure
        String json = """
                {"transferId":"id","fileName":"f.mp3","fileSize":100,
                 "totalChunks":5,"hash":"h","type":"HEADER","extraField":"ignored"}
                """;

        TransferHeader header = PacketSerializer.deserializeHeader(json);
        assertEquals("id", header.getTransferId());
        assertEquals("f.mp3", header.getFileName());
    }
}
