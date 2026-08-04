package com.project.platform.service.impl;

import com.project.platform.agents.tools.ImageSearchTool2;
import com.project.platform.agents.tools.KnowledgeSearchTool;
import com.project.platform.entity.Chapter;
import com.project.platform.entity.Course;
import com.project.platform.entity.ResourcePush;
import com.project.platform.entity.Section;
import com.project.platform.mapper.ChapterMapper;
import com.project.platform.mapper.CourseMapper;
import com.project.platform.mapper.ResourcePushMapper;
import com.project.platform.mapper.SectionMapper;
import com.project.platform.service.LearningWorkspaceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResourcePushServiceImplTest {

    @Test
    @Timeout(10)
    void returnsExistingCourseResourcesWithoutSearchingWhenDatabaseHasData() {
        ResourcePushMapper resourcePushMapper = mock(ResourcePushMapper.class);
        KnowledgeSearchTool knowledgeSearchTool = mock(KnowledgeSearchTool.class);
        ImageSearchTool2 imageSearchTool2 = mock(ImageSearchTool2.class);
        ResourcePushServiceImpl service = new ResourcePushServiceImpl();

        ReflectionTestUtils.setField(service, "resourcePushMapper", resourcePushMapper);
        ReflectionTestUtils.setField(service, "knowledgeSearchTool", knowledgeSearchTool);
        ReflectionTestUtils.setField(service, "imageSearchTool2", imageSearchTool2);

        ResourcePush existing = new ResourcePush();
        existing.setId(100);
        existing.setCourseId(1);
        existing.setTitle("cached resource");
        List<ResourcePush> existingResources = List.of(existing);
        when(resourcePushMapper.selectByCourseId(1)).thenReturn(existingResources);

        List<ResourcePush> result = service.searchAndPush(1, 2, 3, "java");

        assertSame(existingResources, result);
        verify(resourcePushMapper).selectByCourseId(1);
        verify(resourcePushMapper, never()).countByCourseIdAndUrl(any(), any());
        verify(resourcePushMapper, never()).insert(any(ResourcePush.class));
        verifyNoInteractions(knowledgeSearchTool, imageSearchTool2);
    }

    @Test
    void buildsContextualSearchKeywordFromCourseHierarchyAndKnowledgePoint() {
        CourseMapper courseMapper = mock(CourseMapper.class);
        ChapterMapper chapterMapper = mock(ChapterMapper.class);
        SectionMapper sectionMapper = mock(SectionMapper.class);
        ResourcePushServiceImpl service = new ResourcePushServiceImpl();

        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "chapterMapper", chapterMapper);
        ReflectionTestUtils.setField(service, "sectionMapper", sectionMapper);

        Course course = new Course();
        course.setCourseName("算法");
        Chapter chapter = new Chapter();
        chapter.setChapterName("递推");
        Section section = new Section();
        section.setSectionName("动态规划入门");

        when(courseMapper.selectById(1)).thenReturn(course);
        when(chapterMapper.selectById(2)).thenReturn(chapter);
        when(sectionMapper.selectById(3)).thenReturn(section);

        String keyword = service.buildContextualSearchKeyword(1, 2, 3, "状态转移");

        assertEquals("算法 递推 动态规划入门 状态转移", keyword);
    }

    @Test
    void ranksAndDeduplicatesContextualResourcesWithMetadata() {
        ResourcePushMapper resourcePushMapper = mock(ResourcePushMapper.class);
        LearningWorkspaceService learningWorkspaceService = mock(LearningWorkspaceService.class);
        ResourcePushServiceImpl service = new ResourcePushServiceImpl();

        ReflectionTestUtils.setField(service, "resourcePushMapper", resourcePushMapper);
        ReflectionTestUtils.setField(service, "learningWorkspaceService", learningWorkspaceService);

        ResourcePush weakMatch = resource("same-url", "状态转移专项练习", 1, 2, 3);
        ResourcePush duplicateWeak = resource("same-url", "通用课程资料", 1, null, null);
        ResourcePush activeMatch = resource("active-url", "动态规划入门视频", 1, 2, 3);

        when(resourcePushMapper.selectByCourseId(1)).thenReturn(List.of(duplicateWeak, activeMatch, weakMatch));
        when(learningWorkspaceService.listWeakKnowledgePoints(99, 3)).thenReturn(List.of("状态转移"));
        when(learningWorkspaceService.resolveResourceLearningStatus(eq(99), any(ResourcePush.class)))
                .thenReturn("NOT_STARTED");

        List<ResourcePush> result = service.listContextualResources(99, 1, 2, 3, "动态规划");

        assertEquals(2, result.size());
        assertSame(weakMatch, result.get(0));
        assertNotNull(result.get(0).getContextMetadata());
        assertEquals("状态转移", result.get(0).getContextMetadata().getKnowledgePoint());
        assertEquals("REMEDIAL", result.get(0).getContextMetadata().getPurpose());
        assertSame(activeMatch, result.get(1));
    }

    private static ResourcePush resource(String url, String title, Integer courseId, Integer chapterId, Integer sectionId) {
        ResourcePush resource = new ResourcePush();
        resource.setUrl(url);
        resource.setTitle(title);
        resource.setCourseId(courseId);
        resource.setChapterId(chapterId);
        resource.setSectionId(sectionId);
        return resource;
    }
}
