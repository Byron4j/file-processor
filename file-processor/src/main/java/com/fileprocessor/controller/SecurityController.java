package com.fileprocessor.controller;

import com.fileprocessor.dto.FileConvertRequest;
import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.service.SecurityService;
import com.fileprocessor.util.PdfSecurityUtil.EncryptionConfig;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Security REST API controller for PDF encryption/decryption
 */
@RestController
@RequestMapping("/api/security")
public class SecurityController {

    private static final Logger log = LoggerFactory.getLogger(SecurityController.class);

    @Autowired
    private SecurityService securityService;

    /**
     * Encrypt PDF
     */
    @PostMapping("/pdf/encrypt")
    public ResponseEntity<FileResponse> encryptPdf(
            @RequestBody @Valid PdfEncryptRequest request) {
        log.info("REST request to encrypt PDF: {}", request.getSourcePath());

        EncryptionConfig config = new EncryptionConfig();
        config.setCanPrint(request.isCanPrint());
        config.setCanModify(request.isCanModify());
        config.setCanCopy(request.isCanCopy());
        config.setCanAnnotate(request.isCanAnnotate());
        config.setKeyLength(request.getEncryptionLevel().equals("AES_256") ? 256 : 128);

        FileResponse response = securityService.encryptPdf(
                request.getSourcePath(),
                request.getTargetPath(),
                request.getUserPassword(),
                request.getOwnerPassword(),
                config
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Decrypt PDF
     */
    @PostMapping("/pdf/decrypt")
    public ResponseEntity<FileResponse> decryptPdf(
            @RequestBody @Valid PdfDecryptRequest request) {
        log.info("REST request to decrypt PDF: {}", request.getSourcePath());

        FileResponse response = securityService.decryptPdf(
                request.getSourcePath(),
                request.getTargetPath(),
                request.getPassword()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Check PDF encryption status
     */
    @GetMapping("/pdf/check")
    public ResponseEntity<FileResponse> checkEncryption(
            @RequestParam String path) {
        log.info("REST request to check PDF encryption: {}", path);

        FileResponse response = securityService.checkEncryption(path);
        return ResponseEntity.ok(response);
    }

    // ==================== Request DTOs ====================

    public static class PdfEncryptRequest extends FileConvertRequest {
        private String userPassword;
        private String ownerPassword;
        private boolean canPrint = true;
        private boolean canModify = false;
        private boolean canCopy = false;
        private boolean canAnnotate = true;
        private String encryptionLevel = "AES_256"; // AES_256 or AES_128

        public String getUserPassword() { return userPassword; }
        public void setUserPassword(String userPassword) { this.userPassword = userPassword; }
        public String getOwnerPassword() { return ownerPassword; }
        public void setOwnerPassword(String ownerPassword) { this.ownerPassword = ownerPassword; }
        public boolean isCanPrint() { return canPrint; }
        public void setCanPrint(boolean canPrint) { this.canPrint = canPrint; }
        public boolean isCanModify() { return canModify; }
        public void setCanModify(boolean canModify) { this.canModify = canModify; }
        public boolean isCanCopy() { return canCopy; }
        public void setCanCopy(boolean canCopy) { this.canCopy = canCopy; }
        public boolean isCanAnnotate() { return canAnnotate; }
        public void setCanAnnotate(boolean canAnnotate) { this.canAnnotate = canAnnotate; }
        public String getEncryptionLevel() { return encryptionLevel; }
        public void setEncryptionLevel(String encryptionLevel) { this.encryptionLevel = encryptionLevel; }
    }

    public static class PdfDecryptRequest extends FileConvertRequest {
        private String password;

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
