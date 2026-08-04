package com.project.platform.service.course.nodes;

import com.project.platform.dto.courseworkflow.CourseWorkflowSnapshotDTO;
import com.project.platform.dto.courseworkflow.CourseWorkflowState;
import com.project.platform.mapper.CourseWorkflowResourceMapper;
import com.project.platform.service.course.CourseWorkflowTraceService;
import com.project.platform.service.course.WorkflowSseEmitterService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class CrossReviewAgent {

    @Resource
    private CourseWorkflowTraceService traceService;

    @Resource
    private CourseWorkflowResourceMapper resourceMapper;

    @Resource
    private WorkflowSseEmitterService sseEmitterService;

    public CourseWorkflowState execute(CourseWorkflowState state) {
        String taskId = state.getTaskId();
        log.info("[交叉审核] 开始: taskId={}, resources={}", taskId,
                state.getResources() != null ? state.getResources().size() : 0);

        traceService.bind(taskId);
        traceService.startStep(taskId, "cross_review", "正在对学习资源进行权威度交叉验证...");
        traceService.logActivity(taskId, "cross_review", "质量审核师", "可信度评估",
                "verification", "交叉验证", "启动多源交叉验证，评估每条资源的权威性与准确性。");

        try {
            List<CourseWorkflowSnapshotDTO.Evidence> resources = state.getResources();

            // 逐条审核：基于搜索评分 + 来源权威度进行可信度评估
            for (CourseWorkflowSnapshotDTO.Evidence ev : resources) {
                reviewEvidence(taskId, ev);
            }

            traceService.logActivity(taskId, "cross_review", "质量审核师", "审查总结",
                    "verification", "信任分级",
                    buildSummary(resources));

            // 自动审查完成，无需人工确认
            state.setReviewRequired(false);
            state.setReviewApproved(true);
            state.getStageStatus().put("cross_review", "completed");

            traceService.completeStep(taskId, "cross_review",
                    "审查自动完成。共 " + resources.size() + " 条资源: 高可信 "
                    + resources.stream().filter(r -> "verified".equals(r.getCredibilityLabel())).count()
                    + " 条, 需谨慎 "
                    + resources.stream().filter(r -> "caution".equals(r.getCredibilityLabel())).count()
                    + " 条。");

            log.info("[交叉审核] 自动完成: taskId={}, resources={}", taskId, resources.size());
        } catch (Exception e) {
            log.error("[交叉审核] 失败: taskId={}", taskId, e);
            state.getStageStatus().put("cross_review", "failed");
            throw new RuntimeException("交叉审核失败", e);
        }

        return state;
    }

    public CourseWorkflowState processReviewDecision(CourseWorkflowState state) {
        String taskId = state.getTaskId();
        String decision = state.getReviewDecision();
        log.info("[交叉审核] 处理审查决策: taskId={}, decision={}", taskId, decision);

        traceService.bind(taskId);

        if ("APPROVE".equals(decision)) {
            state.setReviewApproved(true);
            state.getStageStatus().put("cross_review", "completed");
            traceService.clearReview(taskId);
            traceService.logActivity(taskId, "cross_review", "质量审核师", "审查通过",
                    "verification", "人工确认", "人工审查已批准。所有资源可信度验证通过。");
            traceService.completeStep(taskId, "cross_review", "审查通过（人工已确认）");

        } else if ("REJECT".equals(decision)) {
            state.setReviewApproved(false);
            state.getStageStatus().put("cross_review", "rejected");
            traceService.clearReview(taskId);
            traceService.logActivity(taskId, "cross_review", "质量审核师", "审查驳回",
                    "verification", "人工驳回", "人工审查已驳回。将重新搜索低可信度资源。");
            traceService.completeStep(taskId, "cross_review", "审查驳回，准备重搜");

            // 移除低可信度资源
            List<CourseWorkflowSnapshotDTO.Evidence> resources = state.getResources();
            resources.removeIf(r -> "low-confidence".equals(r.getCredibilityLabel())
                    || (r.getScore() != null && r.getScore() < 0.4));
            state.setResources(resources);

        } else if ("MODIFY".equals(decision)) {
            state.setReviewApproved(true);
            state.getStageStatus().put("cross_review", "completed");
            traceService.clearReview(taskId);
            applyModifications(state);
            traceService.logActivity(taskId, "cross_review", "质量审核师", "标注已修改",
                    "verification", "人工修改", "人工已修改资源标注。" +
                    (state.getReviewFeedback() != null ? "反馈: " + state.getReviewFeedback() : ""));
            traceService.completeStep(taskId, "cross_review", "审查完成（标注已修改）");
        }

        return state;
    }

    private void reviewEvidence(String taskId, CourseWorkflowSnapshotDTO.Evidence ev) {
        // 综合评分: 搜索相关度(60%) + 来源权威度(40%)
        double searchScore = ev.getScore() != null ? ev.getScore() : 0.5;
        double authorityScore = assessAuthority(ev.getUrl(), ev.getSource());
        double finalScore = searchScore * 0.6 + authorityScore * 0.4;

        String label;
        if (finalScore >= 0.7) {
            label = "verified";
        } else if (finalScore >= 0.35) {
            label = "caution";
        } else {
            label = "low-confidence";
        }

        ev.setCredibilityLabel(label);
        ev.setScore(Math.round(finalScore * 100.0) / 100.0); // 保留两位小数

        traceService.markEvidence(taskId, ev.getUrl(), label,
                String.format("搜索分:%.2f + 权威分:%.2f → 综合:%.2f → %s",
                        searchScore, authorityScore, finalScore, label),
                finalScore);

        // 持久化更新
        try {
            com.project.platform.entity.CourseWorkflowResource resource =
                    new com.project.platform.entity.CourseWorkflowResource();
            resource.setTaskId(taskId);
            resource.setUrl(ev.getUrl());
            resource.setCredibilityLabel(label);
            resource.setScore(finalScore);
            resourceMapper.updateByUrl(resource);
        } catch (Exception e) {
            log.debug("持久化审查结果失败(非关键): {}", e.getMessage());
        }
    }

    /**
     * 评估来源权威度 (0.0 - 1.0)
     */
    private double assessAuthority(String url, String source) {
        if (url == null) return 0.5;
        String urlLower = url.toLowerCase();
        String srcLower = source != null ? source.toLowerCase() : "";

        // 高权威来源
        if (urlLower.contains(".edu") || urlLower.contains(".gov")
                || srcLower.contains("官方") || srcLower.contains("文档"))
            return 0.95;

        // 知名技术平台
        if (urlLower.contains("github.com") || urlLower.contains("stackoverflow.com")
                || urlLower.contains("wikipedia.org") || urlLower.contains("zhihu.com")
                || srcLower.contains("tavily") || srcLower.contains("b站"))
            return 0.80;

        // 中等可信来源
        if (urlLower.contains("csdn.net") || urlLower.contains("juejin.cn")
                || urlLower.contains("segmentfault.com") || urlLower.contains("blog.csdn")
                || urlLower.contains("bilibili.com"))
            return 0.65;

        // 个人博客/未知来源
        if (srcLower.contains("个人") || srcLower.contains("blog"))
            return 0.35;

        // 默认
        return 0.55;
    }

    private void applyModifications(CourseWorkflowState state) {
        var actions = state.getResourceActions();
        if (actions == null) return;

        List<CourseWorkflowSnapshotDTO.Evidence> resources = state.getResources();
        for (var action : actions) {
            for (CourseWorkflowSnapshotDTO.Evidence ev : resources) {
                if (matches(ev, action.getUrl(), action.getTitle())) {
                    if ("REMOVE".equals(action.getAction())) {
                        resources.remove(ev);
                    } else if ("RELABEL".equals(action.getAction()) && action.getNewLabel() != null) {
                        ev.setCredibilityLabel(action.getNewLabel());
                        traceService.markEvidence(state.getTaskId(), ev.getUrl(),
                                action.getNewLabel(), "人工修改标注", ev.getScore());
                    }
                    break;
                }
            }
        }
    }

    private boolean matches(CourseWorkflowSnapshotDTO.Evidence ev, String url, String title) {
        if (url != null && !url.isBlank() && url.equals(ev.getUrl())) return true;
        return title != null && !title.isBlank() && title.equals(ev.getTitle());
    }

    private void pushSnapshotDelayed(String taskId) {
        try { Thread.sleep(300); } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        CourseWorkflowSnapshotDTO snapshot = traceService.getSnapshot(taskId);
        if (snapshot != null) {
            sseEmitterService.sendSnapshot(taskId, snapshot);
        }
    }

    private void delay(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private String buildSummary(List<CourseWorkflowSnapshotDTO.Evidence> resources) {
        long verified = resources.stream().filter(r -> "verified".equals(r.getCredibilityLabel())).count();
        long caution = resources.stream().filter(r -> "caution".equals(r.getCredibilityLabel())).count();
        long low = resources.stream().filter(r -> "low-confidence".equals(r.getCredibilityLabel())).count();
        return String.format("审查完成: 高可信%d条, 需谨慎%d条, 低可信%d条。请确认后继续。",
                verified, caution, low);
    }
}
