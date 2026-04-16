package com.fileprocessor.service;

import com.fileprocessor.dto.FileResponse;
import com.fileprocessor.dto.TranscriptionRequest;
import com.fileprocessor.media.TranscriptionResult;
import com.fileprocessor.media.TranscriptionResult.Segment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 音频转录服务 - 使用 Whisper API
 */
@Service
public class AudioTranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(AudioTranscriptionService.class);

    @Value("${media.whisper.mode:api}")
    private String whisperMode;

    @Value("${media.whisper.api-key:${OPENAI_API_KEY:}}")
    private String openaiApiKey;

    @Value("${media.whisper.model:whisper-1}")
    private String defaultModel;

    @Value("${media.whisper.local-url:http://localhost:9000/asr}")
    private String localWhisperUrl;

    private static final String WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions";

    /**
     * 音频转录
     */
    public FileResponse transcribe(TranscriptionRequest request) {
        log.info("Transcribing audio: {}, language={}, mode={}",
                request.getSourcePath(), request.getLanguage(), whisperMode);

        long startTime = System.currentTimeMillis();

        try {
            File audioFile = new File(request.getSourcePath());
            if (!audioFile.exists()) {
                return FileResponse.builder()
                        .success(false)
                        .message("Audio file not found: " + request.getSourcePath())
                        .build();
            }

            TranscriptionResult result;
            if ("local".equalsIgnoreCase(whisperMode)) {
                result = transcribeLocal(audioFile, request);
            } else {
                result = transcribeApi(audioFile, request);
            }

            long processingTime = System.currentTimeMillis() - startTime;

            if (!result.isSuccess()) {
                return FileResponse.builder()
                        .success(false)
                        .message("Transcription failed: " + result.getErrorMessage())
                        .build();
            }

            return FileResponse.builder()
                    .success(true)
                    .message("Transcription completed")
                    .data(Map.of(
                            "text", result.getText(),
                            "language", result.getLanguage() != null ? result.getLanguage() : request.getLanguage(),
                            "duration", result.getDuration(),
                            "segments", result.getSegments() != null ? result.getSegments() : new ArrayList<>(),
                            "wordCount", result.getWordCount(),
                            "processingTime", processingTime
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Transcription failed", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Transcription failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 使用 OpenAI Whisper API
     */
    private TranscriptionResult transcribeApi(File audioFile, TranscriptionRequest request) {
        try {
            if (openaiApiKey == null || openaiApiKey.isEmpty()) {
                return TranscriptionResult.failure("OpenAI API key not configured");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(openaiApiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(audioFile));
            body.add("model", request.getModel() != null ? request.getModel() : defaultModel);
            if (request.getLanguage() != null) {
                body.add("language", request.getLanguage());
            }
            if (request.getPrompt() != null) {
                body.add("prompt", request.getPrompt());
            }
            body.add("response_format", request.getResponseFormat() != null ? request.getResponseFormat() : "verbose_json");

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    WHISPER_API_URL, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseApiResponse(response.getBody());
            }

            return TranscriptionResult.failure("API returned: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("Whisper API call failed", e);
            return TranscriptionResult.failure(e.getMessage());
        }
    }

    /**
     * 使用本地 Whisper 服务
     */
    private TranscriptionResult transcribeLocal(File audioFile, TranscriptionRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("audio_file", new FileSystemResource(audioFile));
            if (request.getLanguage() != null) {
                body.add("language", request.getLanguage());
            }
            body.add("output", request.getResponseFormat() != null ? request.getResponseFormat() : "srt");

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(
                    localWhisperUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                TranscriptionResult result = new TranscriptionResult();
                result.setSuccess(true);
                result.setText(response.getBody());
                result.setLanguage(request.getLanguage());
                return result;
            }

            return TranscriptionResult.failure("Local service returned: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("Local Whisper call failed", e);
            return TranscriptionResult.failure(e.getMessage());
        }
    }

    /**
     * 解析 API 响应
     */
    private TranscriptionResult parseApiResponse(Map<String, Object> response) {
        String text = (String) response.get("text");
        String language = (String) response.get("language");
        Double duration = response.get("duration") != null ? ((Number) response.get("duration")).doubleValue() : 0;

        List<Segment> segments = new ArrayList<>();
        if (response.get("segments") instanceof List) {
            List<Map<String, Object>> segList = (List<Map<String, Object>>) response.get("segments");
            for (int i = 0; i < segList.size(); i++) {
                Map<String, Object> seg = segList.get(i);
                Segment segment = new Segment();
                segment.setId(i);
                segment.setStart(((Number) seg.get("start")).doubleValue());
                segment.setEnd(((Number) seg.get("end")).doubleValue());
                segment.setText((String) seg.get("text"));
                if (seg.get("avg_logprob") != null) {
                    segment.setConfidence(Math.exp(((Number) seg.get("avg_logprob")).doubleValue()));
                }
                segments.add(segment);
            }
        }

        return TranscriptionResult.success(text, language, duration, segments, 0);
    }

    /**
     * 生成字幕文件
     */
    public FileResponse generateSubtitle(TranscriptionRequest request, String targetPath, String format) {
        FileResponse transcriptionResponse = transcribe(request);

        if (!transcriptionResponse.isSuccess()) {
            return transcriptionResponse;
        }

        try {
            Map<String, Object> data = (Map<String, Object>) transcriptionResponse.getData();
            List<Map<String, Object>> segments = (List<Map<String, Object>>) data.get("segments");

            StringBuilder subtitle = new StringBuilder();

            if ("srt".equalsIgnoreCase(format)) {
                // SRT format
                for (int i = 0; i < segments.size(); i++) {
                    Map<String, Object> seg = segments.get(i);
                    subtitle.append(i + 1).append("\n");
                    subtitle.append(formatTime(((Number) seg.get("start")).doubleValue())).append(" --> ")
                            .append(formatTime(((Number) seg.get("end")).doubleValue())).append("\n");
                    subtitle.append(seg.get("text")).append("\n\n");
                }
            } else if ("vtt".equalsIgnoreCase(format)) {
                // WebVTT format
                subtitle.append("WEBVTT\n\n");
                for (int i = 0; i < segments.size(); i++) {
                    Map<String, Object> seg = segments.get(i);
                    subtitle.append(formatTimeVtt(((Number) seg.get("start")).doubleValue())).append(" --> ")
                            .append(formatTimeVtt(((Number) seg.get("end")).doubleValue())).append("\n");
                    subtitle.append(seg.get("text")).append("\n\n");
                }
            }

            // Write to file
            java.nio.file.Path path = java.nio.file.Paths.get(targetPath);
            java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.writeString(path, subtitle.toString());

            return FileResponse.builder()
                    .success(true)
                    .message("Subtitle generated")
                    .filePath(targetPath)
                    .data(Map.of(
                            "format", format,
                            "totalSubtitles", segments.size(),
                            "language", data.get("language")
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate subtitle", e);
            return FileResponse.builder()
                    .success(false)
                    .message("Failed to generate subtitle: " + e.getMessage())
                    .build();
        }
    }

    private String formatTime(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        int millis = (int) ((seconds % 1) * 1000);
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, secs, millis);
    }

    private String formatTimeVtt(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        int millis = (int) ((seconds % 1) * 1000);
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, secs, millis);
    }
}
