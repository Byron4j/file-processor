package com.fileprocessor.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PageMode;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * PDF editing utility for merge, split, rotate, and page operations
 */
public class PdfEditor {

    private static final Logger log = LoggerFactory.getLogger(PdfEditor.class);

    /**
     * PDF information holder
     */
    public static class PdfInfo {
        private int pageCount;
        private String title;
        private String author;
        private String subject;
        private String creator;
        private String producer;
        private String creationDate;
        private String modificationDate;
        private boolean encrypted;
        private List<Float> pageSizes;

        public int getPageCount() { return pageCount; }
        public void setPageCount(int pageCount) { this.pageCount = pageCount; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getCreator() { return creator; }
        public void setCreator(String creator) { this.creator = creator; }
        public String getProducer() { return producer; }
        public void setProducer(String producer) { this.producer = producer; }
        public String getCreationDate() { return creationDate; }
        public void setCreationDate(String creationDate) { this.creationDate = creationDate; }
        public String getModificationDate() { return modificationDate; }
        public void setModificationDate(String modificationDate) { this.modificationDate = modificationDate; }
        public boolean isEncrypted() { return encrypted; }
        public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
        public List<Float> getPageSizes() { return pageSizes; }
        public void setPageSizes(List<Float> pageSizes) { this.pageSizes = pageSizes; }
    }

    /**
     * Page range for split operations
     */
    public static class PageRange {
        private int start;
        private int end;
        private String name;

        public PageRange(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public PageRange(int start, int end, String name) {
            this(start, end);
            this.name = name;
        }

        public int getStart() { return start; }
        public void setStart(int start) { this.start = start; }
        public int getEnd() { return end; }
        public void setEnd(int end) { this.end = end; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    /**
     * Split configuration
     */
    public static class SplitConfig {
        private SplitMode mode = SplitMode.RANGE;
        private List<PageRange> ranges;
        private int everyNPages = 1;

        public SplitMode getMode() { return mode; }
        public void setMode(SplitMode mode) { this.mode = mode; }
        public List<PageRange> getRanges() { return ranges; }
        public void setRanges(List<PageRange> ranges) { this.ranges = ranges; }
        public int getEveryNPages() { return everyNPages; }
        public void setEveryNPages(int everyNPages) { this.everyNPages = everyNPages; }
    }

    public enum SplitMode {
        RANGE,           // Split by page ranges
        EVERY_N_PAGES,   // Split every N pages
        EXTRACT          // Extract specific pages
    }

    /**
     * Merge multiple PDFs into one
     *
     * @param sourcePaths List of source PDF paths
     * @param targetPath Target PDF path
     * @param addBookmarks Whether to add bookmarks for each source file
     * @return true if merge successful
     */
    public static boolean merge(List<String> sourcePaths, String targetPath, boolean addBookmarks) {
        log.info("Merging {} PDFs into: {}", sourcePaths.size(), targetPath);

        try {
            PDFMergerUtility merger = new PDFMergerUtility();
            merger.setDestinationFileName(targetPath);

            // Ensure target directory exists
            File targetFile = new File(targetPath);
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Add source files
            for (String sourcePath : sourcePaths) {
                merger.addSource(new File(sourcePath));
            }

            // Merge with bookmarks
            if (addBookmarks) {
                merger.mergeDocuments(null);
            } else {
                merger.mergeDocuments(null);
            }

            log.info("Successfully merged PDFs to: {}", targetPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to merge PDFs", e);
            return false;
        }
    }

    /**
     * Split PDF by configuration
     *
     * @param sourcePath Source PDF path
     * @param outputDir Output directory
     * @param config Split configuration
     * @return List of output file paths
     */
    public static List<String> split(String sourcePath, String outputDir, SplitConfig config) {
        log.info("Splitting PDF: {} with mode: {}", sourcePath, config.getMode());

        List<String> outputFiles = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(new File(sourcePath))) {
            int totalPages = document.getNumberOfPages();

            // Ensure output directory exists
            File outputDirectory = new File(outputDir);
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            switch (config.getMode()) {
                case RANGE:
                    outputFiles = splitByRanges(document, outputDir, config.getRanges(), sourcePath);
                    break;
                case EVERY_N_PAGES:
                    outputFiles = splitEveryNPages(document, outputDir, config.getEveryNPages(), sourcePath);
                    break;
                case EXTRACT:
                    // For extract mode, treat ranges as extract ranges
                    outputFiles = extractPages(document, outputDir, config.getRanges(), sourcePath);
                    break;
            }

            log.info("Successfully split PDF into {} files", outputFiles.size());
            return outputFiles;

        } catch (Exception e) {
            log.error("Failed to split PDF: {}", sourcePath, e);
            return outputFiles;
        }
    }

    /**
     * Extract specific pages from PDF
     *
     * @param sourcePath Source PDF path
     * @param targetPath Target PDF path
     * @param pages List of page numbers (1-based)
     * @return true if extraction successful
     */
    public static boolean extractPages(String sourcePath, String targetPath, List<Integer> pages) {
        log.info("Extracting pages {} from PDF: {} -> {}", pages, sourcePath, targetPath);

        try (PDDocument sourceDoc = Loader.loadPDF(new File(sourcePath));
             PDDocument targetDoc = new PDDocument()) {

            // Sort and validate pages
            List<Integer> validPages = new ArrayList<>();
            for (int pageNum : pages) {
                if (pageNum >= 1 && pageNum <= sourceDoc.getNumberOfPages()) {
                    validPages.add(pageNum);
                }
            }

            if (validPages.isEmpty()) {
                log.error("No valid pages to extract");
                return false;
            }

            // Extract pages
            for (int pageNum : validPages) {
                PDPage page = sourceDoc.getPage(pageNum - 1); // Convert to 0-based
                targetDoc.addPage(page);
            }

            // Ensure target directory exists
            File targetFile = new File(targetPath);
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            targetDoc.save(targetPath);

            log.info("Successfully extracted {} pages to: {}", validPages.size(), targetPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to extract pages from PDF: {}", sourcePath, e);
            return false;
        }
    }

    /**
     * Rotate specific pages in PDF
     *
     * @param sourcePath Source PDF path
     * @param targetPath Target PDF path
     * @param pages List of page numbers to rotate (1-based), empty for all pages
     * @param angle Rotation angle (90, 180, 270)
     * @return true if rotation successful
     */
    public static boolean rotate(String sourcePath, String targetPath, List<Integer> pages, int angle) {
        log.info("Rotating PDF pages: {} by {} degrees", pages.isEmpty() ? "all" : pages, angle);

        // Normalize angle
        angle = ((angle % 360) + 360) % 360;
        if (angle % 90 != 0) {
            log.error("Rotation angle must be multiple of 90: {}", angle);
            return false;
        }

        try (PDDocument document = Loader.loadPDF(new File(sourcePath))) {
            int totalPages = document.getNumberOfPages();

            // If no specific pages, rotate all
            List<Integer> pagesToRotate = pages.isEmpty()
                    ? java.util.stream.IntStream.rangeClosed(1, totalPages).boxed().toList()
                    : pages;

            for (int pageNum : pagesToRotate) {
                if (pageNum >= 1 && pageNum <= totalPages) {
                    PDPage page = document.getPage(pageNum - 1);
                    int currentRotation = page.getRotation();
                    page.setRotation(currentRotation + angle);
                }
            }

            // Ensure target directory exists
            File targetFile = new File(targetPath);
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            document.save(targetPath);

            log.info("Successfully rotated PDF to: {}", targetPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to rotate PDF: {}", sourcePath, e);
            return false;
        }
    }

    /**
     * Delete specific pages from PDF
     *
     * @param sourcePath Source PDF path
     * @param targetPath Target PDF path
     * @param pages List of page numbers to delete (1-based)
     * @return true if deletion successful
     */
    public static boolean deletePages(String sourcePath, String targetPath, List<Integer> pages) {
        log.info("Deleting pages {} from PDF: {} -> {}", pages, sourcePath, targetPath);

        try (PDDocument document = Loader.loadPDF(new File(sourcePath))) {
            int totalPages = document.getNumberOfPages();

            // Sort pages in descending order to delete from end to start
            // This prevents index shifting issues
            List<Integer> sortedPages = new ArrayList<>(pages);
            sortedPages.sort((a, b) -> b - a);

            for (int pageNum : sortedPages) {
                if (pageNum >= 1 && pageNum <= totalPages) {
                    document.removePage(pageNum - 1);
                    log.debug("Removed page: {}", pageNum);
                } else {
                    log.warn("Page {} is out of range, skipped", pageNum);
                }
            }

            if (document.getNumberOfPages() == 0) {
                log.error("Cannot save PDF with no pages");
                return false;
            }

            // Ensure target directory exists
            File targetFile = new File(targetPath);
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            document.save(targetPath);

            log.info("Successfully deleted {} pages, saved to: {}",
                    pages.size(), targetPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to delete pages from PDF: {}", sourcePath, e);
            return false;
        }
    }

    /**
     * Get PDF information
     *
     * @param filePath PDF file path
     * @return PdfInfo object
     */
    public static PdfInfo getInfo(String filePath) {
        log.info("Getting PDF info: {}", filePath);

        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            PdfInfo info = new PdfInfo();

            info.setPageCount(document.getNumberOfPages());
            info.setEncrypted(document.isEncrypted());

            // Get document information
            var docInfo = document.getDocumentInformation();
            if (docInfo != null) {
                info.setTitle(docInfo.getTitle());
                info.setAuthor(docInfo.getAuthor());
                info.setSubject(docInfo.getSubject());
                info.setCreator(docInfo.getCreator());
                info.setProducer(docInfo.getProducer());

                if (docInfo.getCreationDate() != null) {
                    info.setCreationDate(docInfo.getCreationDate().toString());
                }
                if (docInfo.getModificationDate() != null) {
                    info.setModificationDate(docInfo.getModificationDate().toString());
                }
            }

            // Get page sizes
            List<Float> pageSizes = new ArrayList<>();
            for (PDPage page : document.getPages()) {
                PDRectangle mediaBox = page.getMediaBox();
                pageSizes.add(mediaBox.getWidth());
                pageSizes.add(mediaBox.getHeight());
            }
            info.setPageSizes(pageSizes);

            return info;

        } catch (Exception e) {
            log.error("Failed to get PDF info: {}", filePath, e);
            return null;
        }
    }

    // ==================== Helper Methods ====================

    private static List<String> splitByRanges(PDDocument document, String outputDir,
                                               List<PageRange> ranges, String sourcePath) throws IOException {
        List<String> outputFiles = new ArrayList<>();

        String baseName = new File(sourcePath).getName();
        baseName = baseName.substring(0, baseName.lastIndexOf('.'));

        int rangeIndex = 1;
        for (PageRange range : ranges) {
            String outputName = range.getName() != null
                    ? range.getName()
                    : baseName + "_part" + rangeIndex + ".pdf";
            String outputPath = outputDir + File.separator + outputName;

            try (PDDocument newDoc = new PDDocument()) {
                for (int i = range.getStart(); i <= range.getEnd() && i <= document.getNumberOfPages(); i++) {
                    if (i >= 1) {
                        newDoc.addPage(document.getPage(i - 1));
                    }
                }
                newDoc.save(outputPath);
                outputFiles.add(outputPath);
            }
            rangeIndex++;
        }

        return outputFiles;
    }

    private static List<String> splitEveryNPages(PDDocument document, String outputDir,
                                                  int everyNPages, String sourcePath) throws IOException {
        List<String> outputFiles = new ArrayList<>();

        String baseName = new File(sourcePath).getName();
        baseName = baseName.substring(0, baseName.lastIndexOf('.'));

        int totalPages = document.getNumberOfPages();
        int partIndex = 1;

        for (int start = 1; start <= totalPages; start += everyNPages) {
            int end = Math.min(start + everyNPages - 1, totalPages);
            String outputPath = outputDir + File.separator + baseName + "_part" + partIndex + ".pdf";

            try (PDDocument newDoc = new PDDocument()) {
                for (int i = start; i <= end; i++) {
                    newDoc.addPage(document.getPage(i - 1));
                }
                newDoc.save(outputPath);
                outputFiles.add(outputPath);
            }
            partIndex++;
        }

        return outputFiles;
    }

    private static List<String> extractPages(PDDocument document, String outputDir,
                                              List<PageRange> ranges, String sourcePath) throws IOException {
        List<String> outputFiles = new ArrayList<>();

        String baseName = new File(sourcePath).getName();
        baseName = baseName.substring(0, baseName.lastIndexOf('.'));

        // For extract mode, create a single PDF with extracted pages
        String outputPath = outputDir + File.separator + baseName + "_extracted.pdf";

        try (PDDocument newDoc = new PDDocument()) {
            for (PageRange range : ranges) {
                for (int i = range.getStart(); i <= range.getEnd() && i <= document.getNumberOfPages(); i++) {
                    if (i >= 1) {
                        newDoc.addPage(document.getPage(i - 1));
                    }
                }
            }
            newDoc.save(outputPath);
            outputFiles.add(outputPath);
        }

        return outputFiles;
    }

    /**
     * Check if file is PDF
     */
    public static boolean isPdf(String filePath) {
        if (filePath == null) return false;
        return filePath.toLowerCase().endsWith(".pdf");
    }
}
