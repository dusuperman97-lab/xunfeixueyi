package com.project.platform.service;

import com.project.platform.dto.QuestionAnswerDTO;
import com.project.platform.entity.Question;

import java.util.List;

/**
 * 随堂练习题Service接口
 */
public interface QuestionService {

    /**
     * 根据小节ID查询所有题目
     */
    List<Question> getBySectionId(Integer sectionId);

    /**
     * 保存或更新小节题目(先删除原有题目,再批量插入新题目)
     */
    List<Question> saveOrUpdateQuestions(List<Question> questions);

    /**
     * 提交答案并保存测试记录
     * @param userId 用户ID
     * @param answers 答案列表
     */
    void submitAnswers(Integer userId, List<QuestionAnswerDTO> answers, Integer spentTime);

    /**
     * 根据课程ID和小节ID查询题目（无批次过滤）
     */
    List<Question> getByCourseAndSectionId(Integer courseId, Integer sectionId);

    /**
     * 根据课程ID、小节ID和测试批次号查询题目（用于考试）
     */
    List<Question> getByCourseAndSectionIdWithBatchNo(Integer courseId, Integer sectionId, String testBatchNo);

    /**
     * 根据课程ID和题目类别查询题目（无批次过滤）
     */
    List<Question> getByCourseIdAndCategory(Integer courseId, String questionCategory);

    /**
     * 根据课程ID、题目类别和测试批次号查询题目（用于考试，按批次隔离）
     */
    List<Question> getByCourseIdAndCategoryWithBatchNo(Integer courseId, String questionCategory, String testBatchNo);

    /**
     * 根据测试批次号查询题目
     */
    List<Question> getByBatchNo(String testBatchNo);

    /**
     * 根据课程ID和题目类别查询最新的 generation batch 题目
     * @param sectionId 可选，用于小节级别的批次隔离（SECTION_PRACTICE 场景，COURSE_EXAM 传 null）
     */
    List<Question>  getLatestGenBatchByCourseAndCategory(Integer courseId, String questionCategory,
                                                         Integer sectionId);

    /**
     * 将 generation batch 的题目迁移到考试 session batch
     */
    void assignQuestionsToSession(String genBatchNo, String sessionBatchNo);

    /**
     * 将指定ID的题目批量迁移到考试 session batch（用于回退路径）
     */
    void assignQuestionsToSession(List<Integer> questionIds, String sessionBatchNo);

    /**
     * 根据题目ID查询题目详情
     */
    Question getById(Integer questionId);
}
