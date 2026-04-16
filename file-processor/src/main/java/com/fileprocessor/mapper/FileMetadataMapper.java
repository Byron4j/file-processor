package com.fileprocessor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fileprocessor.entity.FileMetadata;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * File metadata mapper
 */
@Mapper
public interface FileMetadataMapper extends BaseMapper<FileMetadata> {

    /**
     * Find by fileId
     */
    @Select("SELECT * FROM file_metadata WHERE file_id = #{fileId}")
    FileMetadata findByFileId(@Param("fileId") String fileId);

    /**
     * Find by MD5 hash
     */
    @Select("SELECT * FROM file_metadata WHERE md5_hash = #{md5Hash} LIMIT 1")
    FileMetadata findByMd5Hash(@Param("md5Hash") String md5Hash);

    /**
     * Find by category
     */
    @Select("SELECT * FROM file_metadata WHERE category_id = #{categoryId} AND status = 1")
    List<FileMetadata> findByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * Find expired files
     */
    @Select("SELECT * FROM file_metadata WHERE expire_at < NOW() AND status = 1")
    List<FileMetadata> findExpiredFiles();

    /**
     * Increment reference count
     */
    @Update("UPDATE file_metadata SET reference_count = reference_count + 1 WHERE file_id = #{fileId}")
    int incrementReferenceCount(@Param("fileId") String fileId);

    /**
     * Decrement reference count
     */
    @Update("UPDATE file_metadata SET reference_count = reference_count - 1 WHERE file_id = #{fileId}")
    int decrementReferenceCount(@Param("fileId") String fileId);

    /**
     * Soft delete by fileId
     */
    @Update("UPDATE file_metadata SET status = 0 WHERE file_id = #{fileId}")
    int softDeleteByFileId(@Param("fileId") String fileId);

    /**
     * Update tags
     */
    @Update("UPDATE file_metadata SET tags = #{tags} WHERE file_id = #{fileId}")
    int updateTags(@Param("fileId") String fileId, @Param("tags") String tags);

    /**
     * Count by status
     */
    @Select("SELECT COUNT(*) FROM file_metadata WHERE status = #{status}")
    Long countByStatus(@Param("status") Integer status);

    /**
     * Sum file size by category
     */
    @Select("SELECT SUM(file_size) FROM file_metadata WHERE category_id = #{categoryId} AND status = 1")
    Long sumFileSizeByCategory(@Param("categoryId") Long categoryId);

    /**
     * Find by file hash (SHA-256)
     */
    @Select("SELECT * FROM file_metadata WHERE file_hash = #{fileHash} AND file_size = #{fileSize} AND status = 1 LIMIT 1")
    FileMetadata selectByHash(@Param("fileHash") String fileHash, @Param("fileSize") Long fileSize);

    /**
     * Increment access count
     */
    @Update("UPDATE file_metadata SET access_count = access_count + 1, last_access_at = NOW() WHERE file_id = #{fileId}")
    int incrementAccessCount(@Param("fileId") String fileId);
}
