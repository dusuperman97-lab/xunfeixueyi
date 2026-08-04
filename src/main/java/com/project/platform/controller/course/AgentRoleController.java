package com.project.platform.controller.course;

import com.project.platform.dto.courseworkflow.WorkflowStructureDTO;
import com.project.platform.entity.AgentRole;
import com.project.platform.service.AgentRoleService;
import com.project.platform.vo.PageVO;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/agent-role")
public class AgentRoleController {

    @Resource
    private AgentRoleService agentRoleService;

    @GetMapping("/page")
    public ResponseVO<PageVO<AgentRole>> page(@RequestParam Map<String, Object> query,
                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize) {
        return ResponseVO.ok(agentRoleService.page(query, pageNum, pageSize));
    }

    @GetMapping("/selectById/{id}")
    public ResponseVO<AgentRole> selectById(@PathVariable("id") Integer id) {
        return ResponseVO.ok(agentRoleService.selectById(id));
    }

    @GetMapping("/list")
    public ResponseVO<List<AgentRole>> list() {
        return ResponseVO.ok(agentRoleService.list());
    }

    @GetMapping("/listAll")
    public ResponseVO<List<AgentRole>> listAll() {
        return ResponseVO.ok(agentRoleService.listAll());
    }

    @PostMapping("/add")
    public ResponseVO<?> add(@RequestBody AgentRole entity) {
        agentRoleService.insert(entity);
        return ResponseVO.ok();
    }

    @PutMapping("/update")
    public ResponseVO<?> update(@RequestBody AgentRole entity) {
        agentRoleService.updateById(entity);
        return ResponseVO.ok();
    }

    @DeleteMapping("/delBatch")
    public ResponseVO<?> delBatch(@RequestBody List<Integer> ids) {
        agentRoleService.removeByIds(ids);
        return ResponseVO.ok();
    }

    @GetMapping("/workflow-structure")
    public ResponseVO<WorkflowStructureDTO> workflowStructure() {
        return ResponseVO.ok(agentRoleService.getWorkflowStructure());
    }

    @PutMapping("/update-stage")
    public ResponseVO<?> updateStage(@RequestBody Map<String, Object> body) {
        Integer agentId = (Integer) body.get("agentId");
        String stageId = (String) body.get("stageId");
        String stageName = (String) body.get("stageName");
        agentRoleService.updateAgentStage(agentId, stageId, stageName);
        return ResponseVO.ok();
    }
}
