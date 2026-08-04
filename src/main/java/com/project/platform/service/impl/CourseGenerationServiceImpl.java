package com.project.platform.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.platform.agents.ai.AiCourseHelperService;
import com.project.platform.agents.config.utils.Tools;
import com.project.platform.agents.entry.GenerateOutlineForm;
import com.project.platform.agents.tools.ImageSearchTool;
import com.project.platform.agents.tools.ImageSearchTool2;
import com.project.platform.dto.ChapterOutlineDTO;
import com.project.platform.dto.CourseDTO;
import com.project.platform.entity.Chapter;
import com.project.platform.entity.Course;
import com.project.platform.entity.Question;
import com.project.platform.entity.Section;
import com.project.platform.mapper.ChapterMapper;
import com.project.platform.mapper.SectionMapper;
import com.project.platform.service.ChapterService;
import com.project.platform.service.CourseGenerationService;
import com.project.platform.service.CourseService;
import com.project.platform.service.QuestionService;
import com.project.platform.service.SectionService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;

/**
 * 课程生成服务实现
 */
@Service
@Slf4j
public class CourseGenerationServiceImpl implements CourseGenerationService {

    @Resource
    private AiCourseHelperService aiCourseHelperService;

    @Resource(name = "directAiCourseHelperService")
    private AiCourseHelperService directAiCourseHelperService;

    @Resource
    private CourseService courseService;

    @Resource
    private ChapterService chapterService;

    @Resource
    private SectionService sectionService;

    @Resource
    private SectionMapper sectionMapper;

    @Resource
    private QuestionService questionService;

    @Resource
    private Tools tools;

    @Value("${files.uploads.path:uploads}")
    private String uploadsPath;

    private String system_prompt_chapter;
    private String system_prompt_sections;
    private String system_prompt_content;
    private String system_prompt_question;
    private String system_prompt_exam;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() throws Exception {
        system_prompt_chapter = read("system-prompt-chapter.txt");
        system_prompt_sections = read("system-prompt-chapter-sections.txt");
        system_prompt_content = read("system-prompt-chapter-content.txt");
        system_prompt_question = read("system-prompt-question.txt");
        system_prompt_exam = read("system-prompt-exam.txt");
    }

    private String read(String path) throws Exception {
        org.springframework.core.io.ClassPathResource resource = new org.springframework.core.io.ClassPathResource(path);
        return new String(resource.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public GenerateOutlineForm extractParameters(String message) {
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
            // 清理可能的代码块标记，确保解析的是纯JSON
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
            // 兜底：尝试直接从描述中猜测主题
            GenerateOutlineForm fallback = new GenerateOutlineForm();
            fallback.setTopic(message.replaceAll("我想学|我要学|学习", "").trim());
            fallback.setLevel("beginner");
            fallback.setDuration("30天");
            fallback.setStyle("project-based");
            return fallback;
        }
    }

    @Override
    public List<ChapterOutlineDTO> generateOutline(GenerateOutlineForm form) {
        String prompt = String.format(
                system_prompt_sections,
                form.getTopic(),
                form.getLevel(),
                form.getDuration(),
                form.getStyle()
        );

        log.info("=== 生成课程大纲！主题：{}, 难度：{}", form.getTopic(), form.getLevel());
        String response = aiCourseHelperService.GenerateCourse(prompt);
        // 将AI返回的JSON字符串解析为GenerateContentForm对象
        try {
            // 清理可能的代码块标记
            String cleanJson = response.replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();
            // 解析为列表
            return objectMapper.readValue(cleanJson,
                    new TypeReference<List<ChapterOutlineDTO>>() {});
        } catch (Exception e) {
            log.error("解析课程大纲JSON失败", e);
            throw new RuntimeException("生成课程大纲失败: " + e.getMessage());
        }
    }

    @Override
    public List<Chapter> createChaptersAndSections(Integer courseId, List<ChapterOutlineDTO> outlines) {
        List<Chapter> chapters = new ArrayList<>();

        for (int i = 0; i < outlines.size(); i++) {
            ChapterOutlineDTO outline = outlines.get(i);

            // 创建章节
            Chapter chapter1 = new Chapter();
            chapter1.setCourseId(courseId);
            chapter1.setChapterName(outline.getChapter());
            chapter1.setSortOrder(i + 1);
            chapter1.setCreateTime(LocalDateTime.now());
            chapterService.insert(chapter1);
            Chapter chapter = chapterService.selectByCourseIdAndChapterName(chapter1.getCourseId(),chapter1.getChapterName());

            log.info("=== 创建章节：{} ", outline.getChapter());

            // 创建小节
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

    @Override
    public CourseDTO generateCourseSmart(String message, Integer userId) {
        log.info("=== 开始生成完整课程！用户输入：{}", message);

        // 步骤1：从自然语言中提取参数
        GenerateOutlineForm form = extractParameters(message);
        log.info("=== 参数提取结果：主题={}, 难度={}, 周期={}, 风格={}, 课程描述={}",
                form.getTopic(), form.getLevel(), form.getDuration(), form.getStyle(), form.getDescription());

        // 步骤2：生成课程大纲并创建章节和小节
        List<ChapterOutlineDTO> outlines = generateOutline(form);
        Integer chapterCount = outlines.size();
        log.info("=== 课程大纲生成成功！章节数量：{}", outlines.size());

        // 步骤3：创建课程记录
        Course course1 = new Course(form.getTopic(), form.getDescription(), chapterCount, userId, LocalDateTime.now());
        courseService.insert(course1);
        Course course = courseService.listByCourseName(course1.getCourseName());
        log.info("=== 课程创建成功！课程ID：{}, 课程名称：{}", course.getId(), course.getCourseName());

        // 步骤4：将大纲转换为数据库记录（章节+小节）
        List<Chapter> chapters = createChaptersAndSections(course.getId(), outlines);
        log.info("=== 章节和小节创建成功！章节数量：{}", chapters.size());

        Integer firstChapterId = chapterService.listByCourseId(course.getId()).get(0).getId();
        Integer firstSectionId = sectionService.listByChapterId(chapters.get(0).getId()).get(0).getId();

        // 更新课程的章节数量
        course.setChapterCount(chapters.size());
        courseService.updateById(course);

        return new CourseDTO(form.getTopic(), firstChapterId, firstSectionId, form.getDescription(), outlines);
    }

    @Override
    public Section generateSectionContent(Integer chapterId, Integer sectionId) {
        Section section = sectionService.selectById(sectionId);
        Chapter chapter = chapterService.selectById(chapterId);
        if (section == null) {
            throw new IllegalArgumentException("小节不存在，ID: " + sectionId);
        }

        if (!section.getChapterId().equals(chapterId)) {
            throw new IllegalArgumentException("小节不属于该章节");
        }

        log.info("=== 开始生成小节内容！章节ID：{}, 小节ID：{}, 小节名称：{}",
                chapterId, sectionId, section.getSectionName());

        try {
            // 构建Prompt，让AI自动调用图片搜索和视频搜索工具
            String prompt = system_prompt_content
                    .replaceFirst("%s", chapter.getChapterName())
                    .replaceFirst("%s", section.getSectionName());

            // 调用AI生成内容（AI会自动调用 imageSearch 和 bilibiliVideoSearch 工具）
            String response = aiCourseHelperService.GenerateCourse(prompt);

            Section generatedSection = tools.parseAndMapSection(response, section, section.getSortOrder());
            String content = tools.cleanAiResponse(response);
            String fileName = tools.saveContentToFile(content);
            String imagUrl = tools.saveImageUrl(generatedSection.getImagUrl());

            generatedSection.setImagUrl(imagUrl);
            generatedSection.setContentUrl(fileName);
            generatedSection.setChapterId(chapterId);
            // imageUrl 和 videoUrl 都由AI从工具调用结果中填入

            sectionService.updateById(generatedSection);
            log.info("=== 小节内容已生成并保存：{}, imageUrl={}, videoUrl={}",
                    generatedSection.getSectionName(), generatedSection.getImagUrl(), generatedSection.getVideoUrl());
            return generatedSection;

        } catch (Exception e) {
            log.error("生成小节内容失败：{}", section.getSectionName(), e);
            throw new RuntimeException("生成小节内容失败", e);
        }
    }

    @Override
    public List<Section> generateChapterContent(Integer chapterId) {
        Chapter chapter = chapterService.selectById(chapterId);
        if (chapter == null) {
            throw new IllegalArgumentException("章节不存在，ID: " + chapterId);
        }

        List<Section> sections = chapter.getSectionList();
        log.info("=== 批量生成章节内容！章节ID：{}, 小节数量：{}", chapterId, sections.size());
        if (sections == null || sections.isEmpty()) {
            throw new IllegalArgumentException("该章节下没有小节");
        }

        log.info("=== 开始批量生成章节内容！章节ID：{}, 小节数量：{}", chapterId, sections.size());

        List<Section> updatedSections = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            try {
                // 构建Prompt，让AI自动调用图片搜索和视频搜索工具
                String prompt = system_prompt_content
                        .replaceFirst("%s", chapter.getChapterName())
                        .replaceFirst("%s", section.getSectionName());

                // 调用AI生成内容（AI会自动调用 imageSearch 和 bilibiliVideoSearch 工具）
                String response = aiCourseHelperService.GenerateCourse(prompt);
                log.info("=======response=======:{}", response);

                Section generatedSection = tools.parseAndMapSection(response, section, i + 1);
                String content = tools.cleanAiResponse(response);
                String fileName = tools.saveContentToFile(content);

                generatedSection.setContentUrl(fileName);
                generatedSection.setChapterId(chapterId);
                // imageUrl 和 videoUrl 都由AI从工具调用结果中填入

                sectionService.updateById(generatedSection);

                updatedSections.add(generatedSection);
                log.info("=== 小节内容已生成并保存：{}, imageUrl={}, videoUrl={}",
                        generatedSection.getSectionName(), generatedSection.getImagUrl(), generatedSection.getVideoUrl());

            } catch (Exception e) {
                log.error("生成小节内容失败：{}", section.getSectionName(), e);
            }
        }

        return updatedSections;
    }

    @Override
    public List<Question> generateQuestions(Integer sectionId) {
        Section section = sectionService.selectById(sectionId);
        Chapter chapter = chapterService.selectById(section.getChapterId());
        if (section == null) {
            throw new IllegalArgumentException("小节不存在，ID: " + sectionId);
        }

        log.info("=== 开始生成随堂练习题！小节ID：{}, 小节名称：{}", sectionId, section.getSectionName());

        try {
            String contentUrl = section.getContentUrl();
            String sectionContent = tools.readContentFromFile(contentUrl);
            // 如果小节内容为空,使用默认提示
            if (sectionContent == null || sectionContent.isEmpty()) {
                sectionContent = "暂无详细内容,请根据小节名称生成相关知识点的基础练习题";
            }

            // 构建Prompt - 使用replace替代String.format,避免模板中的%被误解析
            // 使用Matcher.quoteReplacement避免内容中的$被误解析为正则捕获组引用
            String prompt = system_prompt_question
                    .replaceFirst("%s", Matcher.quoteReplacement(section.getSectionName()))
                    .replaceFirst("%s", Matcher.quoteReplacement(sectionContent));
            log.info("=== 构建Prompt完成");

            // 调用AI生成题目
            String response = aiCourseHelperService.GenerateCourse(prompt);
            log.info("=======AI返回的题目JSON=======:\n{}", response);

            // 清理可能的代码块标记
            String cleanJson = response.replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            JsonNode root = objectMapper.readTree(cleanJson);

            // 生成本次题目生成的批次号
            String generationBatchNo = "GEN_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);

            List<Question> questionList = new ArrayList<>();

            for (JsonNode node : root) {
                Question q = new Question();
                if (node.has("id")) {
                    q.setSortOrder(node.get("id").asInt());
                }
                if (node.has("content")) {
                    q.setContent(node.get("content").asText());
                }
                if (node.has("type")) {
                    q.setQuestionType(node.get("type").asText());
                } else if (node.has("questionType")) {
                    q.setQuestionType(node.get("questionType").asText());
                }
                if (node.has("answer")) {
                    // 处理多选题的数组格式答案
                    JsonNode answerNode = node.get("answer");
                    if (answerNode.isArray()) {
                        // 多选题：将数组转换为逗号分隔的字符串
                        StringBuilder answerBuilder = new StringBuilder();
                        for (int i = 0; i < answerNode.size(); i++) {
                            if (i > 0) {
                                answerBuilder.append(",");
                            }
                            answerBuilder.append(answerNode.get(i).asText());
                        }
                        q.setAnswer(answerBuilder.toString());
                    } else {
                        q.setAnswer(answerNode.asText());
                    }
                }
                if (node.has("analysis")) {
                    q.setAnalysis(node.get("analysis").asText());
                }
                if (node.has("knowledgePoint")) {
                    q.setKnowledgePoint(node.get("knowledgePoint").asText());
                }
                if (node.has("options") && node.get("options").isArray()) {
                    q.setOptions(objectMapper.writeValueAsString(node.get("options")));
                } else {
                    q.setOptions("[]");
                }
                q.setResult("UNANSWERED");
                q.setSectionId(sectionId);
                q.setCourseId(chapter.getCourseId());
                // 设置题目类别为小节练习
                q.setQuestionCategory("SECTION_PRACTICE");
                q.setTestBatchNo(generationBatchNo);
                q.setCreateTime(LocalDateTime.now());
                questionList.add(q);
            }

            // 保存到数据库
            List<Question> savedQuestions = questionService.saveOrUpdateQuestions(questionList);
            log.info("=== 随堂练习题已生成并保存，小节ID：{}，题目数量：{}", sectionId, savedQuestions.size());

            return savedQuestions;

        } catch (Exception e) {
            log.error("生成随堂练习题失败：{}", section.getSectionName(), e);
            throw new RuntimeException("生成题目失败：" + e.getMessage(), e);
        }
    }

    @Override
    public List<Question> generateExamQuestions(Integer courseId, String knowledgePoints, String questionTypes) {
        log.info("=== 开始生成考试题！课程ID：{}, 知识点：{}, 题型要求：{}",
                courseId, knowledgePoints, questionTypes);

        try {
            // 根据课程ID获取课程信息
            Course course = courseService.selectById(courseId);
            if (course == null) {
                throw new IllegalArgumentException("课程不存在，ID: " + courseId);
            }
            String courseName = course.getCourseName();
            log.info("=== 课程名称：{}", courseName);

            // 如果知识点为空，使用默认提示
            if (knowledgePoints == null || knowledgePoints.isEmpty()) {
                knowledgePoints = "请根据课程名称自动生成核心知识点的考试题目";
            }

            // 如果题型要求为空，使用空字符串（AI会按默认分布生成）
            if (questionTypes == null || questionTypes.isEmpty()) {
                questionTypes = "";
            }

            // 构建Prompt - 使用replace替代String.format，避免模板中的%被误解析
            // 使用Matcher.quoteReplacement避免内容中的$被误解析为正则捕获组引用
            String prompt = system_prompt_exam
                    .replaceFirst("%s", Matcher.quoteReplacement(courseName))
                    .replaceFirst("%s", Matcher.quoteReplacement(knowledgePoints))
                    .replaceFirst("%s", Matcher.quoteReplacement(questionTypes));
            log.info("=== 构建Prompt完成");

            // 调用AI生成题目
            String response = directAiCourseHelperService.GenerateCourse(prompt);
            log.info("=======AI返回的题目JSON=======:\n{}", response);

            // 清理可能的代码块标记
            String cleanJson = response.replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            JsonNode root = objectMapper.readTree(cleanJson);

            // 生成本次题目生成的批次号
            String generationBatchNo = "GEN_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);

            List<Question> questionList = new ArrayList<>();

            for (JsonNode node : root) {
                Question q = new Question();
                if (node.has("id")) {
                    q.setSortOrder(node.get("id").asInt());
                }
                if (node.has("content")) {
                    q.setContent(node.get("content").asText());
                }
                if (node.has("type")) {
                    q.setQuestionType(node.get("type").asText());
                } else if (node.has("questionType")) {
                    q.setQuestionType(node.get("questionType").asText());
                }
                if (node.has("answer")) {
                    // 处理多选题的数组格式答案
                    JsonNode answerNode = node.get("answer");
                    if (answerNode.isArray()) {
                        // 多选题：将数组转换为逗号分隔的字符串
                        StringBuilder answerBuilder = new StringBuilder();
                        for (int i = 0; i < answerNode.size(); i++) {
                            if (i > 0) {
                                answerBuilder.append(",");
                            }
                            answerBuilder.append(answerNode.get(i).asText());
                        }
                        q.setAnswer(answerBuilder.toString());
                    } else {
                        q.setAnswer(answerNode.asText());
                    }
                }
                if (node.has("analysis")) {
                    q.setAnalysis(node.get("analysis").asText());
                }
                if (node.has("knowledgePoint")) {
                    q.setKnowledgePoint(node.get("knowledgePoint").asText());
                }
                if (node.has("options") && node.get("options").isArray()) {
                    q.setOptions(objectMapper.writeValueAsString(node.get("options")));
                } else {
                    q.setOptions("[]");
                }
                q.setResult("UNANSWERED");
                // 设置sectionId为0，表示课程级别的考试题（不关联具体小节）
                q.setSectionId(0);
                // 设置课程ID
                q.setCourseId(course.getId());
                // 设置题目类别为课程考试
                q.setQuestionCategory("COURSE_EXAM");
                q.setTestBatchNo(generationBatchNo);
                q.setCreateTime(LocalDateTime.now());
                questionList.add(q);
            }

            log.info("=== 考试题已生成，题目数量：{}", questionList.size());
            List<Question> savedQuestions = questionService.saveOrUpdateQuestions(questionList);

            return savedQuestions;

        } catch (Exception e) {
            log.error("生成考试题失败", e);
            throw new RuntimeException("生成考试题失败：" + e.getMessage(), e);
        }
    }

    @Override
    public void updateChapterImages(Integer chapterId) throws IOException {
        List<Section> sections = sectionService.listByChapterId(chapterId);
        if(sections.isEmpty())
        {
            return;
        }
        for(Section section : sections)
        {
            if(section == null)
            {
                continue;
            }
            if(section.getContentUrl().isEmpty() || section.getContentUrl() == null)
            {
                continue;
            }
            String imageKeyword = tools.extractImageKeyword(section.getSectionName());
            List<String> strings = new ImageSearchTool2().searchImage(imageKeyword);
            String imagUrl = tools.saveImageUrl(strings.get(0));
            Section sectionDTO = new Section();
            sectionDTO.setId(section.getId());
            sectionDTO.setImagUrl(imagUrl);
            String contentUrl = section.getContentUrl();
            String content = tools.readContentFromFile(contentUrl);
            String updateContent = tools.updateBaiduImagesInMarkdown(content, imagUrl);
            String fileUrl = tools.saveContentToFile(updateContent);
            sectionDTO.setContentUrl(fileUrl);
            sectionService.updateById(sectionDTO);
        }
    }
}
