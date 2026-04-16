package com.fileprocessor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fileprocessor.entity.FileFingerprint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Optional;

@Mapper
public interface FileFingerprintMapper extends BaseMapper<FileFingerprint> {

    @Select("SELECT * FROM file_fingerprint WHERE md5_hash = #{md5} AND file_size = #{size}")
    Optional<FileFingerprint> findByMd5AndSize(@Param("md5") String md5, @Param("size") Long size);

    @Select("SELECT * FROM file_fingerprint WHERE sha256_hash = #{sha256}")
    Optional<FileFingerprint> findBySha256(@Param("sha256") String sha256);

    @Update("UPDATE file_fingerprint SET reference_count = reference_count + 1, last_accessed_at = NOW() WHERE id = #{id}")
    int incrementReferenceCount(@Param("id") Long id);

    @Update("UPDATE file_fingerprint SET reference_count = reference_count - 1 WHERE id = #{id}")
    int decrementReferenceCount(@Param("id") Long id);
}
