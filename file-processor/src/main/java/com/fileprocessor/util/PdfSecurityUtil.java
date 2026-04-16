package com.fileprocessor.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * PDF security utility for encryption, decryption, and permissions
 */
public class PdfSecurityUtil {

    private static final Logger log = LoggerFactory.getLogger(PdfSecurityUtil.class);

    /**
     * Encryption configuration
     */
    public static class EncryptionConfig {
        private boolean canPrint = true;
        private boolean canModify = false;
        private boolean canCopy = false;
        private boolean canAnnotate = true;
        private int keyLength = 256; // AES-256

        public boolean isCanPrint() { return canPrint; }
        public void setCanPrint(boolean canPrint) { this.canPrint = canPrint; }
        public boolean isCanModify() { return canModify; }
        public void setCanModify(boolean canModify) { this.canModify = canModify; }
        public boolean isCanCopy() { return canCopy; }
        public void setCanCopy(boolean canCopy) { this.canCopy = canCopy; }
        public boolean isCanAnnotate() { return canAnnotate; }
        public void setCanAnnotate(boolean canAnnotate) { this.canAnnotate = canAnnotate; }
        public int getKeyLength() { return keyLength; }
        public void setKeyLength(int keyLength) { this.keyLength = keyLength; }
    }

    /**
     * Security information holder
     */
    public static class SecurityInfo {
        private boolean encrypted;
        private boolean hasUserPassword;
        private boolean hasOwnerPassword;
        private boolean canPrint;
        private boolean canModify;
        private boolean canCopy;
        private boolean canAnnotate;

        public boolean isEncrypted() { return encrypted; }
        public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
        public boolean isHasUserPassword() { return hasUserPassword; }
        public void setHasUserPassword(boolean hasUserPassword) { this.hasUserPassword = hasUserPassword; }
        public boolean isHasOwnerPassword() { return hasOwnerPassword; }
        public void setHasOwnerPassword(boolean hasOwnerPassword) { this.hasOwnerPassword = hasOwnerPassword; }
        public boolean isCanPrint() { return canPrint; }
        public void setCanPrint(boolean canPrint) { this.canPrint = canPrint; }
        public boolean isCanModify() { return canModify; }
        public void setCanModify(boolean canModify) { this.canModify = canModify; }
        public boolean isCanCopy() { return canCopy; }
        public void setCanCopy(boolean canCopy) { this.canCopy = canCopy; }
        public boolean isCanAnnotate() { return canAnnotate; }
        public void setCanAnnotate(boolean canAnnotate) { this.canAnnotate = canAnnotate; }
    }

    /**
     * Encrypt PDF with password protection
     */
    public static boolean encrypt(String sourcePath, String targetPath,
                                  String userPassword, String ownerPassword,
                                  EncryptionConfig config) {
        log.info("Encrypting PDF: {} -> {}", sourcePath, targetPath);

        try (PDDocument document = Loader.loadPDF(new File(sourcePath))) {
            // Set access permissions
            AccessPermission permissions = new AccessPermission();
            permissions.setCanPrint(config.isCanPrint());
            permissions.setCanModify(config.isCanModify());
            permissions.setCanExtractContent(config.isCanCopy());
            permissions.setCanModifyAnnotations(config.isCanAnnotate());

            // Create protection policy
            StandardProtectionPolicy policy = new StandardProtectionPolicy(
                    ownerPassword != null ? ownerPassword : userPassword,
                    userPassword,
                    permissions
            );
            policy.setEncryptionKeyLength(config.getKeyLength());

            // Apply protection
            document.protect(policy);

            // Ensure target directory exists
            File targetFile = new File(targetPath);
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            document.save(targetPath);
            log.info("Successfully encrypted PDF: {}", targetPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to encrypt PDF: {}", sourcePath, e);
            return false;
        }
    }

    /**
     * Decrypt PDF
     */
    public static boolean decrypt(String sourcePath, String targetPath, String password) {
        log.info("Decrypting PDF: {} -> {}", sourcePath, targetPath);

        try (PDDocument document = Loader.loadPDF(new File(sourcePath), password)) {
            if (!document.isEncrypted()) {
                log.warn("PDF is not encrypted: {}", sourcePath);
                // Just copy the file
                document.save(targetPath);
                return true;
            }

            // Remove security
            document.setAllSecurityToBeRemoved(true);

            // Ensure target directory exists
            File targetFile = new File(targetPath);
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            document.save(targetPath);
            log.info("Successfully decrypted PDF: {}", targetPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to decrypt PDF: {}", sourcePath, e);
            return false;
        }
    }

    /**
     * Check if PDF is encrypted
     */
    public static boolean isEncrypted(String filePath) {
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            return document.isEncrypted();
        } catch (Exception e) {
            log.error("Failed to check if PDF is encrypted: {}", filePath, e);
            return false;
        }
    }

    /**
     * Get security information
     */
    public static SecurityInfo getSecurityInfo(String filePath) {
        log.info("Getting security info for PDF: {}", filePath);

        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            SecurityInfo info = new SecurityInfo();
            info.setEncrypted(document.isEncrypted());

            if (document.isEncrypted()) {
                // Try to get current access permission
                AccessPermission permissions = document.getCurrentAccessPermission();
                if (permissions != null) {
                    info.setCanPrint(permissions.canPrint());
                    info.setCanModify(permissions.canModify());
                    info.setCanCopy(permissions.canExtractContent());
                    info.setCanAnnotate(permissions.canModifyAnnotations());
                }
            } else {
                // If not encrypted, all permissions are granted
                info.setCanPrint(true);
                info.setCanModify(true);
                info.setCanCopy(true);
                info.setCanAnnotate(true);
            }

            return info;

        } catch (Exception e) {
            log.error("Failed to get security info: {}", filePath, e);
            return null;
        }
    }

    /**
     * Change PDF permissions (requires owner password)
     */
    public static boolean changePermissions(String sourcePath, String targetPath,
                                            String ownerPassword,
                                            EncryptionConfig config) {
        log.info("Changing permissions for PDF: {} -> {}", sourcePath, targetPath);

        try (PDDocument document = Loader.loadPDF(new File(sourcePath), ownerPassword)) {
            if (!document.isEncrypted()) {
                log.warn("PDF is not encrypted: {}", sourcePath);
                return false;
            }

            // Set new permissions
            AccessPermission permissions = new AccessPermission();
            permissions.setCanPrint(config.isCanPrint());
            permissions.setCanModify(config.isCanModify());
            permissions.setCanExtractContent(config.isCanCopy());
            permissions.setCanModifyAnnotations(config.isCanAnnotate());

            // Get current protection policy and update
            StandardProtectionPolicy policy = new StandardProtectionPolicy(
                    ownerPassword, ownerPassword, permissions
            );
            policy.setEncryptionKeyLength(config.getKeyLength());

            document.protect(policy);

            // Ensure target directory exists
            File targetFile = new File(targetPath);
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            document.save(targetPath);
            log.info("Successfully changed permissions: {}", targetPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to change permissions: {}", sourcePath, e);
            return false;
        }
    }
}
