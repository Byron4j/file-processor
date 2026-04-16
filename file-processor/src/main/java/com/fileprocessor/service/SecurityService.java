package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.util.PdfSecurityUtil;
import com.fileprocessor.util.PdfSecurityUtil.EncryptionConfig;
import com.fileprocessor.util.PdfSecurityUtil.SecurityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;

/**
 * Security service for PDF encryption and decryption
 */
@Service
public class SecurityService {

    private static final Logger log = LoggerFactory.getLogger(SecurityService.class);

    /**
     * Encrypt PDF
     */
    public FileResponse encryptPdf(String sourcePath, String targetPath,
                                   String userPassword, String ownerPassword,
                                   EncryptionConfig config) {
        log.info("Service: Encrypting PDF: {}", sourcePath);

        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file does not exist: " + sourcePath)
                    .build();
        }

        if (!sourcePath.toLowerCase().endsWith(".pdf")) {
            return FileResponse.builder()
                    .success(false)
                    .message("Not a PDF file: " + sourcePath)
                    .build();
        }

        boolean success = PdfSecurityUtil.encrypt(sourcePath, targetPath,
                userPassword, ownerPassword, config);

        if (!success) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to encrypt PDF")
                    .build();
        }

        File targetFile = new File(targetPath);
        return FileResponse.builder()
                .success(true)
                .message("PDF encrypted successfully")
                .filePath(targetPath)
                .fileSize(targetFile.length())
                .data(Map.of(
                        "encryptionLevel", "AES-" + config.getKeyLength(),
                        "canPrint", config.isCanPrint(),
                        "canModify", config.isCanModify(),
                        "canCopy", config.isCanCopy(),
                        "canAnnotate", config.isCanAnnotate()
                ))
                .build();
    }

    /**
     * Decrypt PDF
     */
    public FileResponse decryptPdf(String sourcePath, String targetPath, String password) {
        log.info("Service: Decrypting PDF: {}", sourcePath);

        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file does not exist: " + sourcePath)
                    .build();
        }

        if (!sourcePath.toLowerCase().endsWith(".pdf")) {
            return FileResponse.builder()
                    .success(false)
                    .message("Not a PDF file: " + sourcePath)
                    .build();
        }

        boolean success = PdfSecurityUtil.decrypt(sourcePath, targetPath, password);

        if (!success) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to decrypt PDF (wrong password or not encrypted)")
                    .build();
        }

        File targetFile = new File(targetPath);
        return FileResponse.builder()
                .success(true)
                .message("PDF decrypted successfully")
                .filePath(targetPath)
                .fileSize(targetFile.length())
                .build();
    }

    /**
     * Check if PDF is encrypted
     */
    public FileResponse checkEncryption(String filePath) {
        log.info("Service: Checking encryption for PDF: {}", filePath);

        File file = new File(filePath);
        if (!file.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("File does not exist: " + filePath)
                    .build();
        }

        if (!filePath.toLowerCase().endsWith(".pdf")) {
            return FileResponse.builder()
                    .success(false)
                    .message("Not a PDF file: " + filePath)
                    .build();
        }

        SecurityInfo info = PdfSecurityUtil.getSecurityInfo(filePath);

        if (info == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to get security information")
                    .build();
        }

        return FileResponse.builder()
                .success(true)
                .message("Security information retrieved")
                .filePath(filePath)
                .data(Map.of(
                        "encrypted", info.isEncrypted(),
                        "canPrint", info.isCanPrint(),
                        "canModify", info.isCanModify(),
                        "canCopy", info.isCanCopy(),
                        "canAnnotate", info.isCanAnnotate()
                ))
                .build();
    }
}
