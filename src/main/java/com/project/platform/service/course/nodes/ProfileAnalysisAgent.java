package com.project.platform.service.course.nodes;

import com.project.platform.dto.courseworkflow.CourseWorkflowSnapshotDTO;
import com.project.platform.dto.courseworkflow.CourseWorkflowState;
import com.project.platform.service.course.CourseWorkflowTraceService;
import com.project.platform.service.course.WorkflowSseEmitterService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProfileAnalysisAgent {

    @Resource
    private CourseWorkflowTraceService traceService;
    @Resource
    private WorkflowSseEmitterService sseEmitterService;

    public CourseWorkflowState execute(CourseWorkflowState state) {
        String taskId = state.getTaskId();
        log.info("[学生画像分析] 开始: taskId={}", taskId);
        traceService.bind(taskId);
        traceService.startStep(taskId, "profile_analysis", "正在分析学生画像数据...");

        traceService.logActivity(taskId, "profile_analysis", "画像分析师", "学生画像分析",
                "analysis", "画像解析",
                "正在读取学生画像：学习风格、知识薄弱点、能力雷达图...");

        try {
            // 模拟画像分析
            Thread.sleep(400);

            traceService.logActivity(taskId, "profile_analysis", "画像分析师", "学生画像分析",
                    "result", "画像结果",
                    "已完成学生画像分析。学习偏好: " +
                    (state.getCourseMode() != null ? state.getCourseMode() : "综合") +
                    ", 难度: " +
                    (state.getDifficulty() != null ? state.getDifficulty() : "适中") +
                    "。将据此定制课程内容与节奏。");

            state.getStageStatus().put("profile_analysis", "completed");
            traceService.completeStep(taskId, "profile_analysis", "学生画像分析完成，已生成个性化学习策略");
            pushSnapshot(taskId);

            log.info("[学生画像分析] 完成: taskId={}", taskId);
        } catch (Exception e) {
            log.error("[学生画像分析] 失败: taskId={}", taskId, e);
            state.getStageStatus().put("profile_analysis", "failed");
            throw new RuntimeException("学生画像分析失败", e);
        }
        return state;
    }

    private void pushSnapshot(String taskId) {
        CourseWorkflowSnapshotDTO s = traceService.getSnapshot(taskId);
        if (s != null) sseEmitterService.sendSnapshot(taskId, s);
    }
}
