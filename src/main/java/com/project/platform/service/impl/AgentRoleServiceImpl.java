package com.project.platform.service.impl;

import com.project.platform.dto.courseworkflow.WorkflowStructureDTO;
import com.project.platform.entity.AgentRole;
import com.project.platform.mapper.AgentRoleMapper;
import com.project.platform.service.AgentRoleService;
import com.project.platform.vo.PageVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AgentRoleServiceImpl implements AgentRoleService {

    @Resource
    private AgentRoleMapper agentRoleMapper;

    private static final LinkedHashMap<String, String> STAGE_ORDER = new LinkedHashMap<>();
    static {
        STAGE_ORDER.put("profile_analysis", "学生画像分析");
        STAGE_ORDER.put("demand_extraction", "需求提取");
        STAGE_ORDER.put("orchestration", "编排派遣");
        STAGE_ORDER.put("resource_collection", "资源搜集");
        STAGE_ORDER.put("cross_review", "交叉审核");
        STAGE_ORDER.put("delivery", "资源交付入库");
    }

    @Override
    public PageVO<AgentRole> page(Map<String, Object> query, Integer pageNum, Integer pageSize) {
        PageVO<AgentRole> page = new PageVO();
        List<AgentRole> list = agentRoleMapper.queryPage((pageNum - 1) * pageSize, pageSize, query);
        page.setList(list);
        page.setTotal(agentRoleMapper.queryCount(query));
        return page;
    }

    @Override
    public AgentRole selectById(Integer id) {
        return agentRoleMapper.selectById(id);
    }

    @Override
    public List<AgentRole> list() {
        return agentRoleMapper.list();
    }

    @Override
    public List<AgentRole> listAll() {
        return agentRoleMapper.listAll();
    }

    @Override
    public void insert(AgentRole entity) {
        long now = System.currentTimeMillis();
        if (entity.getIsActive() == null) {
            entity.setIsActive(1);
        }
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        agentRoleMapper.insert(entity);
    }

    @Override
    public void updateById(AgentRole entity) {
        entity.setUpdatedAt(System.currentTimeMillis());
        agentRoleMapper.updateById(entity);
    }

    @Override
    public void removeByIds(List<Integer> ids) {
        agentRoleMapper.removeByIds(ids);
    }

    @Override
    public WorkflowStructureDTO getWorkflowStructure() {
        WorkflowStructureDTO dto = new WorkflowStructureDTO();
        List<AgentRole> allRoles = agentRoleMapper.listAll();

        // 按 stage 分组
        Map<String, List<AgentRole>> grouped = new LinkedHashMap<>();
        for (AgentRole role : allRoles) {
            String stageId = role.getStageId() != null ? role.getStageId() : "_unassigned";
            grouped.computeIfAbsent(stageId, k -> new ArrayList<>()).add(role);
        }

        int order = 0;
        for (Map.Entry<String, String> entry : STAGE_ORDER.entrySet()) {
            String stageId = entry.getKey();
            String stageName = entry.getValue();
            WorkflowStructureDTO.StageNode stageNode = new WorkflowStructureDTO.StageNode();
            stageNode.setStageId(stageId);
            stageNode.setStageName(stageName);
            stageNode.setSortOrder(order++);

            List<AgentRole> agents = grouped.getOrDefault(stageId, Collections.emptyList());
            for (AgentRole role : agents) {
                WorkflowStructureDTO.AgentNode agentNode = new WorkflowStructureDTO.AgentNode();
                agentNode.setId(role.getId());
                agentNode.setRoleCode(role.getRoleCode());
                agentNode.setRoleName(role.getRoleName());
                agentNode.setDescription(role.getDescription());
                agentNode.setAccentColor(role.getAccentColor());
                agentNode.setAvatar(role.getAvatar());
                agentNode.setSortOrder(role.getSortOrder());
                agentNode.setIsActive(role.getIsActive());
                stageNode.getAgents().add(agentNode);
            }
            dto.getStages().add(stageNode);
        }
        return dto;
    }

    @Override
    public void updateAgentStage(Integer agentId, String stageId, String stageName) {
        AgentRole role = agentRoleMapper.selectById(agentId);
        if (role == null) return;
        role.setStageId(stageId);
        role.setStageName(stageName);
        role.setUpdatedAt(System.currentTimeMillis());
        agentRoleMapper.updateById(role);
    }
}
