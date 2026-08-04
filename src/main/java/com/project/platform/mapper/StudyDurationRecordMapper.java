package com.project.platform.mapper;

import com.project.platform.entity.StudyDurationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 学生学习时长记录Mapper
 */
@Mapper
public interface StudyDurationRecordMapper {

    /**
     * 查询学生近N天的学习时长统计（按日期汇总）
     */
    List<Map<String, Object>> getStudyDurationStats(@Param("studentId") Integer studentId,
                                                     @Param("days") Integer days);

    /**
     * 查询学生近N天的总学习时长
     */
    Integer getTotalStudyDuration(@Param("studentId") Integer studentId,
                                   @Param("days") Integer days);

    /**
     * 查询学生指定课程近N天的学习时长统计（按日期汇总）
     */
    List<Map<String, Object>> getStudyDurationStatsByCourse(@Param("studentId") Integer studentId,
                                                             @Param("courseId") Integer courseId,
                                                             @Param("days") Integer days);

    /**
     * 查询学生指定课程近N天的总学习时长
     */
    Integer getTotalStudyDurationByCourse(@Param("studentId") Integer studentId,
                                           @Param("courseId") Integer courseId,
                                           @Param("days") Integer days);

    /**
     * 查询学生近N天各课程的总学习时长（用于课程分布）
     */
    List<Map<String, Object>> getStudyDurationGroupByCourse(@Param("studentId") Integer studentId,
                                                             @Param("days") Integer days);

    /**
     * 插入学习时长记录
     */
    int insert(StudyDurationRecord record);

    /**
     * 更新学习时长记录
     */
    int updateById(StudyDurationRecord record);

    /**
     * 根据学生ID和日期查询记录
     */
    StudyDurationRecord selectByStudentAndDate(@Param("studentId") Integer studentId,
                                                @Param("studyDate") LocalDate studyDate);

    /**
     * 根据学生ID、课程ID和日期查询记录
     */
    StudyDurationRecord selectByStudentCourseAndDate(@Param("studentId") Integer studentId,
                                                      @Param("courseId") Integer courseId,
                                                      @Param("studyDate") LocalDate studyDate);
}
