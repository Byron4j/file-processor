# Phase 8: 音视频增强

## 目标
增强音视频处理能力，实现语音转录、字幕生成、视频剪辑、GIF生成等功能。

## 功能清单

### 1. 音频转录（语音转文字）

```
POST /api/media/audio/transcribe

请求:
{
  "sourcePath": "/uploads/recording.mp3",
  "language": "zh",          // 自动检测或指定
  "model": "whisper-1",      // whisper-1, whisper-large-v3
  "prompt": "会议记录",       // 提示词提高准确率
  "responseFormat": "srt"    // json, text, srt, vtt, verbose_json
}

响应:
{
  "success": true,
  "data": {
    "text": "完整转录文本...",
    "language": "zh",
    "duration": 125.5,
    "segments": [
      {
        "id": 0,
        "start": 0.0,
        "end": 4.2,
        "text": "大家好，今天我们来讨论一下",
        "confidence": 0.95
      },
      {
        "id": 1,
        "start": 4.2,
        "end": 8.5,
        "text": "下个季度的产品规划",
        "confidence": 0.92
      }
    ],
    "wordCount": 150,
    "processingTime": 3000
  }
}
```

**本地 Whisper 部署（可选）**
```yaml
services:
  whisper:
    image: onerahmet/openai-whisper-asr-webservice:latest
    environment:
      - ASR_MODEL=large-v3
      - ASR_ENGINE=faster_whisper
    volumes:
      - whisper-cache:/root/.cache/whisper
```

### 2. 视频字幕生成

```
POST /api/media/video/subtitle/generate

请求:
{
  "sourcePath": "/uploads/video.mp4",
  "targetPath": "/outputs/subtitles.srt",
  "language": "zh",
  "subtitleFormat": "SRT",   // SRT, VTT, ASS
  "translateTo": "en",       // 可选：翻译成其他语言
  "maxLineLength": 40,       // 每行最大字符数
  "maxLines": 2              // 最大行数
}

响应:
{
  "success": true,
  "data": {
    "targetPath": "/outputs/subtitles.srt",
    "format": "SRT",
    "totalSubtitles": 45,
    "language": "zh",
    "duration": 300.5
  }
}
```

### 3. 字幕烧录到视频

```
POST /api/media/video/subtitle/burn

请求:
{
  "sourcePath": "/uploads/video.mp4",
  "subtitlePath": "/uploads/subtitles.srt",
  "targetPath": "/outputs/video_with_subtitles.mp4",
  "subtitleStyle": {
    "fontName": "Noto Sans CJK SC",
    "fontSize": 24,
    "primaryColor": "#FFFFFF",
    "outlineColor": "#000000",
    "outlineWidth": 2,
    "alignment": "bottom-center"
  }
}
```

### 4. 视频合并

```
POST /api/media/video/merge

请求:
{
  "sourcePaths": [
    "/uploads/clip1.mp4",
    "/uploads/clip2.mp4",
    "/uploads/clip3.mp4"
  ],
  "targetPath": "/outputs/merged.mp4",
  "transition": "fade",      // none, fade, slide
  "transitionDuration": 1.0
}
```

### 5. 视频剪辑

```
POST /api/media/video/trim

请求:
{
  "sourcePath": "/uploads/video.mp4",
  "targetPath": "/outputs/trimmed.mp4",
  "segments": [
    {"start": 10.5, "end": 45.0},
    {"start": 120.0, "end": 180.5}
  ],
  "keepAudio": true
}
```

### 6. GIF 生成

```
POST /api/media/video/gif

请求:
{
  "sourcePath": "/uploads/video.mp4",
  "targetPath": "/outputs/animation.gif",
  "startTime": 10.0,
  "duration": 5.0,
  "width": 480,
  "height": 270,
  "fps": 15,
  "quality": 80,             // 0-100
  "optimize": true           // 启用优化
}

响应:
{
  "success": true,
  "data": {
    "targetPath": "/outputs/animation.gif",
    "width": 480,
    "height": 270,
    "duration": 5.0,
    "frameCount": 75,
    "fileSize": 2048000
  }
}
```

### 7. 视频添加水印

```
POST /api/media/video/watermark

请求:
{
  "sourcePath": "/uploads/video.mp4",
  "targetPath": "/outputs/watermarked.mp4",
  "watermarkType": "text",   // text, image
  "text": "CONFIDENTIAL",
  "fontSize": 24,
  "color": "#FF0000",
  "opacity": 0.5,
  "position": "top-right",   // top-left, top-right, bottom-left, bottom-right, center
  "marginX": 20,
  "marginY": 20
}
```

### 8. 视频格式批量转换

```
POST /api/media/video/batch-convert

请求:
{
  "files": [
    "/uploads/video1.avi",
    "/uploads/video2.mov"
  ],
  "outputDir": "/outputs/converted/",
  "targetFormat": "mp4",
  "videoCodec": "h264",
  "preset": "fast",
  "crf": 23
}
```

## 实现代码

```java
@Service
public class AudioTranscriptionService {
    
    @Value("${ai.whisper.api-key}")
    private String openaiApiKey;
    
    @Value("${ai.whisper.mode:api}")
    private String whisperMode;  // api or local
    
    private static final String WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions";
    
    /**
     * 使用 OpenAI Whisper API 转录
     */
    public TranscriptionResult transcribe(String audioPath, TranscriptionRequest request) {
        if ("local".equals(whisperMode)) {
            return transcribeLocal(audioPath, request);
        }
        return transcribeApi(audioPath, request);
    }
    
    private TranscriptionResult transcribeApi(String audioPath, TranscriptionRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openaiApiKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(audioPath));
        body.add("model", request.getModel() != null ? request.getModel() : "whisper-1");
        body.add("language", request.getLanguage());
        body.add("prompt", request.getPrompt());
        body.add("response_format", request.getResponseFormat());
        
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.postForEntity(
            WHISPER_API_URL, entity, Map.class);
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return parseTranscriptionResponse(response.getBody(), request);
        }
        
        throw new TranscriptionException("Whisper API failed: " + response.getStatusCode());
    }
    
    private TranscriptionResult transcribeLocal(String audioPath, TranscriptionRequest request) {
        // 调用本地 Whisper 服务
        String url = "http://localhost:9000/asr";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("audio_file", new FileSystemResource(audioPath));
        body.add("language", request.getLanguage());
        body.add("output", request.getResponseFormat());
        
        // 发送请求并解析响应
        // ...
    }
}
```

```java
@Service
public class VideoEditingService {
    
    /**
     * 合并多个视频
     */
    public void mergeVideos(List<String> sourcePaths, String targetPath, 
                           MergeOptions options) {
        // 创建 concat 文件列表
        String concatFile = createConcatFile(sourcePaths);
        
        StringBuilder cmd = new StringBuilder();
        cmd.append("ffmpeg -f concat -safe 0 -i ").append(concatFile);
        
        // 添加转场效果
        if ("fade".equals(options.getTransition())) {
            cmd.append(" -vf 'fade=st=0:d=1:alpha=1,fade=t=out:st=0:d=1:alpha=1'");
        }
        
        cmd.append(" -c copy ").append(targetPath);
        
        executeCommand(cmd.toString());
    }
    
    /**
     * 视频剪辑 - 提取多个片段
     */
    public void trimVideo(String sourcePath, String targetPath, 
                         List<TimeSegment> segments) {
        if (segments.size() == 1) {
            // 单片段直接裁剪
            TimeSegment seg = segments.get(0);
            String cmd = String.format(
                "ffmpeg -i %s -ss %f -t %f -c copy %s",
                sourcePath, seg.getStart(), 
                seg.getEnd() - seg.getStart(), targetPath
            );
            executeCommand(cmd);
        } else {
            // 多片段先分割再合并
            List<String> tempFiles = new ArrayList<>();
            for (int i = 0; i < segments.size(); i++) {
                TimeSegment seg = segments.get(i);
                String tempFile = targetPath + ".part" + i + ".mp4";
                String cmd = String.format(
                    "ffmpeg -i %s -ss %f -t %f -c copy %s",
                    sourcePath, seg.getStart(),
                    seg.getEnd() - seg.getStart(), tempFile
                );
                executeCommand(cmd);
                tempFiles.add(tempFile);
            }
            
            // 合并片段
            mergeVideos(tempFiles, targetPath, new MergeOptions());
            
            // 清理临时文件
            tempFiles.forEach(f -> new File(f).delete());
        }
    }
    
    /**
     * 生成 GIF
     */
    public void generateGif(String sourcePath, String targetPath, GifOptions options) {
        StringBuilder cmd = new StringBuilder();
        cmd.append("ffmpeg -i ").append(sourcePath);
        
        // 时间范围
        if (options.getStartTime() != null) {
            cmd.append(" -ss ").append(options.getStartTime());
        }
        if (options.getDuration() != null) {
            cmd.append(" -t ").append(options.getDuration());
        }
        
        // 尺寸和帧率
        cmd.append(" -vf 'fps=").append(options.getFps())
           .append(",scale=").append(options.getWidth()).append(":")
           .append(options.getHeight()).append(":flags=lanczos'");
        
        // 颜色优化
        if (options.isOptimize()) {
            cmd.append(" -colors 128");
        }
        
        cmd.append(" -loop 0 ").append(targetPath);
        
        executeCommand(cmd.toString());
    }
    
    /**
     * 烧录字幕
     */
    public void burnSubtitles(String videoPath, String subtitlePath, 
                             String targetPath, SubtitleStyle style) {
        String filter = String.format(
            "subtitles=%s:force_style='FontName=%s,FontSize=%s,PrimaryColour=%s,OutlineColour=%s,Outline=%s,Alignment=%s'",
            subtitlePath,
            style.getFontName(),
            style.getFontSize(),
            style.getPrimaryColor(),
            style.getOutlineColor(),
            style.getOutlineWidth(),
            getAlignmentValue(style.getAlignment())
        );
        
        String cmd = String.format(
            "ffmpeg -i %s -vf \"%s\" -c:a copy %s",
            videoPath, filter, targetPath
        );
        
        executeCommand(cmd);
    }
}
```

## API 端点汇总

| 方法 | 端点 | 描述 |
|------|------|------|
| POST | `/api/media/audio/transcribe` | 音频转录 |
| POST | `/api/media/video/subtitle/generate` | 生成字幕 |
| POST | `/api/media/video/subtitle/burn` | 字幕烧录 |
| POST | `/api/media/video/merge` | 视频合并 |
| POST | `/api/media/video/trim` | 视频剪辑 |
| POST | `/api/media/video/gif` | GIF 生成 |
| POST | `/api/media/video/watermark` | 视频水印 |
| POST | `/api/media/video/batch-convert` | 批量转换 |

## 配置

```yaml
media:
  whisper:
    mode: api  # api or local
    api-key: ${OPENAI_API_KEY:}
    model: whisper-1
    local-url: http://localhost:9000/asr
  
  ffmpeg:
    path: /usr/bin/ffmpeg
    timeout: 600
    thread-count: 4
    
  subtitle:
    default-font: "Noto Sans CJK SC"
    default-font-size: 24
```

## 验收标准

- [ ] 支持音频转文字（支持中文/英文）
- [ ] 支持生成 SRT/VTT 字幕文件
- [ ] 支持字幕烧录到视频
- [ ] 支持视频合并（带转场效果）
- [ ] 支持视频剪辑（单/多片段）
- [ ] 支持 GIF 生成（可配置尺寸/帧率）
- [ ] 支持视频添加文字/图片水印
- [ ] 支持批量视频格式转换
- [ ] 本地 Whisper 部署方案
