package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.util.ArchiveUtil;
import com.fileprocessor.util.ArchiveUtil.ArchiveInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;

/**
 * Archive processing service for 7z and RAR
 */
@Service
public class ArchiveService {

    private static final Logger log = LoggerFactory.getLogger(ArchiveService.class);

    /**
     * Create archive (ZIP/7z/TAR)
     */
    public FileResponse createArchive(java.util.List<String> sourcePaths, String targetPath, String format,
                                      Integer compressionLevel, String password) {
        log.info("Service: Creating archive: format={}, files={}", format, sourcePaths.size());

        // Validate source paths
        for (String sourcePath : sourcePaths) {
            File sourceFile = new File(sourcePath);
            if (!sourceFile.exists()) {
                return FileResponse.builder()
                        .success(false)
                        .message("Source path does not exist: " + sourcePath)
                        .build();
            }
        }

        // Generate target path if not provided
        if (targetPath == null || targetPath.isEmpty()) {
            targetPath = "./outputs/archive_" + System.currentTimeMillis() + "." + getExtension(format);
        }

        // Default compression level
        if (compressionLevel == null) {
            compressionLevel = 6;
        }

        String archiveFormat = format != null ? format.toUpperCase() : "ZIP";

        try {
            // Create archive using ArchiveCreationService
            ArchiveCreationService creationService = new ArchiveCreationService();
            boolean success = false;

            switch (archiveFormat) {
                case "ZIP":
                    success = createZipArchive(sourcePaths, targetPath, compressionLevel, password);
                    break;
                case "7Z":
                    success = create7zArchive(sourcePaths, targetPath, compressionLevel, password);
                    break;
                case "TAR":
                    success = createTarArchive(sourcePaths, targetPath, false);
                    break;
                case "TAR_GZ":
                case "TAR.GZ":
                    success = createTarArchive(sourcePaths, targetPath, true);
                    break;
                default:
                    return FileResponse.builder()
                            .success(false)
                            .message("Unsupported archive format: " + archiveFormat)
                            .build();
            }

            if (!success) {
                return FileResponse.builder()
                        .success(false)
                        .message("Failed to create archive")
                        .build();
            }

            File targetFile = new File(targetPath);
            long originalSize = calculateTotalSize(sourcePaths);
            long compressedSize = targetFile.length();
            double compressionRatio = originalSize > 0 ? (double) compressedSize / originalSize : 1.0;

            return FileResponse.builder()
                    .success(true)
                    .message("Archive created successfully")
                    .filePath(targetPath)
                    .fileSize(compressedSize)
                    .data(Map.of(
                            "targetPath", targetPath,
                            "format", archiveFormat,
                            "totalFiles", sourcePaths.size(),
                            "originalSize", originalSize,
                            "compressedSize", compressedSize,
                            "compressionRatio", String.format("%.2f%%", compressionRatio * 100)
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Failed to create archive", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to create archive: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Create ZIP archive
     */
    private boolean createZipArchive(java.util.List<String> sourcePaths, String targetPath,
                                     int compressionLevel, String password) {
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(targetPath))) {
            zos.setLevel(compressionLevel);

            for (String sourcePath : sourcePaths) {
                File sourceFile = new File(sourcePath);
                addFileToZip(zos, sourceFile, sourceFile.getName(), password);
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to create ZIP archive", e);
            return false;
        }
    }

    private void addFileToZip(java.util.zip.ZipOutputStream zos, File file, String entryName, String password) throws java.io.IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    addFileToZip(zos, child, entryName + "/" + child.getName(), password);
                }
            }
        } else {
            java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(entryName);
            zos.putNextEntry(entry);
            java.nio.file.Files.copy(file.toPath(), zos);
            zos.closeEntry();
        }
    }

    /**
     * Create 7z archive - simplified implementation using ZIP as fallback
     */
    private boolean create7zArchive(java.util.List<String> sourcePaths, String targetPath,
                                    int compressionLevel, String password) {
        // For now, create a ZIP archive with .7z extension
        // Full 7z creation requires more complex implementation
        log.warn("7z creation not fully implemented, using ZIP format");
        return createZipArchive(sourcePaths, targetPath, compressionLevel, password);
    }

    /**
     * Create TAR archive
     */
    private boolean createTarArchive(java.util.List<String> sourcePaths, String targetPath, boolean compress) {
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(targetPath);
             java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(fos)) {

            java.io.OutputStream out = compress ? new java.util.zip.GZIPOutputStream(bos) : bos;
            org.apache.commons.compress.archivers.tar.TarArchiveOutputStream tos =
                    new org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(out);
            tos.setLongFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU);

            for (String sourcePath : sourcePaths) {
                File sourceFile = new File(sourcePath);
                addFileToTar(tos, sourceFile, sourceFile.getName());
            }
            tos.close();
            return true;
        } catch (Exception e) {
            log.error("Failed to create TAR archive", e);
            return false;
        }
    }

    private void addFileToTar(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream tos, File file, String entryName) throws java.io.IOException {
        if (file.isDirectory()) {
            org.apache.commons.compress.archivers.tar.TarArchiveEntry entry = new org.apache.commons.compress.archivers.tar.TarArchiveEntry(file, entryName);
            tos.putArchiveEntry(entry);
            tos.closeArchiveEntry();

            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    addFileToTar(tos, child, entryName + "/" + child.getName());
                }
            }
        } else {
            org.apache.commons.compress.archivers.tar.TarArchiveEntry entry = new org.apache.commons.compress.archivers.tar.TarArchiveEntry(file, entryName);
            tos.putArchiveEntry(entry);
            java.nio.file.Files.copy(file.toPath(), tos);
            tos.closeArchiveEntry();
        }
    }

    /**
     * Calculate total size of source paths
     */
    private long calculateTotalSize(java.util.List<String> sourcePaths) {
        long totalSize = 0;
        for (String sourcePath : sourcePaths) {
            File file = new File(sourcePath);
            if (file.isFile()) {
                totalSize += file.length();
            } else if (file.isDirectory()) {
                try {
                    totalSize += java.nio.file.Files.walk(file.toPath())
                            .filter(p -> p.toFile().isFile())
                            .mapToLong(p -> p.toFile().length())
                            .sum();
                } catch (java.io.IOException e) {
                    log.warn("Failed to calculate size for: {}", sourcePath);
                }
            }
        }
        return totalSize;
    }

    private String getExtension(String format) {
        if (format == null) return "zip";
        return switch (format.toUpperCase()) {
            case "7Z" -> "7z";
            case "TAR" -> "tar";
            case "TAR_GZ", "TAR.GZ" -> "tar.gz";
            default -> "zip";
        };
    }

    /**
     * Extract 7z archive
     */
    public FileResponse extract7z(String sourcePath, String targetPath, String password) {
        log.info("Service: Extracting 7z: {} -> {}", sourcePath, targetPath);

        // Validate source file
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file does not exist: " + sourcePath)
                    .build();
        }

        // Validate format
        if (!sourcePath.toLowerCase().endsWith(".7z")) {
            return FileResponse.builder()
                    .success(false)
                    .message("Not a 7z file: " + sourcePath)
                    .build();
        }

        // Perform extraction
        boolean success = ArchiveUtil.extract7z(sourcePath, targetPath, password);

        if (!success) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to extract 7z archive (may be encrypted or corrupted)")
                    .build();
        }

        return FileResponse.builder()
                .success(true)
                .message("7z archive extracted successfully")
                .filePath(targetPath)
                .build();
    }

    /**
     * Extract RAR archive
     */
    public FileResponse extractRar(String sourcePath, String targetPath, String password) {
        log.info("Service: Extracting RAR: {} -> {}", sourcePath, targetPath);

        // Validate source file
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Source file does not exist: " + sourcePath)
                    .build();
        }

        // Validate format
        if (!sourcePath.toLowerCase().endsWith(".rar")) {
            return FileResponse.builder()
                    .success(false)
                    .message("Not a RAR file: " + sourcePath)
                    .build();
        }

        // Perform extraction
        boolean success = ArchiveUtil.extractRar(sourcePath, targetPath, password);

        if (!success) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to extract RAR archive (may be encrypted or corrupted)")
                    .build();
        }

        return FileResponse.builder()
                .success(true)
                .message("RAR archive extracted successfully")
                .filePath(targetPath)
                .build();
    }

    /**
     * Get archive information
     */
    public FileResponse getArchiveInfo(String archivePath) {
        log.info("Service: Getting archive info: {}", archivePath);

        // Validate file
        File file = new File(archivePath);
        if (!file.exists()) {
            return FileResponse.builder()
                    .success(false)
                    .message("Archive file does not exist: " + archivePath)
                    .build();
        }

        // Validate format
        if (!ArchiveUtil.isSupportedFormat(archivePath)) {
            return FileResponse.builder()
                    .success(false)
                    .message("Unsupported archive format. Supported: 7z, RAR")
                    .build();
        }

        // Get info
        ArchiveInfo info = ArchiveUtil.getInfo(archivePath);

        if (info == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to get archive information")
                    .build();
        }

        return FileResponse.builder()
                .success(true)
                .message("Archive information retrieved")
                .filePath(archivePath)
                .data(Map.of(
                        "format", info.getFormat(),
                        "totalFiles", info.getTotalFiles(),
                        "totalDirectories", info.getTotalDirectories(),
                        "totalSize", info.getTotalSize(),
                        "compressedSize", info.getCompressedSize(),
                        "compressionRatio", String.format("%.2f%%",
                                (1 - (double) info.getCompressedSize() / info.getTotalSize()) * 100),
                        "entries", info.getEntries()
                ))
                .build();
    }

    /**
     * Extract archive (auto-detect format)
     */
    public FileResponse extract(String sourcePath, String targetPath, String password) {
        log.info("Service: Extracting archive: {} -> {}", sourcePath, targetPath);

        String format = ArchiveUtil.getArchiveFormat(sourcePath);

        if (format == null) {
            return FileResponse.builder()
                    .success(false)
                    .message("Unsupported archive format")
                    .build();
        }

        return switch (format) {
            case "7z" -> extract7z(sourcePath, targetPath, password);
            case "RAR" -> extractRar(sourcePath, targetPath, password);
            default -> FileResponse.builder()
                    .success(false)
                    .message("Unsupported archive format: " + format)
                    .build();
        };
    }
}
