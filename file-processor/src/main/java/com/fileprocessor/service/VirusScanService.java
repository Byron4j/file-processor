package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
public class VirusScanService {

    private static final Logger log = LoggerFactory.getLogger(VirusScanService.class);

    @Value("${clamav.host:localhost}")
    private String clamavHost;

    @Value("${clamav.port:3310}")
    private int clamavPort;

    @Value("${clamav.enabled:false}")
    private boolean clamavEnabled;

    @Value("${clamav.timeout:30000}")
    private int timeout;

    /**
     * 扫描文件
     */
    public FileResponse scanFile(String filePath) {
        if (!clamavEnabled) {
            log.warn("ClamAV is disabled, skipping virus scan for: {}", filePath);
            return FileResponse.builder()
                    .success(true)
                    .message("Virus scan skipped (disabled)")
                    .data(Map.of("clean", true, "enabled", false))
                    .build();
        }

        try {
            Path path = Path.of(filePath);
            if (!Files.exists(path)) {
                return FileResponse.builder()
                        .success(false)
                        .message("File not found: " + filePath)
                        .build();
            }

            ScanResult result = scanWithClamAV(path);

            if (result.isError()) {
                return FileResponse.builder()
                        .success(false)
                        .message("Scan error: " + result.getMessage())
                        .build();
            }

            return FileResponse.builder()
                    .success(true)
                    .message(result.isClean() ? "File is clean" : "Threat detected!")
                    .data(Map.of(
                            "clean", result.isClean(),
                            "threats", result.getThreats(),
                            "scanTime", result.getScanTimeMs()
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Virus scan failed", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Scan failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 使用 ClamAV 扫描
     */
    private ScanResult scanWithClamAV(Path filePath) throws IOException {
        long startTime = System.currentTimeMillis();

        try (Socket socket = new Socket(clamavHost, clamavPort);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {

            socket.setSoTimeout(timeout);

            // Send INSTREAM command
            out.write("zINSTREAM\0".getBytes());

            // Send file data in chunks
            try (InputStream fileIn = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fileIn.read(buffer)) != -1) {
                    // Send chunk size (4 bytes, big-endian)
                    out.write((read >>> 24) & 0xFF);
                    out.write((read >>> 16) & 0xFF);
                    out.write((read >>> 8) & 0xFF);
                    out.write(read & 0xFF);
                    out.write(buffer, 0, read);
                }

                // Send zero-length chunk to indicate end
                out.write(new byte[]{0, 0, 0, 0});
                out.flush();
            }

            // Read response
            ByteArrayOutputStream response = new ByteArrayOutputStream();
            byte[] respBuffer = new byte[1024];
            int respRead;
            while ((respRead = in.read(respBuffer)) != -1) {
                response.write(respBuffer, 0, respRead);
            }

            String result = response.toString().trim();
            long scanTime = System.currentTimeMillis() - startTime;

            // Parse response
            // "stream: OK" = clean
            // "stream: <virus name> FOUND" = infected
            if (result.endsWith("OK")) {
                return new ScanResult(true, null, scanTime);
            } else if (result.contains("FOUND")) {
                String threat = result.substring(result.indexOf(":") + 1, result.indexOf("FOUND")).trim();
                return new ScanResult(false, threat, scanTime);
            } else {
                return new ScanResult(false, true, "Unknown response: " + result, scanTime);
            }

        }
    }

    /**
     * 快速扫描（检查是否启用）
     */
    public boolean isEnabled() {
        return clamavEnabled;
    }

    /**
     * 批量扫描
     */
    public FileResponse scanFiles(java.util.List<String> filePaths) {
        if (!clamavEnabled) {
            return FileResponse.builder()
                    .success(true)
                    .message("Virus scan skipped (disabled)")
                    .build();
        }

        int total = filePaths.size();
        int clean = 0;
        int infected = 0;
        java.util.List<String> threats = new java.util.ArrayList<>();

        for (String path : filePaths) {
            FileResponse result = scanFile(path);
            if (result.isSuccess()) {
                Map<String, Object> data = (Map<String, Object>) result.getData();
                if (Boolean.TRUE.equals(data.get("clean"))) {
                    clean++;
                } else {
                    infected++;
                    threats.add(path + ": " + data.get("threats"));
                }
            }
        }

        return FileResponse.builder()
                .success(true)
                .message(infected > 0 ? "Threats detected!" : "All files clean")
                .data(Map.of(
                        "total", total,
                        "clean", clean,
                        "infected", infected,
                        "threats", threats
                ))
                .build();
    }

    // Inner class for scan result
    private static class ScanResult {
        private final boolean clean;
        private final String threats;
        private final boolean error;
        private final String message;
        private final long scanTimeMs;

        ScanResult(boolean clean, String threats, long scanTimeMs) {
            this.clean = clean;
            this.threats = threats;
            this.error = false;
            this.message = null;
            this.scanTimeMs = scanTimeMs;
        }

        ScanResult(boolean clean, boolean error, String message, long scanTimeMs) {
            this.clean = clean;
            this.threats = null;
            this.error = error;
            this.message = message;
            this.scanTimeMs = scanTimeMs;
        }

        boolean isClean() { return clean; }
        String getThreats() { return threats; }
        boolean isError() { return error; }
        String getMessage() { return message; }
        long getScanTimeMs() { return scanTimeMs; }
    }
}
