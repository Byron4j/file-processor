package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.template.WordTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Template service for document generation
 */
@Service
public class TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateService.class);

    @Autowired
    private WordTemplateEngine wordEngine;

    @Autowired
    private TaskService taskService;

    /**
     * Render Word template
     */
    public FileResponse renderWordTemplate(String templatePath, String targetPath,
                                           Map<String, Object> data) {
        log.info("Rendering Word template: {} -> {}", templatePath, targetPath);

        File templateFile = new File(templatePath);
        if (!templateFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Template file not found: " + templatePath)
                    .build();
        }

        boolean success = wordEngine.render(templatePath, targetPath, data);

        if (success) {
            File targetFile = new File(targetPath);
            return FileResponse.builder()
                    .success(true)
                    .message("Template rendered successfully")
                    .filePath(targetPath)
                    .fileSize(targetFile.length())
                    .build();
        } else {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to render template")
                    .build();
        }
    }

    /**
     * Get template placeholders
     */
    public FileResponse getPlaceholders(String templatePath) {
        log.info("Getting placeholders from: {}", templatePath);

        File templateFile = new File(templatePath);
        if (!templateFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Template file not found: " + templatePath)
                    .build();
        }

        List<String> placeholders = wordEngine.getPlaceholders(templatePath);

        return FileResponse.builder()
                .success(true)
                .message("Placeholders extracted")
                .data(Map.of("placeholders", placeholders))
                .build();
    }

    /**
     * Batch render templates
     */
    public FileResponse batchRender(String templatePath, String outputDir,
                                    List<Map<String, Object>> dataList) {
        log.info("Batch rendering {} documents", dataList.size());

        // Submit async task
        TaskService.TaskSubmitRequest submitRequest = new TaskService.TaskSubmitRequest();
        submitRequest.setTaskType("BATCH_TEMPLATE");
        submitRequest.setTaskName("Batch Template Render");
        submitRequest.setTotalItems(dataList.size());

        Map<String, Object> parameters = new java.util.HashMap<>();
        parameters.put("operation", "BATCH_RENDER");
        parameters.put("templatePath", templatePath);
        parameters.put("outputDir", outputDir);
        parameters.put("dataList", dataList);
        submitRequest.setParameters(parameters);

        var record = taskService.submitTask(submitRequest);

        return FileResponse.builder()
                .success(true)
                .message("Batch render task submitted")
                .data(Map.of(
                        "taskId", record.getTaskId(),
                        "status", record.getStatus(),
                        "documentCount", dataList.size()
                ))
                .build();
    }
}
