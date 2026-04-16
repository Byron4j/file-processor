package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.util.FileHashCalculator;
import com.fileprocessor.util.FileHashCalculator.HashAlgorithm;
import com.fileprocessor.util.FileHashCalculator.HashResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * File verification service for hash calculation and verification
 */
@Service
public class FileVerifyService {

    private static final Logger log = LoggerFactory.getLogger(FileVerifyService.class);

    /**
     * Calculate file hash
     */
    public FileResponse calculateHash(String filePath, String algorithmName) {
        log.info("Service: Calculating {} hash for: {}", algorithmName, filePath);

        // Validate file
        File file = new File(filePath);
        if (!file.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("File does not exist: " + filePath)
                    .build();
        }

        // Parse algorithm
        HashAlgorithm algorithm = FileHashCalculator.parseAlgorithm(algorithmName);
        if (algorithm == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("Unsupported algorithm: " + algorithmName)
                    .build();
        }

        // Calculate hash
        String hash = FileHashCalculator.calculateHash(filePath, algorithm);

        if (hash == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to calculate hash")
                    .build();
        }

        return FileResponse.builder()
                .success(true)
                .message("Hash calculated successfully")
                .filePath(filePath)
                .data(Map.of(
                        "algorithm", algorithm.getAlgorithmName(),
                        "hash", hash,
                        "fileSize", file.length()
                ))
                .build();
    }

    /**
     * Calculate multiple hashes for file
     */
    public FileResponse calculateHashes(String filePath, List<String> algorithmNames) {
        log.info("Service: Calculating hashes for: {}, algorithms: {}",
                filePath, algorithmNames);

        // Validate file
        File file = new File(filePath);
        if (!file.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("File does not exist: " + filePath)
                    .build();
        }

        // Parse algorithms
        List<HashAlgorithm> algorithms = algorithmNames.stream()
                .map(FileHashCalculator::parseAlgorithm)
                .filter(algo -> algo != null)
                .distinct()
                .collect(Collectors.toList());

        if (algorithms.isEmpty()) {
            return FileResponse.builder()
                    .success(false)
                    .message("No valid algorithms specified")
                    .build();
        }

        // Calculate hashes
        HashResult result = FileHashCalculator.calculateHashes(filePath, algorithms);

        if (result == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to calculate hashes")
                    .build();
        }

        return FileResponse.builder()
                .success(true)
                .message("Hashes calculated successfully")
                .filePath(filePath)
                .data(Map.of(
                        "hashes", result.getHashes(),
                        "fileSize", file.length()
                ))
                .build();
    }

    /**
     * Verify file hash
     */
    public FileResponse verifyHash(String filePath, String algorithmName, String expectedHash) {
        log.info("Service: Verifying {} hash for: {}", algorithmName, filePath);

        // Validate file
        File file = new File(filePath);
        if (!file.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("File does not exist: " + filePath)
                    .build();
        }

        // Parse algorithm
        HashAlgorithm algorithm = FileHashCalculator.parseAlgorithm(algorithmName);
        if (algorithm == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("Unsupported algorithm: " + algorithmName)
                    .build();
        }

        // Verify hash
        boolean matched = FileHashCalculator.verifyHash(filePath, algorithm, expectedHash);
        String actualHash = FileHashCalculator.calculateHash(filePath, algorithm);

        return FileResponse.builder()
                .success(true)
                .message(matched ? "Hash verified successfully" : "Hash mismatch")
                .filePath(filePath)
                .data(Map.of(
                        "matched", matched,
                        "algorithm", algorithm.getAlgorithmName(),
                        "expectedHash", expectedHash,
                        "actualHash", actualHash
                ))
                .build();
    }

    /**
     * Get supported hash algorithms
     */
    public FileResponse getSupportedAlgorithms() {
        String[] algorithms = FileHashCalculator.getSupportedAlgorithms();

        return FileResponse.builder()
                .success(true)
                .message("Supported algorithms retrieved")
                .data(Map.of(
                        "algorithms", algorithms,
                        "recommendations", Map.of(
                                "fast", "MD5",
                                "secure", "SHA-256",
                                "mostSecure", "SHA-512"
                        )
                ))
                .build();
    }
}
