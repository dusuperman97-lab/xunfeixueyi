package com.project.platform.service.course;

import com.project.platform.dto.courseworkflow.CourseWorkflowSnapshotDTO;
import com.project.platform.entity.AgentRole;
import com.project.platform.entity.CourseWorkflowActivity;
import com.project.platform.mapper.AgentRoleMapper;
import com.project.platform.mapper.CourseWorkflowActivityMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Service
public class CourseWorkflowTraceService {

    private static final long RETENTION_MILLIS = 30 * 60 * 1000L;

    @Resource
    private CourseWorkflowActivityMapper activityMapper;

    @Resource
    private AgentRoleMapper agentRoleMapper;

    private final Map<String, TaskState> tasks = new ConcurrentHashMap<>();
    private final ThreadLocal<String> currentTaskId = new ThreadLocal<>();

    public void beginCourseTask(String taskId, String requestText) {
        cleanupExpiredTasks();
        TaskState state = new TaskState();
        state.taskId = taskId;
        state.taskType = "course-multi-agent";
        state.title = "多智能体协作构建课程";
        state.subtitle = trimText(requestText, 120);
        state.status = "running";
        state.createdAt = System.currentTimeMillis();
        state.updatedAt = state.createdAt;
        state.experts = buildExperts();

        addStep(state, "profile_analysis", "学生画像分析", "画像分析师");
        addStep(state, "demand_extraction", "需求提取", "需求解析师");
        addStep(state, "orchestration", "编排派遣", "任务调度师");
        addStep(state, "resource_collection", "资源搜集", "图片猎手/视频猎手/网页检索师/社区探勘师");
        addStep(state, "cross_review", "交叉审核", "质量审核师/资料可信度验证专家");
        addStep(state, "delivery", "资源交付入库", "内容生成专家/习题设计专家");

        state.phase = "RUNNING"; // 初始化阶段状态
        tasks.put(taskId, state);
        logActivity(taskId, "profile_analysis", "任务调度师", "流程启动", "dispatch", "任务创建", "10位专家已就位。6阶段流水线: 学生画像分析(画像分析师)→需求提取(需求解析师)→编排派遣(任务调度师)→资源搜集(4位搜索专家)→交叉审核(质量审核师)→资源生成(内容生成专家/习题设计专家)。");
    }

    public void beginSectionTask(String taskId, String sectionTitle) {
        cleanupExpiredTasks();
        TaskState state = new TaskState();
        state.taskId = taskId;
        state.taskType = "section-content";
        state.title = "专家团队协作生成讲义";
        state.subtitle = trimText(sectionTitle, 120);
        state.status = "running";
        state.createdAt = System.currentTimeMillis();
        state.updatedAt = state.createdAt;
        state.experts = buildExperts();

        addStep(state, "demand_analysis", "需求理解", "任务规划专家");
        addStep(state, "orchestration", "编排派遣", "协调层");
        addStep(state, "resource_collection", "证据采集", "学习资源获取专家");
        addStep(state, "credibility_verification", "交叉分析", "质量审核师");
        addStep(state, "content_assembly", "报告撰写", "内容整理专家");
        addStep(state, "persistence", "签发交付", "协调层");

        tasks.put(taskId, state);
        logActivity(taskId, "orchestration", "协调层", "流程协调", "dispatch", "任务创建", "已创建小节讲义生成任务。");
    }

    public void bind(String taskId) {
        if (taskId != null && !taskId.isBlank()) {
            currentTaskId.set(taskId);
        }
    }

    public void clear() {
        currentTaskId.remove();
    }

    public String currentTaskId() {
        return currentTaskId.get();
    }

    public CourseWorkflowSnapshotDTO getSnapshot(String taskId) {
        TaskState state = tasks.get(taskId);
        if (state == null) {
            return null;
        }
        synchronized (state) {
            return toSnapshot(state);
        }
    }

    public void updateTitle(String title, String subtitle) {
        updateTitle(currentTaskId(), title, subtitle);
    }

    public void updateTitle(String taskId, String title, String subtitle) {
        mutate(taskId, state -> {
            if (title != null && !title.isBlank()) {
                state.title = title;
            }
            if (subtitle != null && !subtitle.isBlank()) {
                state.subtitle = trimText(subtitle, 120);
            }
        });
    }

    public void startStep(String stageId, String detail) {
        startStep(currentTaskId(), stageId, detail);
    }

    public void startStep(String taskId, String stageId, String detail) {
        mutate(taskId, state -> {
            StepState step = state.steps.get(stageId);
            if (step == null) {
                return;
            }
            step.status = "running";
            if (detail != null && !detail.isBlank()) {
                step.detail = detail;
            }
            state.status = "running";
        });
    }

    public void completeStep(String stageId, String detail) {
        completeStep(currentTaskId(), stageId, detail);
    }

    public void completeStep(String taskId, String stageId, String detail) {
        mutate(taskId, state -> {
            StepState step = state.steps.get(stageId);
            if (step == null) {
                return;
            }
            step.status = "completed";
            if (detail != null && !detail.isBlank()) {
                step.detail = detail;
            }
        });
    }

    public void logActivity(String stageId,
                            String agentName,
                            String agentRole,
                            String type,
                            String label,
                            String message) {
        logActivity(currentTaskId(), stageId, agentName, agentRole, type, label, message);
    }

    public void logActivity(String taskId,
                            String stageId,
                            String agentName,
                            String agentRole,
                            String type,
                            String label,
                            String message) {
        String activityId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();

        // 内存存储
        mutate(taskId, state -> {
            CourseWorkflowSnapshotDTO.Activity activity = new CourseWorkflowSnapshotDTO.Activity();
            activity.setId(activityId);
            activity.setStageId(stageId);
            activity.setAgentName(agentName);
            activity.setAgentRole(agentRole);
            activity.setType(type);
            activity.setLabel(label);
            activity.setMessage(message);
            activity.setTimestamp(timestamp);
            state.activities.add(activity);
            if (state.activities.size() > 60) {
                state.activities = new ArrayList<>(
                        state.activities.subList(state.activities.size() - 60, state.activities.size())
                );
            }
        });

        // DB 持久化（供历史查询）
        try {
            CourseWorkflowActivity dbAct = new CourseWorkflowActivity();
            dbAct.setTaskId(taskId);
            dbAct.setActivityId(activityId);
            dbAct.setStageId(stageId);
            dbAct.setAgentName(agentName);
            dbAct.setAgentRole(agentRole);
            dbAct.setType(type);
            dbAct.setLabel(label);
            dbAct.setMessage(message);
            dbAct.setTimestamp(timestamp);
            activityMapper.insert(dbAct);
        } catch (Exception e) {
            log.debug("持久化活动日志失败(非关键): {}", e.getMessage());
        }
    }

    public void addEvidence(String kind,
                            String title,
                            String url,
                            String source,
                            String snippet,
                            Double score,
                            String credibilityLabel) {
        addEvidence(currentTaskId(), kind, title, url, source, snippet, score, credibilityLabel);
    }

    public void addEvidence(String taskId,
                            String kind,
                            String title,
                            String url,
                            String source,
                            String snippet,
                            Double score,
                            String credibilityLabel) {
        mutate(taskId, state -> {
            CourseWorkflowSnapshotDTO.Evidence target = findEvidence(state, title, url);
            if (target == null) {
                target = new CourseWorkflowSnapshotDTO.Evidence();
                target.setId(UUID.randomUUID().toString());
                state.evidences.add(target);
            }
            target.setKind(kind);
            target.setTitle(title);
            target.setUrl(url);
            target.setSource(source);
            target.setSnippet(trimText(snippet, 180));
            target.setScore(score);
            target.setCredibilityLabel(credibilityLabel);
        });
    }

    public void markEvidence(String url, String credibilityLabel, String note, Double score) {
        markEvidence(currentTaskId(), url, credibilityLabel, note, score);
    }

    public void markEvidence(String taskId, String url, String credibilityLabel, String note, Double score) {
        mutate(taskId, state -> {
            for (CourseWorkflowSnapshotDTO.Evidence evidence : state.evidences) {
                if (url != null && !url.isBlank() && url.equals(evidence.getUrl())) {
                    evidence.setCredibilityLabel(credibilityLabel);
                    evidence.setNote(trimText(note, 180));
                    evidence.setScore(score);
                    return;
                }
            }
        });
    }

    public void complete(String summary) {
        complete(currentTaskId(), summary);
    }

    public void complete(String taskId, String summary) {
        mutate(taskId, state -> {
            state.status = "completed";
            state.summary = summary;
            state.finishedAt = System.currentTimeMillis();
        });
    }

    public void fail(String summary) {
        fail(currentTaskId(), summary);
    }

    public void setPhase(String taskId, String phase) {
        mutate(taskId, state -> { state.phase = phase; });
    }

    public void setCourseInfo(String taskId, Integer courseId, String courseName) {
        mutate(taskId, state -> {
            state.courseId = courseId;
            state.courseName = courseName;
        });
    }

    public void setReviewPending(String taskId,
                                  List<CourseWorkflowSnapshotDTO.Evidence> reviewEvidences) {
        mutate(taskId, state -> {
            state.phase = "REVIEW_PENDING";
            state.reviewRequired = true;
            state.reviewEvidences = reviewEvidences != null
                    ? new ArrayList<>(reviewEvidences) : new ArrayList<>();
        });
    }

    public void clearReview(String taskId) {
        mutate(taskId, state -> {
            state.phase = "RUNNING";
            state.reviewRequired = false;
            state.reviewEvidences.clear();
        });
    }

    public void markDeliveryInProgress(String taskId) {
        mutate(taskId, state -> { state.phase = "DELIVERY_IN_PROGRESS"; });
    }

    public void markDeliveryComplete(String taskId) {
        mutate(taskId, state -> { state.phase = "DELIVERY_COMPLETE"; });
    }

    public void fail(String taskId, String summary) {
        mutate(taskId, state -> {
            state.status = "failed";
            state.summary = summary;
            state.finishedAt = System.currentTimeMillis();
            for (StepState step : state.steps.values()) {
                if ("running".equals(step.status)) {
                    step.status = "failed";
                }
            }
        });
    }

    private void mutate(String taskId, Consumer<TaskState> consumer) {
        if (taskId == null || taskId.isBlank()) {
            return;
        }
        TaskState state = tasks.get(taskId);
        if (state == null) {
            return;
        }
        synchronized (state) {
            consumer.accept(state);
            state.updatedAt = System.currentTimeMillis();
        }
    }

    private CourseWorkflowSnapshotDTO toSnapshot(TaskState state) {
        CourseWorkflowSnapshotDTO dto = new CourseWorkflowSnapshotDTO();
        dto.setTaskId(state.taskId);
        dto.setTaskType(state.taskType);
        dto.setTitle(state.title);
        dto.setSubtitle(state.subtitle);
        dto.setStatus(state.status);
        dto.setSummary(state.summary);
        dto.setCreatedAt(state.createdAt);
        dto.setUpdatedAt(state.updatedAt);
        dto.setFinishedAt(state.finishedAt);

        for (StepState step : state.steps.values()) {
            CourseWorkflowSnapshotDTO.Step item = new CourseWorkflowSnapshotDTO.Step();
            item.setId(step.id);
            item.setTitle(step.title);
            item.setAgent(step.agent);
            item.setStatus(step.status);
            item.setDetail(step.detail);
            dto.getSteps().add(item);
        }

        dto.getActivities().addAll(state.activities);
        dto.getEvidences().addAll(state.evidences);
        dto.getExperts().addAll(state.experts);
        dto.setPhase(state.phase);
        dto.setReviewRequired(state.reviewRequired);
        dto.setReviewEvidences(new ArrayList<>(state.reviewEvidences));
        dto.setCourseId(state.courseId);
        dto.setCourseName(state.courseName);
        return dto;
    }

    private void addStep(TaskState state, String id, String title, String agent) {
        StepState step = new StepState();
        step.id = id;
        step.title = title;
        step.agent = agent;
        step.status = "pending";
        state.steps.put(id, step);
    }

    private List<CourseWorkflowSnapshotDTO.Expert> buildExperts() {
        List<CourseWorkflowSnapshotDTO.Expert> list = new ArrayList<>();
        try {
            List<AgentRole> roles = agentRoleMapper.list();
            for (AgentRole role : roles) {
                list.add(expert(
                        role.getRoleCode(),
                        role.getRoleName(),
                        role.getDescription() != null ? role.getDescription() : "",
                        role.getAccentColor() != null ? role.getAccentColor() : "#52525b"
                ));
            }
        } catch (Exception e) {
            log.warn("从DB加载专家角色失败，使用默认列表: {}", e.getMessage());
            return buildFallbackExperts();
        }
        if (list.isEmpty()) {
            return buildFallbackExperts();
        }
        return list;
    }

    private List<CourseWorkflowSnapshotDTO.Expert> buildFallbackExperts() {
        return List.of(
                expert("profile_analyst", "画像分析师", "分析学生画像数据，定制个性化学习策略", "#8B5CF6"),
                expert("demand_analyst", "需求解析师", "从用户输入中提取学习需求与参数", "#6366F1"),
                expert("task_scheduler", "任务调度师", "课程大纲规划与搜索策略制定", "#0F766E"),
                expert("image_hunter", "图片猎手", "百度等图库教学配图资源检索", "#F59E0B"),
                expert("video_hunter", "视频猎手", "B站等平台视频教学资源搜索", "#DC2626"),
                expert("web_searcher", "网页检索师", "Tavily/Google网页与文章检索", "#7C3AED"),
                expert("community_scout", "社区探勘师", "知乎/CSDN/GitHub经验帖挖掘", "#0891B2"),
                expert("quality_reviewer", "质量审核师", "资源可信度交叉验证与权威度评估", "#BE123C"),
                expert("content_verifier", "资料可信度验证专家", "生成内容的联网事实核查与AI幻觉消除", "#B91C1C"),
                expert("content_writer", "内容生成专家", "课程章节讲义内容AI生成", "#1D4ED8"),
                expert("exercise_designer", "习题设计专家", "随堂练习题与考试题设计", "#7E22CE")
        );
    }

    private CourseWorkflowSnapshotDTO.Expert expert(String id, String name, String role, String accent) {
        CourseWorkflowSnapshotDTO.Expert expert = new CourseWorkflowSnapshotDTO.Expert();
        expert.setId(id);
        expert.setName(name);
        expert.setRole(role);
        expert.setAccent(accent);
        return expert;
    }

    private CourseWorkflowSnapshotDTO.Evidence findEvidence(TaskState state, String title, String url) {
        for (CourseWorkflowSnapshotDTO.Evidence evidence : state.evidences) {
            if (url != null && !url.isBlank() && url.equals(evidence.getUrl())) {
                return evidence;
            }
            if ((url == null || url.isBlank()) && title != null && title.equals(evidence.getTitle())) {
                return evidence;
            }
        }
        return null;
    }

    private String trimText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String clean = text.trim();
        if (clean.length() <= maxLength) {
            return clean;
        }
        return clean.substring(0, maxLength) + "...";
    }

    private void cleanupExpiredTasks() {
        long now = System.currentTimeMillis();
        tasks.entrySet().removeIf(entry -> now - entry.getValue().updatedAt > RETENTION_MILLIS);
    }

    private static class TaskState {
        private String taskId;
        private String taskType;
        private String title;
        private String subtitle;
        private String status;
        private String summary;
        private String phase = "RUNNING";
        private long createdAt;
        private long updatedAt;
        private Long finishedAt;
        private LinkedHashMap<String, StepState> steps = new LinkedHashMap<>();
        private List<CourseWorkflowSnapshotDTO.Activity> activities = new ArrayList<>();
        private List<CourseWorkflowSnapshotDTO.Evidence> evidences = new ArrayList<>();
        private List<CourseWorkflowSnapshotDTO.Expert> experts = new ArrayList<>();
        private boolean reviewRequired;
        private List<CourseWorkflowSnapshotDTO.Evidence> reviewEvidences = new ArrayList<>();
        private Integer courseId;
        private String courseName;
    }

    private static class StepState {
        private String id;
        private String title;
        private String agent;
        private String status;
        private String detail;
    }
}
