package com.project.platform.service;

import com.project.platform.agents.entry.GenerateOutlineForm;
import com.project.platform.dto.ChapterOutlineDTO;
import com.project.platform.dto.CourseDTO;
import com.project.platform.entity.Chapter;
import com.project.platform.entity.Question;
import com.project.platform.entity.Section;

import java.io.IOException;
import java.util.List;

/**
 * 课程生成服务接口
 */
public interface CourseGenerationService {

    /**
     * 从自然语言中提取结构化参数
     */
    GenerateOutlineForm extractParameters(String message);

    /**
     * 根据提取的参数生成课程大纲
     */
    List<ChapterOutlineDTO> generateOutline(GenerateOutlineForm form);

    /**
     * 根据大纲创建章节和小节
     */
    List<Chapter> createChaptersAndSections(Integer courseId, List<ChapterOutlineDTO> outlines);

    /**
     * 智能生成完整课程（三步流程）
     */
    CourseDTO generateCourseSmart(String message, Integer userId);

    /**
     * 生成单个小节的详细内容并持久化到数据库
     */
    Section generateSectionContent(Integer chapterId, Integer sectionId);

    /**
     * 批量生成某个章节下所有小节的内容
     */
    List<Section> generateChapterContent(Integer chapterId);

    /**
     * 生成小节的随堂练习题
     * 根据小节名称和小节内容,使用AI生成练习题并保存到数据库
     */
    List<Question> generateQuestions(Integer sectionId);

    /**
     * 根据课程ID和知识点生成考试题
     * @param courseId 课程ID
     * @param knowledgePoints 知识点列表(逗号分隔)
     * @param questionTypes 题型要求(可选),格式: "CHOICE:5,MULTIPLE:2,JUDGE:2,FILL:1"
     * @return 生成的题目列表
     */
    List<Question> generateExamQuestions(Integer courseId, String knowledgePoints, String questionTypes);

    void updateChapterImages(Integer chapterId) throws IOException;
}
