package com.fileprocessor.task;

/**
 * Task progress listener interface
 */
public interface TaskProgressListener {

    /**
     * Called when progress updates
     *
     * @param progress Current progress (0-100)
     * @param message  Current step description
     */
    void onProgress(int progress, String message);

    /**
     * Called when task completes
     *
     * @param result Task result
     */
    void onComplete(TaskResult result);

    /**
     * Called when task fails
     *
     * @param error Error message
     * @param stackTrace Error stack trace
     */
    void onError(String error, String stackTrace);

    /**
     * Check if task is cancelled
     *
     * @return true if cancelled
     */
    boolean isCancelled();
}
