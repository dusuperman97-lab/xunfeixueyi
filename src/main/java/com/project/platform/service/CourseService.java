package com.project.platform.service;

import com.project.platform.dto.CourseCompletionDTO;
import com.project.platform.dto.CourseQuestionStatisticsDTO;
import com.project.platform.entity.Course;
import com.project.platform.entity.Question;
import com.project.platform.vo.PageVO;

import java.util.List;
import java.util.Map;

/**
 * 课程服务类
 */
public interface CourseService {

    PageVO<Course> page(Map<String, Object> query, Integer pageNum, Integer pageSize);

    Course selectById(Integer id);

    List<Course> list();

    List<Course> listByCreator(Integer creatorId);  // 确认有此方法

    Course listByCourseName(String CourseName);

    void insert(Course course);

    void updateById(Course course);

    void removeByIds(List<Integer> ids);


    /**
     * 获取课程完成情况统计
     */
    CourseCompletionDTO getCourseCompletion(Integer courseId);

    /**
     * 获取用户所有课程的完成情况列表
     */
    List<CourseCompletionDTO> getUserCoursesCompletion(Integer userId);

    /**
     * 统计课程所有测试题
     */
    CourseQuestionStatisticsDTO getCourseQuestionStatistics(Integer courseId);

    /**
     * 获取课程下所有测试题列表
     */
    List<Question> getCourseQuestions(Integer courseId);

    /**
     * 分页查询课程下的测试题（支持按题型筛选）
     *
     * @param courseId     课程ID
     * @param questionType 题型（允许为null或空字符串）
     * @param pageNum      当前页码
     * @param pageSize     每页大小
     * @return 分页数据封装 (包含列表和总数)
     */
    PageVO<Question> getCourseQuestionsPage(Integer courseId, String questionType, Integer pageNum, Integer pageSize);

    List<String> getCourseQuestionKnowledges(Integer courseId);
}
