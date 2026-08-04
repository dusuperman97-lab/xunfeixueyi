package com.project.platform.service.course;

import com.project.platform.dto.courseworkflow.CourseMultiAgentReviewRequest;
import com.project.platform.dto.courseworkflow.CourseWorkflowSnapshotDTO;
import com.project.platform.dto.courseworkflow.CourseWorkflowState;
import com.project.platform.entity.CourseWorkflowTask;
import com.project.platform.mapper.CourseWorkflowTaskMapper;
import com.project.platform.service.course.nodes.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CourseMultiAgentWorkflowEngine {

    @Resource
    private ProfileAnalysisAgent profileAnalysisAgent;

    @Resource
    private DemandExtractionAgent demandExtractionAgent;

    @Resource
    private OrchestrationAgent orchestrationAgent;

    @Resource
    private ResourceCollectionAgent resourceCollectionAgent;

    @Resource
    private CrossReviewAgent crossReviewAgent;

    @Resource
    private DeliveryAgent deliveryAgent;

    @Resource
    private CourseWorkflowTraceService traceService;

    @Resource
    private CourseWorkflowTaskMapper taskMapper;

    @Resource
    private WorkflowSseEmitterService sseEmitterService;

    private final ExecutorService workflowExecutor = Executors.newFixedThreadPool(4);
    private final Map<String, CompletableFuture<CourseWorkflowState>> reviewFutures = new ConcurrentHashMap<>();
    private final Map<String, CourseWorkflowState> waitingStates = new ConcurrentHashMap<>();

    /**
     * 启动多智能体工作流（异步）
     */
    public String startWorkflow(CourseWorkflowState state) {
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        state.setTaskId(taskId);

        // 初始化阶段状态
        Map<String, String> stages = new LinkedHashMap<>();
        stages.put("profile_analysis", "pending");
        stages.put("demand_extraction", "pending");
        stages.put("orchestration", "pending");
        stages.put("resource_collection", "pending");
        stages.put("cross_review", "pending");
        stages.put("delivery", "pending");
        state.setStageStatus(stages);

        // 初始化trace
        traceService.beginCourseTask(taskId, state.getMessage());

        // 初始快照推送到SSE
        CourseWorkflowSnapshotDTO initialSnapshot = traceService.getSnapshot(taskId);
        if (initialSnapshot != null) {
            sseEmitterService.sendSnapshot(taskId, initialSnapshot);
        }

        // 持久化任务
        persistTask(state);

        // 异步执行工作流
        CompletableFuture.runAsync(() -> executeWorkflow(state), workflowExecutor)
                .exceptionally(ex -> {
                    log.error("工作流执行异常: taskId={}", taskId, ex);
                    traceService.fail(taskId, "工作流异常: " + ex.getMessage());
                    sseEmitterService.sendWorkflowFailed(taskId,
                            "工作流异常: " + ex.getMessage());
                    return null;
                });

        return taskId;
    }

    private void executeWorkflow(CourseWorkflowState state) {
        String taskId = state.getTaskId();

        try {
            // === 阶段1: 学生画像分析 ===
            notifyStageStart(taskId, "profile_analysis");
            progressiveDelay();
            profileAnalysisAgent.execute(state);
            notifyStageComplete(taskId, "profile_analysis");
            pushSnapshot(taskId);
            progressiveDelay();

            // === 阶段2: 需求提取 ===
            notifyStageStart(taskId, "demand_extraction");
            progressiveDelay();
            demandExtractionAgent.execute(state);
            notifyStageComplete(taskId, "demand_extraction");
            pushSnapshot(taskId);
            progressiveDelay();

            // === 阶段2: 编排派遣 ===
            notifyStageStart(taskId, "orchestration");
            progressiveDelay();
            orchestrationAgent.execute(state);
            notifyStageComplete(taskId, "orchestration");
            pushSnapshot(taskId);
            progressiveDelay();

            // === 阶段3: 资源搜集 ===
            notifyStageStart(taskId, "resource_collection");
            progressiveDelay();
            resourceCollectionAgent.execute(state);
            notifyStageComplete(taskId, "resource_collection");
            pushSnapshot(taskId);
            progressiveDelay();

            // === 阶段4: 交叉审核 ===
            notifyStageStart(taskId, "cross_review");
            progressiveDelay();
            crossReviewAgent.execute(state);
            pushSnapshot(taskId);

            // AI自动审查完成，直接进入下一阶段

            // === 阶段5: 资源交付入库 ===
            notifyStageStart(taskId, "delivery");
            deliveryAgent.execute(state);
            notifyStageComplete(taskId, "delivery");
            pushSnapshot(taskId);

            // 持久化完成状态
            persistCompletion(state);

            log.info("[工作流引擎] 全部完成: taskId={}, courseId={}", taskId,
                    state.getCourseId());

        } catch (Exception e) {
            log.error("[工作流引擎] 执行失败: taskId={}", taskId, e);
            traceService.fail(taskId, "工作流执行失败: " + e.getMessage());
            sseEmitterService.sendWorkflowFailed(taskId,
                    "工作流执行失败: " + e.getMessage());
            reviewFutures.remove(taskId);
            waitingStates.remove(taskId);
        }
    }

    /**
     * 提交审查决策，恢复工作流
     */
    public boolean submitReview(String taskId, String decision,
                                 String feedback,
                                 List<CourseMultiAgentReviewRequest.ResourceAction> resourceActions) {
        CompletableFuture<CourseWorkflowState> future = reviewFutures.get(taskId);
        CourseWorkflowState state = waitingStates.get(taskId);

        if (future == null || state == null) {
            log.warn("未找到等待审查的工作流: taskId={}", taskId);
            return false;
        }

        state.setReviewDecision(decision);
        state.setReviewFeedback(feedback);
        state.setResourceActions(resourceActions);

        future.complete(state);
        log.info("审查决策已提交: taskId={}, decision={}", taskId, decision);
        return true;
    }

    /**
     * 等待人工审查决策，120秒超时自动批准
     */
    private CourseWorkflowState waitForReview(String taskId, CourseWorkflowState state) {
        CompletableFuture<CourseWorkflowState> reviewFuture = new CompletableFuture<>();
        reviewFutures.put(taskId, reviewFuture);
        waitingStates.put(taskId, state);

        try {
            state = reviewFuture.get(120, TimeUnit.SECONDS);
            log.info("[审查] 收到人工决策: taskId={}, decision={}", taskId,
                    state.getReviewDecision());
        } catch (Exception e) {
            log.info("[审查] 超时未收到人工决策, 自动批准: taskId={}", taskId);
            state.setReviewDecision("APPROVE");
            state.setReviewApproved(true);
        } finally {
            reviewFutures.remove(taskId);
            waitingStates.remove(taskId);
        }

        crossReviewAgent.processReviewDecision(state);
        notifyStageComplete(taskId, "cross_review");
        pushSnapshot(taskId);
        return state;
    }

    public CourseWorkflowSnapshotDTO getSnapshot(String taskId) {
        return traceService.getSnapshot(taskId);
    }

    private void notifyStageStart(String taskId, String stageId) {
        sseEmitterService.sendStageStart(taskId, stageId, "");
        persistStage(taskId, stageId, "running");
    }

    private void notifyStageComplete(String taskId, String stageId) {
        sseEmitterService.sendStageComplete(taskId, stageId, "");
        persistStage(taskId, stageId, "completed");
    }

    private void pushSnapshot(String taskId) {
        CourseWorkflowSnapshotDTO snapshot = traceService.getSnapshot(taskId);
        if (snapshot != null) {
            sseEmitterService.sendSnapshot(taskId, snapshot);
        }
    }

    private void persistTask(CourseWorkflowState state) {
        try {
            CourseWorkflowTask task = new CourseWorkflowTask();
            task.setTaskId(state.getTaskId());
            task.setUserId(state.getUserId());
            task.setStatus("running");
            task.setCurrentStage("profile_analysis");
            task.setRequestMessage(state.getMessage());
            task.setCourseMode(state.getCourseMode());
            task.setDifficulty(state.getDifficulty());
            long now = System.currentTimeMillis();
            task.setCreatedAt(now);
            task.setUpdatedAt(now);
            taskMapper.insert(task);
        } catch (Exception e) {
            log.warn("持久化任务失败(非关键): {}", e.getMessage());
        }
    }

    private void persistStage(String taskId, String stageId, String status) {
        try {
            CourseWorkflowTask task = taskMapper.selectByTaskId(taskId);
            if (task != null) {
                task.setCurrentStage(stageId);
                task.setStatus(status.equals("failed") ? "failed" : "running");
                task.setUpdatedAt(System.currentTimeMillis());
                taskMapper.updateById(task);
            }
        } catch (Exception e) {
            log.warn("持久化阶段失败(非关键): {}", e.getMessage());
        }
    }

    /**
     * 渐进式延迟 — 营造流式逐个出现的体验
     */
    private void progressiveDelay() {
        try {
            Thread.sleep(600);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void persistCompletion(CourseWorkflowState state) {
        try {
            CourseWorkflowTask task = taskMapper.selectByTaskId(state.getTaskId());
            if (task != null) {
                task.setStatus("completed");
                task.setCurrentStage("delivery");
                task.setCourseId(state.getCourseId());
                task.setSummary("课程生成完成: " + state.getCourseName());
                long now = System.currentTimeMillis();
                task.setUpdatedAt(now);
                task.setFinishedAt(now);
                taskMapper.updateById(task);
            }
        } catch (Exception e) {
            log.warn("持久化完成状态失败(非关键): {}", e.getMessage());
        }
    }
}
