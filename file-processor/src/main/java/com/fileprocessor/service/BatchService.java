package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.entity.TaskRecord;
import com.fileprocessor.task.TaskResult;
import com.fileprocessor.task.TaskType;
import com.fileprocessor.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

/**
 * Batch processing service
 */
@Service
public class BatchService {

    private static final Logger log = LoggerFactory.getLogger(BatchService.class);

    @Autowired
    private TaskService taskService;

    @Autowired
    private FileConvertService fileConvertService;

    @Autowired
    private WatermarkService watermarkService;

    /**
     * Submit batch convert task
     */
    public FileResponse batchConvert(BatchConvertRequest request) {
        log.info("Submitting batch convert task for {} files", request.getFiles().size());

        // Create async task
        TaskService.TaskSubmitRequest submitRequest = new TaskService.TaskSubmitRequest();
        submitRequest.setTaskType(TaskType.BATCH.getCode());
        submitRequest.setTaskName("Batch Convert: " + request.getTargetFormat());
        submitRequest.setTotalItems(request.getFiles().size());
        submitRequest.setCallbackUrl(request.getCallbackUrl());
        submitRequest.setMaxRetry(2);

        // Prepare source files
        List<Map<String, String>> sourceFiles = new ArrayList<>();
        for (String filePath : request.getFiles()) {
            Map<String, String> fileInfo = new HashMap<>();
            fileInfo.put("path", filePath);
            fileInfo.put("name", new File(filePath).getName());
            sourceFiles.add(fileInfo);
        }
        submitRequest.setSourceFiles(sourceFiles);

        // Prepare parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("operation", "CONVERT");
        parameters.put("targetFormat", request.getTargetFormat());
        parameters.put("outputDir", request.getOutputDir());
        parameters.put("files", request.getFiles());
        submitRequest.setParameters(parameters);

        TaskRecord record = taskService.submitTask(submitRequest);

        return FileResponse.builder()
                .success(true)
                .message("Batch convert task submitted")
                .data(Map.of(
                        "taskId", record.getTaskId(),
                        "status", record.getStatus(),
                        "fileCount", request.getFiles().size()
                ))
                .build();
    }

    /**
     * Submit batch watermark task
     */
    public FileResponse batchWatermark(BatchWatermarkRequest request) {
        log.info("Submitting batch watermark task for {} files", request.getFiles().size());

        TaskService.TaskSubmitRequest submitRequest = new TaskService.TaskSubmitRequest();
        submitRequest.setTaskType(TaskType.BATCH.getCode());
        submitRequest.setTaskName("Batch Watermark: " + request.getWatermark().getText());
        submitRequest.setTotalItems(request.getFiles().size());
        submitRequest.setCallbackUrl(request.getCallbackUrl());

        // Prepare source files
        List<Map<String, String>> sourceFiles = new ArrayList<>();
        for (String filePath : request.getFiles()) {
            Map<String, String> fileInfo = new HashMap<>();
            fileInfo.put("path", filePath);
            fileInfo.put("name", new File(filePath).getName());
            sourceFiles.add(fileInfo);
        }
        submitRequest.setSourceFiles(sourceFiles);

        // Prepare parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("operation", "WATERMARK");
        parameters.put("files", request.getFiles());
        parameters.put("watermark", request.getWatermark());
        submitRequest.setParameters(parameters);

        TaskRecord record = taskService.submitTask(submitRequest);

        return FileResponse.builder()
                .success(true)
                .message("Batch watermark task submitted")
                .data(Map.of(
                        "taskId", record.getTaskId(),
                        "status", record.getStatus(),
                        "fileCount", request.getFiles().size()
                ))
                .build();
    }

    /**
     * Submit batch extract task
     */
    public FileResponse batchExtract(BatchExtractRequest request) {
        log.info("Submitting batch extract task for {} files", request.getFiles().size());

        TaskService.TaskSubmitRequest submitRequest = new TaskService.TaskSubmitRequest();
        submitRequest.setTaskType(TaskType.BATCH.getCode());
        submitRequest.setTaskName("Batch Text Extract");
        submitRequest.setTotalItems(request.getFiles().size());
        submitRequest.setCallbackUrl(request.getCallbackUrl());

        // Prepare source files
        List<Map<String, String>> sourceFiles = new ArrayList<>();
        for (String filePath : request.getFiles()) {
            Map<String, String> fileInfo = new HashMap<>();
            fileInfo.put("path", filePath);
            fileInfo.put("name", new File(filePath).getName());
            sourceFiles.add(fileInfo);
        }
        submitRequest.setSourceFiles(sourceFiles);

        // Prepare parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("operation", "EXTRACT");
        parameters.put("files", request.getFiles());
        parameters.put("outputFormat", request.getOutputFormat());
        parameters.put("combineOutput", request.isCombineOutput());
        parameters.put("targetPath", request.getTargetPath());
        submitRequest.setParameters(parameters);

        TaskRecord record = taskService.submitTask(submitRequest);

        return FileResponse.builder()
                .success(true)
                .message("Batch extract task submitted")
                .data(Map.of(
                        "taskId", record.getTaskId(),
                        "status", record.getStatus(),
                        "fileCount", request.getFiles().size()
                ))
                .build();
    }

    /**
     * Calculate batch hash synchronously
     */
    public FileResponse batchHash(BatchHashRequest request) {
        log.info("Calculating hash for {} files", request.getFiles().size());

        List<Map<String, Object>> results = new ArrayList<>();
        String algorithm = request.getAlgorithm() != null ? request.getAlgorithm() : "SHA-256";

        for (String filePath : request.getFiles()) {
            try {
                FileHashCalculator.HashAlgorithm hashAlgorithm = FileHashCalculator.parseAlgorithm(algorithm);
                if (hashAlgorithm == null) {
                    hashAlgorithm = FileHashCalculator.HashAlgorithm.SHA_256;
                }
                String hash = FileHashCalculator.calculateHash(filePath, hashAlgorithm);
                Map<String, Object> result = new HashMap<>();
                result.put("filePath", filePath);
                result.put("algorithm", algorithm);
                result.put("hash", hash);
                result.put("success", hash != null);
                results.add(result);
            } catch (Exception e) {
                Map<String, Object> result = new HashMap<>();
                result.put("filePath", filePath);
                result.put("algorithm", algorithm);
                result.put("success", false);
                result.put("error", e.getMessage());
                results.add(result);
            }
        }

        return FileResponse.builder()
                .success(true)
                .message("Batch hash calculation completed")
                .data(Map.of(
                        "results", results,
                        "totalCount", request.getFiles().size(),
                        "successCount", results.stream().filter(r -> (Boolean) r.get("success")).count()
                ))
                .build();
    }

    // ==================== Request DTOs ====================

    public static class BatchConvertRequest {
        private List<String> files;
        private String targetFormat;
        private String outputDir;
        private boolean async = true;
        private String callbackUrl;

        public List<String> getFiles() { return files; }
        public void setFiles(List<String> files) { this.files = files; }
        public String getTargetFormat() { return targetFormat; }
        public void setTargetFormat(String targetFormat) { this.targetFormat = targetFormat; }
        public String getOutputDir() { return outputDir; }
        public void setOutputDir(String outputDir) { this.outputDir = outputDir; }
        public boolean isAsync() { return async; }
        public void setAsync(boolean async) { this.async = async; }
        public String getCallbackUrl() { return callbackUrl; }
        public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
    }

    public static class BatchWatermarkRequest {
        private List<String> files;
        private WatermarkConfig watermark;
        private boolean async = true;
        private String callbackUrl;

        public List<String> getFiles() { return files; }
        public void setFiles(List<String> files) { this.files = files; }
        public WatermarkConfig getWatermark() { return watermark; }
        public void setWatermark(WatermarkConfig watermark) { this.watermark = watermark; }
        public boolean isAsync() { return async; }
        public void setAsync(boolean async) { this.async = async; }
        public String getCallbackUrl() { return callbackUrl; }
        public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
    }

    public static class WatermarkConfig {
        private String text = "CONFIDENTIAL";
        private float opacity = 0.3f;
        private int rotation = 45;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public float getOpacity() { return opacity; }
        public void setOpacity(float opacity) { this.opacity = opacity; }
        public int getRotation() { return rotation; }
        public void setRotation(int rotation) { this.rotation = rotation; }
    }

    public static class BatchExtractRequest {
        private List<String> files;
        private String outputFormat = "TXT";
        private boolean combineOutput = false;
        private String targetPath;
        private boolean async = true;
        private String callbackUrl;

        public List<String> getFiles() { return files; }
        public void setFiles(List<String> files) { this.files = files; }
        public String getOutputFormat() { return outputFormat; }
        public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }
        public boolean isCombineOutput() { return combineOutput; }
        public void setCombineOutput(boolean combineOutput) { this.combineOutput = combineOutput; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public boolean isAsync() { return async; }
        public void setAsync(boolean async) { this.async = async; }
        public String getCallbackUrl() { return callbackUrl; }
        public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
    }

    public static class BatchHashRequest {
        private List<String> files;
        private String algorithm = "SHA-256";

        public List<String> getFiles() { return files; }
        public void setFiles(List<String> files) { this.files = files; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    }
}
