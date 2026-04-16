package com.fileprocessor.service;

import com.fileprocessor.websocket.ProgressWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 进度通知服务 - 用于发送实时进度更新
 */
@Service
public class ProgressNotifier {

    private static final Logger log = LoggerFactory.getLogger(ProgressNotifier.class);

    @Autowired
    private ProgressWebSocketHandler webSocketHandler;

    /**
     * 发送上传进度
     *
     * @param userId   用户ID
     * @param taskId   任务ID（uploadId 或 fileId）
     * @param progress 进度百分比 (0-100)
     * @param status   状态: PENDING, UPLOADING, PROCESSING, COMPLETED, FAILED
     * @param message  状态消息
     */
    public void notifyUploadProgress(String userId, String taskId, int progress, String status, String message) {
        log.debug("Notifying progress: userId={}, taskId={}, progress={}%", userId, taskId, progress);
        webSocketHandler.sendProgress(userId, taskId, progress, status, message);
    }

    /**
     * 发送分片上传进度
     *
     * @param userId        用户ID
     * @param uploadId      上传会话ID
     * @param chunkNumber   当前分片号
     * @param totalChunks   总分片数
     */
    public void notifyChunkProgress(String userId, String uploadId, int chunkNumber, int totalChunks) {
        int progress = (int) ((chunkNumber * 100.0) / totalChunks);
        String message = String.format("上传分片 %d/%d", chunkNumber, totalChunks);
        notifyUploadProgress(userId, uploadId, progress, "UPLOADING", message);
    }

    /**
     * 通知上传完成
     */
    public void notifyUploadComplete(String userId, String taskId, String fileId, long fileSize) {
        notifyUploadProgress(userId, taskId, 100, "COMPLETED",
                String.format("上传完成，文件ID: %s, 大小: %d bytes", fileId, fileSize));
    }

    /**
     * 通知上传失败
     */
    public void notifyUploadFailed(String userId, String taskId, String errorMessage) {
        notifyUploadProgress(userId, taskId, 0, "FAILED", errorMessage);
    }

    /**
     * 通知处理中状态
     */
    public void notifyProcessing(String userId, String taskId, String message) {
        notifyUploadProgress(userId, taskId, -1, "PROCESSING", message);
    }

    /**
     * 通知打包下载进度
     */
    public void notifyArchiveProgress(String userId, String taskId, int progress, int currentFile, int totalFiles, String fileName) {
        String message = String.format("正在打包 %d/%d: %s", currentFile, totalFiles, fileName);
        notifyUploadProgress(userId, taskId, progress, "PROCESSING", message);
    }

    /**
     * 通知打包完成
     */
    public void notifyArchiveComplete(String userId, String taskId, String downloadUrl) {
        notifyUploadProgress(userId, taskId, 100, "COMPLETED",
                "打包完成，下载链接: " + downloadUrl);
    }

    /**
     * 获取WebSocket连接数
     */
    public int getActiveConnectionCount() {
        return webSocketHandler.getConnectionCount();
    }
}
