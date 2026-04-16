package com.fileprocessor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fileprocessor.entity.FileCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * File category mapper
 */
@Mapper
public interface FileCategoryMapper extends BaseMapper<FileCategory> {

    /**
     * Find by code
     */
    @Select("SELECT * FROM file_category WHERE code = #{code} AND status = 1")
    FileCategory findByCode(@Param("code") String code);

    /**
     * Find by parentId
     */
    @Select("SELECT * FROM file_category WHERE parent_id = #{parentId} AND status = 1 ORDER BY sort_order")
    List<FileCategory> findByParentId(@Param("parentId") Long parentId);

    /**
     * Find all active categories
     */
    @Select("SELECT * FROM file_category WHERE status = 1 ORDER BY sort_order")
    List<FileCategory> findAllActive();

    /**
     * Find root categories
     */
    @Select("SELECT * FROM file_category WHERE parent_id = 0 AND status = 1 ORDER BY sort_order")
    List<FileCategory> findRootCategories();
}
