package com.fileprocessor.service.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Local file system storage service
 */
@Component
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    @Value("${storage.local.base-path:./uploads}")
    private String basePath;

    @Value("${storage.local.url-prefix:/uploads}")
    private String urlPrefix;

    @Override
    public StorageType getType() {
        return StorageType.LOCAL;
    }

    @Override
    public void initialize() {
        try {
            Path path = Paths.get(basePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created local storage directory: {}", basePath);
            }
        } catch (IOException e) {
            log.error("Failed to initialize local storage", e);
        }
    }

    @Override
    public String save(InputStream inputStream, String path, String filename) {
        try {
            String fullPath = basePath + "/" + path;
            Path directory = Paths.get(fullPath);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            Path filePath = directory.resolve(filename);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

            String storagePath = path + "/" + filename;
            log.info("Saved file to local storage: {}", storagePath);
            return storagePath;

        } catch (IOException e) {
            log.error("Failed to save file to local storage", e);
            throw new RuntimeException("Failed to save file", e);
        }
    }

    @Override
    public InputStream read(String path) {
        try {
            Path filePath = Paths.get(basePath, path);
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            log.error("Failed to read file from local storage: {}", path, e);
            throw new RuntimeException("Failed to read file", e);
        }
    }

    @Override
    public boolean delete(String path) {
        try {
            Path filePath = Paths.get(basePath, path);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete file from local storage: {}", path, e);
            return false;
        }
    }

    @Override
    public boolean exists(String path) {
        Path filePath = Paths.get(basePath, path);
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }

    @Override
    public String getUrl(String path) {
        return urlPrefix + "/" + path;
    }

    @Override
    public long getSize(String path) {
        try {
            Path filePath = Paths.get(basePath, path);
            return Files.size(filePath);
        } catch (IOException e) {
            log.error("Failed to get file size: {}", path, e);
            return 0;
        }
    }

    /**
     * Get full file path
     */
    public String getFullPath(String path) {
        return basePath + "/" + path;
    }
}
