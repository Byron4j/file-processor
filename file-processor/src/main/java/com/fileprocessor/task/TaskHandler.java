package com.fileprocessor.task;

import java.util.Map;

/**
 * Task handler interface
 *
 * @param <T> Task parameters type
 */
public interface TaskHandler<T> {

    /**
     * Get supported task type
     *
     * @return TaskType
     */
    TaskType getTaskType();

    /**
     * Parse parameters from map
     *
     * @param parameters Parameter map
     * @return Parsed parameters object
     */
    T parseParameters(Map<String, Object> parameters);

    /**
     * Execute task
     *
     * @param parameters Task parameters
     * @param listener   Progress listener
     * @return Task result
     */
    TaskResult execute(T parameters, TaskProgressListener listener);

    /**
     * Validate parameters before execution
     *
     * @param parameters Task parameters
     * @return Validation result
     */
    default boolean validate(T parameters) {
        return parameters != null;
    }

    /**
     * Get estimated execution time in milliseconds
     *
     * @param parameters Task parameters
     * @return Estimated time
     */
    default long estimateTime(T parameters) {
        return 30000; // Default 30 seconds
    }
}
