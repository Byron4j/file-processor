package com.fileprocessor.controller;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.service.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Template engine REST API controller
 */
@RestController
@RequestMapping("/api/template")
public class TemplateController {

    private static final Logger log = LoggerFactory.getLogger(TemplateController.class);

    @Autowired
    private TemplateService templateService;

    /**
     * Render Word template
     */
    @PostMapping("/word/render")
    public ResponseEntity<FileResponse> renderWordTemplate(
            @RequestBody RenderRequest request) {
        log.info("REST request to render Word template: {}", request.getTemplatePath());

        FileResponse response = templateService.renderWordTemplate(
                request.getTemplatePath(),
                request.getTargetPath(),
                request.getData()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get template placeholders
     */
    @PostMapping("/placeholders")
    public ResponseEntity<FileResponse> getPlaceholders(
            @RequestBody PlaceholderRequest request) {
        log.info("REST request to get placeholders: {}", request.getTemplatePath());

        FileResponse response = templateService.getPlaceholders(request.getTemplatePath());
        return ResponseEntity.ok(response);
    }

    /**
     * Batch render templates
     */
    @PostMapping("/batch-render")
    public ResponseEntity<FileResponse> batchRender(
            @RequestBody BatchRenderRequest request) {
        log.info("REST request to batch render: {} documents", request.getDataList().size());

        FileResponse response = templateService.batchRender(
                request.getTemplatePath(),
                request.getOutputDir(),
                request.getDataList()
        );
        return ResponseEntity.ok(response);
    }

    // ==================== Request DTOs ====================

    public static class RenderRequest {
        private String templatePath;
        private String targetPath;
        private Map<String, Object> data;

        public String getTemplatePath() { return templatePath; }
        public void setTemplatePath(String templatePath) { this.templatePath = templatePath; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }

    public static class PlaceholderRequest {
        private String templatePath;

        public String getTemplatePath() { return templatePath; }
        public void setTemplatePath(String templatePath) { this.templatePath = templatePath; }
    }

    public static class BatchRenderRequest {
        private String templatePath;
        private String outputDir;
        private List<Map<String, Object>> dataList;

        public String getTemplatePath() { return templatePath; }
        public void setTemplatePath(String templatePath) { this.templatePath = templatePath; }
        public String getOutputDir() { return outputDir; }
        public void setOutputDir(String outputDir) { this.outputDir = outputDir; }
        public List<Map<String, Object>> getDataList() { return dataList; }
        public void setDataList(List<Map<String, Object>> dataList) { this.dataList = dataList; }
    }
}
