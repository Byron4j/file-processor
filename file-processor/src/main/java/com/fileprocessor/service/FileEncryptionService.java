package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Map;

@Service
public class FileEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(FileEncryptionService.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 65536;

    @Value("${encryption.master-key:FileMasterProDefaultEncryptionKeyChangeInProduction}")
    private String masterKey;

    /**
     * 加密文件
     */
    public FileResponse encryptFile(String sourcePath, String targetPath, String password) {
        try {
            Path source = Path.of(sourcePath);
            if (!Files.exists(source)) {
                return FileResponse.builder()
                        .success(false)
                        .message("Source file not found: " + sourcePath)
                        .build();
            }

            // Use password or derive from master key
            String keyPassword = password != null ? password : masterKey;
            SecretKey key = deriveKey(keyPassword);

            // Generate IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // Create cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            // Read and encrypt
            byte[] fileContent = Files.readAllBytes(source);
            byte[] encrypted = cipher.doFinal(fileContent);

            // Combine IV + encrypted data
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);

            // Write to target
            Path target = Path.of(targetPath);
            Files.createDirectories(target.getParent());
            Files.write(target, buffer.array());

            return FileResponse.builder()
                    .success(true)
                    .message("File encrypted successfully")
                    .filePath(targetPath)
                    .fileSize(target.toFile().length())
                    .data(Map.of(
                            "originalSize", fileContent.length,
                            "encryptedSize", buffer.array().length,
                            "algorithm", "AES-256-GCM"
                    ))
                    .build();

        } catch (Exception e) {
            log.error("File encryption failed", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Encryption failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 解密文件
     */
    public FileResponse decryptFile(String sourcePath, String targetPath, String password) {
        try {
            Path source = Path.of(sourcePath);
            if (!Files.exists(source)) {
                return FileResponse.builder()
                        .success(false)
                        .message("Source file not found: " + sourcePath)
                        .build();
            }

            // Use password or derive from master key
            String keyPassword = password != null ? password : masterKey;
            SecretKey key = deriveKey(keyPassword);

            // Read encrypted data
            byte[] encryptedData = Files.readAllBytes(source);

            // Extract IV
            ByteBuffer buffer = ByteBuffer.wrap(encryptedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);

            // Extract encrypted content
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            // Create cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            // Decrypt
            byte[] decrypted = cipher.doFinal(encrypted);

            // Write to target
            Path target = Path.of(targetPath);
            Files.createDirectories(target.getParent());
            Files.write(target, decrypted);

            return FileResponse.builder()
                    .success(true)
                    .message("File decrypted successfully")
                    .filePath(targetPath)
                    .fileSize(target.toFile().length())
                    .data(Map.of(
                            "encryptedSize", encryptedData.length,
                            "decryptedSize", decrypted.length,
                            "algorithm", "AES-256-GCM"
                    ))
                    .build();

        } catch (Exception e) {
            log.error("File decryption failed", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Decryption failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 生成数据密钥 (用于客户端加密)
     */
    public FileResponse generateDataKey() {
        try {
            byte[] keyBytes = new byte[32]; // 256 bits
            new SecureRandom().nextBytes(keyBytes);

            String base64Key = Base64.getEncoder().encodeToString(keyBytes);

            return FileResponse.builder()
                    .success(true)
                    .message("Data key generated")
                    .data(Map.of(
                            "key", base64Key,
                            "algorithm", "AES-256"
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate data key", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to generate key: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 从密码派生密钥
     */
    private SecretKey deriveKey(String password) throws Exception {
        // Use PBKDF2 with SHA-256
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), password.getBytes(StandardCharsets.UTF_8), ITERATION_COUNT, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    /**
     * 流式加密大文件
     */
    public FileResponse encryptFileStreaming(String sourcePath, String targetPath, String password) {
        try {
            Path source = Path.of(sourcePath);
            if (!Files.exists(source)) {
                return FileResponse.builder()
                        .success(false)
                        .message("Source file not found: " + sourcePath)
                        .build();
            }

            String keyPassword = password != null ? password : masterKey;
            SecretKey key = deriveKey(keyPassword);

            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            Path target = Path.of(targetPath);
            Files.createDirectories(target.getParent());

            try (InputStream in = Files.newInputStream(source);
                 OutputStream out = Files.newOutputStream(target)) {

                // Write IV first
                out.write(iv);

                // Encrypt in chunks
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    byte[] encrypted = cipher.update(buffer, 0, read);
                    if (encrypted != null) {
                        out.write(encrypted);
                    }
                }

                // Finalize
                byte[] finalBlock = cipher.doFinal();
                if (finalBlock != null) {
                    out.write(finalBlock);
                }
            }

            return FileResponse.builder()
                    .success(true)
                    .message("File encrypted (streaming) successfully")
                    .filePath(targetPath)
                    .fileSize(target.toFile().length())
                    .build();

        } catch (Exception e) {
            log.error("Streaming encryption failed", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Encryption failed: " + e.getMessage())
                    .build();
        }
    }
}
