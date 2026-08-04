package com.project.platform.service.course.nodes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.platform.agents.config.utils.Tools;
import com.project.platform.agents.verify.CourseContentVerificationAgent;
import com.project.platform.agents.verify.VerificationReport;
import com.project.platform.dto.courseworkflow.CourseWorkflowSnapshotDTO;
import com.project.platform.dto.courseworkflow.CourseWorkflowState;
import com.project.platform.entity.Chapter;
import com.project.platform.entity.Section;
import com.project.platform.mapper.CourseWorkflowResourceMapper;
import com.project.platform.mapper.CourseWorkflowTaskMapper;
import com.project.platform.service.ChapterService;
import com.project.platform.service.CourseGenerationService;
import com.project.platform.service.SectionService;
import com.project.platform.service.course.CourseWorkflowTraceService;
import com.project.platform.service.course.WorkflowSseEmitterService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DeliveryAgent {

    @Resource
    private CourseWorkflowTraceService traceService;

    @Resource
    private CourseWorkflowTaskMapper taskMapper;

    @Resource
    private CourseWorkflowResourceMapper resourceMapper;

    @Resource
    private WorkflowSseEmitterService sseEmitterService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private CourseGenerationService courseGenerationService;

    @Resource
    private CourseContentVerificationAgent verificationAgent;

    @Resource
    private ChapterService chapterService;

    @Resource
    private SectionService sectionService;

    @Resource
    private Tools tools;

    public CourseWorkflowState execute(CourseWorkflowState state) {
        String taskId = state.getTaskId();
        log.info("[资源交付入库] 开始: taskId={}, courseId={}", taskId, state.getCourseId());

        traceService.bind(taskId);
        traceService.startStep(taskId, "delivery", "正在生成课程内容并交付入库...");
        traceService.markDeliveryInProgress(taskId); // 标记交付开始
        pushSnapshotToFrontend(taskId);

        try {
            // 1. 汇总已验证资源
            traceService.logActivity(taskId, "delivery", "内容生成专家", "资源汇总",
                    "writing", "资源整理",
                    "正在整理 " + state.getResources().size() + " 条已验证学习资源。");
            pushSnapshotToFrontend(taskId);
            delay(400);

            List<CourseWorkflowSnapshotDTO.Evidence> verifiedResources = state.getResources().stream()
                    .filter(r -> !"low-confidence".equals(r.getCredibilityLabel()))
                    .collect(Collectors.toList());

            // 2. 生成第一节内容
            generateFirstSectionContent(taskId, state);

            // 3. 生成第一节习题
            generateFirstSectionExercises(taskId, state);

            // 4. 构建最终结果
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("courseId", state.getCourseId());
            result.put("courseName", state.getCourseName());
            result.put("description", state.getDescription());
            result.put("level", state.getLevel());
            result.put("duration", state.getDuration());
            result.put("style", state.getStyle());
            result.put("resources", verifiedResources);
            result.put("resourceCount", verifiedResources.size());

            String resultJson = objectMapper.writeValueAsString(result);

            // 4. 持久化
            persistCompletion(taskId, state.getCourseId(), resultJson, verifiedResources);

            state.getStageStatus().put("delivery", "completed");

            // 5. 设置 trace 状态（前端据此弹窗）
            String title = state.getCourseName() != null ? state.getCourseName() : "课程生成";
            traceService.setCourseInfo(taskId, state.getCourseId(), state.getCourseName());
            traceService.markDeliveryComplete(taskId);

            traceService.completeStep(taskId, "delivery",
                    "已生成首节内容并交付 " + verifiedResources.size() + " 条学习资源");
            traceService.complete(taskId, "课程生成完成: " + title);

            // 推送最终快照（含 phase=DELIVERY_COMPLETE + courseId，前端 watch 直接触发弹窗）
            pushSnapshotToFrontend(taskId);
            // 发送完成事件（双保险）
            sseEmitterService.sendWorkflowComplete(taskId,
                    "课程生成完成。共交付 " + verifiedResources.size() + " 条学习资源。",
                    state.getCourseId(), state.getCourseName());
            // 延长 SSE 存活，确保前端收到
            delay(600);
            sseEmitterService.close(taskId);

            log.info("[资源交付入库] 完成: taskId={}, courseId={}, resources={}",
                    taskId, state.getCourseId(), verifiedResources.size());
        } catch (Exception e) {
            log.error("[资源交付入库] 失败: taskId={}", taskId, e);
            state.getStageStatus().put("delivery", "failed");
            sseEmitterService.sendWorkflowFailed(taskId,
                    "交付失败: " + e.getMessage());
            throw new RuntimeException("资源交付入库失败", e);
        }

        return state;
    }

    /**
     * 生成课程第一节内容并执行真实性校验
     */
    private void generateFirstSectionContent(String taskId, CourseWorkflowState state) {
        Integer courseId = state.getCourseId();
        if (courseId == null) {
            log.warn("[交付] 无 courseId，跳过内容生成");
            return;
        }

        List<Chapter> chapters = chapterService.listByCourseId(courseId);
        if (chapters == null || chapters.isEmpty()) {
            log.warn("[交付] 课程 {} 无章节", courseId);
            return;
        }

        Chapter firstChapter = chapters.get(0);
        List<Section> sections = sectionService.listByChapterId(firstChapter.getId());
        if (sections == null || sections.isEmpty()) {
            log.warn("[交付] 章节 {} 无小节", firstChapter.getId());
            return;
        }

        Section firstSection = sections.get(0);
        String sectionName = firstSection.getSectionName();
        log.info("[交付] 开始生成第一节: chapterId={}, sectionId={}, name={}",
                firstChapter.getId(), firstSection.getId(), sectionName);

        traceService.logActivity(taskId, "delivery", "内容生成专家", "内容生成",
                "writing", "AI 写作",
                "正在为第一节「" + sectionName + "」生成详细讲义内容...");
        pushSnapshotToFrontend(taskId);
        delay(800);

        try {
            // 生成内容
            Section generated = courseGenerationService.generateSectionContent(
                    firstChapter.getId(), firstSection.getId());

            traceService.logActivity(taskId, "delivery", "内容生成专家", "生成完成",
                    "writing", "内容已生成",
                    "第一节「" + sectionName + "」讲义内容已生成完毕。");
            pushSnapshotToFrontend(taskId);
            delay(600);

            // 执行真实性校验
            String content = tools.readContentFromFile(generated.getContentUrl());
            if (content != null && !content.isBlank()) {
                traceService.logActivity(taskId, "delivery", "资料可信度验证专家",
                        "内容校验", "verification", "联网验证",
                        "正在对第一节内容进行联网真实性校验...");
                pushSnapshotToFrontend(taskId);
                delay(500);

                VerificationReport report = verificationAgent.verify(content);
                log.info("[交付] 第一节校验完成: truthScore={}, verified={}, suspicious={}",
                        report.getTruthScore(), report.getVerifiedCount(),
                        report.getSuspiciousCount());

                traceService.logActivity(taskId, "delivery", "资料可信度验证专家",
                        "校验完成", "verification", "校验报告",
                        String.format("第一节内容校验完成 — 真实度: %.0f%%, 确认 %d 条, 存疑 %d 条",
                                report.getTruthScore() * 100,
                                report.getVerifiedCount(),
                                report.getSuspiciousCount()));
                pushSnapshotToFrontend(taskId);
            } else {
                traceService.logActivity(taskId, "delivery", "内容生成专家",
                        "空内容", "warning", "内容缺失",
                        "第一节内容为空，跳过校验。");
            }

        } catch (Exception e) {
            log.error("[交付] 第一节内容生成失败(非致命)", e);
            traceService.logActivity(taskId, "delivery", "内容生成专家",
                    "生成异常", "warning", "内容异常",
                    "第一节内容生成失败: " + e.getMessage() + "，交付继续。");
        }
    }

    /**
     * 为第一节生成随堂练习题
     */
    private void generateFirstSectionExercises(String taskId, CourseWorkflowState state) {
        Integer courseId = state.getCourseId();
        if (courseId == null) return;

        try {
            List<Chapter> chapters = chapterService.listByCourseId(courseId);
            if (chapters == null || chapters.isEmpty()) return;
            Chapter firstChapter = chapters.get(0);
            List<Section> sections = sectionService.listByChapterId(firstChapter.getId());
            if (sections == null || sections.isEmpty()) return;
            Section firstSection = sections.get(0);

            traceService.logActivity(taskId, "delivery", "习题设计专家", "习题生成",
                    "analysis", "AI 出题",
                    "正在为第一节「" + firstSection.getSectionName() + "」生成随堂练习题...");
            pushSnapshotToFrontend(taskId);
            delay(500);

            List<com.project.platform.entity.Question> questions =
                    courseGenerationService.generateQuestions(firstSection.getId());

            traceService.logActivity(taskId, "delivery", "习题设计专家", "习题完成",
                    "result", "习题就绪",
                    "第一节随堂练习已生成，共 " + (questions != null ? questions.size() : 0) + " 道题。");
            pushSnapshotToFrontend(taskId);
        } catch (Exception e) {
            log.warn("[交付] 习题生成失败(非致命): {}", e.getMessage());
            traceService.logActivity(taskId, "delivery", "习题设计专家",
                    "生成异常", "warning", "习题异常",
                    "习题生成失败: " + e.getMessage());
        }
    }

    private void persistCompletion(String taskId, Integer courseId, String resultJson,
                                    List<CourseWorkflowSnapshotDTO.Evidence> resources) {
        try {
            com.project.platform.entity.CourseWorkflowTask task =
                    taskMapper.selectByTaskId(taskId);
            if (task != null) {
                task.setStatus("completed");
                task.setCurrentStage("delivery");
                task.setCourseId(courseId);
                task.setResultJson(resultJson);
                long now = System.currentTimeMillis();
                task.setUpdatedAt(now);
                task.setFinishedAt(now);
                task.setSummary("课程生成完成。共交付 " + resources.size()
                        + " 条学习资源，已生成首节内容。");
                taskMapper.updateById(task);
            }
        } catch (Exception e) {
            log.warn("持久化完成状态失败(非关键): {}", e.getMessage());
        }
    }

    /**
     * 推送快照到前端（SSE），不改变 phase，只推送当前状态
     */
    private void pushSnapshotToFrontend(String taskId) {
        CourseWorkflowSnapshotDTO snapshot = traceService.getSnapshot(taskId);
        if (snapshot != null) {
            sseEmitterService.sendSnapshot(taskId, snapshot);
        }
    }

    private void delay(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
