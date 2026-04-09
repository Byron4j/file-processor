package com.fileprocessor.dto;

import jakarta.validation.constraints.NotBlank;

public class FileConvertRequest {

    @NotBlank(message = "Source file path cannot be empty")
    private String sourcePath;

    @NotBlank(message = "Target file path cannot be empty")
    private String targetPath;

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }
}
