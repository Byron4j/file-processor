package com.fileprocessor.util;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Archive utility for 7z and RAR support
 */
public class ArchiveUtil {

    private static final Logger log = LoggerFactory.getLogger(ArchiveUtil.class);

    /**
     * Archive entry information
     */
    public static class ArchiveEntry {
        private String name;
        private long size;
        private long compressedSize;
        private boolean isDirectory;
        private Date lastModified;
        private String path;

        public ArchiveEntry(String name, long size, boolean isDirectory) {
            this.name = name;
            this.size = size;
            this.isDirectory = isDirectory;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public long getCompressedSize() { return compressedSize; }
        public void setCompressedSize(long compressedSize) { this.compressedSize = compressedSize; }
        public boolean isDirectory() { return isDirectory; }
        public void setDirectory(boolean directory) { isDirectory = directory; }
        public Date getLastModified() { return lastModified; }
        public void setLastModified(Date lastModified) { this.lastModified = lastModified; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    /**
     * Archive information
     */
    public static class ArchiveInfo {
        private String format;
        private int totalFiles;
        private int totalDirectories;
        private long totalSize;
        private long compressedSize;
        private List<ArchiveEntry> entries;

        public ArchiveInfo() {
            this.entries = new ArrayList<>();
        }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public int getTotalFiles() { return totalFiles; }
        public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }
        public int getTotalDirectories() { return totalDirectories; }
        public void setTotalDirectories(int totalDirectories) { this.totalDirectories = totalDirectories; }
        public long getTotalSize() { return totalSize; }
        public void setTotalSize(long totalSize) { this.totalSize = totalSize; }
        public long getCompressedSize() { return compressedSize; }
        public void setCompressedSize(long compressedSize) { this.compressedSize = compressedSize; }
        public List<ArchiveEntry> getEntries() { return entries; }
        public void setEntries(List<ArchiveEntry> entries) { this.entries = entries; }
    }

    // ==================== 7z Support ====================

    /**
     * Extract 7z archive
     *
     * @param archivePath Path to 7z file
     * @param outputDir Output directory
     * @param password Password (optional, can be null)
     * @return true if extraction successful
     */
    public static boolean extract7z(String archivePath, String outputDir, String password) {
        log.info("Extracting 7z archive: {} -> {}", archivePath, outputDir);

        try {
            File outputDirectory = new File(outputDir);
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            try (RandomAccessFile randomAccessFile = new RandomAccessFile(archivePath, "r");
                 IInArchive inArchive = SevenZip.openInArchive(null, new RandomAccessFileInStream(randomAccessFile))) {

                ISimpleInArchive simpleInArchive = inArchive.getSimpleInterface();
                ISimpleInArchiveItem[] items = simpleInArchive.getArchiveItems();

                for (ISimpleInArchiveItem item : items) {
                    final String itemPath = item.getPath();
                    File outputFile = new File(outputDirectory, itemPath);

                    if (item.isFolder()) {
                        outputFile.mkdirs();
                        continue;
                    }

                    // Ensure parent directory exists
                    File parentDir = outputFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }

                    // Extract file
                    ExtractOperationResult result;
                    if (password != null && !password.isEmpty()) {
                        result = item.extractSlow(data -> {
                            try (FileOutputStream fos = new FileOutputStream(outputFile, true)) {
                                fos.write(data);
                            } catch (IOException e) {
                                log.error("Failed to write file: {}", outputFile, e);
                                return Integer.MAX_VALUE;
                            }
                            return data.length;
                        }, password);
                    } else {
                        result = item.extractSlow(data -> {
                            try (FileOutputStream fos = new FileOutputStream(outputFile, true)) {
                                fos.write(data);
                            } catch (IOException e) {
                                log.error("Failed to write file: {}", outputFile, e);
                                return Integer.MAX_VALUE;
                            }
                            return data.length;
                        });
                    }

                    if (result != ExtractOperationResult.OK) {
                        log.error("Failed to extract item: {}, result: {}", itemPath, result);
                        return false;
                    }
                }

                log.info("Successfully extracted 7z archive: {}", archivePath);
                return true;
            }

        } catch (Exception e) {
            log.error("Failed to extract 7z archive: {}", archivePath, e);
            return false;
        }
    }

    /**
     * Get 7z archive information
     *
     * @param archivePath Path to 7z file
     * @return ArchiveInfo object
     */
    public static ArchiveInfo get7zInfo(String archivePath) {
        log.info("Getting 7z archive info: {}", archivePath);

        ArchiveInfo info = new ArchiveInfo();
        info.setFormat("7z");

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(archivePath, "r");
             IInArchive inArchive = SevenZip.openInArchive(null, new RandomAccessFileInStream(randomAccessFile))) {

            ISimpleInArchive simpleInArchive = inArchive.getSimpleInterface();
            ISimpleInArchiveItem[] items = simpleInArchive.getArchiveItems();

            for (ISimpleInArchiveItem item : items) {
                ArchiveEntry entry = new ArchiveEntry(
                        item.getPath(),
                        item.getSize(),
                        item.isFolder()
                );
                entry.setCompressedSize(item.getPackedSize());
                info.getEntries().add(entry);

                if (item.isFolder()) {
                    info.setTotalDirectories(info.getTotalDirectories() + 1);
                } else {
                    info.setTotalFiles(info.getTotalFiles() + 1);
                }
                info.setTotalSize(info.getTotalSize() + item.getSize());
                info.setCompressedSize(info.getCompressedSize() + item.getPackedSize());
            }

            return info;

        } catch (Exception e) {
            log.error("Failed to get 7z archive info: {}", archivePath, e);
            return null;
        }
    }

    // ==================== RAR Support ====================

    /**
     * Extract RAR archive
     *
     * @param archivePath Path to RAR file
     * @param outputDir Output directory
     * @param password Password (optional, can be null)
     * @return true if extraction successful
     */
    public static boolean extractRar(String archivePath, String outputDir, String password) {
        log.info("Extracting RAR archive: {} -> {}", archivePath, outputDir);

        try {
            File outputDirectory = new File(outputDir);
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            try (Archive archive = new Archive(new File(archivePath), password)) {
                if (archive.isEncrypted()) {
                    log.warn("RAR archive is encrypted but no password provided or wrong password");
                    return false;
                }

                FileHeader fileHeader;
                while ((fileHeader = archive.nextFileHeader()) != null) {
                    String fileName = fileHeader.getFileName();
                    File outputFile = new File(outputDirectory, fileName);

                    if (fileHeader.isDirectory()) {
                        outputFile.mkdirs();
                        continue;
                    }

                    // Ensure parent directory exists
                    File parentDir = outputFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }

                    // Extract file
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        archive.extractFile(fileHeader, fos);
                    }
                }

                log.info("Successfully extracted RAR archive: {}", archivePath);
                return true;
            }

        } catch (Exception e) {
            log.error("Failed to extract RAR archive: {}", archivePath, e);
            return false;
        }
    }

    /**
     * Get RAR archive information
     *
     * @param archivePath Path to RAR file
     * @return ArchiveInfo object
     */
    public static ArchiveInfo getRarInfo(String archivePath) {
        log.info("Getting RAR archive info: {}", archivePath);

        ArchiveInfo info = new ArchiveInfo();
        info.setFormat("RAR");

        try (Archive archive = new Archive(new File(archivePath))) {
            FileHeader fileHeader;
            while ((fileHeader = archive.nextFileHeader()) != null) {
                ArchiveEntry entry = new ArchiveEntry(
                        fileHeader.getFileName(),
                        fileHeader.getFullUnpackSize(),
                        fileHeader.isDirectory()
                );
                entry.setCompressedSize(fileHeader.getFullPackSize());
                entry.setLastModified(fileHeader.getMTime());
                info.getEntries().add(entry);

                if (fileHeader.isDirectory()) {
                    info.setTotalDirectories(info.getTotalDirectories() + 1);
                } else {
                    info.setTotalFiles(info.getTotalFiles() + 1);
                }
                info.setTotalSize(info.getTotalSize() + fileHeader.getFullUnpackSize());
                info.setCompressedSize(info.getCompressedSize() + fileHeader.getFullPackSize());
            }

            return info;

        } catch (Exception e) {
            log.error("Failed to get RAR archive info: {}", archivePath, e);
            return null;
        }
    }

    // ==================== Generic Methods ====================

    /**
     * Extract archive based on file extension
     *
     * @param archivePath Path to archive file
     * @param outputDir Output directory
     * @param password Password (optional)
     * @return true if extraction successful
     */
    public static boolean extract(String archivePath, String outputDir, String password) {
        String lowerPath = archivePath.toLowerCase();
        if (lowerPath.endsWith(".7z")) {
            return extract7z(archivePath, outputDir, password);
        } else if (lowerPath.endsWith(".rar")) {
            return extractRar(archivePath, outputDir, password);
        } else {
            log.error("Unsupported archive format: {}", archivePath);
            return false;
        }
    }

    /**
     * Get archive information based on file extension
     *
     * @param archivePath Path to archive file
     * @return ArchiveInfo object
     */
    public static ArchiveInfo getInfo(String archivePath) {
        String lowerPath = archivePath.toLowerCase();
        if (lowerPath.endsWith(".7z")) {
            return get7zInfo(archivePath);
        } else if (lowerPath.endsWith(".rar")) {
            return getRarInfo(archivePath);
        } else {
            log.error("Unsupported archive format: {}", archivePath);
            return null;
        }
    }

    /**
     * Check if file is supported archive format
     */
    public static boolean isSupportedFormat(String filePath) {
        if (filePath == null) return false;
        String lowerPath = filePath.toLowerCase();
        return lowerPath.endsWith(".7z") || lowerPath.endsWith(".rar");
    }

    /**
     * Get archive format name
     */
    public static String getArchiveFormat(String filePath) {
        if (filePath == null) return null;
        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".7z")) return "7z";
        if (lowerPath.endsWith(".rar")) return "RAR";
        return null;
    }
}
