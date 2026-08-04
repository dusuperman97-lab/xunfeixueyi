package com.project.platform.service.course.nodes;

import com.project.platform.dto.ChapterOutlineDTO;
import com.project.platform.dto.courseworkflow.CourseWorkflowSnapshotDTO;
import com.project.platform.dto.courseworkflow.CourseWorkflowState;
import com.project.platform.service.course.CourseWorkflowTraceService;
import com.project.platform.service.course.WorkflowSseEmitterService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class OrchestrationAgent {

    @Resource
    private CourseWorkflowTraceService traceService;
    @Resource
    private WorkflowSseEmitterService sseEmitterService;

    public CourseWorkflowState execute(CourseWorkflowState state) {
        String taskId = state.getTaskId();
        log.info("[编排派遣] 开始: taskId={}", taskId);
        traceService.bind(taskId);
        traceService.startStep(taskId, "orchestration", "正在规划资源搜索策略...");

        traceService.logActivity(taskId, "orchestration", "任务调度师", "任务编排",
                "dispatch", "策略规划",
                "根据课程「" + state.getCourseName() + "」的" +
                (state.getOutline() != null ? state.getOutline().size() : 0) +
                "个章节规划多维度搜索策略。");

        try {
            List<String> searchQueries = buildSearchQueries(state);
            state.setSearchQueries(searchQueries);
            state.getStageStatus().put("orchestration", "completed");

            // 逐条展示搜索策略
            StringBuilder queryDetail = new StringBuilder("已生成 " + searchQueries.size() + " 条搜索策略：");
            for (int i = 0; i < Math.min(searchQueries.size(), 5); i++) {
                queryDetail.append("\n").append(i + 1).append(". ").append(searchQueries.get(i));
            }
            if (searchQueries.size() > 5) queryDetail.append("\n...等共 ").append(searchQueries.size()).append(" 条");

            traceService.logActivity(taskId, "orchestration", "任务调度师", "派遣决策",
                    "dispatch", "搜索派遣", queryDetail.toString());
            traceService.completeStep(taskId, "orchestration",
                    "已派遣 " + searchQueries.size() + " 个维度搜索任务");
            pushSnapshot(taskId);

            log.info("[编排派遣] 完成: taskId={}, queries={}", taskId, searchQueries.size());
        } catch (Exception e) {
            log.error("[编排派遣] 失败: taskId={}", taskId, e);
            state.getStageStatus().put("orchestration", "failed");
            throw new RuntimeException("编排派遣失败", e);
        }
        return state;
    }

    private List<String> buildSearchQueries(CourseWorkflowState state) {
        List<String> queries = new ArrayList<>();
        String topic = state.getTopic() != null ? state.getTopic() : state.getCourseName();

        queries.add(topic + " 学习资源 教程");

        List<ChapterOutlineDTO> outline = state.getOutline();
        if (outline != null) {
            for (ChapterOutlineDTO chapter : outline) {
                if (chapter.getChapter() != null) {
                    queries.add(chapter.getChapter() + " 学习资料 知识点");
                }
                if (queries.size() >= 8) break;
            }
        }

        String level = state.getLevel();
        String difficulty = state.getDifficulty();
        if ("beginner".equals(level) || "easy".equals(difficulty)) {
            queries.add(topic + " 入门教程 基础概念");
        } else if ("advanced".equals(level) || "advanced".equals(difficulty)) {
            queries.add(topic + " 进阶 高级技巧 实战");
        }

        return queries;
    }

    private void pushSnapshot(String taskId) {
        CourseWorkflowSnapshotDTO s = traceService.getSnapshot(taskId);
        if (s != null) sseEmitterService.sendSnapshot(taskId, s);
    }
}
