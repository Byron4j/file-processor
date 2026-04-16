package com.fileprocessor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fileprocessor.entity.FileSplitRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 文件分割记录 Mapper
 */
@Mapper
public interface FileSplitRecordMapper extends BaseMapper<FileSplitRecord> {

    /**
     * 根据分割ID查询
     */
    @Select("SELECT * FROM file_split_record WHERE split_id = #{splitId}")
    FileSplitRecord selectBySplitId(@Param("splitId") String splitId);

    /**
     * 更新状态
     */
    @Update("UPDATE file_split_record SET status = #{status}, completed_at = #{completedAt} WHERE split_id = #{splitId}")
    int updateStatus(@Param("splitId") String splitId, @Param("status") String status, @Param("completedAt") LocalDateTime completedAt);
}
