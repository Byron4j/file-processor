package com.fileprocessor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fileprocessor.entity.TaskRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Task record mapper
 */
@Mapper
public interface TaskRecordMapper extends BaseMapper<TaskRecord> {

    /**
     * Find by taskId
     */
    @Select("SELECT * FROM task_record WHERE task_id = #{taskId}")
    TaskRecord findByTaskId(@Param("taskId") String taskId);

    /**
     * Find by status
     */
    @Select("SELECT * FROM task_record WHERE status = #{status} ORDER BY created_at DESC")
    List<TaskRecord> findByStatus(@Param("status") String status);

    /**
     * Find pending tasks
     */
    @Select("SELECT * FROM task_record WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT #{limit}")
    List<TaskRecord> findPendingTasks(@Param("limit") Integer limit);

    /**
     * Find processing tasks
     */
    @Select("SELECT * FROM task_record WHERE status = 'PROCESSING' ORDER BY started_at DESC")
    List<TaskRecord> findProcessingTasks();

    /**
     * Update task status
     */
    @Update("UPDATE task_record SET status = #{status}, updated_at = NOW() WHERE task_id = #{taskId}")
    int updateStatus(@Param("taskId") String taskId, @Param("status") String status);

    /**
     * Update task progress
     */
    @Update("UPDATE task_record SET progress = #{progress}, current_step = #{currentStep}, " +
            "processed_items = #{processedItems}, updated_at = NOW() WHERE task_id = #{taskId}")
    int updateProgress(@Param("taskId") String taskId,
                       @Param("progress") Integer progress,
                       @Param("currentStep") String currentStep,
                       @Param("processedItems") Integer processedItems);

    /**
     * Mark task as completed
     */
    @Update("UPDATE task_record SET status = 'SUCCESS', progress = 100, " +
            "result = #{result}, completed_at = NOW(), updated_at = NOW() WHERE task_id = #{taskId}")
    int markCompleted(@Param("taskId") String taskId, @Param("result") String result);

    /**
     * Mark task as failed
     */
    @Update("UPDATE task_record SET status = 'FAILED', error_message = #{errorMessage}, " +
            "error_stack = #{errorStack}, completed_at = NOW(), updated_at = NOW() WHERE task_id = #{taskId}")
    int markFailed(@Param("taskId") String taskId,
                   @Param("errorMessage") String errorMessage,
                   @Param("errorStack") String errorStack);

    /**
     * Increment retry count
     */
    @Update("UPDATE task_record SET retry_count = retry_count + 1, updated_at = NOW() WHERE task_id = #{taskId}")
    int incrementRetryCount(@Param("taskId") String taskId);

    /**
     * Update callback status
     */
    @Update("UPDATE task_record SET callback_status = #{callbackStatus}, updated_at = NOW() WHERE task_id = #{taskId}")
    int updateCallbackStatus(@Param("taskId") String taskId, @Param("callbackStatus") String callbackStatus);

    /**
     * Count by status and type
     */
    @Select("SELECT COUNT(*) FROM task_record WHERE status = #{status} AND task_type = #{taskType}")
    Long countByStatusAndType(@Param("status") String status, @Param("taskType") String taskType);

    /**
     * Find tasks needing callback
     */
    @Select("SELECT * FROM task_record WHERE callback_url IS NOT NULL " +
            "AND callback_status = 'PENDING' AND status IN ('SUCCESS', 'FAILED')")
    List<TaskRecord> findPendingCallbacks();
}
