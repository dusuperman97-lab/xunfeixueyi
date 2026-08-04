package com.project.platform.service.impl;

import com.project.platform.dto.CourseCompletionDTO;
import com.project.platform.dto.CourseQuestionStatisticsDTO;
import com.project.platform.entity.Chapter;
import com.project.platform.entity.Course;
import com.project.platform.entity.Question;
import com.project.platform.entity.Section;
import com.project.platform.exception.CustomException;
import com.project.platform.mapper.ChapterMapper;
import com.project.platform.mapper.CourseMapper;
import com.project.platform.mapper.QuestionMapper;
import com.project.platform.mapper.SectionMapper;
import com.project.platform.service.ChapterService;
import com.project.platform.service.CourseService;
import com.project.platform.service.QuestionService;
import com.project.platform.service.SectionService;
import com.project.platform.utils.CopyTools;
import com.project.platform.vo.PageVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 课程服务实现类
 */
@Service
public class CourseServiceImpl implements CourseService {

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private ChapterMapper chapterMapper;

    @Resource
    private SectionMapper sectionMapper;

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private ChapterService chapterService;

    @Resource
    private SectionService sectionService;

    @Resource
    private QuestionService questionService;

    @Override
    public PageVO<Course> page(Map<String, Object> query, Integer pageNum, Integer pageSize) {
        PageVO<Course> page = new PageVO<>();
        List<Course> list = courseMapper.queryPage((pageNum - 1) * pageSize, pageSize, query);
        page.setList(list);
        page.setTotal(courseMapper.queryCount(query));
        return page;
    }

    @Override
    public Course selectById(Integer id) {
        return courseMapper.selectById(id);
    }


    @Override
    public List<Course> list() {
        return courseMapper.list();
    }

    @Override
    public List<Course> listByCreator(Integer creatorId) {
        return courseMapper.listByCreator(creatorId);
    }

    @Override
    public Course listByCourseName(String CourseName) {
        return courseMapper.selectByCourseName(CourseName);
    }

    @Override
    public void insert(Course course) {
        check(course);
        if (course.getCreatorId() == null) {
            throw new CustomException("创建人不能为空");
        }
        courseMapper.insert(course);
    }

    @Override
    public void updateById(Course course) {
        check(course);
        // 不允许修改创建人
        Course existing = courseMapper.selectById(course.getId());
        if (existing != null) {
            course.setCreatorId(existing.getCreatorId());
        }
        courseMapper.updateById(course);
    }

    private void check(Course course) {
        Course existing = courseMapper.selectByCourseName(course.getCourseName());
        if (existing != null && !existing.getId().equals(course.getId())) {
            throw new CustomException("课程名称已存在");
        }
    }

    @Override
    public void removeByIds(List<Integer> ids) {
        courseMapper.removeByIds(ids);
    }

    @Override
    public CourseCompletionDTO getCourseCompletion(Integer courseId) {
        // 1. 获取课程信息
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new CustomException("课程不存在，ID: " + courseId);
        }

        // 2. 获取所有章节
        List<Chapter> chapters = chapterService.listByCourseId(courseId);
        int totalChapters = chapters.size();

        // 3. 批量查询所有小节（1次查询，替代逐章查询）
        int totalSections = 0;
        int completedSections = 0;
        //  在数据库中进行批量查询，不用for语法经行循环单一查询，将多次数据交互变成一次
        if (!chapters.isEmpty()) {
            List<Integer> chapterIds = chapters.stream()
                    .map(Chapter::getId)
                    .collect(Collectors.toList());
            List<Section> allSections = sectionMapper.listByChapterIds(chapterIds);
            totalSections = allSections.size();
            completedSections = (int) allSections.stream()
                    .filter(s -> s.getIsCompleted() != null && s.getIsCompleted())
                    .count();
        }

        // 4. 计算完成率
        double completionRate = totalSections > 0 ? (double) completedSections / totalSections * 100 : 0.0;
        completionRate = Math.round(completionRate * 100.0) / 100.0; // 保留两位小数

        // 5. 确定完成状态
        String completionStatus;
        if (completedSections == 0) {
            completionStatus = "NOT_STARTED";
        } else if (completedSections == totalSections) {
            completionStatus = "COMPLETED";
        } else {
            completionStatus = "IN_PROGRESS";
        }

        // 6. 构建返回对象
        CourseCompletionDTO dto = new CourseCompletionDTO();
        dto.setCourseId(course.getId());
        dto.setCourseName(course.getCourseName());
        dto.setDescription(course.getDescription());
        dto.setTotalChapters(totalChapters);
        dto.setTotalSections(totalSections);
        dto.setCompletedSections(completedSections);
        dto.setCompletionRate(completionRate);
        dto.setCompletionStatus(completionStatus);

        return dto;
    }

    @Override
    public List<CourseCompletionDTO> getUserCoursesCompletion(Integer userId) {
        // 1. 获取用户的所有课程（1次查询）
        List<Course> courses = courseMapper.listByCreator(userId);
        if (courses == null || courses.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 提取所有课程ID，批量查询所有章节（1次查询）
        List<Integer> courseIds = courses.stream()
                .map(Course::getId)
                .collect(Collectors.toList());
        List<Chapter> allChapters = chapterMapper.listByCourseIds(courseIds);

        // 3. 按课程ID分组章节
        Map<Integer, List<Chapter>> chaptersByCourse = allChapters.stream()
                .collect(Collectors.groupingBy(Chapter::getCourseId));

        // 4. 提取所有章节ID，批量查询所有小节（1次查询）
        List<Integer> chapterIds = allChapters.stream()
                .map(Chapter::getId)
                .collect(Collectors.toList());
        List<Section> allSections = chapterIds.isEmpty()
                ? new ArrayList<>()
                : sectionMapper.listByChapterIds(chapterIds);

        // 5. 按章节ID分组小节
        Map<Integer, List<Section>> sectionsByChapter = allSections.stream()
                .collect(Collectors.groupingBy(Section::getChapterId));

        // 6. 在内存中为每个课程聚合统计
        List<CourseCompletionDTO> completions = new ArrayList<>();
        for (Course course : courses) {
            try {
                List<Chapter> chapters = chaptersByCourse.getOrDefault(course.getId(), new ArrayList<>());
                int totalChapters = chapters.size();
                int totalSections = 0;
                int completedSections = 0;

                for (Chapter chapter : chapters) {
                    List<Section> sections = sectionsByChapter.getOrDefault(chapter.getId(), new ArrayList<>());
                    totalSections += sections.size();
                    for (Section section : sections) {
                        if (section.getIsCompleted() != null && section.getIsCompleted()) {
                            completedSections++;
                        }
                    }
                }

                double completionRate = totalSections > 0 ? (double) completedSections / totalSections * 100 : 0.0;
                completionRate = Math.round(completionRate * 100.0) / 100.0;

                String completionStatus;
                if (completedSections == 0) {
                    completionStatus = "NOT_STARTED";
                } else if (completedSections == totalSections) {
                    completionStatus = "COMPLETED";
                } else {
                    completionStatus = "IN_PROGRESS";
                }

                CourseCompletionDTO dto = new CourseCompletionDTO();
                dto.setCourseId(course.getId());
                dto.setCourseName(course.getCourseName());
                dto.setDescription(course.getDescription());
                dto.setTotalChapters(totalChapters);
                dto.setTotalSections(totalSections);
                dto.setCompletedSections(completedSections);
                dto.setCompletionRate(completionRate);
                dto.setCompletionStatus(completionStatus);
                completions.add(dto);
            } catch (Exception e) {
                // 如果某个课程统计失败，记录日志但继续处理其他课程
                e.printStackTrace();
            }
        }

        return completions;
    }

    @Override
    public CourseQuestionStatisticsDTO getCourseQuestionStatistics(Integer courseId) {
        // 1. 获取课程信息
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new CustomException("课程不存在，ID: " + courseId);
        }

        // 2. 获取所有章节
        List<Chapter> chapters = chapterService.listByCourseId(courseId);
        int totalChapters = chapters.size();

        // 3. 批量查询所有小节（1次查询）
        int totalSections = 0;
        List<Section> allSections = new ArrayList<>();
        if (!chapters.isEmpty()) {
            List<Integer> chapterIds = chapters.stream()
                    .map(Chapter::getId)
                    .collect(Collectors.toList());
            allSections = sectionMapper.listByChapterIds(chapterIds);
            totalSections = allSections.size();
        }

        // 4. 批量查询所有题目（1次查询）
        int totalQuestions = 0;
        int choiceQuestions = 0;
        int multipleQuestions = 0;
        int judgeQuestions = 0;
        int fillQuestions = 0;
        Set<String> knowledgePoints = new HashSet<>();

        if (!allSections.isEmpty()) {
            List<Integer> sectionIds = allSections.stream()
                    .map(Section::getId)
                    .collect(Collectors.toList());
            List<Question> allQuestions = questionMapper.listBySectionIds(sectionIds);
            if (allQuestions != null) {
                totalQuestions = allQuestions.size();
                for (Question question : allQuestions) {
                    String questionType = question.getQuestionType();
                    if (questionType != null) {
                        switch (questionType.toUpperCase()) {
                            case "CHOICE":
                                choiceQuestions++;
                                break;
                            case "MULTIPLE":
                                multipleQuestions++;
                                break;
                            case "JUDGE":
                                judgeQuestions++;
                                break;
                            case "FILL":
                                fillQuestions++;
                                break;
                        }
                    }
                    if (question.getKnowledgePoint() != null && !question.getKnowledgePoint().trim().isEmpty()) {
                        knowledgePoints.add(question.getKnowledgePoint());
                    }
                }
            }
        }

        // 5. 构建返回对象
        CourseQuestionStatisticsDTO dto = new CourseQuestionStatisticsDTO();
        dto.setCourseId(course.getId());
        dto.setCourseName(course.getCourseName());
        dto.setTotalChapters(totalChapters);
        dto.setTotalSections(totalSections);
        dto.setTotalQuestions(totalQuestions);
        dto.setChoiceQuestions(choiceQuestions);
        dto.setMultipleQuestions(multipleQuestions);
        dto.setJudgeQuestions(judgeQuestions);
        dto.setFillQuestions(fillQuestions);
        dto.setTotalKnowledgePoints(knowledgePoints.size());

        // 将知识点集合转换为数组
        if (!knowledgePoints.isEmpty()) {
            dto.setKnowledgePoints(knowledgePoints.toArray(new String[0]));
        } else {
            dto.setKnowledgePoints(new String[0]);
        }

        return dto;
    }

    @Override
    public List<Question> getCourseQuestions(Integer courseId) {
        // 1. 验证课程是否存在
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new CustomException("课程不存在，ID: " + courseId);
        }

        // 2. 获取所有章节
        List<Chapter> chapters = chapterService.listByCourseId(courseId);

        // 3. 批量查询所有小节，再批量查所有题目（共2次查询）
        if (chapters.isEmpty()) {
            return new ArrayList<>();
        }
        List<Integer> chapterIds = chapters.stream()
                .map(Chapter::getId)
                .collect(Collectors.toList());
        List<Section> allSections = sectionMapper.listByChapterIds(chapterIds);
        if (allSections.isEmpty()) {
            return new ArrayList<>();
        }
        List<Integer> sectionIds = allSections.stream()
                .map(Section::getId)
                .collect(Collectors.toList());
        return questionMapper.listBySectionIds(sectionIds);
    }

    // ========== 新增的分页方法 ==========
    @Override
    public PageVO<Question> getCourseQuestionsPage(Integer courseId, String questionType, Integer pageNum, Integer pageSize) {
        // 1. 复用你已有的方法，获取该课程下的所有题目
        List<Question> allQuestions = this.getCourseQuestions(courseId);

        // 2. 如果前端传了 questionType (题型)，则在内存中进行过滤
        if (questionType != null && !questionType.trim().isEmpty()) {
            List<Question> filteredQuestions = new ArrayList<>();
            for (Question question : allQuestions) {
                // 如果题型匹配，则保留
                if (questionType.equals(question.getQuestionType())) {
                    filteredQuestions.add(question);
                }
            }
            // 将过滤后的结果重新赋值给 allQuestions
            allQuestions = filteredQuestions;
        }

        // 3. 执行内存分页 (计算 fromIndex 和 toIndex)
        int total = allQuestions.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<Question> pagedList;
        // 如果请求的起始索引大于等于总记录数，说明页码超出了范围，返回空列表
        if (fromIndex >= total) {
            pagedList = new ArrayList<>();
        } else {
            // 截取当前页需要的数据
            pagedList = allQuestions.subList(fromIndex, toIndex);
        }

        // 4. 封装成 PageVO 统一返回
        PageVO<Question> pageVO = new PageVO<>();
        pageVO.setList(pagedList);
        pageVO.setTotal((long) total); // total 转换为 long 类型

        return pageVO;
    }

    @Override
    public List<String> getCourseQuestionKnowledges(Integer courseId) {
        // 1. 验证课程是否存在
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new CustomException("课程不存在，ID: " + courseId);
        }

        // 2. 获取所有章节
        List<Chapter> chapters = chapterService.listByCourseId(courseId);

        // 3. 批量查询所有小节和题目（共2次查询）
        if (chapters.isEmpty()) {
            return new ArrayList<>();
        }
        List<Integer> chapterIds = chapters.stream()
                .map(Chapter::getId)
                .collect(Collectors.toList());
        List<Section> allSections = sectionMapper.listByChapterIds(chapterIds);
        if (allSections.isEmpty()) {
            return new ArrayList<>();
        }
        List<Integer> sectionIds = allSections.stream()
                .map(Section::getId)
                .collect(Collectors.toList());
        List<Question> allQuestions = questionMapper.listBySectionIds(sectionIds);

        // 4. 提取知识点
        List<String> questionKnowledgePoints = new ArrayList<>();
        for (Question question : allQuestions) {
            if (question.getKnowledgePoint() != null && !question.getKnowledgePoint().trim().isEmpty()) {
                questionKnowledgePoints.add(question.getKnowledgePoint());
            }
        }

        return questionKnowledgePoints;
    }
}
