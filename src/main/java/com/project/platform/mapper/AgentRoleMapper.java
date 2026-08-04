package com.project.platform.mapper;

import com.project.platform.entity.AgentRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface AgentRoleMapper {

    List<AgentRole> queryPage(Integer offset, Integer pageSize, @Param("query") Map<String, Object> query);

    int queryCount(@Param("query") Map<String, Object> query);

    @Select("SELECT * FROM agent_role WHERE id = #{id}")
    AgentRole selectById(Integer id);

    @Select("SELECT * FROM agent_role WHERE is_active = 1 ORDER BY sort_order")
    List<AgentRole> list();

    @Select("SELECT * FROM agent_role ORDER BY sort_order")
    List<AgentRole> listAll();

    int insert(AgentRole entity);

    int updateById(AgentRole entity);

    boolean removeByIds(List<Integer> ids);
}
