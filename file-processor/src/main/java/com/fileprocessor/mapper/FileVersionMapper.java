package com.fileprocessor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fileprocessor.entity.FileVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文件版本 Mapper
 */
@Mapper
public interface FileVersionMapper extends BaseMapper<FileVersion> {

    /**
     * 根据文件ID查询版本列表
     */
    @Select("SELECT * FROM file_version WHERE file_id = #{fileId} ORDER BY version_number DESC")
    List<FileVersion> selectByFileId(@Param("fileId") String fileId);

    /**
     * 查询最大版本号
     */
    @Select("SELECT MAX(version_number) FROM file_version WHERE file_id = #{fileId}")
    Integer selectMaxVersionByFileId(@Param("fileId") String fileId);

    /**
     * 根据版本ID查询
     */
    @Select("SELECT * FROM file_version WHERE version_id = #{versionId}")
    FileVersion selectByVersionId(@Param("versionId") String versionId);

    /**
     * 查询版本数量
     */
    @Select("SELECT COUNT(*) FROM file_version WHERE file_id = #{fileId}")
    Long countByFileId(@Param("fileId") String fileId);

    /**
     * 删除旧版本（保留最近的N个）
     */
    @Select("DELETE FROM file_version WHERE file_id = #{fileId} AND version_number <= " +
            "(SELECT version_number FROM (SELECT version_number FROM file_version " +
            "WHERE file_id = #{fileId} ORDER BY version_number DESC LIMIT 1 OFFSET #{keepCount}) t)")
    int deleteOldVersions(@Param("fileId") String fileId, @Param("keepCount") int keepCount);
}
