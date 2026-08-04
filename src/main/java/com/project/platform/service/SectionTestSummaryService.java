package com.project.platform.service;

import com.project.platform.dto.QuestionAnswerDTO;
import com.project.platform.entity.SectionTestSummary;

import java.util.List;

/**
 * 小节测试汇总服务接口
 */
public interface SectionTestSummaryService {

    /**
     * 创建或更新测试汇总记录
     */
    void createOrUpdateSummary(SectionTestSummary summary);

    /**
     * 根据用户ID和小节ID查询测试汇总
     */
    List<SectionTestSummary> getTestSummaries(Integer userId, Integer sectionId);

    /**
     * 根据测试批次号查询测试汇总
     */
    SectionTestSummary getTestSummaryByBatchNo(String testBatchNo);

    /**
     * 查询用户的测试历史汇总
     */
    List<SectionTestSummary> getTestHistory(Integer userId);

    SectionTestSummary submitAnswers(List<QuestionAnswerDTO> answers, Integer spentTime);

    /**
     * 查询随堂测试历史详情(包含题目列表)
     */
    List<com.project.platform.vo.TestHistoryDetailVO> getSectionTestHistoryDetail(Integer userId, Integer sectionId);

    /**
     * 查询课程考试历史详情(包含题目列表)
     */
    List<com.project.platform.vo.TestHistoryDetailVO> getCourseExamHistoryDetail(Integer userId, Integer courseId);
}
