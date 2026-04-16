package com.fileprocessor.service.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Storage manager for managing multiple storage backends
 */
@Service
public class StorageManager {

    private static final Logger log = LoggerFactory.getLogger(StorageManager.class);

    @Value("${storage.default:LOCAL}")
    private String defaultStorageType;

    @Autowired
    private List<StorageService> storageServices;

    private final Map<String, StorageService> serviceMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (StorageService service : storageServices) {
            serviceMap.put(service.getType().getCode(), service);
            service.initialize();
            log.info("Registered storage service: {}", service.getType());
        }
    }

    /**
     * Get storage service by type
     */
    public StorageService getService(StorageType type) {
        StorageService service = serviceMap.get(type.getCode());
        if (service == null) {
            throw new IllegalArgumentException("Storage service not found: " + type);
        }
        return service;
    }

    /**
     * Get storage service by code
     */
    public StorageService getService(String code) {
        StorageService service = serviceMap.get(code);
        if (service == null) {
            throw new IllegalArgumentException("Storage service not found: " + code);
        }
        return service;
    }

    /**
     * Get default storage service
     */
    public StorageService getDefaultService() {
        return getService(defaultStorageType);
    }

    /**
     * Get default storage type
     */
    public StorageType getDefaultType() {
        return StorageType.fromCode(defaultStorageType);
    }

    /**
     * Check if storage type is available
     */
    public boolean isAvailable(StorageType type) {
        return serviceMap.containsKey(type.getCode());
    }

    /**
     * Get all available storage types
     */
    public Map<String, StorageService> getAllServices() {
        return new HashMap<>(serviceMap);
    }

    /**
     * Check if file exists in storage
     */
    public boolean exists(String storagePath) {
        if (storagePath == null || storagePath.isEmpty()) {
            return false;
        }
        try {
            return getDefaultService().exists(storagePath);
        } catch (Exception e) {
            return false;
        }
    }
}
