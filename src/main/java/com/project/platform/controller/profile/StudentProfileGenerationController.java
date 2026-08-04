package com.project.platform.controller.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.platform.agents.ai.AiCodeHelperService;
import com.project.platform.agents.config.mongdb.PersistentChatMemoryStore;
import com.project.platform.agents.config.utils.Tools;
import com.project.platform.entity.*;
import com.project.platform.dto.profile.ProfileConversationRequest;
import com.project.platform.service.*;
import com.project.platform.vo.ResponseVO;
import com.project.platform.vo.profile.ProfileConversationVO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 学生画像生成控制器
 * 负责基于对话历史和行为数据生成学生画像
 */
@RestController
@RequestMapping("/student-profile")
@Slf4j
public class StudentProfileGenerationController {

    @Autowired
    private AiCodeHelperService aiCodeHelperService;

    @Autowired
    private StudentProfileService studentProfileService;

    @Autowired
    private StudyDurationService studyDurationService;

    @Autowired
    private AiToolUsageService aiToolUsageService;

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @Autowired
    private PersistentChatMemoryStore chatMemoryStore;

    @Autowired
    private ChapterService chapterService;

    @Autowired
    private Tools tools;

    @Autowired
    private ProfileConversationService profileConversationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 接收一轮自然语言描述并立即增量更新画像。该接口使用确定性抽取保底，
     * 与已有的大模型引导对话、行为分析接口共同组成随学随新的画像链路。
     */
    @PostMapping("/conversation-turn")
    public ResponseVO<ProfileConversationVO> conversationTurn(@RequestBody ProfileConversationRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseVO.error(400, "请先说说你的学习情况");
        }
        try {
            log.info("=== 接收一轮自然语言描述并立即增量更新画像。");
            Integer studentId = tools.getCurrentUserIdInteger();
            return ResponseVO.ok(profileConversationService.converse(studentId, request.getMessage()));
        } catch (IllegalArgumentException exception) {
            return ResponseVO.error(400, exception.getMessage());
        }
    }

    // 系统提示词
    private String system_prompt_student_profile;
    private String system_prompt_student_profile_behavior;
    @Autowired
    private CourseService courseService;

    @Autowired
    private StudentService studentService;

    @PostConstruct
    public void init() throws Exception {
        system_prompt_student_profile = read("system-prompt-student-profile.txt");
        system_prompt_student_profile_behavior = read("system-prompt-student-profile-behavior.txt");
    }

    private String read(String path) throws Exception {
        ClassPathResource resource = new ClassPathResource(path);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * 分析学生画像（从引导对话历史中提取学生特征）
     * 自动获取当前用户最新的画像引导会话进行分析，无需传入参数
     */
    @PostMapping("/analyze")
    public ResponseVO<StudentProfile> analyzeStudentProfileFromGuide() {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== 开始分析学生画像！studentId: {}", studentId);

        try {
            String keyword = "_profile_";
            // 自动获取该用户最新的画像引导会话（模糊匹配包含 _profile_ 的会话）
            List<String> memoryIds = chatMemoryStore.getMemoryIdsByUserIdAndKeyword(studentId.toString(), keyword);

            if (memoryIds == null || memoryIds.isEmpty()) {
                return ResponseVO.fail(404, "暂无画像引导对话记录，请先完成引导对话");
            }

            // 使用最新的会话（列表已按createTime降序排列）
            String latestMemoryId = memoryIds.get(0);
            log.info("=== 使用最新的画像引导会话：{}", latestMemoryId);

            // 获取该会话的对话历史
            List<dev.langchain4j.data.message.ChatMessage> messages = chatMemoryStore.getSessionMessages(latestMemoryId);

            if (messages == null || messages.isEmpty()) {
                return ResponseVO.fail(404, "对话历史为空，无法分析画像");
            }

            log.info("=== 获取到 {} 条对话消息", messages.size());

            // 拼接对话历史作为分析素材
            StringBuilder conversationBuilder = new StringBuilder();
            for (dev.langchain4j.data.message.ChatMessage msg : messages) {
                if (msg instanceof dev.langchain4j.data.message.UserMessage) {
                    conversationBuilder.append("学生: ")
                            .append(((dev.langchain4j.data.message.UserMessage) msg).singleText())
                            .append("\n");
                } else if (msg instanceof dev.langchain4j.data.message.AiMessage) {
                    conversationBuilder.append("AI: ")
                            .append(((dev.langchain4j.data.message.AiMessage) msg).text())
                            .append("\n");
                }
            }

            String conversationText = conversationBuilder.toString();
            log.info("=== 拼接对话历史，长度: {} 字符", conversationText.length());

            // 调用AI分析画像
            String response = aiCodeHelperService.GenerateProfile(
                    system_prompt_student_profile + "\n\n【对话历史】\n" + conversationText);
            log.info("=== AI返回的画像分析结果：{}", response);

            // 清理JSON并解析
            String cleanJson = response.replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();
            JsonNode jsonNode = objectMapper.readTree(cleanJson);

            // 构建StudentProfile对象
            StudentProfile profile = new StudentProfile();
            profile.setStudentId(studentId);

            // 基础信息维度
            profile.setMajor(jsonNode.path("major").asText(null));
            profile.setLearningGoal(jsonNode.path("learningGoal").asText(null));

            // 知识基础维度（1-5分转换为0-100分）
            profile.setKnowledgeDepth(convertScoreToPercentage(jsonNode.path("knowledgeDepth").asInt(3)));
            profile.setConceptClarity(convertScoreToPercentage(jsonNode.path("conceptClarity").asInt(3)));
            profile.setMemoryStability(convertScoreToPercentage(jsonNode.path("memoryStability").asInt(3)));
            profile.setTransferAbility(convertScoreToPercentage(jsonNode.path("transferAbility").asInt(3)));
            profile.setStructuralIntegrity(convertScoreToPercentage(jsonNode.path("structuralIntegrity").asInt(3)));

            // 认知风格维度（1-5分转换为0-100分，六维雷达图）
            profile.setInformationProcessing(convertScoreToPercentage(jsonNode.path("informationProcessing").asInt(3)));
            profile.setKnowledgeRepresentation(convertScoreToPercentage(jsonNode.path("knowledgeRepresentation").asInt(3)));
            profile.setLearningInitiative(convertScoreToPercentage(jsonNode.path("learningInitiative").asInt(3)));
            profile.setCognitiveControl(convertScoreToPercentage(jsonNode.path("cognitiveControl").asInt(3)));
            profile.setPracticeOrientation(convertScoreToPercentage(jsonNode.path("practiceOrientation").asInt(3)));
            profile.setTaskManagement(convertScoreToPercentage(jsonNode.path("taskManagement").asInt(3)));

            // 学习投入度维度（需要后续从实际学习行为中获取）
            profile.setAvgDailyStudyMinutes(0.0);
            profile.setAvgConcentration(50.0);

            // 知识获取偏好维度（从learningStyle转换）
            String learningStyle = jsonNode.path("learningStyle").asText("");
            int[] preferences = convertLearningStyleToPreferences(learningStyle);
            profile.setTextPreference(preferences[0]);
            profile.setImagePreference(preferences[1]);
            profile.setExamplePreference(preferences[2]);
            profile.setVideoPreference(preferences[3]);

            // 处理weaknessPoints数组 -> JSON字符串
            if (jsonNode.has("weaknessPoints") && jsonNode.get("weaknessPoints").isArray()) {
                profile.setErrorPreferences(objectMapper.writeValueAsString(jsonNode.get("weaknessPoints")));
                profile.setErrorTypeCount(jsonNode.get("weaknessPoints").size());
            }

            // 设置总结
            profile.setInterestSummary(jsonNode.path("summary").asText(null));

            // 保存或增量更新画像
            StudentProfile savedProfile = studentProfileService.saveOrUpdateProfile(profile);
            log.info("=== 学生画像分析完成！studentId: {}, version: {}", studentId, savedProfile.getVersion());

            // 画像分析完成后，将学生的 isRegistered 字段设置为 true
            Student student = studentService.selectById(studentId);
            if (student != null) {
                student.setIsRegistered(true);
                studentService.updateById(student);
                log.info("=== 学生画像引导完成，已更新 isRegistered 为 true，studentId: {}", studentId);
            }

            return ResponseVO.ok(savedProfile);

        } catch (Exception e) {
            log.error("分析学生画像失败", e);
            return ResponseVO.fail(500, "分析画像失败：" + e.getMessage());
        }
    }

    /**
     * 分析学生画像（基于行为数据）
     * 通过学习时长、AI工具使用、课程完成、闲聊对话、知识库等五类数据进行综合分析
     */
    @PostMapping("/analyze-behavior")
    public ResponseVO<StudentProfile> analyzeStudentProfileFromBehavior() {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== 开始基于行为数据分析学生画像！studentId: {}", studentId);

        try {
            // 1. 获取学习时长数据（近7天）
            Student studyStats = studyDurationService.getStudyDurationStats(studentId, 7);
            String studyDurationData = buildStudyDurationDescription(studyStats);
            log.info("=== 学习时长数据: {}", studyDurationData);

            // 2. 获取AI工具使用数据
            AiToolUsage aiToolUsage = aiToolUsageService.getUsageByUserId(studentId);
            String aiToolData = buildAiToolUsageDescription(aiToolUsage);
            log.info("=== AI工具使用数据: {}", aiToolData);

            // 3. 获取课程目录完成情况（获取用户所有课程的章节完成情况）
            String courseCompletionData = buildCourseCompletionDescription(studentId);
            log.info("=== 课程完成情况数据长度: {} 字符", courseCompletionData.length());

            // 4. 获取闲聊对话记录（_chat_关键词）
            List<String> chatMemoryIds = chatMemoryStore.getMemoryIdsByUserIdAndKeyword(studentId.toString(), "_chat_");
            String chatConversationData = buildChatConversationDescription(chatMemoryIds);
            log.info("=== 闲聊对话数据: {}", chatConversationData);

            // 5. 获取知识库文档情况
            List<KnowledgeDocument> documents = knowledgeDocumentService.getDocumentsByStudentId(studentId.longValue());
            String knowledgeBaseData = buildKnowledgeBaseDescription(documents);
            log.info("=== 知识库数据: {}", knowledgeBaseData);

            // 6. 构建完整的分析提示
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append(system_prompt_student_profile_behavior);
            promptBuilder.append("\n\n【学生行为数据】\n\n");
            promptBuilder.append("一、学习时长数据\n").append(studyDurationData).append("\n\n");
            promptBuilder.append("二、AI工具使用数据\n").append(aiToolData).append("\n\n");
            promptBuilder.append("三、课程目录完成情况\n").append(courseCompletionData).append("\n\n");
            promptBuilder.append("四、闲聊对话记录\n").append(chatConversationData).append("\n\n");
            promptBuilder.append("五、知识库文档情况\n").append(knowledgeBaseData).append("\n\n");

            String fullPrompt = promptBuilder.toString();
            log.info("=== 完整提示长度: {} 字符", fullPrompt.length());

            // 7. 调用AI分析画像
            String response = aiCodeHelperService.GenerateProfile(fullPrompt);
            log.info("=== AI返回的画像分析结果：{}", response);

            // 8. 清理JSON并解析
            String cleanJson = response.replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();
            JsonNode jsonNode = objectMapper.readTree(cleanJson);

            // 9. 获取现有画像（如果存在）
            StudentProfile existingProfile = studentProfileService.getProfileByStudentId(studentId);
            StudentProfile profile = existingProfile != null ? existingProfile : new StudentProfile();
            profile.setStudentId(studentId);

            // 10. 更新知识基础维度（1-5分转换为0-100分）
            profile.setKnowledgeDepth(convertScoreToPercentage(jsonNode.path("knowledgeDepth").asInt(3)));
            profile.setConceptClarity(convertScoreToPercentage(jsonNode.path("conceptClarity").asInt(3)));
            profile.setMemoryStability(convertScoreToPercentage(jsonNode.path("memoryStability").asInt(3)));
            profile.setTransferAbility(convertScoreToPercentage(jsonNode.path("transferAbility").asInt(3)));
            profile.setStructuralIntegrity(convertScoreToPercentage(jsonNode.path("structuralIntegrity").asInt(3)));

            // 11. 更新认知风格维度（1-5分转换为0-100分，六维雷达图）
            profile.setInformationProcessing(convertScoreToPercentage(jsonNode.path("informationProcessing").asInt(3)));
            profile.setKnowledgeRepresentation(convertScoreToPercentage(jsonNode.path("knowledgeRepresentation").asInt(3)));
            profile.setLearningInitiative(convertScoreToPercentage(jsonNode.path("learningInitiative").asInt(3)));
            profile.setCognitiveControl(convertScoreToPercentage(jsonNode.path("cognitiveControl").asInt(3)));
            profile.setPracticeOrientation(convertScoreToPercentage(jsonNode.path("practiceOrientation").asInt(3)));
            profile.setTaskManagement(convertScoreToPercentage(jsonNode.path("taskManagement").asInt(3)));

            // 12. 更新学习风格
            String learningStyle = jsonNode.path("learningStyle").asText("");
            if (!learningStyle.isEmpty()) {
                profile.setLearningStyle(learningStyle);
                int[] preferences = convertLearningStyleToPreferences(learningStyle);
                profile.setTextPreference(preferences[0]);
                profile.setImagePreference(preferences[1]);
                profile.setExamplePreference(preferences[2]);
                profile.setVideoPreference(preferences[3]);
            }

            // 13. 处理薄弱知识点
            if (jsonNode.has("weaknessPoints") && jsonNode.get("weaknessPoints").isArray()) {
                profile.setErrorPreferences(objectMapper.writeValueAsString(jsonNode.get("weaknessPoints")));
                profile.setErrorTypeCount(jsonNode.get("weaknessPoints").size());
            }

            // 14. 更新偏好学习风格
            String preferredStyle = jsonNode.path("preferredStyle").asText("");
            if (!preferredStyle.isEmpty()) {
                profile.setPreferredStyle(preferredStyle);
            }

            // 15. 设置总结
            String summary = jsonNode.path("summary").asText(null);
            if (summary != null) {
                profile.setInterestSummary(summary);
            }

            // 16. 保存或增量更新画像
            StudentProfile savedProfile = studentProfileService.saveOrUpdateProfile(profile);
            log.info("=== 学生行为画像分析完成！studentId: {}, version: {}", studentId, savedProfile.getVersion());

            return ResponseVO.ok(savedProfile);

        } catch (Exception e) {
            log.error("基于行为数据分析学生画像失败", e);
            return ResponseVO.fail(500, "分析画像失败：" + e.getMessage());
        }
    }

    /**
     * 将1-5分评分转换为0-100分
     */
    private int convertScoreToPercentage(int score) {
        if (score < 1) score = 1;
        if (score > 5) score = 5;
        return score * 20; // 1->20, 2->40, 3->60, 4->80, 5->100
    }

    /**
     * 将学习风格转换为四个偏好维度的百分比
     */
    private int[] convertLearningStyleToPreferences(String learningStyle) {
        int textPref = 25, imagePref = 25, examplePref = 25, videoPref = 25; // 默认平均分配

        if (learningStyle != null && !learningStyle.isEmpty()) {
            String[] styles = learningStyle.toLowerCase().split(",");
            int styleCount = styles.length;

            for (String style : styles) {
                style = style.trim();
                switch (style) {
                    case "visual":
                        imagePref += 20;
                        break;
                    case "auditory":
                        videoPref += 20;
                        break;
                    case "kinesthetic":
                        examplePref += 20;
                        break;
                    case "reading":
                        textPref += 20;
                        break;
                }
            }

            // 归一化到100%
            int total = textPref + imagePref + examplePref + videoPref;
            if (total > 0) {
                textPref = textPref * 100 / total;
                imagePref = imagePref * 100 / total;
                examplePref = examplePref * 100 / total;
                videoPref = videoPref * 100 / total;
            }
        }

        return new int[]{textPref, imagePref, examplePref, videoPref};
    }

    /**
     * 构建学习时长描述
     */
    private String buildStudyDurationDescription(Student studyStats) {
        StringBuilder sb = new StringBuilder();
        if (studyStats.getTotalStudyMinutes() != null) {
            sb.append("- 近7天总学习时长：").append(studyStats.getTotalStudyMinutes()).append("分钟\n");
        }
        if (studyStats.getAvgStudyMinutes() != null) {
            sb.append("- 日均学习时长：").append(studyStats.getAvgStudyMinutes()).append("分钟\n");
        }
        if (studyStats.getStudyDurationDates() != null && !studyStats.getStudyDurationDates().isEmpty()) {
            sb.append("- 每日学习分布：\n");
            for (int i = 0; i < studyStats.getStudyDurationDates().size(); i++) {
                String date = studyStats.getStudyDurationDates().get(i);
                Integer duration = studyStats.getStudyDurationMinutes() != null && i < studyStats.getStudyDurationMinutes().size()
                        ? studyStats.getStudyDurationMinutes().get(i) : 0;
                sb.append("  ").append(date).append(": ").append(duration).append("分钟\n");
            }
        }
        return sb.toString();
    }

    /**
     * 构建AI工具使用描述
     */
    private String buildAiToolUsageDescription(AiToolUsage usage) {
        StringBuilder sb = new StringBuilder();
        sb.append("- 总使用次数：").append(usage.getTotalUsageCount() != null ? usage.getTotalUsageCount() : 0).append("次\n");
        sb.append("- 总使用时长：").append(usage.getTotalDurationMinutes() != null ? usage.getTotalDurationMinutes() : 0).append("分钟\n");

        if (usage.getChatUsageCount() != null && usage.getChatUsageCount() > 0) {
            sb.append("- 大模型对话：").append(usage.getChatUsageCount()).append("次，")
              .append(usage.getChatDurationMinutes() != null ? usage.getChatDurationMinutes() : 0).append("分钟\n");
        }
        if (usage.getPptUsageCount() != null && usage.getPptUsageCount() > 0) {
            sb.append("- PPT生成：").append(usage.getPptUsageCount()).append("次，")
              .append(usage.getPptDurationMinutes() != null ? usage.getPptDurationMinutes() : 0).append("分钟\n");
        }
        if (usage.getMindmapUsageCount() != null && usage.getMindmapUsageCount() > 0) {
            sb.append("- 思维导图：").append(usage.getMindmapUsageCount()).append("次，")
              .append(usage.getMindmapDurationMinutes() != null ? usage.getMindmapDurationMinutes() : 0).append("分钟\n");
        }
        if (usage.getResourceRecommendUsageCount() != null && usage.getResourceRecommendUsageCount() > 0) {
            sb.append("- 资源推荐：").append(usage.getResourceRecommendUsageCount()).append("次，")
              .append(usage.getResourceDurationMinutes() != null ? usage.getResourceDurationMinutes() : 0).append("分钟\n");
        }
        if (usage.getImmersiveCourseUsageCount() != null && usage.getImmersiveCourseUsageCount() > 0) {
            sb.append("- 沉浸式课程：").append(usage.getImmersiveCourseUsageCount()).append("次，")
              .append(usage.getCourseDurationMinutes() != null ? usage.getCourseDurationMinutes() : 0).append("分钟\n");
        }
        if (usage.getVirtualTeacherUsageCount() != null && usage.getVirtualTeacherUsageCount() > 0) {
            sb.append("- 虚拟教师：").append(usage.getVirtualTeacherUsageCount()).append("次，")
              .append(usage.getTeacherDurationMinutes() != null ? usage.getTeacherDurationMinutes() : 0).append("分钟\n");
        }

        if (usage.getMostUsedTool() != null) {
            sb.append("- 最常用工具：").append(usage.getMostUsedTool()).append("\n");
        }
        if (usage.getEngagementScore() != null) {
            sb.append("- 学习投入度评分：").append(String.format("%.1f", usage.getEngagementScore())).append("/100\n");
        }
        if (usage.getToolMasteryLevel() != null) {
            sb.append("- AI工具掌握水平：").append(String.format("%.1f", usage.getToolMasteryLevel())).append("/100\n");
        }
        if (usage.getLearningStyleByAI() != null) {
            sb.append("- 学习风格推断：").append(usage.getLearningStyleByAI()).append("\n");
        }
        if (usage.getUsagePattern() != null) {
            sb.append("- 使用模式：").append(usage.getUsagePattern()).append("\n");
        }

        return sb.toString();
    }

    /**
     * 构建课程完成情况描述
     */
    private String buildCourseCompletionDescription(Integer studentId) {
        StringBuilder sb = new StringBuilder();

        // 获取学生所有课程
        List<Course> courses = courseService.listByCreator(studentId);

        if (courses == null || courses.isEmpty()) {
            sb.append("- 暂无课程记录\n");
        } else {
            sb.append("- 课程总数：").append(courses.size()).append("个\n");
            sb.append("- 课程列表：\n");
            for (Course course : courses) {
                sb.append("  - ").append(course.getCourseName() != null ? course.getCourseName() : "未命名课程").append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 构建闲聊对话描述
     */
    private String buildChatConversationDescription(List<String> chatMemoryIds) {
        StringBuilder sb = new StringBuilder();

        if (chatMemoryIds == null || chatMemoryIds.isEmpty()) {
            sb.append("- 暂无闲聊对话记录\n");
            return sb.toString();
        }

        sb.append("- 闲聊会话数量：").append(chatMemoryIds.size()).append("个\n");

        // 只分析最近3个会话，避免数据过多
        int maxSessions = Math.min(3, chatMemoryIds.size());
        for (int i = 0; i < maxSessions; i++) {
            String memoryId = chatMemoryIds.get(i);
            List<dev.langchain4j.data.message.ChatMessage> messages = chatMemoryStore.getSessionMessages(memoryId);

            if (messages != null && !messages.isEmpty()) {
                sb.append("\n会话").append(i + 1).append("（共").append(messages.size()).append("条消息）：\n");

                // 提取用户消息的关键内容（最多5条）
                int msgCount = 0;
                for (dev.langchain4j.data.message.ChatMessage msg : messages) {
                    if (msg instanceof dev.langchain4j.data.message.UserMessage && msgCount < 5) {
                        String text = ((dev.langchain4j.data.message.UserMessage) msg).singleText();
                        // 截取前100字符作为摘要
                        String summary = text.length() > 100 ? text.substring(0, 100) + "..." : text;
                        sb.append("  - 学生提问：").append(summary).append("\n");
                        msgCount++;
                    }
                }
            }
        }

        return sb.toString();
    }

    /**
     * 构建知识库描述
     */
    private String buildKnowledgeBaseDescription(List<KnowledgeDocument> documents) {
        StringBuilder sb = new StringBuilder();

        if (documents == null || documents.isEmpty()) {
            sb.append("- 暂无上传的文档\n");
            return sb.toString();
        }

        sb.append("- 文档总数：").append(documents.size()).append("个\n");

        // 统计文件类型
        Map<String, Long> fileTypeCount = documents.stream()
                .filter(doc -> doc.getFileType() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        KnowledgeDocument::getFileType,
                        java.util.stream.Collectors.counting()));

        sb.append("- 文件类型分布：\n");
        for (Map.Entry<String, Long> entry : fileTypeCount.entrySet()) {
            sb.append("  - ").append(entry.getKey().toUpperCase()).append(": ").append(entry.getValue()).append("个\n");
        }

        // 统计处理状态
        long completedCount = documents.stream()
                .filter(doc -> doc.getEmbeddingStatus() != null && doc.getEmbeddingStatus() == 2)
                .count();
        long processingCount = documents.stream()
                .filter(doc -> doc.getEmbeddingStatus() != null && doc.getEmbeddingStatus() == 1)
                .count();
        long failedCount = documents.stream()
                .filter(doc -> doc.getEmbeddingStatus() != null && doc.getEmbeddingStatus() == 3)
                .count();

        sb.append("- 处理状态：已完成").append(completedCount)
          .append("个，处理中").append(processingCount)
          .append("个，失败").append(failedCount).append("个\n");

        // 显示最近的5个文档
        sb.append("- 最近上传的文档：\n");
        documents.stream()
                .sorted((d1, d2) -> d2.getCreatedAt().compareTo(d1.getCreatedAt()))
                .limit(5)
                .forEach(doc -> {
                    sb.append("  - ").append(doc.getFileName())
                      .append(" (").append(formatFileSize(doc.getFileSize())).append(")\n");
                    if (doc.getContentSummary() != null && !doc.getContentSummary().isEmpty()) {
                        String summary = doc.getContentSummary().length() > 80
                                ? doc.getContentSummary().substring(0, 80) + "..."
                                : doc.getContentSummary();
                        sb.append("    摘要：").append(summary).append("\n");
                    }
                });

        return sb.toString();
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(Long bytes) {
        if (bytes == null) return "未知";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
