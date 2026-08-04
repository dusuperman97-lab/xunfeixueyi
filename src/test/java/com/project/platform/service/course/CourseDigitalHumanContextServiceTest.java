package com.project.platform.service.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.platform.agents.config.utils.Tools;
import com.project.platform.dto.CourseDigitalHumanChatRequest;
import com.project.platform.dto.CourseDigitalHumanChatType;
import com.project.platform.entity.Chapter;
import com.project.platform.entity.Course;
import com.project.platform.entity.DigitalHuman;
import com.project.platform.entity.Section;
import com.project.platform.mapper.ChapterMapper;
import com.project.platform.mapper.CourseMapper;
import com.project.platform.mapper.DigitalHumanMapper;
import com.project.platform.mapper.QuestionMapper;
import com.project.platform.mapper.SectionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseDigitalHumanContextServiceTest {

    @Mock CourseMapper courseMapper;
    @Mock ChapterMapper chapterMapper;
    @Mock SectionMapper sectionMapper;
    @Mock DigitalHumanMapper digitalHumanMapper;
    @Mock QuestionMapper questionMapper;
    @Mock Tools tools;

    private CourseDigitalHumanContextService service;
    private CourseDigitalHumanChatRequest request;

    @BeforeEach
    void setUp() {
        service = new CourseDigitalHumanContextService(
                courseMapper, chapterMapper, sectionMapper, digitalHumanMapper, questionMapper, tools);
        request = new CourseDigitalHumanChatRequest();
        request.setCourseId(1);
        request.setChapterId(2);
        request.setSectionId(3);
        request.setDigitalHumanId("teacher-1");
        request.setType(CourseDigitalHumanChatType.FREE_QA);
        request.setMessage("请解释这一节");
    }

    @Test
    void rejectsChapterOutsideCourse() {
        Course course = new Course();
        course.setId(1);
        Chapter chapter = new Chapter();
        chapter.setId(2);
        chapter.setCourseId(99);
        when(courseMapper.selectById(1)).thenReturn(course);
        when(chapterMapper.selectById(2)).thenReturn(chapter);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.build(request));

        assertEquals("章节不属于指定课程", error.getMessage());
    }

    @Test
    void validatesHierarchyAndTruncatesLecture() {
        Course course = new Course();
        course.setId(1);
        course.setCourseName("Java");
        Chapter chapter = new Chapter();
        chapter.setId(2);
        chapter.setCourseId(1);
        chapter.setChapterName("集合");
        Section section = new Section();
        section.setId(3);
        section.setChapterId(2);
        section.setSectionName("List");
        section.setContentUrl("section.md");
        DigitalHuman human = new DigitalHuman();
        human.setDigitalHumanId("teacher-1");
        human.setName("小弈");
        human.setStatus("启用");
        when(courseMapper.selectById(1)).thenReturn(course);
        when(chapterMapper.selectById(2)).thenReturn(chapter);
        when(sectionMapper.selectById(3)).thenReturn(section);
        when(digitalHumanMapper.selectByDigitalHumanId("teacher-1")).thenReturn(human);
        when(tools.readContentFromFile("section.md")).thenReturn("a".repeat(20_100));

        CourseDigitalHumanContextService.Context context = service.build(request);

        assertTrue(context.getLecture().endsWith("（内容已截断）"));
        assertTrue(context.getPrompt().contains("【课程】Java"));
        assertTrue(context.getPrompt().contains("【学生消息】请解释这一节"));
    }

    @Test
    void requiresSelectionForSelectionExplain() throws Exception {
        request.setType(CourseDigitalHumanChatType.SELECTION_EXPLAIN);
        Course course = new Course(); course.setId(1);
        Chapter chapter = new Chapter(); chapter.setId(2); chapter.setCourseId(1);
        Section section = new Section(); section.setId(3); section.setChapterId(2);
        DigitalHuman human = new DigitalHuman(); human.setDigitalHumanId("teacher-1"); human.setStatus("ENABLED");
        when(courseMapper.selectById(1)).thenReturn(course);
        when(chapterMapper.selectById(2)).thenReturn(chapter);
        when(sectionMapper.selectById(3)).thenReturn(section);
        when(digitalHumanMapper.selectByDigitalHumanId("teacher-1")).thenReturn(human);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.build(request));
        assertEquals("SELECTION_EXPLAIN 的 selection 不能为空", error.getMessage());

        request.setSelection(new ObjectMapper().readTree("{\"text\":\"ArrayList\"}"));
        when(tools.readContentFromFile(null)).thenReturn("");
        assertTrue(service.build(request).getPrompt().contains("ArrayList"));
    }
}
