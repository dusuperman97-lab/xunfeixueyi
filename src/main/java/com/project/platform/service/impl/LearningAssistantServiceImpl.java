package com.project.platform.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.platform.dto.learning.LearningAssistantContextDTO;
import com.project.platform.dto.learning.LearningAssistantContextRequest;
import com.project.platform.dto.learning.LearningContextDTO;
import com.project.platform.dto.learning.RecommendedActionDTO;
import com.project.platform.entity.ResourcePush;
import com.project.platform.entity.StudentProfile;
import com.project.platform.service.LearningAssistantService;
import com.project.platform.service.LearningWorkspaceService;
import com.project.platform.service.ResourcePushService;
import com.project.platform.service.StudentProfileService;
import com.project.platform.vo.StudentProfileDashboardVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Slf4j
@Service
public class LearningAssistantServiceImpl implements LearningAssistantService {

    private static final String COURSE_AGENT_ENDPOINT = "/course/digital-human/chat";
    private static final String EVENT_ENDPOINT = "/learning-assistant/events";
    private final LearningWorkspaceService learningWorkspaceService;
    private final StudentProfileService studentProfileService;
    private final ResourcePushService resourcePushService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LearningAssistantServiceImpl(LearningWorkspaceService learningWorkspaceService,
                                        StudentProfileService studentProfileService,
                                        ResourcePushService resourcePushService) {
        this.learningWorkspaceService = learningWorkspaceService;
        this.studentProfileService = studentProfileService;
        this.resourcePushService = resourcePushService;
    }

    @Override
    public LearningAssistantContextDTO getContext(Integer studentId, LearningAssistantContextRequest request) {
        LearningAssistantContextRequest safeRequest = request == null
                ? new LearningAssistantContextRequest()
                : request;
        LearningAssistantContextDTO result = new LearningAssistantContextDTO();
        result.setStudentId(studentId);
        result.setAgentEndpoint(COURSE_AGENT_ENDPOINT);
        result.setEventEndpoint(EVENT_ENDPOINT);

        LearningContextDTO learningContext = loadLearningContext(studentId, safeRequest, result);
        if (learningContext != null) {
            applyLearningContext(result, learningContext);
            result.setContextualResources(loadContextualResources(studentId, learningContext));
        } else {
            result.setActiveModule(defaultText(safeRequest.getActiveModule(), "learning"));
            result.setActiveKnowledgePoint(safeRequest.getActiveKnowledgePoint());
        }

        StudentProfile profile = loadProfile(studentId);
        StudentProfileDashboardVO dashboard = loadDashboard(studentId);
        result.setProfileSummary(buildProfileSummary(profile, dashboard));
        result.setWeakKnowledgePoints(mergeWeakPoints(result.getWeakKnowledgePoints(),
                result.getProfileSummary().getWeaknesses()));
        if (result.getNextRecommendedAction() == null) {
            result.setNextRecommendedAction(inferRecommendedAction(result));
        }
        result.setGreeting(buildGreeting(result));
        result.setTips(buildTips(result));
        result.setActions(buildActions(result));
        result.setContextStatus(resolveStatus(result));
        return result;
    }

    private LearningContextDTO loadLearningContext(Integer studentId,
                                                   LearningAssistantContextRequest request,
                                                   LearningAssistantContextDTO result) {
        if (studentId == null || request.getCourseId() == null || request.getSectionId() == null) {
            result.setLearningContextAvailable(false);
            return null;
        }
        try {
            LearningContextDTO context = learningWorkspaceService.getContext(studentId, request.getCourseId(),
                    request.getSectionId(), request.getActiveModule(), request.getActiveKnowledgePoint());
            result.setLearningContextAvailable(true);
            return context;
        } catch (Exception e) {
            log.warn("Failed to build learning assistant course context: studentId={}, courseId={}, sectionId={}",
                    studentId, request.getCourseId(), request.getSectionId(), e);
            result.setLearningContextAvailable(false);
            result.setContextError(e.getMessage());
            return null;
        }
    }

    private void applyLearningContext(LearningAssistantContextDTO result, LearningContextDTO context) {
        result.setLearningContext(context);
        result.setCourseId(context.getCourseId());
        result.setCourseName(context.getCourseName());
        result.setChapterId(context.getChapterId());
        result.setChapterName(context.getChapterName());
        result.setSectionId(context.getSectionId());
        result.setSectionName(context.getSectionName());
        result.setActiveModule(defaultText(context.getActiveModule(), "learning"));
        result.setActiveKnowledgePoint(context.getActiveKnowledgePoint());
        result.setWeakKnowledgePoints(copyTextList(context.getWeakKnowledgePoints()));
        result.setNextRecommendedAction(context.getNextRecommendedAction());
    }

    private List<ResourcePush> loadContextualResources(Integer studentId, LearningContextDTO context) {
        if (context.getCourseId() == null) {
            return List.of();
        }
        try {
            return resourcePushService.listContextualResources(studentId, context.getCourseId(), context.getChapterId(),
                    context.getSectionId(), context.getActiveKnowledgePoint());
        } catch (Exception e) {
            log.warn("Failed to load learning assistant contextual resources: studentId={}, courseId={}",
                    studentId, context.getCourseId(), e);
            return List.of();
        }
    }

    private StudentProfile loadProfile(Integer studentId) {
        if (studentId == null) {
            return null;
        }
        try {
            return studentProfileService.getProfileByStudentId(studentId);
        } catch (Exception e) {
            log.warn("Failed to load student profile for learning assistant: studentId={}", studentId, e);
            return null;
        }
    }

    private StudentProfileDashboardVO loadDashboard(Integer studentId) {
        if (studentId == null) {
            return null;
        }
        try {
            return studentProfileService.getDashboard(studentId);
        } catch (Exception e) {
            log.warn("Failed to load student profile dashboard for learning assistant: studentId={}", studentId, e);
            return null;
        }
    }

    private LearningAssistantContextDTO.StudentProfileSummaryDTO buildProfileSummary(
            StudentProfile profile, StudentProfileDashboardVO dashboard) {
        LearningAssistantContextDTO.StudentProfileSummaryDTO summary =
                new LearningAssistantContextDTO.StudentProfileSummaryDTO();
        summary.setProfileAvailable(profile != null);
        if (profile != null) {
            summary.setLearningGoal(profile.getLearningGoal());
            summary.setLearningStyle(profile.getLearningStyle());
            summary.setPreferredStyle(profile.getPreferredStyle());
            summary.setWeaknesses(parseWeaknesses(profile.getWeaknessPoints()));
        }
        StudentProfileDashboardVO.StatsOverview stats = dashboard == null ? null : dashboard.getStatsOverview();
        if (stats != null) {
            summary.setOverallScore(stats.getOverallScore());
            summary.setStudyMinutes(stats.getStudyMinutes());
            summary.setConcentration(stats.getConcentration());
            summary.setKnowledgeMastery(stats.getKnowledgeMastery());
            summary.setRiskWarning(stats.getRiskWarning());
        }
        return summary;
    }

    private RecommendedActionDTO inferRecommendedAction(LearningAssistantContextDTO context) {
        if (!context.getWeakKnowledgePoints().isEmpty()) {
            return new RecommendedActionDTO("REMEDIAL_RESOURCE", "查看补救资源",
                    "resource", context.getSectionId(), context.getWeakKnowledgePoints().get(0));
        }
        if (context.isLearningContextAvailable()) {
            return new RecommendedActionDTO("ASK_COURSE_AGENT", "问课程助手",
                    context.getActiveModule(), context.getSectionId(), context.getActiveKnowledgePoint());
        }
        return new RecommendedActionDTO("PROFILE_DASHBOARD", "查看学习画像",
                null, null, null);
    }

    private String buildGreeting(LearningAssistantContextDTO context) {
        if (hasText(context.getSectionName())) {
            return "正在学习：" + context.getSectionName();
        }
        if (hasText(context.getProfileSummary().getLearningGoal())) {
            return "围绕目标继续推进：" + context.getProfileSummary().getLearningGoal();
        }
        return "我会根据你的学习数据给出下一步建议";
    }

    private List<LearningAssistantContextDTO.AssistantTipDTO> buildTips(LearningAssistantContextDTO context) {
        List<LearningAssistantContextDTO.AssistantTipDTO> tips = new ArrayList<>();
        if (!context.getWeakKnowledgePoints().isEmpty()) {
            tips.add(new LearningAssistantContextDTO.AssistantTipDTO("weak-point",
                    "先处理薄弱点：" + context.getWeakKnowledgePoints().get(0), "learning-progress"));
        }
        if (hasText(context.getProfileSummary().getPreferredStyle())) {
            tips.add(new LearningAssistantContextDTO.AssistantTipDTO("profile-style",
                    "你的偏好是" + context.getProfileSummary().getPreferredStyle() + "，学习时优先匹配这种材料。",
                    "student-profile"));
        }
        Integer concentration = context.getProfileSummary().getConcentration();
        if (concentration != null && concentration < 60) {
            tips.add(new LearningAssistantContextDTO.AssistantTipDTO("focus-risk",
                    "最近专注度偏低，建议缩短单次学习并增加回顾。", "student-profile"));
        }
        if (tips.isEmpty() && context.getNextRecommendedAction() != null) {
            tips.add(new LearningAssistantContextDTO.AssistantTipDTO("next-action",
                    context.getNextRecommendedAction().getLabel(), "learning-assistant"));
        }
        if (tips.isEmpty()) {
            tips.add(new LearningAssistantContextDTO.AssistantTipDTO("start",
                    "先选一个小目标，再开始本轮学习。", "fallback"));
        }
        return tips;
    }

    private List<LearningAssistantContextDTO.AssistantActionDTO> buildActions(LearningAssistantContextDTO context) {
        List<LearningAssistantContextDTO.AssistantActionDTO> actions = new ArrayList<>();
        if (context.isLearningContextAvailable()) {
            actions.add(courseAgentAction(context));
        }
        RecommendedActionDTO next = context.getNextRecommendedAction();
        if (next != null && !"ASK_COURSE_AGENT".equals(normalize(next.getActionType()))) {
            actions.add(nextAction(context, next));
        }
        LearningAssistantContextDTO.AssistantActionDTO profile = new LearningAssistantContextDTO.AssistantActionDTO();
        profile.setKey("profile-dashboard");
        profile.setLabel("看画像");
        profile.setTitle("查看学生画像统计");
        profile.setActionType("PROFILE_DASHBOARD");
        profile.setRoute("/student/new-profile");
        profile.setEventType("PROFILE_DASHBOARD_OPENED");
        actions.add(profile);
        return actions;
    }

    private LearningAssistantContextDTO.AssistantActionDTO courseAgentAction(LearningAssistantContextDTO context) {
        LearningAssistantContextDTO.AssistantActionDTO action = new LearningAssistantContextDTO.AssistantActionDTO();
        action.setKey("ask-course-agent");
        action.setLabel("问助手");
        action.setTitle("唤起课程数字人解释当前学习点");
        action.setActionType("ASK_COURSE_AGENT");
        action.setTargetModule(context.getActiveModule());
        action.setKnowledgePoint(context.getActiveKnowledgePoint());
        action.setEventType("AI_EXPLANATION_REQUESTED");
        action.setEndpoint(COURSE_AGENT_ENDPOINT);
        action.setMessage(buildAgentPrompt(context));
        return action;
    }

    private LearningAssistantContextDTO.AssistantActionDTO nextAction(LearningAssistantContextDTO context,
                                                                      RecommendedActionDTO next) {
        LearningAssistantContextDTO.AssistantActionDTO action = new LearningAssistantContextDTO.AssistantActionDTO();
        String actionType = normalize(next.getActionType());
        action.setKey(actionType.toLowerCase(Locale.ROOT).replace('_', '-'));
        action.setLabel(defaultText(next.getLabel(), "继续学习"));
        action.setTitle(defaultText(next.getLabel(), "继续学习"));
        action.setActionType(actionType);
        action.setTargetModule(defaultText(next.getTargetModule(), context.getActiveModule()));
        action.setKnowledgePoint(defaultText(next.getKnowledgePoint(), context.getActiveKnowledgePoint()));
        action.setEventType(actionType + "_CLICKED");
        action.setMessage(defaultText(next.getLabel(), "继续学习"));
        return action;
    }

    private String buildAgentPrompt(LearningAssistantContextDTO context) {
        if (hasText(context.getActiveKnowledgePoint())) {
            return "请结合当前课程上下文，讲解我现在卡住的知识点：" + context.getActiveKnowledgePoint();
        }
        if (hasText(context.getSectionName())) {
            return "请结合我的学习进度，告诉我当前小节下一步该怎么学：" + context.getSectionName();
        }
        return "请根据我的学习画像和最近学习记录，给出下一步学习建议。";
    }

    private String resolveStatus(LearningAssistantContextDTO context) {
        if (context.isLearningContextAvailable() && context.getProfileSummary().isProfileAvailable()) {
            return "FULL_CONTEXT";
        }
        if (context.isLearningContextAvailable()) {
            return "LEARNING_CONTEXT";
        }
        if (context.getProfileSummary().isProfileAvailable()) {
            return "PROFILE_CONTEXT";
        }
        return "FALLBACK";
    }

    private List<String> parseWeaknesses(String raw) {
        if (!hasText(raw)) {
            return List.of();
        }
        try {
            return copyTextList(objectMapper.readValue(raw, new TypeReference<List<String>>() {
            }));
        } catch (Exception ignored) {
            String[] parts = raw.split("[,，;；、\\n]");
            List<String> result = new ArrayList<>();
            for (String part : parts) {
                if (hasText(part)) {
                    result.add(part.trim());
                }
            }
            return result;
        }
    }

    private List<String> mergeWeakPoints(List<String> primary, List<String> secondary) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        copyTextList(primary).forEach(values::add);
        copyTextList(secondary).forEach(values::add);
        return new ArrayList<>(values);
    }

    private List<String> copyTextList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
