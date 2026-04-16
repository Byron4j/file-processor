package com.fileprocessor.ocr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;

/**
 * PaddleOCR 服务 - 中文识别效果更好的 OCR 引擎（简化版，需要外部 PaddleOCR 服务）
 */
@Service
public class PaddleOcrService {

    private static final Logger log = LoggerFactory.getLogger(PaddleOcrService.class);

    @Value("${ocr.paddle.model-path:./models}")
    private String modelPath;

    @Value("${ocr.paddle.use-gpu:false}")
    private boolean useGpu;

    @Value("${ocr.paddle.enabled:false}")
    private boolean enabled;

    private boolean initialized = false;

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("PaddleOCR is disabled. Enable it by setting ocr.paddle.enabled=true");
            return;
        }

        // TODO: 集成外部 PaddleOCR 服务或 JNI 调用
        // 目前需要用户自行部署 PaddleOCR 服务
        log.warn("PaddleOCR integration requires external service setup. Using Tesseract as fallback.");
        initialized = false;
    }

    /**
     * 识别图片文字 - 当前返回未初始化错误，使用 Tesseract 作为 fallback
     */
    public OcrResult recognize(File imageFile, boolean includeBoundingBox) {
        return OcrResult.failure("PaddleOCR not available. Please use Tesseract engine.");
    }

    /**
     * 识别图片（字节数组）
     */
    public OcrResult recognize(byte[] imageBytes, boolean includeBoundingBox) {
        return OcrResult.failure("PaddleOCR not available. Please use Tesseract engine.");
    }

    public boolean isInitialized() {
        return initialized;
    }
}
