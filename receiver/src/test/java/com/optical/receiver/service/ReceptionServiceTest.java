package com.optical.receiver.service;

import com.optical.shared.model.ChunkPacket;
import com.optical.shared.model.TransferEnd;
import com.optical.shared.model.TransferHeader;
import com.optical.shared.protocol.PacketSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ReceptionService state machine and packet processing.
 */
class ReceptionServiceTest {

    private ReceptionService service;

    @BeforeEach
    void setUp() {
        service = new ReceptionService();
    }

    @Test
    void initialState_isIdle() {
        assertEquals(ReceptionService.State.IDLE, service.getState());
    }

    @Test
    void processHeader_transitionsToReceiving() {
        TransferHeader header = new TransferHeader("id-1", "song.mp3", 5000, 5, "hash123");
        service.processPacket(PacketSerializer.serialize(header));

        assertEquals(ReceptionService.State.RECEIVING, service.getState());
        assertEquals("id-1", service.getTransferId());
        assertEquals("song.mp3", service.getFileName());
        assertEquals(5000, service.getFileSize());
        assertEquals(5, service.getTotalChunks());
        assertEquals("hash123", service.getExpectedHash());
    }

    @Test
    void processChunks_tracksProgress() {
        // Send header first
        TransferHeader header = new TransferHeader("id-1", "test.mp3", 3000, 3, "hash");
        service.processPacket(PacketSerializer.serialize(header));

        // Send chunk 1
        ChunkPacket chunk1 = new ChunkPacket("id-1", 1, 3,
                Base64.getEncoder().encodeToString(new byte[100]));
        service.processPacket(PacketSerializer.serialize(chunk1));

        assertEquals(1, service.getReceivedChunkCount());
        assertEquals(2, service.getMissingChunkCount());
    }

    @Test
    void processAllChunks_transitionsToComplete() {
        TransferHeader header = new TransferHeader("id-1", "test.mp3", 200, 2, "hash");
        service.processPacket(PacketSerializer.serialize(header));

        String payload = Base64.getEncoder().encodeToString(new byte[100]);

        service.processPacket(PacketSerializer.serialize(
                new ChunkPacket("id-1", 1, 2, payload)));
        service.processPacket(PacketSerializer.serialize(
                new ChunkPacket("id-1", 2, 2, payload)));

        assertEquals(ReceptionService.State.COMPLETE, service.getState());
        assertEquals(2, service.getReceivedChunkCount());
        assertEquals(0, service.getMissingChunkCount());
    }

    @Test
    void duplicateChunks_areIgnored() {
        TransferHeader header = new TransferHeader("id-1", "test.mp3", 1000, 3, "hash");
        service.processPacket(PacketSerializer.serialize(header));

        String payload = Base64.getEncoder().encodeToString(new byte[100]);
        ChunkPacket chunk1 = new ChunkPacket("id-1", 1, 3, payload);

        // Send same chunk twice
        service.processPacket(PacketSerializer.serialize(chunk1));
        service.processPacket(PacketSerializer.serialize(chunk1));

        assertEquals(1, service.getReceivedChunkCount());
    }

    @Test
    void wrongTransferId_chunkIgnored() {
        TransferHeader header = new TransferHeader("id-1", "test.mp3", 1000, 1, "hash");
        service.processPacket(PacketSerializer.serialize(header));

        ChunkPacket wrongChunk = new ChunkPacket("wrong-id", 1, 1,
                Base64.getEncoder().encodeToString(new byte[100]));
        service.processPacket(PacketSerializer.serialize(wrongChunk));

        assertEquals(0, service.getReceivedChunkCount());
    }

    @Test
    void processEnd_tracksEndReceived() {
        TransferHeader header = new TransferHeader("id-1", "test.mp3", 1000, 5, "hash");
        service.processPacket(PacketSerializer.serialize(header));

        TransferEnd end = new TransferEnd("id-1");
        service.processPacket(PacketSerializer.serialize(end));

        assertTrue(service.isEndReceived());
    }

    @Test
    void reset_clearsAllState() {
        TransferHeader header = new TransferHeader("id-1", "test.mp3", 1000, 2, "hash");
        service.processPacket(PacketSerializer.serialize(header));

        String payload = Base64.getEncoder().encodeToString(new byte[100]);
        service.processPacket(PacketSerializer.serialize(
                new ChunkPacket("id-1", 1, 2, payload)));

        service.reset();

        assertEquals(ReceptionService.State.IDLE, service.getState());
        assertNull(service.getTransferId());
        assertEquals(0, service.getReceivedChunkCount());
    }

    @Test
    void stateChangeListener_isCalled() {
        AtomicReference<ReceptionService.State> receivedState = new AtomicReference<>();
        service.setStateChangeListener(receivedState::set);

        TransferHeader header = new TransferHeader("id-1", "test.mp3", 1000, 1, "hash");
        service.processPacket(PacketSerializer.serialize(header));

        assertEquals(ReceptionService.State.RECEIVING, receivedState.get());
    }

    @Test
    void processPacket_nullOrBlank_ignored() {
        // Should not throw
        service.processPacket(null);
        service.processPacket("");
        service.processPacket("   ");

        assertEquals(ReceptionService.State.IDLE, service.getState());
    }

    @Test
    void processPacket_invalidJson_ignored() {
        // Should not throw
        service.processPacket("not valid json at all");

        assertEquals(ReceptionService.State.IDLE, service.getState());
    }
}
