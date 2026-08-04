package com.project.platform.controller.course;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.platform.agents.ai.AiCourseHelperService;
import com.project.platform.agents.config.utils.Tools;
import com.project.platform.agents.entry.ChatForm;
import com.project.platform.agents.entry.GenerateOutlineForm;
import com.project.platform.agents.verify.ClaimVerdict;
import com.project.platform.agents.verify.CourseContentVerificationAgent;
import com.project.platform.agents.verify.VerificationClaim;
import com.project.platform.agents.verify.VerificationEvidence;
import com.project.platform.agents.verify.VerificationReport;
import com.project.platform.annotation.ConsumePoints;
import com.project.platform.annotation.TrackAiUsage;
import com.project.platform.dto.ChapterOutlineDTO;
import com.project.platform.dto.CourseDTO;
import com.project.platform.dto.ExamQuestionRequest;
import com.project.platform.dto.QuestionAnswerDTO;
import com.project.platform.entity.*;
import com.project.platform.service.ChapterService;
import com.project.platform.service.CourseGenerationService;
import com.project.platform.service.CourseService;
import com.project.platform.service.QuestionService;
import com.project.platform.service.SectionService;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 课程生成控制器
 * 处理课程的智能生成、小节内容生成、题目生成等功能
 */
@RestController
@RequestMapping("/xunfei/course")
@Slf4j
public class CourseGenerationController {

    @Resource
    private CourseGenerationService courseGenerationService;

    @Resource
    private QuestionService questionService;

    @Resource
    private SectionService sectionService;

    @Resource
    private Tools tools;

    @Resource
    private AiCourseHelperService aiCourseHelperService;

    @Resource
    private CourseService courseService;

    @Resource
    private ChapterService chapterService;

    @Resource
    private CourseContentVerificationAgent verificationAgent;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 智能生成完整课程（三步流程）
     * 1. 生成课程基本信息
     * 2. 生成课程大纲（章节）
     * 3. 生成小节（可选，按需生成）
     */
    @PostMapping("/generate-smart")
    @TrackAiUsage(AiToolUsage.ToolType.IMMERSIVE_COURSE)
    @ConsumePoints(value = 10, bizType = "COURSE")
    public ResponseVO<CourseDTO> generateCourseSmart(@RequestBody ChatForm chatForm) {
        Integer userId = tools.getCurrentUserIdInteger();
        log.info("=== 开始生成完整课程！用户输入：{}", chatForm.getMessage());

        CourseDTO courseDTO = courseGenerationService.generateCourseSmart(chatForm.getMessage(), userId);
        return ResponseVO.ok(courseDTO);
    }

    /**
     * 从自然语言中提取结构化参数
     */
    private GenerateOutlineForm extractParameters(String message) {
        String system_prompt_chapter = loadSystemPrompt("system-prompt-chapter.txt");
        String defaultLevel = "beginner";
        String defaultDuration = "30天";
        String defaultStyle = "project-based";

        String prompt = String.format(
                system_prompt_chapter,
                message,
                defaultLevel,
                defaultDuration,
                defaultStyle
        );

        try {
            String response = aiCourseHelperService.GenerateCourse(prompt);
            String cleanJson = response.replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();
            JsonNode jsonNode = objectMapper.readTree(cleanJson);
            GenerateOutlineForm form = new GenerateOutlineForm();
            form.setTopic(jsonNode.path("topic").asText(""));
            form.setLevel(jsonNode.path("level").asText("beginner"));
            form.setDuration(jsonNode.path("duration").asText("30天"));
            form.setStyle(jsonNode.path("style").asText("project-based"));
            form.setDescription(jsonNode.path("description").asText());
            return form;
        } catch (Exception e) {
            log.error("参数提取失败，使用默认值", e);
            GenerateOutlineForm fallback = new GenerateOutlineForm();
            fallback.setTopic(message.replaceAll("我想学|我要学|学习", "").trim());
            fallback.setLevel("beginner");
            fallback.setDuration("30天");
            fallback.setStyle("project-based");
            return fallback;
        }
    }

    /**
     * 根据提取的参数生成课程大纲
     */
    private List<ChapterOutlineDTO> generateOutline(GenerateOutlineForm form) {
        String system_prompt_sections = loadSystemPrompt("system-prompt-chapter-sections.txt");

        String prompt = String.format(
                system_prompt_sections,
                form.getTopic(),
                form.getLevel(),
                form.getDuration(),
                form.getStyle()
        );

        log.info("=== 生成课程大纲！主题：{}, 难度：{}", form.getTopic(), form.getLevel());
        String response = aiCourseHelperService.GenerateCourse(prompt);

        try {
            String cleanJson = response.replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();
            return objectMapper.readValue(cleanJson,
                    new TypeReference<List<ChapterOutlineDTO>>() {});
        } catch (Exception e) {
            log.error("解析课程大纲JSON失败", e);
            throw new RuntimeException("生成课程大纲失败: " + e.getMessage());
        }
    }

    /**
     * 根据大纲创建章节和小节
     */
    private List<Chapter> createChaptersAndSections(Integer courseId, List<ChapterOutlineDTO> outlines) {
        List<Chapter> chapters = new ArrayList<>();

        for (int i = 0; i < outlines.size(); i++) {
            ChapterOutlineDTO outline = outlines.get(i);

            Chapter chapter1 = new Chapter();
            chapter1.setCourseId(courseId);
            chapter1.setChapterName(outline.getChapter());
            chapter1.setSortOrder(i + 1);
            chapter1.setCreateTime(LocalDateTime.now());
            chapterService.insert(chapter1);
            Chapter chapter = chapterService.selectByCourseIdAndChapterName(chapter1.getCourseId(), chapter1.getChapterName());

            log.info("=== 创建章节：{}", outline.getChapter());

            List<String> sections = outline.getSections();
            if (sections != null && !sections.isEmpty()) {
                for (int j = 0; j < sections.size(); j++) {
                    Section section = new Section();
                    section.setChapterId(chapter.getId());
                    section.setSectionName(sections.get(j));
                    section.setSortOrder(j + 1);
                    section.setDuration(0);
                    section.setCreateTime(LocalDateTime.now());
                    sectionService.insert(section);

                    log.info("=== 创建小节：{} (ID: {})", sections.get(j), section.getId());
                }
            }

            chapters.add(chapter);
        }

        return chapters;
    }

    /**
     * 加载系统提示词
     */
    private String loadSystemPrompt(String path) {
        try {
            org.springframework.core.io.ClassPathResource resource =
                new org.springframework.core.io.ClassPathResource(path);
            return new String(resource.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("加载系统提示词失败: {}", path, e);
            return "";
        }
    }

    /**
     * 生成单个小节的详细内容并持久化到数据库
     */
    @PostMapping("/generate-section-content")
    @ConsumePoints(value = 10, bizType = "COURSE")
    public ResponseVO<Section> generateSectionContent(@RequestParam Integer chapterId,
                                                      @RequestParam Integer sectionId) {
        log.info("=== 开始生成小节内容！章节ID：{}, 小节ID：{}", chapterId, sectionId);

        try {
            Section section = courseGenerationService.generateSectionContent(chapterId, sectionId);
            return ResponseVO.ok(section);
        } catch (IllegalArgumentException e) {
            return ResponseVO.fail(404, e.getMessage());
        } catch (Exception e) {
            log.error("生成小节内容失败", e);
            return ResponseVO.fail(500, "生成小节内容失败");
        }
    }

    /**
     * 批量生成某个章节下所有小节的内容
     */
    @PostMapping("/generate-chapter-content")
    @ConsumePoints(value = 10, bizType = "COURSE")
    public ResponseVO<List<Section>> generateChapterContent(@RequestParam Integer chapterId) {
        log.info("=== 批量生成章节内容！章节ID：{}", chapterId);

        try {
            List<Section> sections = courseGenerationService.generateChapterContent(chapterId);
            return ResponseVO.ok(sections);
        } catch (IllegalArgumentException e) {
            return ResponseVO.fail(404, e.getMessage());
        } catch (Exception e) {
            log.error("批量生成章节内容失败", e);
            return ResponseVO.fail(500, "批量生成章节内容失败");
        }
    }

    /**
     * 批量更新某章节下所有小节的图片内容
     */
    @PostMapping("/update-chapter-images")
    public ResponseVO<Void> updateChapterImages(@RequestParam Integer chapterId) throws IOException {
        courseGenerationService.updateChapterImages(chapterId);
        return ResponseVO.ok();
    }

    /**
     * 生成小节的随堂练习题
     * 根据小节名称和小节内容,使用AI生成练习题并保存到数据库
     */
    @PostMapping("/generate-questions")
    @TrackAiUsage(AiToolUsage.ToolType.IMMERSIVE_COURSE)
    @ConsumePoints(value = 5, bizType = "EXAM")
    public ResponseVO<List<Question>> generateQuestions(@RequestParam Integer sectionId) {
        log.info("=== 开始生成随堂练习题！小节ID：{}", sectionId);

        try {
            List<Question> questions = courseGenerationService.generateQuestions(sectionId);
            return ResponseVO.ok(questions);
        } catch (IllegalArgumentException e) {
            return ResponseVO.fail(404, e.getMessage());
        } catch (Exception e) {
            log.error("生成随堂练习题失败", e);
            return ResponseVO.fail(500, "生成题目失败：" + e.getMessage());
        }
    }

    /**
     * 查询小节的随堂练习题
     */
    @GetMapping("/get-questions")
    public ResponseVO<List<Question>> getQuestions(@RequestParam Integer sectionId) {
        log.info("=== 查询小节练习题，sectionId：{}", sectionId);
        List<Question> questions = questionService.getBySectionId(sectionId);
        return ResponseVO.ok(questions);
    }

    /**
     * 提交答案并保存测试记录(用于随堂练习)
     */
    @PostMapping("/submit-questions")
    @TrackAiUsage(AiToolUsage.ToolType.IMMERSIVE_COURSE)
    public ResponseVO<Void> submitQuestions(@RequestBody List<QuestionAnswerDTO> answers,@RequestParam Integer spentTime) {
        Integer userId = tools.getCurrentUserIdInteger();
        sectionService.learnComplete(userId,answers.get(0).getSectionId());
        log.info("=== 开始提交答案！用户ID：{}，题目数量：{}", userId, answers.size());

        try {
            questionService.submitAnswers(userId, answers,spentTime);
            return ResponseVO.ok(null);
        } catch (IllegalArgumentException e) {
            return ResponseVO.fail(400, e.getMessage());
        } catch (Exception e) {
            log.error("提交答案失败", e);
            return ResponseVO.fail(500, "提交答案失败：" + e.getMessage());
        }
    }

    /**
     * 根据课程名称和知识点生成考试题
     * 支持自定义题型分布，如果不指定则按默认考试模板生成
     * 保证10道题以上
     */
    @PostMapping("/generate-exam-questions")
    @TrackAiUsage(AiToolUsage.ToolType.IMMERSIVE_COURSE)
    @ConsumePoints(value = 5, bizType = "EXAM")
    public ResponseVO<List<Question>> generateExamQuestions(@RequestBody ExamQuestionRequest request) {
        Integer userId = tools.getCurrentUserIdInteger();
        log.info("=== 开始生成考试题！用户ID：{}，课程ID：{}，知识点：{}，题型要求：{}",
                userId, request.getCourseId(), request.getKnowledgePoints(), request.getQuestionTypes());

        try {
            // 参数验证
            if (request.getCourseId() == null) {
                return ResponseVO.fail(400, "课程ID不能为空");
            }

            List<Question> questions = courseGenerationService.generateExamQuestions(
                    request.getCourseId(),
                    request.getKnowledgePoints(),
                    request.getQuestionTypes()
            );

            // 验证题目数量
            if (questions.size() < 10) {
                log.warn("生成的题目数量不足10道，实际数量：{}", questions.size());
            }

            return ResponseVO.ok(questions);
        } catch (Exception e) {
            log.error("生成考试题失败", e);
            return ResponseVO.fail(500, "生成考试题失败：" + e.getMessage());
        }
    }

    /**
     * 生成小节内容并自动执行真实性校验
     * <p>
     * 先生成课程小节内容，然后自动对其执行联网查证。
     * 返回小节实体和校验报告的组合结果。
     */
    @PostMapping("/generate-section-content-verified")
    @ConsumePoints(value = 10, bizType = "COURSE")
    public ResponseVO<VerifiedSectionResult> generateSectionContentVerified(
            @RequestParam Integer chapterId,
            @RequestParam Integer sectionId) {
        log.info("=== 生成小节内容+校验！章节ID：{}, 小节ID：{}", chapterId, sectionId);

        try {
            // 1. 生成小节内容
            Section section = courseGenerationService.generateSectionContent(chapterId, sectionId);

            // 2. 从文件读取生成的内容
            String sectionContent = tools.readContentFromFile(section.getContentUrl());
            VerificationReport report = null;
            if (sectionContent != null && !sectionContent.isBlank()) {
                report = verificationAgent.verify(sectionContent);
                log.info("小节内容校验完成: sectionId={}, truthScore={}, verified={}, suspicious={}, false={}",
                        sectionId, report.getTruthScore(),
                        report.getVerifiedCount(), report.getSuspiciousCount(), report.getFalseCount());
            }

            VerifiedSectionResult result = new VerifiedSectionResult();
            result.setSection(section);
            result.setVerificationReport(report);
            return ResponseVO.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseVO.fail(404, e.getMessage());
        } catch (Exception e) {
            log.error("生成小节内容+校验失败", e);
            return ResponseVO.fail(500, "操作失败: " + e.getMessage());
        }
    }

    /**
     * 对已有小节内容执行真实性校验
     */
    @PostMapping("/verify-section-content")
    @ConsumePoints(value = 3, bizType = "VERIFY")
    public ResponseVO<VerificationReport> verifySectionContent(@RequestParam Integer sectionId) {
        log.info("=== 校验已有小节内容！小节ID：{}", sectionId);

        try {
            Section section = sectionService.selectById(sectionId);
            if (section == null || section.getContentUrl() == null
                    || section.getContentUrl().isBlank()) {
                return ResponseVO.fail(404, "小节内容为空或小节不存在");
            }
            String content = tools.readContentFromFile(section.getContentUrl());
            if (content == null || content.isBlank()) {
                return ResponseVO.fail(404, "小节内容文件为空");
            }

            VerificationReport report = verificationAgent.verify(content);
            log.info("小节内容校验完成: sectionId={}, truthScore={}", sectionId, report.getTruthScore());
            return ResponseVO.ok(report);
        } catch (Exception e) {
            log.error("校验小节内容失败", e);
            return ResponseVO.fail(500, "校验失败: " + e.getMessage());
        }
    }

    /**
     * 生成随堂练习题并自动执行答案真实性校验
     * <p>
     * 先生成题目，然后逐题联网验证答案是否正确。
     */
    @PostMapping("/generate-questions-verified")
    @TrackAiUsage(AiToolUsage.ToolType.IMMERSIVE_COURSE)
    @ConsumePoints(value = 5, bizType = "EXAM")
    public ResponseVO<VerifiedQuestionsResult> generateQuestionsVerified(
            @RequestParam Integer sectionId) {
        log.info("=== 生成题目+校验！小节ID：{}", sectionId);

        try {
            // 1. 生成题目
            List<Question> questions = courseGenerationService.generateQuestions(sectionId);

            // 2. 逐题校验答案
            List<QuestionVerificationResult> questionResults = new ArrayList<>();
            if (questions != null && !questions.isEmpty()) {
                for (Question q : questions) {
                    QuestionVerificationResult qr = verifyQuestion(q);
                    questionResults.add(qr);
                }
            }

            VerifiedQuestionsResult result = new VerifiedQuestionsResult();
            result.setQuestions(questions);
            result.setQuestionResults(questionResults);

            long correctCount = questionResults.stream()
                    .filter(r -> r.getVerdict() == ClaimVerdict.VERIFIED).count();
            log.info("题目校验完成: sectionId={}, total={}, verified={}",
                    sectionId, questionResults.size(), correctCount);

            return ResponseVO.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseVO.fail(404, e.getMessage());
        } catch (Exception e) {
            log.error("生成题目+校验失败", e);
            return ResponseVO.fail(500, "操作失败: " + e.getMessage());
        }
    }

    /**
     * 校验已有题目的答案正确性
     */
    @PostMapping("/verify-questions")
    @ConsumePoints(value = 3, bizType = "VERIFY")
    public ResponseVO<List<QuestionVerificationResult>> verifyQuestions(
            @RequestParam Integer sectionId) {
        log.info("=== 校验已有题目！小节ID：{}", sectionId);

        try {
            List<Question> questions = questionService.getBySectionId(sectionId);
            if (questions == null || questions.isEmpty()) {
                return ResponseVO.fail(404, "未找到该小节的题目");
            }

            List<QuestionVerificationResult> results = new ArrayList<>();
            for (Question q : questions) {
                QuestionVerificationResult qr = verifyQuestion(q);
                results.add(qr);
            }
            return ResponseVO.ok(results);
        } catch (Exception e) {
            log.error("校验题目失败", e);
            return ResponseVO.fail(500, "校验失败: " + e.getMessage());
        }
    }

    /**
     * 单题校验：构造搜索查询 → 联网查证 → 文本比对判定
     */
    private QuestionVerificationResult verifyQuestion(Question q) {
        // 构造查询："知识点 + 题目核心 + 答案"
        String searchQuery = (q.getKnowledgePoint() != null ? q.getKnowledgePoint() + " " : "")
                + q.getContent() + " 答案";
        // 限制长度
        if (searchQuery.length() > 100) {
            searchQuery = searchQuery.substring(0, 100);
        }

        VerificationClaim claim = VerificationClaim.builder()
                .claimId("Q" + q.getId())
                .originalText("题目: " + q.getContent() + " | 答案: " + q.getAnswer()
                        + (q.getAnalysis() != null ? " | 解析: " + q.getAnalysis() : ""))
                .searchQuery(searchQuery)
                .build();

        // 复用联网查证管线
        VerificationClaim result = verificationAgent.checkSingleClaim(claim);

        QuestionVerificationResult qr = new QuestionVerificationResult();
        qr.setQuestionId(q.getId());
        qr.setQuestionContent(q.getContent());
        qr.setClaimedAnswer(q.getAnswer());
        qr.setVerdict(result.getVerdict() != null ? result.getVerdict() : ClaimVerdict.SUSPICIOUS);
        qr.setReason(result.getReason());
        qr.setEvidences(result.getEvidences());
        return qr;
    }



    // ======================== 内部结果类 ========================

    @Data
    public static class VerifiedSectionResult {
        private Section section;
        private VerificationReport verificationReport;
    }

    @Data
    public static class VerifiedQuestionsResult {
        private List<Question> questions;
        private List<QuestionVerificationResult> questionResults;
    }

    @Data
    public static class QuestionVerificationResult {
        private Integer questionId;
        private String questionContent;
        private String claimedAnswer;
        private ClaimVerdict verdict;
        private String reason;
        private List<VerificationEvidence> evidences;
    }
}
