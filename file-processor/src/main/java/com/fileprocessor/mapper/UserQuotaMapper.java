package com.fileprocessor.mapper;

import com.fileprocessor.entity.UserQuota;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 用户配额 Mapper
 */
@Mapper
public interface UserQuotaMapper {

    /**
     * 根据用户ID查询配额
     */
    @Select("SELECT * FROM user_quota WHERE user_id = #{userId}")
    UserQuota selectByUserId(@Param("userId") Long userId);

    /**
     * 插入配额记录
     */
    int insert(UserQuota quota);

    /**
     * 更新配额记录
     */
    int updateById(UserQuota quota);

    /**
     * 增加已使用存储空间
     */
    @Update("UPDATE user_quota SET used_storage_quota = used_storage_quota + #{size}, " +
            "updated_at = NOW() WHERE user_id = #{userId}")
    int increaseUsedStorage(@Param("userId") Long userId, @Param("size") Long size);

    /**
     * 减少已使用存储空间
     */
    @Update("UPDATE user_quota SET used_storage_quota = GREATEST(0, used_storage_quota - #{size}), " +
            "updated_at = NOW() WHERE user_id = #{userId}")
    int decreaseUsedStorage(@Param("userId") Long userId, @Param("size") Long size);

    /**
     * 增加今日已上传
     */
    @Update("UPDATE user_quota SET daily_upload_used = daily_upload_used + #{size}, " +
            "updated_at = NOW() WHERE user_id = #{userId}")
    int increaseDailyUpload(@Param("userId") Long userId, @Param("size") Long size);

    /**
     * 重置每日上传限制
     */
    @Update("UPDATE user_quota SET daily_upload_used = 0, daily_reset_at = #{resetTime}, " +
            "updated_at = NOW() WHERE user_id = #{userId}")
    int resetDailyUpload(@Param("userId") Long userId, @Param("resetTime") LocalDateTime resetTime);

    /**
     * 增加文件计数
     */
    @Update("UPDATE user_quota SET current_file_count = current_file_count + 1, " +
            "updated_at = NOW() WHERE user_id = #{userId}")
    int increaseFileCount(@Param("userId") Long userId);

    /**
     * 减少文件计数
     */
    @Update("UPDATE user_quota SET current_file_count = GREATEST(0, current_file_count - 1), " +
            "updated_at = NOW() WHERE user_id = #{userId}")
    int decreaseFileCount(@Param("userId") Long userId);
}
