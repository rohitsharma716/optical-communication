package com.optical.shared.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SHA-256 hash utility.
 */
class HashUtilTest {

    @Test
    void sha256_knownInput() {
        // SHA-256 of empty byte array
        String hash = HashUtil.sha256(new byte[0]);
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);
    }

    @Test
    void sha256_helloWorld() {
        byte[] data = "Hello, World!".getBytes();
        String hash = HashUtil.sha256(data);
        assertEquals("dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f", hash);
    }

    @Test
    void sha256_deterministic() {
        byte[] data = "test data for hashing".getBytes();
        String hash1 = HashUtil.sha256(data);
        String hash2 = HashUtil.sha256(data);
        assertEquals(hash1, hash2, "Same input should produce same hash");
    }

    @Test
    void sha256_differentInputs_differentHashes() {
        String hash1 = HashUtil.sha256("input1".getBytes());
        String hash2 = HashUtil.sha256("input2".getBytes());
        assertNotEquals(hash1, hash2);
    }

    @Test
    void verify_matchingHash() {
        byte[] data = "verify me".getBytes();
        String hash = HashUtil.sha256(data);
        assertTrue(HashUtil.verify(data, hash));
    }

    @Test
    void verify_mismatchedHash() {
        byte[] data = "original data".getBytes();
        assertFalse(HashUtil.verify(data, "0000000000000000000000000000000000000000000000000000000000000000"));
    }

    @Test
    void verify_caseInsensitive() {
        byte[] data = "test".getBytes();
        String hash = HashUtil.sha256(data);
        assertTrue(HashUtil.verify(data, hash.toUpperCase()));
    }

    @Test
    void sha256_outputLength() {
        String hash = HashUtil.sha256("any data".getBytes());
        assertEquals(64, hash.length(), "SHA-256 hex string should be 64 characters");
    }
}
