package com.fileprocessor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fileprocessor.entity.FileUploadSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 文件上传会话 Mapper
 */
@Mapper
public interface FileUploadSessionMapper extends BaseMapper<FileUploadSession> {

    /**
     * 根据上传ID查询
     */
    @Select("SELECT * FROM file_upload_session WHERE upload_id = #{uploadId}")
    FileUploadSession selectByUploadId(@Param("uploadId") String uploadId);

    /**
     * 更新已上传分片
     */
    @Update("UPDATE file_upload_session SET uploaded_chunks = #{uploadedChunks}, " +
            "status = #{status}, updated_at = NOW() WHERE upload_id = #{uploadId}")
    int updateChunks(@Param("uploadId") String uploadId,
                     @Param("uploadedChunks") String uploadedChunks,
                     @Param("status") String status);

    /**
     * 清理过期会话
     */
    @Update("DELETE FROM file_upload_session WHERE expire_at < NOW()")
    int deleteExpiredSessions();
}
