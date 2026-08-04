package com.project.platform.service.course.nodes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.platform.agents.ai.AiCourseHelperService;
import com.project.platform.dto.ChapterOutlineDTO;
import com.project.platform.dto.courseagent.CourseMultiAgentOutlineResponse;
import com.project.platform.dto.courseworkflow.CourseWorkflowSnapshotDTO;
import com.project.platform.dto.courseworkflow.CourseWorkflowState;
import com.project.platform.entity.Chapter;
import com.project.platform.entity.Course;
import com.project.platform.entity.Section;
import com.project.platform.service.ChapterService;
import com.project.platform.service.CourseService;
import com.project.platform.service.SectionService;
import com.project.platform.service.course.CourseWorkflowTraceService;
import com.project.platform.service.course.WorkflowSseEmitterService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DemandExtractionAgent {

    @Resource
    private AiCourseHelperService aiCourseHelperService;
    @Resource
    private CourseService courseService;
    @Resource
    private ChapterService chapterService;
    @Resource
    private SectionService sectionService;
    @Resource
    private CourseWorkflowTraceService traceService;
    @Resource
    private WorkflowSseEmitterService sseEmitterService;
    @Resource
    private ObjectMapper objectMapper;

    private String systemPromptChapter;
    private String systemPromptSections;

    @jakarta.annotation.PostConstruct
    void init() {
        systemPromptChapter = loadPrompt("system-prompt-chapter.txt");
        systemPromptSections = loadPrompt("system-prompt-chapter-sections.txt");
    }

    public CourseWorkflowState execute(CourseWorkflowState state) {
        String taskId = state.getTaskId();
        log.info("[需求提取] 开始: taskId={}", taskId);
        traceService.bind(taskId);
        traceService.startStep(taskId, "demand_extraction", "正在解析学习需求...");

        traceService.logActivity(taskId, "demand_extraction", "需求解析师",
                "需求分析", "analysis", "开始分析",
                "用户输入: \"" + truncate(state.getMessage(), 100) + "\"，难度: " +
                (state.getDifficulty() != null ? state.getDifficulty() : "默认") +
                "，模式: " + (state.getCourseMode() != null ? state.getCourseMode() : "默认"));
        pushSnapshot(taskId);
        delay(400);

        try {
            // 子步骤1: 提取参数
            traceService.logActivity(taskId, "demand_extraction", "需求解析师",
                    "AI 调用", "analysis", "参数提取",
                    "正在调用 AI 模型从用户输入中提取课程主题、难度、时长等参数...");
            pushSnapshot(taskId);
            delay(300);

            String paramPrompt = String.format(systemPromptChapter,
                    state.getMessage(),
                    state.getDifficulty() != null ? state.getDifficulty() : "beginner",
                    "30天",
                    state.getCourseMode() != null ? state.getCourseMode() : "project-based");

            String paramResponse = aiCourseHelperService.GenerateCourse(paramPrompt);
            String paramJson = cleanJson(paramResponse);
            JsonNode root = objectMapper.readTree(paramJson);

            String topic = root.path("topic").asText(state.getMessage());
            String level = root.path("level").asText("beginner");
            String duration = root.path("duration").asText("30天");
            String style = root.path("style").asText("project-based");
            String description = root.path("description").asText("");

            traceService.logActivity(taskId, "demand_extraction", "需求解析师",
                    "提取完成", "result", "参数确认",
                    String.format("主题: %s | 难度: %s | 时长: %s | 风格: %s", topic, level, duration, style));
            pushSnapshot(taskId);
            delay(300);

            // 子步骤2: 生成大纲
            traceService.logActivity(taskId, "demand_extraction", "需求解析师",
                    "AI 调用", "analysis", "大纲生成",
                    "正在调用 AI 模型根据主题「" + topic + "」生成课程章节大纲...");
            pushSnapshot(taskId);
            delay(300);

            String sectionsPrompt = String.format(systemPromptSections, topic, level, duration, style);
            String sectionsResponse = aiCourseHelperService.GenerateCourse(sectionsPrompt);
            String sectionsJson = cleanJson(sectionsResponse);
            List<ChapterOutlineDTO> outline = objectMapper.readValue(sectionsJson,
                    new TypeReference<List<ChapterOutlineDTO>>() {});

            CourseMultiAgentOutlineResponse outlineResp = new CourseMultiAgentOutlineResponse();
            outlineResp.setCourseName(topic);
            outlineResp.setLevel(level);
            outlineResp.setDuration(duration);
            outlineResp.setStyle(style);
            outlineResp.setDescription(description);
            outlineResp.setOutline(outline);

            // 子步骤3: 持久化
            traceService.logActivity(taskId, "demand_extraction", "需求解析师",
                    "数据写入", "result", "课程创建",
                    "正在创建课程「" + topic + "」并写入数据库...");
            pushSnapshot(taskId);
            delay(200);

            Course course = persistCourse(state, outlineResp);
            List<Chapter> chapters = persistChaptersAndSections(course.getId(), outlineResp.getOutline());

            state.setCourseId(course.getId());
            state.setCourseName(topic);
            state.setDescription(description);
            state.setTopic(topic);
            state.setLevel(level);
            state.setDuration(duration);
            state.setStyle(style);
            state.setOutline(outline);
            state.getStageStatus().put("demand_extraction", "completed");

            // 打印章节结构
            StringBuilder chapterList = new StringBuilder();
            for (int i = 0; i < chapters.size(); i++) {
                Chapter ch = chapters.get(i);
                List<Section> secs = sectionService.listByChapterId(ch.getId());
                chapterList.append(ch.getChapterName()).append("(").append(secs != null ? secs.size() : 0).append("节)");
                if (i < chapters.size() - 1) chapterList.append(", ");
            }

            traceService.logActivity(taskId, "demand_extraction", "需求解析师",
                    "完成", "result", "骨架完成",
                    "课程骨架构建完成: " + topic + " | 共 " + chapters.size() + " 章 | " +
                    chapterList.toString());
            traceService.completeStep(taskId, "demand_extraction",
                    "课程「" + topic + "」已创建，" + chapters.size() + " 章");
            pushSnapshot(taskId);

            log.info("[需求提取] 完成: taskId={}, courseId={}", taskId, course.getId());
        } catch (Exception e) {
            log.error("[需求提取] 失败: taskId={}", taskId, e);
            state.getStageStatus().put("demand_extraction", "failed");
            traceService.fail(taskId, "需求提取失败: " + e.getMessage());
            throw new RuntimeException("需求提取失败", e);
        }
        return state;
    }

    private Course persistCourse(CourseWorkflowState state, CourseMultiAgentOutlineResponse resp) {
        Course course = new Course();
        course.setCourseName(resp.getCourseName());
        course.setDescription(resp.getDescription());
        course.setChapterCount(resp.getOutline().size());
        course.setCreatorId(state.getUserId());
        course.setCreateTime(LocalDateTime.now());
        courseService.insert(course);
        return courseService.listByCourseName(course.getCourseName());
    }

    private List<Chapter> persistChaptersAndSections(Integer courseId, List<ChapterOutlineDTO> outlines) {
        List<Chapter> chapters = new ArrayList<>();
        for (int i = 0; i < outlines.size(); i++) {
            ChapterOutlineDTO o = outlines.get(i);
            Chapter ch = new Chapter();
            ch.setCourseId(courseId);
            ch.setChapterName(o.getChapter());
            ch.setSortOrder(i + 1);
            ch.setCreateTime(LocalDateTime.now());
            chapterService.insert(ch);
            Chapter saved = chapterService.selectByCourseIdAndChapterName(ch.getCourseId(), ch.getChapterName());
            chapters.add(saved);

            List<String> sections = o.getSections();
            if (sections != null) {
                for (int j = 0; j < sections.size(); j++) {
                    Section sec = new Section();
                    sec.setChapterId(saved.getId());
                    sec.setSectionName(sections.get(j));
                    sec.setSortOrder(j + 1);
                    sec.setDuration(0);
                    sec.setCreateTime(LocalDateTime.now());
                    sectionService.insert(sec);
                }
            }
        }
        return chapters;
    }

    private void pushSnapshot(String taskId) {
        CourseWorkflowSnapshotDTO s = traceService.getSnapshot(taskId);
        if (s != null) sseEmitterService.sendSnapshot(taskId, s);
    }

    private String cleanJson(String r) {
        return r.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
    }

    private String truncate(String t, int max) {
        return t != null && t.length() > max ? t.substring(0, max) + "..." : t;
    }

    private void delay(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private String loadPrompt(String path) {
        try {
            org.springframework.core.io.ClassPathResource resource =
                    new org.springframework.core.io.ClassPathResource(path);
            return new String(resource.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("加载提示词失败: {}", path, e);
            return "";
        }
    }
}
