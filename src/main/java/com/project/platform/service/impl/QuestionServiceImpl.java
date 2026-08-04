package com.project.platform.service.impl;

import com.project.platform.dto.QuestionAnswerDTO;
import com.project.platform.entity.Question;
import com.project.platform.entity.QuestionTestRecord;
import com.project.platform.entity.SectionTestSummary;
import com.project.platform.mapper.QuestionMapper;
import com.project.platform.mapper.SectionTestSummaryMapper;
import com.project.platform.service.QuestionService;
import com.project.platform.service.QuestionTestRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 随堂练习题服务实现
 */
@Service
@Slf4j
public class QuestionServiceImpl implements QuestionService {

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private QuestionTestRecordService testRecordService;

    @Autowired
    private SectionTestSummaryMapper summaryMapper;

    @Override
    public List<Question> getBySectionId(Integer sectionId) {
        return questionMapper.selectBySectionId(sectionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Question> saveOrUpdateQuestions(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return Collections.emptyList();
        }
        // 取第一条的sectionId，删除该小节下所有旧题目，再批量插入新题目
        Integer sectionId = questions.get(0).getSectionId();
        questionMapper.deleteBySectionId(sectionId);
        questionMapper.batchInsert(questions);
        return questions;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitAnswers(Integer userId, List<QuestionAnswerDTO> answers, Integer spentTime) {
        // 1. 参数验证
        if (answers == null || answers.isEmpty()) {
            throw new IllegalArgumentException("答案不能为空");
        }
        if (spentTime == null || spentTime < 0) {
            throw new IllegalArgumentException("答题时间不能为负数");
        }

        // 2. 获取课程ID和小节ID
        Integer courseId = answers.get(0).getCourseId();
        Integer sectionId = answers.get(0).getSectionId();
        if (courseId == null) {
            throw new IllegalArgumentException("课程ID不能为空");
        }

        // 3. 生成测试批次号
        String testBatchNo = "PRACTICE_" + System.currentTimeMillis() + "_" + userId + "_"
                + UUID.randomUUID().toString().substring(0, 8);
        log.info("=== 生成随堂练习批次号：{}", testBatchNo);

        // 4. 查询最新的 generation batch 题目并迁移到本次练习批次
        List<Question> questions = getLatestGenBatchByCourseAndCategory(courseId, "SECTION_PRACTICE",
                sectionId);
        if (questions == null || questions.isEmpty()) {
            // 兼容旧数据：回退到全量查询
            log.warn("=== 未找到 generation batch 题目，回退到全量查询，courseId={}, sectionId={}",
                    courseId, sectionId);
            questions = getByCourseAndSectionId(courseId, sectionId);
        } else {
            String genBatchNo = questions.get(0).getTestBatchNo();
            assignQuestionsToSession(genBatchNo, testBatchNo);
            log.info("=== 题目批次迁移完成：genBatch={} → sessionBatch={}, 题目数={}",
                    genBatchNo, testBatchNo, questions.size());
        }

        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("该小节没有可用的练习题");
        }

        // 5. 构建题目ID到题目对象的映射
        Map<Integer, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 6. 判题并构建测试记录
        List<QuestionTestRecord> records = new ArrayList<>();
        int correctCount = 0;
        int wrongCount = 0;
        int skippedCount = 0;

        for (QuestionAnswerDTO answer : answers) {
            Question question = questionMap.get(answer.getQuestionId());
            if (question == null) {
                log.warn("=== 题目不存在，跳过，questionId：{}", answer.getQuestionId());
                skippedCount++;
                continue;
            }

            boolean isCorrect = checkAnswer(question, answer.getUserAnswer());
            if (isCorrect) {
                correctCount++;
            } else {
                wrongCount++;
            }

            QuestionTestRecord record = new QuestionTestRecord();
            record.setUserId(userId);
            record.setCourseId(question.getCourseId());
            record.setSectionId(sectionId);
            record.setQuestionId(question.getId());
            record.setQuestionCategory(question.getQuestionCategory());
            record.setQuestionType(question.getQuestionType());
            record.setContent(question.getContent() != null ? question.getContent() : "");
            record.setKnowledgePoint(question.getKnowledgePoint() != null
                    ? question.getKnowledgePoint() : "未知知识点");
            record.setCorrectAnswer(question.getAnswer());
            record.setUserAnswer(answer.getUserAnswer() != null ? answer.getUserAnswer() : "");
            record.setIsCorrect(isCorrect);
            record.setScore(isCorrect ? BigDecimal.TEN : BigDecimal.ZERO);
            record.setTimeSpent(spentTime);
            record.setTestBatchNo(testBatchNo);
            record.setCreateTime(LocalDateTime.now());
            records.add(record);
        }

        log.info("=== 判题完成！正确：{}，错误：{}，跳过：{}", correctCount, wrongCount, skippedCount);

        // 7. 保存测试记录和汇总
        if (!records.isEmpty()) {
            testRecordService.batchInsertTestRecords(records);
            log.info("=== 测试记录保存成功，批次号：{}，总记录数：{}", testBatchNo, records.size());

            // 更新知识点统计
            testRecordService.updateKnowledgePointStats(userId, sectionId);

            // 创建随堂练习测试汇总
            int answeredCount = correctCount + wrongCount;
            BigDecimal accuracyRate = answeredCount > 0
                    ? BigDecimal.valueOf(correctCount).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(answeredCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal totalScore = BigDecimal.valueOf(correctCount).multiply(BigDecimal.TEN);

            SectionTestSummary summary = new SectionTestSummary();
            summary.setUserId(userId);
            summary.setCourseId(courseId);
            summary.setSectionId(sectionId != null ? sectionId : 0);
            summary.setTestName("随堂练习 - " + sectionId);
            summary.setTestBatchNo(testBatchNo);
            summary.setTotalQuestions(questions.size());
            summary.setAnsweredQuestions(answeredCount);
            summary.setCorrectQuestions(correctCount);
            summary.setWrongQuestions(wrongCount);
            summary.setUnansweredQuestions(questions.size() - answeredCount);
            summary.setTotalScore(totalScore);
            summary.setAccuracyRate(accuracyRate);
            summary.setTotalTimeSpent(spentTime != null ? spentTime : 0);
            summary.setStartTime(LocalDateTime.now().minusSeconds(spentTime != null ? spentTime : 0));
            summary.setEndTime(LocalDateTime.now());
            summary.setStatus("COMPLETED");
            summary.setCreateTime(LocalDateTime.now());
            summary.setUpdateTime(LocalDateTime.now());
            summaryMapper.insert(summary);
            log.info("=== 随堂练习提交完成！批次号：{}，总分：{}，正确率：{}%",
                    testBatchNo, totalScore, accuracyRate);
        } else {
            log.error("=== 没有有效的测试记录可保存");
            throw new RuntimeException("没有有效的测试记录可保存");
        }
    }

    @Override
    public List<Question> getByCourseAndSectionId(Integer courseId, Integer sectionId) {
        return questionMapper.selectByCourseAndSectionId(courseId, sectionId, null);
    }

    @Override
    public List<Question> getByCourseAndSectionIdWithBatchNo(Integer courseId, Integer sectionId,
                                                              String testBatchNo) {
        return questionMapper.selectByCourseAndSectionId(courseId, sectionId, testBatchNo);
    }

    @Override
    public List<Question> getByCourseIdAndCategory(Integer courseId, String questionCategory) {
        return questionMapper.selectByCourseIdAndCategory(courseId, questionCategory, null);
    }

    @Override
    public List<Question> getByCourseIdAndCategoryWithBatchNo(Integer courseId, String questionCategory,
                                                               String testBatchNo) {
        return questionMapper.selectByCourseIdAndCategory(courseId, questionCategory, testBatchNo);
    }

    @Override
    public List<Question> getByBatchNo(String testBatchNo) {
        return questionMapper.selectByBatchNo(testBatchNo);
    }

    @Override
    public List<Question> getLatestGenBatchByCourseAndCategory(Integer courseId, String questionCategory,
                                                                Integer sectionId) {
        return questionMapper.selectLatestGenBatchByCourseAndCategory(courseId, questionCategory, sectionId);
    }

    @Override
    public void assignQuestionsToSession(String genBatchNo, String sessionBatchNo) {
        int updated = questionMapper.updateTestBatchNo(genBatchNo, sessionBatchNo);
        log.info("=== 题目批次迁移：{} → {}, 影响行数：{}", genBatchNo, sessionBatchNo, updated);
    }

    @Override
    public void assignQuestionsToSession(List<Integer> questionIds, String sessionBatchNo) {
        if (questionIds == null || questionIds.isEmpty()) {
            log.warn("=== 题目批次迁移：questionIds为空，跳过");
            return;
        }
        int updated = questionMapper.updateTestBatchNoByIds(questionIds, sessionBatchNo);
        log.info("=== 题目批次迁移（byIds）：{} 道题目 → {}, 影响行数：{}",
                questionIds.size(), sessionBatchNo, updated);
    }

    @Override
    public Question getById(Integer questionId) {
        return questionMapper.selectById(questionId);
    }

    /**
     * 判题逻辑
     * @param question 题目对象
     * @param userAnswer 用户答案
     * @return 是否正确
     */
    private boolean checkAnswer(Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return false;
        }

        String correctAnswer = question.getAnswer();
        String questionType = question.getQuestionType();

        // 多选题：需要排序后比较
        if ("MULTIPLE".equals(questionType)) {
            List<String> correctList = Arrays.stream(correctAnswer.split(","))
                    .map(String::trim)
                    .sorted()
                    .collect(Collectors.toList());
            List<String> userList = Arrays.stream(userAnswer.split(","))
                    .map(String::trim)
                    .sorted()
                    .collect(Collectors.toList());
            return correctList.equals(userList);
        }

        // 其他题型：直接比较（忽略大小写和空格）
        return correctAnswer.trim().equalsIgnoreCase(userAnswer.trim());
    }
}
