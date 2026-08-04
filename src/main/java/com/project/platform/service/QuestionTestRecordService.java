package com.project.platform.service;

import com.project.platform.dto.QuestionTestRecordDTO;
import com.project.platform.entity.QuestionTestRecord;

import java.util.List;
import java.util.Map;

/**
 * 题目测试记录服务接口
 */
public interface QuestionTestRecordService {

    /**
     * 批量保存测试记录并更新统计
     */
    void saveTestRecords(List<QuestionTestRecord> records);

    /**
     * 根据用户ID和小节ID查询测试记录
     */
    List<QuestionTestRecord> getTestRecords(Integer userId, Integer sectionId);

    /**
     * 根据测试批次号查询测试记录
     */
    List<QuestionTestRecord> getTestRecordsByBatchNo(String testBatchNo);

    /**
     * 获取知识点正确率统计
     */
    List<Map<String, Object>> getKnowledgePointAccuracy(Integer userId, Integer sectionId);

    /**
     * 获取测试历史
     */
    List<Map<String, Object>> getTestHistory(Integer userId, Integer sectionId);

    /**
     * 获取练习情况记录（按测试批次聚合）
     */
    List<QuestionTestRecordDTO> getPracticeRecords(Integer userId);

    /**
     * 查询用户的错题记录
     */
    List<QuestionTestRecord> getWrongQuestions(Integer userId, Integer sectionId);

    /**
     * 批量插入测试记录（不更新统计）
     */
    void batchInsertTestRecords(List<QuestionTestRecord> records);

    /**
     * 更新知识点统计
     */
    void updateKnowledgePointStats(Integer userId, Integer sectionId);

    void updateKnowledgePointStats2(Integer userId, Integer sectionId);
}
