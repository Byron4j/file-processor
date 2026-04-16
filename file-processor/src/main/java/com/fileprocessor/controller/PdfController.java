package com.fileprocessor.controller;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.service.PdfService;
import com.fileprocessor.util.PdfEditor.PageRange;
import com.fileprocessor.util.PdfEditor.SplitConfig;
import com.fileprocessor.util.PdfEditor.SplitMode;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PDF processing REST API controller
 */
@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    private static final Logger log = LoggerFactory.getLogger(PdfController.class);

    @Autowired
    private PdfService pdfService;

    /**
     * Merge multiple PDFs
     */
    @PostMapping("/merge")
    public ResponseEntity<FileResponse> merge(
            @RequestBody @Valid PdfMergeRequest request) {
        log.info("REST request to merge {} PDFs", request.getSourcePaths().size());

        FileResponse response = pdfService.merge(
                request.getSourcePaths(),
                request.getTargetPath(),
                request.isBookmarks()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Split PDF
     */
    @PostMapping("/split")
    public ResponseEntity<FileResponse> split(
            @RequestBody @Valid PdfSplitRequest request) {
        log.info("REST request to split PDF: {}", request.getSourcePath());

        SplitConfig config = new SplitConfig();
        config.setMode(request.getMode());
        config.setRanges(request.getRanges());
        config.setEveryNPages(request.getEveryNPages());

        FileResponse response = pdfService.split(
                request.getSourcePath(),
                request.getOutputDir(),
                config
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Extract specific pages
     */
    @PostMapping("/extract")
    public ResponseEntity<FileResponse> extractPages(
            @RequestBody @Valid PdfExtractRequest request) {
        log.info("REST request to extract pages {} from PDF", request.getPages());

        FileResponse response = pdfService.extractPages(
                request.getSourcePath(),
                request.getTargetPath(),
                request.getPages()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Rotate PDF pages
     */
    @PostMapping("/rotate")
    public ResponseEntity<FileResponse> rotate(
            @RequestBody @Valid PdfRotateRequest request) {
        log.info("REST request to rotate PDF pages by {} degrees", request.getAngle());

        FileResponse response = pdfService.rotate(
                request.getSourcePath(),
                request.getTargetPath(),
                request.getPages(),
                request.getAngle()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Delete PDF pages
     */
    @PostMapping("/delete-pages")
    public ResponseEntity<FileResponse> deletePages(
            @RequestBody @Valid PdfDeleteRequest request) {
        log.info("REST request to delete pages {} from PDF", request.getPages());

        FileResponse response = pdfService.deletePages(
                request.getSourcePath(),
                request.getTargetPath(),
                request.getPages()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get PDF information
     */
    @GetMapping("/info")
    public ResponseEntity<FileResponse> getPdfInfo(
            @RequestParam String path) {
        log.info("REST request to get PDF info: {}", path);

        FileResponse response = pdfService.getPdfInfo(path);
        return ResponseEntity.ok(response);
    }

    // ==================== Request DTOs ====================

    public static class PdfMergeRequest {
        private List<String> sourcePaths;
        private String targetPath;
        private boolean bookmarks = true;

        public List<String> getSourcePaths() { return sourcePaths; }
        public void setSourcePaths(List<String> sourcePaths) { this.sourcePaths = sourcePaths; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public boolean isBookmarks() { return bookmarks; }
        public void setBookmarks(boolean bookmarks) { this.bookmarks = bookmarks; }
    }

    public static class PdfSplitRequest {
        private String sourcePath;
        private String outputDir;
        private SplitMode mode = SplitMode.RANGE;
        private List<PageRange> ranges;
        private int everyNPages = 1;

        public String getSourcePath() { return sourcePath; }
        public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
        public String getOutputDir() { return outputDir; }
        public void setOutputDir(String outputDir) { this.outputDir = outputDir; }
        public SplitMode getMode() { return mode; }
        public void setMode(SplitMode mode) { this.mode = mode; }
        public List<PageRange> getRanges() { return ranges; }
        public void setRanges(List<PageRange> ranges) { this.ranges = ranges; }
        public int getEveryNPages() { return everyNPages; }
        public void setEveryNPages(int everyNPages) { this.everyNPages = everyNPages; }
    }

    public static class PdfExtractRequest {
        private String sourcePath;
        private String targetPath;
        private List<Integer> pages;

        public String getSourcePath() { return sourcePath; }
        public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public List<Integer> getPages() { return pages; }
        public void setPages(List<Integer> pages) { this.pages = pages; }
    }

    public static class PdfRotateRequest {
        private String sourcePath;
        private String targetPath;
        private List<Integer> pages; // Empty for all pages
        private int angle = 90; // 90, 180, 270

        public String getSourcePath() { return sourcePath; }
        public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public List<Integer> getPages() { return pages; }
        public void setPages(List<Integer> pages) { this.pages = pages; }
        public int getAngle() { return angle; }
        public void setAngle(int angle) { this.angle = angle; }
    }

    public static class PdfDeleteRequest {
        private String sourcePath;
        private String targetPath;
        private List<Integer> pages;

        public String getSourcePath() { return sourcePath; }
        public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
        public String getTargetPath() { return targetPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public List<Integer> getPages() { return pages; }
        public void setPages(List<Integer> pages) { this.pages = pages; }
    }
}
