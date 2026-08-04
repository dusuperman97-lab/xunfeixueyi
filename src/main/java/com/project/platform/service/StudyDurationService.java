package com.project.platform.service;

import com.project.platform.dto.CourseStudyStatsDTO;
import com.project.platform.entity.Student;

import java.util.List;

/**
 * 学生学习时长统计Service
 */
public interface StudyDurationService {

    /**
     * 获取学生近N天的学习时长统计
     */
    Student getStudyDurationStats(Integer studentId, Integer days);

    /**
     * 获取学生指定课程近N天的学习时长统计
     */
    CourseStudyStatsDTO getCourseStudyDurationStats(Integer studentId, Integer courseId, Integer days);

    /**
     * 获取学生近N天各课程的学习时长分布
     */
    List<CourseStudyStatsDTO> getStudyDurationGroupByCourse(Integer studentId, Integer days);

    /**
     * 记录学习时长
     */
    void recordStudyDuration(Integer studentId, Integer courseId, Integer durationMinutes,
                             String studyType, Integer sectionId);

    List<Student> getStudentList();
}
