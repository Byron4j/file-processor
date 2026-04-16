package com.fileprocessor.service.storage;

import java.io.InputStream;

/**
 * Storage service interface
 */
public interface StorageService {

    /**
     * Get storage type
     *
     * @return StorageType
     */
    StorageType getType();

    /**
     * Save file
     *
     * @param inputStream Input stream
     * @param path        Storage path
     * @param filename    Filename
     * @return Full storage path
     */
    String save(InputStream inputStream, String path, String filename);

    /**
     * Read file
     *
     * @param path Storage path
     * @return Input stream
     */
    InputStream read(String path);

    /**
     * Delete file
     *
     * @param path Storage path
     * @return true if successful
     */
    boolean delete(String path);

    /**
     * Check if file exists
     *
     * @param path Storage path
     * @return true if exists
     */
    boolean exists(String path);

    /**
     * Get file URL
     *
     * @param path Storage path
     * @return File URL
     */
    String getUrl(String path);

    /**
     * Get file size
     *
     * @param path Storage path
     * @return File size in bytes
     */
    long getSize(String path);

    /**
     * Initialize storage (create bucket/directory if needed)
     */
    default void initialize() {
        // Override in implementations if needed
    }
}
