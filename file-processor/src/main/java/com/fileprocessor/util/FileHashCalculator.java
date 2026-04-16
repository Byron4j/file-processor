package com.fileprocessor.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * File hash calculator utility for MD5, SHA-1, SHA-256, SHA-512
 */
public class FileHashCalculator {

    private static final Logger log = LoggerFactory.getLogger(FileHashCalculator.class);
    private static final int BUFFER_SIZE = 8192; // 8KB buffer

    /**
     * Hash algorithm types
     */
    public enum HashAlgorithm {
        MD5("MD5"),
        SHA_1("SHA-1"),
        SHA_256("SHA-256"),
        SHA_384("SHA-384"),
        SHA_512("SHA-512");

        private final String algorithmName;

        HashAlgorithm(String algorithmName) {
            this.algorithmName = algorithmName;
        }

        public String getAlgorithmName() {
            return algorithmName;
        }
    }

    /**
     * Hash result holder
     */
    public static class HashResult {
        private final String filePath;
        private final Map<HashAlgorithm, String> hashes;

        public HashResult(String filePath) {
            this.filePath = filePath;
            this.hashes = new HashMap<>();
        }

        public void addHash(HashAlgorithm algorithm, String hash) {
            hashes.put(algorithm, hash);
        }

        public String getFilePath() {
            return filePath;
        }

        public Map<HashAlgorithm, String> getHashes() {
            return hashes;
        }

        public String getHash(HashAlgorithm algorithm) {
            return hashes.get(algorithm);
        }
    }

    /**
     * Calculate single hash for file
     *
     * @param filePath Path to file
     * @param algorithm Hash algorithm
     * @return Hash string (hex), or null if failed
     */
    public static String calculateHash(String filePath, HashAlgorithm algorithm) {
        log.info("Calculating {} hash for: {}", algorithm.getAlgorithmName(), filePath);

        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm.getAlgorithmName());

            try (InputStream is = Files.newInputStream(Paths.get(filePath));
                 BufferedInputStream bis = new BufferedInputStream(is, BUFFER_SIZE)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            String hash = bytesToHex(digest.digest());
            log.info("{} hash calculated: {}", algorithm.getAlgorithmName(), hash);
            return hash;

        } catch (NoSuchAlgorithmException e) {
            log.error("Algorithm not available: {}", algorithm.getAlgorithmName(), e);
            return null;
        } catch (IOException e) {
            log.error("Failed to read file: {}", filePath, e);
            return null;
        }
    }

    /**
     * Calculate multiple hashes for file in single pass
     *
     * @param filePath Path to file
     * @param algorithms List of hash algorithms
     * @return HashResult containing all hashes
     */
    public static HashResult calculateHashes(String filePath, List<HashAlgorithm> algorithms) {
        log.info("Calculating {} hashes for: {}", algorithms.size(), filePath);

        HashResult result = new HashResult(filePath);

        try {
            // Initialize all requested digests
            Map<HashAlgorithm, MessageDigest> digests = new HashMap<>();
            for (HashAlgorithm algorithm : algorithms) {
                digests.put(algorithm, MessageDigest.getInstance(algorithm.getAlgorithmName()));
            }

            // Read file once and update all digests
            try (InputStream is = Files.newInputStream(Paths.get(filePath));
                 BufferedInputStream bis = new BufferedInputStream(is, BUFFER_SIZE)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    for (MessageDigest digest : digests.values()) {
                        digest.update(buffer, 0, bytesRead);
                    }
                }
            }

            // Convert all digests to hex strings
            for (Map.Entry<HashAlgorithm, MessageDigest> entry : digests.entrySet()) {
                String hash = bytesToHex(entry.getValue().digest());
                result.addHash(entry.getKey(), hash);
                log.debug("{} hash: {}", entry.getKey().getAlgorithmName(), hash);
            }

            log.info("All hashes calculated for: {}", filePath);
            return result;

        } catch (NoSuchAlgorithmException e) {
            log.error("Algorithm not available", e);
            return null;
        } catch (IOException e) {
            log.error("Failed to read file: {}", filePath, e);
            return null;
        }
    }

    /**
     * Calculate all supported hashes (MD5, SHA-1, SHA-256)
     *
     * @param filePath Path to file
     * @return HashResult containing all hashes
     */
    public static HashResult calculateAllHashes(String filePath) {
        return calculateHashes(filePath, List.of(
                HashAlgorithm.MD5,
                HashAlgorithm.SHA_1,
                HashAlgorithm.SHA_256
        ));
    }

    /**
     * Verify file hash
     *
     * @param filePath Path to file
     * @param algorithm Hash algorithm
     * @param expectedHash Expected hash value
     * @return true if hash matches, false otherwise
     */
    public static boolean verifyHash(String filePath, HashAlgorithm algorithm, String expectedHash) {
        log.info("Verifying {} hash for: {}", algorithm.getAlgorithmName(), filePath);

        String actualHash = calculateHash(filePath, algorithm);

        if (actualHash == null) {
            return false;
        }

        boolean matches = actualHash.equalsIgnoreCase(expectedHash);
        log.info("Hash verification result: {} (expected: {}, actual: {})",
                matches ? "MATCH" : "MISMATCH", expectedHash, actualHash);

        return matches;
    }

    /**
     * Calculate hash for string content
     *
     * @param content String content
     * @param algorithm Hash algorithm
     * @return Hash string
     */
    public static String calculateStringHash(String content, HashAlgorithm algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm.getAlgorithmName());
            byte[] hashBytes = digest.digest(content.getBytes());
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("Algorithm not available: {}", algorithm.getAlgorithmName(), e);
            return null;
        }
    }

    /**
     * Calculate hash for byte array
     *
     * @param data Byte array
     * @param algorithm Hash algorithm
     * @return Hash string
     */
    public static String calculateBytesHash(byte[] data, HashAlgorithm algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm.getAlgorithmName());
            byte[] hashBytes = digest.digest(data);
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("Algorithm not available: {}", algorithm.getAlgorithmName(), e);
            return null;
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Convert byte array to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString().toLowerCase();
    }

    /**
     * Parse hash algorithm from string
     *
     * @param algorithmName Algorithm name (e.g., "MD5", "SHA-256")
     * @return HashAlgorithm enum value, or null if not found
     */
    public static HashAlgorithm parseAlgorithm(String algorithmName) {
        if (algorithmName == null) return null;

        String upper = algorithmName.toUpperCase().replace("-", "_");
        try {
            return HashAlgorithm.valueOf(upper);
        } catch (IllegalArgumentException e) {
            // Try matching by algorithm name
            for (HashAlgorithm algo : HashAlgorithm.values()) {
                if (algo.getAlgorithmName().equalsIgnoreCase(algorithmName)) {
                    return algo;
                }
            }
            return null;
        }
    }

    /**
     * Get all supported algorithm names
     */
    public static String[] getSupportedAlgorithms() {
        return new String[]{
                HashAlgorithm.MD5.getAlgorithmName(),
                HashAlgorithm.SHA_1.getAlgorithmName(),
                HashAlgorithm.SHA_256.getAlgorithmName(),
                HashAlgorithm.SHA_384.getAlgorithmName(),
                HashAlgorithm.SHA_512.getAlgorithmName()
        };
    }

    /**
     * Check if algorithm is supported
     */
    public static boolean isSupportedAlgorithm(String algorithmName) {
        return parseAlgorithm(algorithmName) != null;
    }
}
