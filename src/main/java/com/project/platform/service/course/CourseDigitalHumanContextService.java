package com.project.platform.service.course;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.platform.agents.config.utils.Tools;
import com.project.platform.dto.CourseDigitalHumanChatRequest;
import com.project.platform.dto.CourseDigitalHumanChatType;
import com.project.platform.entity.Chapter;
import com.project.platform.entity.Course;
import com.project.platform.entity.DigitalHuman;
import com.project.platform.entity.Question;
import com.project.platform.entity.Section;
import com.project.platform.mapper.ChapterMapper;
import com.project.platform.mapper.CourseMapper;
import com.project.platform.mapper.DigitalHumanMapper;
import com.project.platform.mapper.QuestionMapper;
import com.project.platform.mapper.SectionMapper;
import lombok.Value;
import org.springframework.stereotype.Service;

@Service
public class CourseDigitalHumanContextService {

    static final int MAX_LECTURE_LENGTH = 20_000;
    static final int MAX_INPUT_LENGTH = 8_000;

    private final CourseMapper courseMapper;
    private final ChapterMapper chapterMapper;
    private final SectionMapper sectionMapper;
    private final DigitalHumanMapper digitalHumanMapper;
    private final QuestionMapper questionMapper;
    private final Tools tools;

    public CourseDigitalHumanContextService(CourseMapper courseMapper,
                                            ChapterMapper chapterMapper,
                                            SectionMapper sectionMapper,
                                            DigitalHumanMapper digitalHumanMapper,
                                            QuestionMapper questionMapper,
                                            Tools tools) {
        this.courseMapper = courseMapper;
        this.chapterMapper = chapterMapper;
        this.sectionMapper = sectionMapper;
        this.digitalHumanMapper = digitalHumanMapper;
        this.questionMapper = questionMapper;
        this.tools = tools;
    }

    public Context build(CourseDigitalHumanChatRequest request) {
        require(request != null, "请求体不能为空");
        require(request.getCourseId() != null, "courseId 不能为空");
        require(request.getChapterId() != null, "chapterId 不能为空");
        require(request.getSectionId() != null, "sectionId 不能为空");
        require(hasText(request.getDigitalHumanId()), "digitalHumanId 不能为空");
        require(request.getType() != null, "type 不能为空");

        Course course = courseMapper.selectById(request.getCourseId());
        require(course != null, "课程不存在");
        require(course.getCreatorId() == null
                || course.getCreatorId().equals(tools.getCurrentUserIdInteger()), "无权访问该课程");
        Chapter chapter = chapterMapper.selectById(request.getChapterId());
        require(chapter != null, "章节不存在");
        require(request.getCourseId().equals(chapter.getCourseId()), "章节不属于指定课程");
        Section section = sectionMapper.selectById(request.getSectionId());
        require(section != null, "小节不存在");
        require(request.getChapterId().equals(section.getChapterId()), "小节不属于指定章节");
        DigitalHuman digitalHuman = digitalHumanMapper.selectByDigitalHumanId(request.getDigitalHumanId());
        require(digitalHuman != null, "数字人不存在");
        require(digitalHuman.getStatus() == null || "启用".equals(digitalHuman.getStatus())
                || "ENABLED".equalsIgnoreCase(digitalHuman.getStatus()), "数字人未启用");

        validateTypePayload(request);
        String lecture = limit(tools.readContentFromFile(section.getContentUrl()), MAX_LECTURE_LENGTH);
        String practice = resolvePractice(request, section);
        return new Context(course, chapter, section, digitalHuman, lecture,
                buildPrompt(request, course, chapter, section, digitalHuman, lecture, practice));
    }

    private void validateTypePayload(CourseDigitalHumanChatRequest request) {
        if (request.getType() == CourseDigitalHumanChatType.FREE_QA) {
            require(hasText(request.getMessage()), "FREE_QA 的 message 不能为空");
        } else if (request.getType() == CourseDigitalHumanChatType.SELECTION_EXPLAIN) {
            require(present(request.getSelection()), "SELECTION_EXPLAIN 的 selection 不能为空");
        } else if (request.getType() == CourseDigitalHumanChatType.PRACTICE_EXPLAIN) {
            require(present(request.getPractice()), "PRACTICE_EXPLAIN 的 practice 不能为空");
        }
    }

    private String resolvePractice(CourseDigitalHumanChatRequest request, Section section) {
        if (request.getType() != CourseDigitalHumanChatType.PRACTICE_EXPLAIN) {
            return json(request.getPractice());
        }
        JsonNode practice = request.getPractice();
        JsonNode questionIdNode = practice == null ? null : practice.get("questionId");
        require(questionIdNode != null && questionIdNode.canConvertToInt(), "practice.questionId 不能为空");
        Question question = questionMapper.selectById(questionIdNode.intValue());
        require(question != null, "题目不存在");
        require(section.getId().equals(question.getSectionId()), "题目不属于当前小节");
        String studentAnswer = practice.hasNonNull("studentAnswer")
                ? practice.get("studentAnswer").asText() : "（未作答）";
        return """
                题目ID：%s
                题型：%s
                题干：%s
                选项：%s
                学生答案：%s
                正确答案：%s
                解析：%s
                知识点：%s
                """.formatted(question.getId(), safe(question.getQuestionType()), safe(question.getContent()),
                safe(question.getOptions()), limit(studentAnswer, MAX_INPUT_LENGTH), safe(question.getAnswer()),
                safe(question.getAnalysis()), safe(question.getKnowledgePoint()));
    }

    private String buildPrompt(CourseDigitalHumanChatRequest request, Course course, Chapter chapter,
                               Section section, DigitalHuman digitalHuman, String lecture, String practice) {
        String task = switch (request.getType()) {
            case FREE_QA -> "回答学生问题";
            case SECTION_LECTURE -> "按教学逻辑讲解当前小节";
            case SELECTION_EXPLAIN -> "解释学生选中的内容，并联系讲义上下文";
            case PRACTICE_EXPLAIN -> "讲解练习，分析思路、答案与易错点";
            case VIDEO_SUMMARY -> "总结当前小节视频或学生提供的视频信息";
        };
        return """
                【教师角色】%s（%s）
                【任务】%s
                【课程】%s
                【章节】%s
                【小节】%s
                【讲义】\n%s
                【学生消息】%s
                【选中内容】%s
                【练习信息】%s
                请优先依据以上上下文回答，不要声称看过未提供的视频内容。
                """.formatted(
                safe(digitalHuman.getName()), safe(digitalHuman.getDescription()), task,
                safe(course.getCourseName()), safe(chapter.getChapterName()), safe(section.getSectionName()),
                hasText(lecture) ? lecture : "（当前小节暂无可读取讲义）",
                limit(safe(request.getMessage()), MAX_INPUT_LENGTH),
                limit(json(request.getSelection()), MAX_INPUT_LENGTH),
                limit(practice, MAX_INPUT_LENGTH));
    }

    private static String json(JsonNode value) {
        return present(value) ? value.toString() : "（无）";
    }

    private static boolean present(JsonNode value) {
        return value != null && !value.isNull() && !(value.isTextual() && value.asText().isBlank());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    static String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength) + "\n（内容已截断）";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    @Value
    public static class Context {
        Course course;
        Chapter chapter;
        Section section;
        DigitalHuman digitalHuman;
        String lecture;
        String prompt;
    }
}
