package com.fileprocessor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.entity.FileMetadata;
import com.fileprocessor.entity.FileSplitRecord;
import com.fileprocessor.mapper.FileMetadataMapper;
import com.fileprocessor.mapper.FileSplitRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 文件分割与合并服务
 */
@Service
public class SplitMergeService {

    private static final Logger log = LoggerFactory.getLogger(SplitMergeService.class);

    @Autowired
    private FileMetadataMapper fileMetadataMapper;

    @Autowired
    private FileSplitRecordMapper fileSplitRecordMapper;

    @Value("${file.output.path:./outputs}")
    private String outputPath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 分割大文件
     */
    public FileResponse splitFile(String fileId, long chunkSize, String namingPattern, String outputDir) {
        log.info("Splitting file: {}, chunkSize: {} bytes", fileId, chunkSize);

        try {
            // 1. 获取文件信息
            FileMetadata file = fileMetadataMapper.findByFileId(fileId);
            if (file == null) {
                return FileResponse.builder()
                        .success(false)
                        .message("File not found: " + fileId)
                        .build();
            }

            // 2. 检查文件大小
            Path sourcePath = Paths.get(file.getStoragePath());
            if (!Files.exists(sourcePath)) {
                return FileResponse.builder()
                        .success(false)
                        .message("Source file not found: " + file.getStoragePath())
                        .build();
            }

            long fileSize = Files.size(sourcePath);
            if (fileSize <= chunkSize) {
                return FileResponse.builder()
                        .success(false)
                        .message("File size (" + fileSize + ") is smaller than chunk size (" + chunkSize + ")")
                        .build();
            }

            // 3. 计算分块信息
            int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);

            // 4. 生成输出目录
            if (outputDir == null || outputDir.isEmpty()) {
                outputDir = generateSplitOutputDir(fileId);
            }
            Path outputPathDir = Paths.get(outputDir);
            Files.createDirectories(outputPathDir);

            // 5. 生成分割记录
            String splitId = generateSplitId();
            FileSplitRecord record = new FileSplitRecord();
            record.setSplitId(splitId);
            record.setOriginalFileId(fileId);
            record.setOriginalFileName(file.getOriginalName());
            record.setOriginalSize(fileSize);
            record.setOriginalHash(file.getFileHash());
            record.setChunkSize(chunkSize);
            record.setTotalChunks(totalChunks);
            record.setOutputDir(outputDir);
            record.setStatus("PROCESSING");
            record.setCreatedAt(LocalDateTime.now());
            fileSplitRecordMapper.insert(record);

            // 6. 执行分割
            List<Map<String, Object>> chunks = new ArrayList<>();
            List<ChunkInfo> chunkInfos = new ArrayList<>();

            try (InputStream is = Files.newInputStream(sourcePath)) {
                byte[] buffer = new byte[8192];

                for (int i = 0; i < totalChunks; i++) {
                    String chunkFileName = generateChunkName(namingPattern, i + 1);
                    Path chunkPath = outputPathDir.resolve(chunkFileName);

                    long bytesToRead = Math.min(chunkSize, fileSize - (long) i * chunkSize);
                    long bytesRead = 0;

                    try (OutputStream os = Files.newOutputStream(chunkPath)) {
                        while (bytesRead < bytesToRead) {
                            int read = is.read(buffer, 0, (int) Math.min(buffer.length, bytesToRead - bytesRead));
                            if (read == -1) break;
                            os.write(buffer, 0, read);
                            bytesRead += read;
                        }
                    }

                    // 计算分块哈希
                    String chunkHash = calculateFileHash(chunkPath.toFile());

                    ChunkInfo chunkInfo = new ChunkInfo();
                    chunkInfo.setIndex(i + 1);
                    chunkInfo.setFileName(chunkFileName);
                    chunkInfo.setSize(bytesRead);
                    chunkInfo.setHash(chunkHash);
                    chunkInfos.add(chunkInfo);

                    Map<String, Object> chunkData = new HashMap<>();
                    chunkData.put("index", i + 1);
                    chunkData.put("fileName", chunkFileName);
                    chunkData.put("path", chunkPath.toString());
                    chunkData.put("size", bytesRead);
                    chunks.add(chunkData);

                    log.debug("Created chunk {}: {} ({} bytes)", i + 1, chunkFileName, bytesRead);
                }
            }

            // 7. 生成清单文件
            Manifest manifest = new Manifest();
            manifest.setOriginalFileName(file.getOriginalName());
            manifest.setOriginalSize(fileSize);
            manifest.setOriginalHash(file.getFileHash());
            manifest.setChunkSize(chunkSize);
            manifest.setTotalChunks(totalChunks);
            manifest.setChunks(chunkInfos);
            manifest.setSplitId(splitId);
            manifest.setCreatedAt(LocalDateTime.now().toString());

            String manifestPath = outputDir + "/manifest.json";
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(manifestPath), manifest);

            // 8. 更新记录
            record.setStatus("COMPLETED");
            record.setManifestPath(manifestPath);
            record.setCompletedAt(LocalDateTime.now());
            fileSplitRecordMapper.updateById(record);

            log.info("File split completed: {} into {} chunks", fileId, totalChunks);

            return FileResponse.builder()
                    .success(true)
                    .message("File split completed")
                    .fileId(splitId)
                    .data(Map.of(
                            "splitId", splitId,
                            "totalChunks", totalChunks,
                            "chunkSize", chunkSize,
                            "chunks", chunks,
                            "manifestPath", manifestPath
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Failed to split file: {}", fileId, e);
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to split file: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 合并文件
     */
    public FileResponse mergeFile(String manifestPath, String targetPath, boolean verifyHash) {
        log.info("Merging file from manifest: {}, verifyHash: {}", manifestPath, verifyHash);

        try {
            // 1. 读取清单文件
            File manifestFile = new File(manifestPath);
            if (!manifestFile.exists()) {
                return FileResponse.builder()
                        .success(false)
                        .message("Manifest file not found: " + manifestPath)
                        .build();
            }

            Manifest manifest = objectMapper.readValue(manifestFile, Manifest.class);

            // 2. 确定输出路径
            if (targetPath == null || targetPath.isEmpty()) {
                targetPath = outputPath + "/merged/" + manifest.getOriginalFileName();
            }

            Path target = Paths.get(targetPath);
            Files.createDirectories(target.getParent());

            // 3. 执行合并
            String baseDir = manifestFile.getParent();
            try (OutputStream os = Files.newOutputStream(target)) {
                for (ChunkInfo chunk : manifest.getChunks()) {
                    Path chunkPath = Paths.get(baseDir, chunk.getFileName());

                    if (!Files.exists(chunkPath)) {
                        throw new IOException("Chunk file not found: " + chunkPath);
                    }

                    // 验证分块哈希
                    if (verifyHash) {
                        String actualHash = calculateFileHash(chunkPath.toFile());
                        if (!actualHash.equals(chunk.getHash())) {
                            throw new IOException("Hash verification failed for chunk: " + chunk.getFileName());
                        }
                    }

                    Files.copy(chunkPath, os);
                    log.debug("Merged chunk: {}", chunk.getFileName());
                }
            }

            // 4. 验证整体哈希
            boolean hashVerified = false;
            if (verifyHash && manifest.getOriginalHash() != null) {
                String mergedHash = calculateFileHash(target.toFile());
                hashVerified = mergedHash.equals(manifest.getOriginalHash());

                if (!hashVerified) {
                    log.warn("Merged file hash does not match original hash");
                }
            }

            long mergedSize = Files.size(target);

            log.info("File merge completed: {} ({} bytes)", targetPath, mergedSize);

            return FileResponse.builder()
                    .success(true)
                    .message("File merge completed")
                    .filePath(targetPath)
                    .fileSize(mergedSize)
                    .data(Map.of(
                            "targetPath", targetPath,
                            "size", mergedSize,
                            "hash", verifyHash ? calculateFileHash(target.toFile()) : null,
                            "hashVerified", hashVerified,
                            "totalChunks", manifest.getTotalChunks()
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Failed to merge file from manifest: {}", manifestPath, e);
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to merge file: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 生成分块名称
     */
    private String generateChunkName(String pattern, int index) {
        if (pattern == null || pattern.isEmpty()) {
            pattern = "part-{index}.bin";
        }
        return pattern.replace("{index}", String.format("%03d", index));
    }

    /**
     * 生成分割输出目录
     */
    private String generateSplitOutputDir(String fileId) {
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("%s/split/%s/%s", outputPath, datePath, fileId);
    }

    /**
     * 生成分割ID
     */
    private String generateSplitId() {
        return "s-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 计算文件哈希
     */
    private String calculateFileHash(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // ==================== 内部类 ====================

    public static class Manifest {
        private String originalFileName;
        private long originalSize;
        private String originalHash;
        private long chunkSize;
        private int totalChunks;
        private List<ChunkInfo> chunks;
        private String splitId;
        private String createdAt;

        public String getOriginalFileName() { return originalFileName; }
        public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
        public long getOriginalSize() { return originalSize; }
        public void setOriginalSize(long originalSize) { this.originalSize = originalSize; }
        public String getOriginalHash() { return originalHash; }
        public void setOriginalHash(String originalHash) { this.originalHash = originalHash; }
        public long getChunkSize() { return chunkSize; }
        public void setChunkSize(long chunkSize) { this.chunkSize = chunkSize; }
        public int getTotalChunks() { return totalChunks; }
        public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
        public List<ChunkInfo> getChunks() { return chunks; }
        public void setChunks(List<ChunkInfo> chunks) { this.chunks = chunks; }
        public String getSplitId() { return splitId; }
        public void setSplitId(String splitId) { this.splitId = splitId; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    public static class ChunkInfo {
        private int index;
        private String fileName;
        private long size;
        private String hash;

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public String getHash() { return hash; }
        public void setHash(String hash) { this.hash = hash; }
    }
}