package com.optical.shared.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 hash utility for file integrity verification.
 */
public class HashUtil {

    private static final String ALGORITHM = "SHA-256";

    /**
     * Compute SHA-256 hash of the given data.
     *
     * @param data raw bytes to hash
     * @return lowercase hex-encoded SHA-256 hash string
     */
    public static String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(data);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available in every JDK
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verify that the given data matches the expected SHA-256 hash.
     *
     * @param data         raw bytes to verify
     * @param expectedHash expected lowercase hex-encoded SHA-256 hash
     * @return true if the hash matches
     */
    public static boolean verify(byte[] data, String expectedHash) {
        String actualHash = sha256(data);
        return actualHash.equalsIgnoreCase(expectedHash);
    }
}
