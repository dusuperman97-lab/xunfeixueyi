package com.project.platform.mapper;

import com.project.platform.entity.QuestionTestRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 题目测试记录Mapper接口
 */
@Mapper
public interface QuestionTestRecordMapper {
    
    /**
     * 插入测试记录
     */
    int insert(QuestionTestRecord record);
    
    /**
     * 批量插入测试记录
     */
    int batchInsert(@Param("records") List<QuestionTestRecord> records);
    
    /**
     * 根据用户ID和小节ID查询测试记录
     */
    List<QuestionTestRecord> selectByUserAndSection(@Param("userId") Integer userId, 
                                                     @Param("sectionId") Integer sectionId);
    
    /**
     * 根据测试批次号查询测试记录
     */
    List<QuestionTestRecord> selectByBatchNo(@Param("testBatchNo") String testBatchNo);
    
    /**
     * 根据知识点统计正确率
     */
    List<Map<String, Object>> selectAccuracyByKnowledgePoint(@Param("userId") Integer userId, @Param("sectionId") Integer sectionId);
    
    /**
     * 查询用户的测试历史
     */
    List<Map<String, Object>> selectTestHistory(@Param("userId") Integer userId, @Param("sectionId") Integer sectionId);
    
    /**
     * 查询用户的练习情况记录（按测试批次聚合）
     */
    List<Map<String, Object>> selectPracticeRecords(@Param("userId") Integer userId);
    
    /**
     * 查询用户的错题记录
     */
    List<QuestionTestRecord> selectWrongQuestions(@Param("userId") Integer userId,
                                                   @Param("sectionId") Integer sectionId);

    /**
     * 根据测试批次号删除所有记录
     */
    int deleteByBatchNo(@Param("testBatchNo") String testBatchNo);

    /**
     * 根据测试批次号和题目ID列表删除记录
     */
    int deleteByBatchNoAndQuestionIds(@Param("testBatchNo") String testBatchNo,
                                       @Param("questionIds") List<Integer> questionIds);
}
