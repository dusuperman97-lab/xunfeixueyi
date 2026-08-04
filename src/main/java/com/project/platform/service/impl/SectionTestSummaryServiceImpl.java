package com.project.platform.service.impl;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.dto.QuestionAnswerDTO;
import com.project.platform.entity.Question;
import com.project.platform.entity.QuestionTestRecord;
import com.project.platform.entity.SectionTestSummary;
import com.project.platform.mapper.QuestionTestRecordMapper;
import com.project.platform.mapper.SectionTestSummaryMapper;
import com.project.platform.service.QuestionService;
import com.project.platform.service.QuestionTestRecordService;
import com.project.platform.service.SectionTestSummaryService;
import com.project.platform.vo.QuestionDetailVO;
import com.project.platform.vo.TestHistoryDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 小节测试汇总服务实现类
 */
@Service
@Slf4j
public class SectionTestSummaryServiceImpl implements SectionTestSummaryService {

    @Autowired
    private SectionTestSummaryMapper sectionTestSummaryMapper;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionTestRecordService testRecordService;

    @Autowired
    private Tools tools;

    /**
     * 创建或更新测试汇总记录
     */
    @Override
    public void createOrUpdateSummary(SectionTestSummary summary) {
        SectionTestSummary existing = sectionTestSummaryMapper.selectByBatchNo(summary.getTestBatchNo());
        if (existing != null) {
            sectionTestSummaryMapper.update(summary);
        } else {
            sectionTestSummaryMapper.insert(summary);
        }
    }

    /**
     * 根据用户ID和小节ID查询测试汇总
     */
    @Override
    public List<SectionTestSummary> getTestSummaries(Integer userId, Integer sectionId) {
        return sectionTestSummaryMapper.selectByUserAndSection(userId, sectionId);
    }

    /**
     * 根据测试批次号查询测试汇总
     */
    @Override
    public SectionTestSummary getTestSummaryByBatchNo(String testBatchNo) {
        return sectionTestSummaryMapper.selectByBatchNo(testBatchNo);
    }

    /**
     * 查询用户的测试历史汇总
     */
    @Override
    public List<SectionTestSummary> getTestHistory(Integer userId) {
        return sectionTestSummaryMapper.selectTestHistory(userId);
    }

    /**
     * 提交用户答案并生成测试结果(课程考试)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SectionTestSummary submitAnswers(List<QuestionAnswerDTO> answers, Integer spentTime) {
        Integer userId = tools.getCurrentUserIdInteger();
        log.info("=== 开始提交课堂考试答案！用户ID：{}，题目数量：{}", userId, answers != null ? answers.size() : 0);

        // 1. 参数验证
        if (answers == null || answers.isEmpty()) {
            throw new IllegalArgumentException("答案不能为空");
        }

        if (spentTime == null || spentTime < 0) {
            throw new IllegalArgumentException("答题时间不能为负数");
        }

        // 2. 获取课程ID（从第一个答案中获取）
        Integer courseId = answers.get(0).getCourseId();
        if (courseId == null) {
            throw new IllegalArgumentException("课程ID不能为空");
        }

        // 3. 生成唯一的测试批次号
        String testBatchNo = "EXAM_" + System.currentTimeMillis() + "_" + userId + "_" +
                            UUID.randomUUID().toString().substring(0, 8);
        log.info("=== 生成测试批次号：{}", testBatchNo);

        // 4. 查询最新的 generation batch 题目并迁移到本次考试批次
        // COURSE_EXAM 为课程级别，sectionId 传 null
        List<Question> questions = questionService.getLatestGenBatchByCourseAndCategory(courseId, "COURSE_EXAM", null);
        if (questions == null || questions.isEmpty()) {
            // 兼容旧数据：回退到全量查询，同时迁移到 session batch
            log.warn("=== 未找到 generation batch 题目，回退到全量查询，courseId={}", courseId);
            questions = questionService.getByCourseIdAndCategory(courseId, "COURSE_EXAM");
            // 关键修复：回退路径也必须迁移题目到 session batch
            if (questions != null && !questions.isEmpty()) {
                List<Integer> questionIds = questions.stream()
                        .map(Question::getId)
                        .collect(Collectors.toList());
                questionService.assignQuestionsToSession(questionIds, testBatchNo);
                log.info("=== 回退路径题目批次迁移完成：{} 道题目 → sessionBatch={}",
                        questionIds.size(), testBatchNo);
            }
        } else {
            String genBatchNo = questions.get(0).getTestBatchNo();
            questionService.assignQuestionsToSession(genBatchNo, testBatchNo);
            log.info("=== 题目批次迁移完成：genBatch={} → sessionBatch={}, 题目数={}",
                    genBatchNo, testBatchNo, questions.size());
        }

        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("该课程没有可用的考试题目");
        }

        log.info("=== 查询到课程考试题目数量：{}", questions.size());

        // 5. 构建题目ID到题目对象的映射
        Map<Integer, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 6. 判题并构建测试记录
        List<QuestionTestRecord> testRecords = new ArrayList<>();
        int correctCount = 0;
        int wrongCount = 0;
        int skippedCount = 0;

        for (QuestionAnswerDTO answer : answers) {
            Question question = questionMap.get(answer.getQuestionId());
            if (question == null) {
                log.warn("=== 题目不存在，跳过该题目，questionId：{}", answer.getQuestionId());
                skippedCount++;
                continue;
            }

            // 验证答案格式
            if (answer.getUserAnswer() == null || answer.getUserAnswer().trim().isEmpty()) {
                log.warn("=== 用户答案为，questionId：{}", answer.getQuestionId());
            }

            // 判题
            boolean isCorrect = checkAnswer(question, answer.getUserAnswer());
            if (isCorrect) {
                correctCount++;
            } else {
                wrongCount++;
            }

            // 构建测试记录
            QuestionTestRecord record = new QuestionTestRecord();
            record.setUserId(userId);
            record.setCourseId(question.getCourseId());
            record.setSectionId(0); // 课程考试的sectionId固定为0
            record.setQuestionId(question.getId());
            record.setQuestionCategory(question.getQuestionCategory());
            record.setQuestionType(question.getQuestionType());
            record.setContent(question.getContent() != null ? question.getContent() : "");
            record.setKnowledgePoint(question.getKnowledgePoint() != null ? question.getKnowledgePoint() : "未知知识点");
            record.setCorrectAnswer(question.getAnswer());
            record.setUserAnswer(answer.getUserAnswer() != null ? answer.getUserAnswer() : "");
            record.setIsCorrect(isCorrect);
            record.setScore(isCorrect ? BigDecimal.TEN : BigDecimal.ZERO); // 正确得10分，错误得0分
            record.setTimeSpent(spentTime);
            record.setTestBatchNo(testBatchNo);
            record.setCreateTime(LocalDateTime.now());
            testRecords.add(record);
        }

        log.info("=== 判题完成！正确：{}，错误：{}，跳过：{}", correctCount, wrongCount, skippedCount);

        // 7. 保存测试记录和汇总信息
        if (!testRecords.isEmpty()) {

            // 7.1 批量插入测试记录
            testRecordService.batchInsertTestRecords(testRecords);
            log.info("=== 测试记录保存成功，批次号：{}，总记录数：{}", testBatchNo, testRecords.size());

            // 7.2 更新知识点统计（课堂考试也需要统计知识点掌握情况）
            testRecordService.updateKnowledgePointStats(userId, 0);

            // 7.3 创建课堂考试的测试汇总记录
            SectionTestSummary summary = tools.createCourseExamSummary(userId, courseId, testBatchNo, testRecords);

            log.info("=== 课堂考试答案提交完成！批次号：{}，总分：{}，正确率：{}%",
                    testBatchNo, summary.getTotalScore(), summary.getAccuracyRate());
            return summary;
        } else {
            log.error("=== 没有有效的测试记录可保存");
            throw new RuntimeException("没有有效的测试记录可保存");
        }
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

    /**
     * 查询随堂测试历史详情(包含题目列表)
     */
    @Override
    public List<TestHistoryDetailVO> getSectionTestHistoryDetail(Integer userId, Integer sectionId) {
        log.info("=== 查询随堂测试历史详情，用户ID：{}，小节ID：{}", userId, sectionId);

        // 1. 查询该小节的随堂测试汇总记录
        List<SectionTestSummary> summaries = sectionTestSummaryMapper.selectSectionTestHistory(userId, sectionId);
        if (summaries == null || summaries.isEmpty()) {
            log.info("=== 未查询到随堂测试历史记录");
            return new ArrayList<>();
        }

        // 2. 转换为VO并填充题目详情
        List<TestHistoryDetailVO> result = new ArrayList<>();
        for (SectionTestSummary summary : summaries) {
            TestHistoryDetailVO vo = new TestHistoryDetailVO();
            vo.setSummaryId(summary.getId());
            vo.setTestName(summary.getTestName());
            vo.setTestBatchNo(summary.getTestBatchNo());
            vo.setTotalQuestions(summary.getTotalQuestions());
            vo.setAnsweredQuestions(summary.getAnsweredQuestions());
            vo.setCorrectQuestions(summary.getCorrectQuestions());
            vo.setWrongQuestions(summary.getWrongQuestions());
            vo.setUnansweredQuestions(summary.getUnansweredQuestions());
            vo.setTotalScore(summary.getTotalScore());
            vo.setAccuracyRate(summary.getAccuracyRate());
            vo.setTotalTimeSpent(summary.getTotalTimeSpent());
            vo.setStartTime(summary.getStartTime());
            vo.setEndTime(summary.getEndTime());
            vo.setStatus(summary.getStatus());

            // 3. 查询该批次的题目详情
            List<QuestionTestRecord> records = testRecordService.getTestRecordsByBatchNo(summary.getTestBatchNo());
            List<QuestionDetailVO> questionDetails = buildQuestionDetails(records);
            vo.setQuestions(questionDetails);

            result.add(vo);
        }

        log.info("=== 查询到随堂测试历史详情数量：{}", result.size());
        return result;
    }

    /**
     * 查询课程考试历史详情(包含题目列表)
     */
    @Override
    public List<TestHistoryDetailVO> getCourseExamHistoryDetail(Integer userId, Integer courseId) {
        log.info("=== 查询课程考试历史详情，用户ID：{}，课程ID：{}", userId, courseId);

        // 1. 查询该课程的考试汇总记录
        List<SectionTestSummary> summaries = sectionTestSummaryMapper.selectCourseExamHistory(userId, courseId);
        if (summaries == null || summaries.isEmpty()) {
            log.info("=== 未查询到课程考试历史记录");
            return new ArrayList<>();
        }

        // 2. 转换为VO并填充题目详情
        List<TestHistoryDetailVO> result = new ArrayList<>();
        for (SectionTestSummary summary : summaries) {
            TestHistoryDetailVO vo = new TestHistoryDetailVO();
            vo.setSummaryId(summary.getId());
            vo.setTestName(summary.getTestName());
            vo.setTestBatchNo(summary.getTestBatchNo());
            vo.setTotalQuestions(summary.getTotalQuestions());
            vo.setAnsweredQuestions(summary.getAnsweredQuestions());
            vo.setCorrectQuestions(summary.getCorrectQuestions());
            vo.setWrongQuestions(summary.getWrongQuestions());
            vo.setUnansweredQuestions(summary.getUnansweredQuestions());
            vo.setTotalScore(summary.getTotalScore());
            vo.setAccuracyRate(summary.getAccuracyRate());
            vo.setTotalTimeSpent(summary.getTotalTimeSpent());
            vo.setStartTime(summary.getStartTime());
            vo.setEndTime(summary.getEndTime());
            vo.setStatus(summary.getStatus());

            // 3. 查询该批次的题目详情
            List<QuestionTestRecord> records = testRecordService.getTestRecordsByBatchNo(summary.getTestBatchNo());
            List<QuestionDetailVO> questionDetails = buildQuestionDetails(records);
            vo.setQuestions(questionDetails);

            result.add(vo);
        }

        log.info("=== 查询到课程考试历史详情数量：{}", result.size());
        return result;
    }

    /**
     * 构建题目详情列表
     */
    private List<QuestionDetailVO> buildQuestionDetails(List<QuestionTestRecord> records) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }

        List<QuestionDetailVO> details = new ArrayList<>();
        for (QuestionTestRecord record : records) {
            QuestionDetailVO vo = new QuestionDetailVO();
            vo.setQuestionId(record.getQuestionId());
            vo.setQuestionType(record.getQuestionType());
            vo.setQuestionTypeLabel(getQuestionTypeLabel(record.getQuestionType()));
            vo.setContent(record.getContent());
            vo.setUserAnswer(record.getUserAnswer());
            vo.setCorrectAnswer(record.getCorrectAnswer());
            vo.setKnowledgePoint(record.getKnowledgePoint());
            vo.setIsCorrect(record.getIsCorrect());

            // 查询题目的选项和解析
            Question question = questionService.getById(record.getQuestionId());
            if (question != null) {
                vo.setOptions(question.getOptions());
                vo.setAnalysis(question.getAnalysis());
            }

            details.add(vo);
        }

        return details;
    }

    /**
     * 获取题目类型标签(中文)
     */
    private String getQuestionTypeLabel(String questionType) {
        if (questionType == null) {
            return "未知";
        }
        switch (questionType) {
            case "CHOICE":
                return "单选题";
            case "MULTIPLE":
                return "多选题";
            case "JUDGE":
                return "判断题";
            case "FILL":
                return "填空题";
            default:
                return questionType;
        }
    }
}
