package com.fileprocessor.dto;

/**
 * 音频转录请求
 */
public class TranscriptionRequest {

    private String sourcePath;
    private String language = "zh";
    private String model = "whisper-1";
    private String prompt;
    private String responseFormat = "srt";  // json, text, srt, vtt, verbose_json

    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getResponseFormat() { return responseFormat; }
    public void setResponseFormat(String responseFormat) { this.responseFormat = responseFormat; }
}
