package com.fileprocessor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fileprocessor.entity.TeamMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TeamMemberMapper extends BaseMapper<TeamMember> {
}
