package com.project.platform.service;

import com.project.platform.dto.courseworkflow.WorkflowStructureDTO;
import com.project.platform.entity.AgentRole;
import com.project.platform.vo.PageVO;

import java.util.List;
import java.util.Map;

public interface AgentRoleService {

    PageVO<AgentRole> page(Map<String, Object> query, Integer pageNum, Integer pageSize);

    AgentRole selectById(Integer id);

    List<AgentRole> list();

    List<AgentRole> listAll();

    void insert(AgentRole entity);

    void updateById(AgentRole entity);

    void removeByIds(List<Integer> ids);

    WorkflowStructureDTO getWorkflowStructure();

    void updateAgentStage(Integer agentId, String stageId, String stageName);
}
