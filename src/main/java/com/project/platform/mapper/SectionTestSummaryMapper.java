package com.project.platform.mapper;

import com.project.platform.entity.SectionTestSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 小节测试汇总Mapper接口
 */
@Mapper
public interface SectionTestSummaryMapper {

    /**
     * 插入测试汇总记录
     */
    int insert(SectionTestSummary summary);

    /**
     * 更新测试汇总记录
     */
    int update(SectionTestSummary summary);

    /**
     * 根据用户ID和小节ID查询测试汇总
     */
    List<SectionTestSummary> selectByUserAndSection(@Param("userId") Integer userId,
                                                     @Param("sectionId") Integer sectionId);

    /**
     * 根据用户ID和小节ID查询测试汇总
     */
    List<SectionTestSummary> selectByUserAndCourse(@Param("userId") Integer userId,
                                                    @Param("courseId") Integer courseId);

    /**
     * 根据测试批次号查询测试汇总
     */
    SectionTestSummary selectByBatchNo(@Param("testBatchNo") String testBatchNo);

    /**
     * 查询用户的测试历史汇总
     */
    List<SectionTestSummary> selectTestHistory(@Param("userId") Integer userId);

    /**
     * 根据用户ID和小节ID查询随堂测试历史(按批次号分组)
     */
    List<SectionTestSummary> selectSectionTestHistory(@Param("userId") Integer userId,
                                                       @Param("sectionId") Integer sectionId);

    /**
     * 根据用户ID和课程ID查询课程考试历史(按批次号分组)
     */
    List<SectionTestSummary> selectCourseExamHistory(@Param("userId") Integer userId,
                                                      @Param("courseId") Integer courseId);

    /**
     * 查询已过期但仍在进行中的考试会话（用于定时自动交卷）
     */
    List<SectionTestSummary> selectExpiredInProgress();

    /**
     * 根据用户ID和课程ID查询进行中的考试会话
     */
    SectionTestSummary selectInProgressByUserAndCourse(@Param("userId") Integer userId,
                                                        @Param("courseId") Integer courseId);

    /**
     * 根据用户ID和小节ID查询进行中的考试会话
     */
    SectionTestSummary selectInProgressByUserAndSection(@Param("userId") Integer userId,
                                                         @Param("sectionId") Integer sectionId);

    /**
     * 根据主键ID查询测试汇总
     */
    SectionTestSummary selectById(@Param("id") Long id);

    /**
     * 查询用户所有已完成的测试汇总（按开始时间排序）
     */
    List<SectionTestSummary> selectCompletedByUser(@Param("userId") Integer userId);

    /**
     * 查询用户测试活跃时段分布（按小时统计）
     */
    List<Map<String, Object>> selectHourDistributionByUser(@Param("userId") Integer userId);

    /**
     * 更新考试会话状态为自动交卷
     */
    int updateStatusToAutoSubmitted(@Param("id") Long id,
                                     @Param("endTime") LocalDateTime endTime,
                                     @Param("totalTimeSpent") Integer totalTimeSpent);
}
