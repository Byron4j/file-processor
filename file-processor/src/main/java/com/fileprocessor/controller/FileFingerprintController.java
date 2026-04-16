package com.fileprocessor.controller;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.service.FileFingerprintService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fingerprint")
public class FileFingerprintController {

    @Autowired
    private FileFingerprintService fingerprintService;

    /**
     * 秒传检查
     */
    @PostMapping("/check")
    public FileResponse checkFingerprint(@Valid @RequestBody CheckRequest request) {
        return fingerprintService.checkFileExists(request.getMd5(), request.getFileSize());
    }

    /**
     * 执行秒传
     */
    @PostMapping("/instant-transfer")
    public FileResponse instantTransfer(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody InstantTransferRequest request) {
        return fingerprintService.instantTransfer(
                request.getMd5(),
                request.getFileSize(),
                userId,
                request.getOriginalFilename()
        );
    }

    /**
     * 秒传检查请求
     */
    public static class CheckRequest {
        @NotBlank(message = "MD5不能为空")
        private String md5;

        @NotNull(message = "文件大小不能为空")
        private Long fileSize;

        public String getMd5() { return md5; }
        public void setMd5(String md5) { this.md5 = md5; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    }

    /**
     * 秒传请求
     */
    public static class InstantTransferRequest {
        @NotBlank(message = "MD5不能为空")
        private String md5;

        @NotNull(message = "文件大小不能为空")
        private Long fileSize;

        @NotBlank(message = "原文件名不能为空")
        private String originalFilename;

        public String getMd5() { return md5; }
        public void setMd5(String md5) { this.md5 = md5; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        public String getOriginalFilename() { return originalFilename; }
        public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    }
}
