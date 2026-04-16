package com.fileprocessor.controller;

import com.fileprocessor.annotation.AuditLog;
import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.security.UserPrincipal;
import com.fileprocessor.service.RecycleBinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recycle-bin")
@PreAuthorize("hasRole('USER')")
public class RecycleBinController {

    private static final Logger log = LoggerFactory.getLogger(RecycleBinController.class);

    @Autowired
    private RecycleBinService recycleBinService;

    @GetMapping("/list")
    public ResponseEntity<FileResponse> listRecycleBin(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("List recycle bin for user: {}", principal.getId());

        FileResponse response = recycleBinService.listRecycleBin(principal.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/restore/{fileId}")
    @AuditLog(action = "FILE_RESTORE", resourceType = "FILE")
    public ResponseEntity<FileResponse> restoreFile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String fileId) {
        log.info("Restore file: {} by user: {}", fileId, principal.getId());

        FileResponse response = recycleBinService.restore(fileId, principal.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{fileId}")
    @AuditLog(action = "FILE_PERMANENT_DELETE", resourceType = "FILE")
    public ResponseEntity<FileResponse> permanentDelete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String fileId) {
        log.info("Permanent delete file: {} by user: {}", fileId, principal.getId());

        FileResponse response = recycleBinService.permanentDelete(fileId, principal.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/empty")
    @AuditLog(action = "RECYCLE_BIN_EMPTY", resourceType = "SYSTEM")
    public ResponseEntity<FileResponse> emptyRecycleBin(
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Empty recycle bin for user: {}", principal.getId());

        FileResponse response = recycleBinService.emptyRecycleBin(principal.getId());
        return ResponseEntity.ok(response);
    }
}
