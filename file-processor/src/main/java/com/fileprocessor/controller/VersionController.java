package com.fileprocessor.controller;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.service.VersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文件版本管理控制器
 */
@RestController
@RequestMapping("/api/files/{fileId}/versions")
public class VersionController {

    private static final Logger log = LoggerFactory.getLogger(VersionController.class);

    @Autowired
    private VersionService versionService;

    /**
     * 保存版本
     */
    @PostMapping("/save")
    public ResponseEntity<FileResponse> saveVersion(
            @PathVariable String fileId,
            @RequestBody SaveVersionRequest request) {
        log.info("Save version request for file: {}", fileId);

        FileResponse response = versionService.saveVersion(
                fileId,
                request.getDescription(),
                request.getTags()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 获取版本列表
     */
    @GetMapping
    public ResponseEntity<FileResponse> listVersions(@PathVariable String fileId) {
        log.info("List versions for file: {}", fileId);

        FileResponse response = versionService.listVersions(fileId);
        return ResponseEntity.ok(response);
    }

    /**
     * 恢复到指定版本
     */
    @PostMapping("/restore")
    public ResponseEntity<FileResponse> restoreVersion(
            @PathVariable String fileId,
            @RequestBody RestoreVersionRequest request) {
        log.info("Restore file: {} to version: {}", fileId, request.getVersionId());

        FileResponse response = versionService.restoreVersion(fileId, request.getVersionId());
        return ResponseEntity.ok(response);
    }

    /**
     * 删除版本
     */
    @DeleteMapping("/{versionId}")
    public ResponseEntity<FileResponse> deleteVersion(@PathVariable String versionId) {
        log.info("Delete version: {}", versionId);

        FileResponse response = versionService.deleteVersion(versionId);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取版本详情
     */
    @GetMapping("/{versionId}/detail")
    public ResponseEntity<FileResponse> getVersionDetail(@PathVariable String versionId) {
        log.info("Get version detail: {}", versionId);

        FileResponse response = versionService.getVersionDetail(versionId);
        return ResponseEntity.ok(response);
    }

    // ==================== Request DTOs ====================

    public static class SaveVersionRequest {
        private String description;
        private List<String> tags;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }
    }

    public static class RestoreVersionRequest {
        private String versionId;

        public String getVersionId() {
            return versionId;
        }

        public void setVersionId(String versionId) {
            this.versionId = versionId;
        }
    }
}
