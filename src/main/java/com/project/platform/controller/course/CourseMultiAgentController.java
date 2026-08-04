package com.project.platform.controller.course;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.dto.courseworkflow.CourseMultiAgentReviewRequest;
import com.project.platform.dto.courseworkflow.CourseMultiAgentStartRequest;
import com.project.platform.dto.courseworkflow.CourseWorkflowSnapshotDTO;
import com.project.platform.dto.courseworkflow.CourseWorkflowState;
import com.project.platform.service.course.CourseMultiAgentWorkflowEngine;
import com.project.platform.service.course.WorkflowSseEmitterService;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 多智能体课程生成控制器
 * 5阶段工作流: 需求提取 -> 编排派遣 -> 资源搜集 -> 交叉审核 -> 资源交付入库
 */
@RestController
@RequestMapping("/course/multi-agent")
@Slf4j
public class CourseMultiAgentController {

    @Resource
    private CourseMultiAgentWorkflowEngine workflowEngine;

    @Resource
    private WorkflowSseEmitterService sseEmitterService;

    @Resource
    private com.project.platform.mapper.CourseWorkflowTaskMapper taskMapper;

    @Resource
    private com.project.platform.mapper.CourseWorkflowActivityMapper activityMapper;

    @Resource
    private com.project.platform.mapper.CourseWorkflowResourceMapper resourceMapper;

    @Resource
    private Tools tools;

    /**
     * 启动多智能体课程生成工作流
     */
    @PostMapping("/start")
    public ResponseVO<Map<String, String>> startWorkflow(@RequestBody CourseMultiAgentStartRequest request) {
        Integer userId = tools.getCurrentUserIdInteger();
        log.info("[多智能体工作流] 启动请求: userId={}, message={}", userId, request.getMessage());

        CourseWorkflowState state = new CourseWorkflowState();
        state.setMessage(request.getMessage());
        state.setUserId(userId);
        state.setCourseMode(request.getCourseMode());
        state.setDifficulty(request.getDifficulty());

        String taskId = workflowEngine.startWorkflow(state);

        log.info("[多智能体工作流] 已启动: taskId={}", taskId);
        return ResponseVO.ok(Map.of("taskId", taskId));
    }

    /**
     * SSE 实时流 — 推送工作流进度
     */
    @GetMapping(value = "/stream/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String taskId) {
        log.info("[SSE] 客户端连接: taskId={}", taskId);

        // 先发送当前快照
        SseEmitter emitter = sseEmitterService.register(taskId);
        CourseWorkflowSnapshotDTO snapshot = workflowEngine.getSnapshot(taskId);
        if (snapshot != null) {
            sseEmitterService.sendSnapshot(taskId, snapshot);
        }

        return emitter;
    }

    /**
     * 获取工作流当前快照（轮询兜底）
     */
    @GetMapping("/snapshot/{taskId}")
    public ResponseVO<CourseWorkflowSnapshotDTO> getSnapshot(@PathVariable String taskId) {
        CourseWorkflowSnapshotDTO snapshot = workflowEngine.getSnapshot(taskId);
        if (snapshot == null) {
            return ResponseVO.fail(404, "任务不存在或已过期");
        }
        return ResponseVO.ok(snapshot);
    }

    /**
     * 根据课程ID查找对应的工作流任务ID（用于查看历史生成过程）
     */
    @GetMapping("/history/{courseId}")
    public ResponseVO<Map<String, Object>> getHistoryByCourseId(@PathVariable Integer courseId) {
        com.project.platform.entity.CourseWorkflowTask task =
                taskMapper.selectByCourseId(courseId);
        if (task == null) {
            return ResponseVO.fail(404, "未找到该课程的工作流记录");
        }

        String taskId = task.getTaskId();

        // 优先从内存获取快照
        CourseWorkflowSnapshotDTO snapshot = workflowEngine.getSnapshot(taskId);

        // 内存没有则从 DB 重建
        if (snapshot == null) {
            snapshot = rebuildSnapshot(taskId, task);
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("courseId", task.getCourseId());
        result.put("status", task.getStatus());
        result.put("snapshot", snapshot);
        return ResponseVO.ok(result);
    }

    /**
     * 从数据库重建工作流快照（trace 数据过期时兜底）
     */
    private CourseWorkflowSnapshotDTO rebuildSnapshot(String taskId,
                                                       com.project.platform.entity.CourseWorkflowTask task) {
        CourseWorkflowSnapshotDTO dto = new CourseWorkflowSnapshotDTO();
        dto.setTaskId(taskId);
        dto.setTaskType("course-multi-agent");
        dto.setTitle(task.getTitle() != null ? task.getTitle() : "多智能体协作构建课程");
        dto.setSubtitle(task.getRequestMessage());
        dto.setStatus(task.getStatus() != null ? task.getStatus() : "completed");
        dto.setSummary(task.getSummary());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());
        dto.setFinishedAt(task.getFinishedAt());
        dto.setPhase("DELIVERY_COMPLETE");
        dto.setCourseId(task.getCourseId());

        // 重建阶段状态
        String[] stageIds = {"profile_analysis", "demand_extraction", "orchestration",
                "resource_collection", "cross_review", "delivery"};
        String[] stageTitles = {"学生画像分析", "需求提取", "编排派遣", "资源搜集", "交叉审核", "资源交付入库"};
        String[] stageAgents = {"画像分析师", "需求解析师", "任务调度师",
                "图片猎手/视频猎手/网页检索师/社区探勘师", "质量审核师/资料可信度验证专家",
                "内容生成专家/习题设计专家"};
        for (int i = 0; i < stageIds.length; i++) {
            CourseWorkflowSnapshotDTO.Step step = new CourseWorkflowSnapshotDTO.Step();
            step.setId(stageIds[i]);
            step.setTitle(stageTitles[i]);
            step.setAgent(stageAgents[i]);
            step.setStatus(i <= 5 ? "completed" : "pending");
            dto.getSteps().add(step);
        }

        // 从 DB 加载活动日志
        try {
            List<com.project.platform.entity.CourseWorkflowActivity> activities =
                    activityMapper.selectByTaskId(taskId);
            for (com.project.platform.entity.CourseWorkflowActivity act : activities) {
                CourseWorkflowSnapshotDTO.Activity a = new CourseWorkflowSnapshotDTO.Activity();
                a.setId(act.getActivityId());
                a.setStageId(act.getStageId());
                a.setAgentName(act.getAgentName());
                a.setAgentRole(act.getAgentRole());
                a.setType(act.getType());
                a.setLabel(act.getLabel());
                a.setMessage(act.getMessage());
                a.setTimestamp(act.getTimestamp());
                dto.getActivities().add(a);
            }
        } catch (Exception e) {
            log.warn("加载活动日志失败: taskId={}", taskId);
        }

        // 从 DB 加载资源
        try {
            List<com.project.platform.entity.CourseWorkflowResource> resources =
                    resourceMapper.selectByTaskId(taskId);
            for (com.project.platform.entity.CourseWorkflowResource res : resources) {
                CourseWorkflowSnapshotDTO.Evidence ev = new CourseWorkflowSnapshotDTO.Evidence();
                ev.setId(String.valueOf(res.getId()));
                ev.setKind(res.getKind());
                ev.setTitle(res.getTitle());
                ev.setUrl(res.getUrl());
                ev.setSource(res.getSource());
                ev.setSnippet(res.getSnippet());
                ev.setCredibilityLabel(res.getCredibilityLabel());
                ev.setScore(res.getScore());
                dto.getEvidences().add(ev);
            }
        } catch (Exception e) {
            log.warn("加载资源列表失败: taskId={}", taskId);
        }

        return dto;
    }

    /**
     * 提交审查决策，恢复工作流执行
     */
    @PostMapping("/review/{taskId}")
    public ResponseVO<Map<String, String>> submitReview(
            @PathVariable String taskId,
            @RequestBody CourseMultiAgentReviewRequest request) {
        log.info("[审查] 收到决策: taskId={}, decision={}", taskId, request.getDecision());

        boolean success = workflowEngine.submitReview(
                taskId,
                request.getDecision(),
                request.getFeedback(),
                request.getResourceActions()
        );

        if (!success) {
            return ResponseVO.fail(404, "任务不存在或不在审查等待状态");
        }

        return ResponseVO.ok(Map.of(
                "taskId", taskId,
                "status", "resumed"
        ));
    }
}
