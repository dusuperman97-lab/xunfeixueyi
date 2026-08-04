package com.project.platform.service.impl;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.dto.ExamStartRequest;
import com.project.platform.dto.ExamSubmitRequest;
import com.project.platform.dto.QuestionAnswerDTO;
import com.project.platform.dto.SaveAnswerRequest;
import com.project.platform.entity.Course;
import com.project.platform.entity.Question;
import com.project.platform.entity.QuestionTestRecord;
import com.project.platform.entity.SectionTestSummary;
import com.project.platform.mapper.CourseMapper;
import com.project.platform.mapper.QuestionTestRecordMapper;
import com.project.platform.mapper.SectionTestSummaryMapper;
import com.project.platform.service.ExamSessionService;
import com.project.platform.service.QuestionService;
import com.project.platform.service.QuestionTestRecordService;
import com.project.platform.vo.ExamSessionVO;
import com.project.platform.vo.ExamSubmitResultVO;
import com.project.platform.vo.QuestionDetailVO;
import com.project.platform.vo.QuestionResultVO;
import com.project.platform.vo.SavedAnswerVO;
import com.project.platform.vo.TestHistoryDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 考试会话管理服务实现
 */
@Service
@Slf4j
public class ExamSessionServiceImpl implements ExamSessionService {

    @Autowired
    private SectionTestSummaryMapper summaryMapper;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionTestRecordService testRecordService;

    @Autowired
    private QuestionTestRecordMapper testRecordMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private Tools tools;

    /** 默认考试时长（秒），从配置文件读取 */
    @Value("${exam.default-duration:3600}")
    private int defaultExamDuration;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamSessionVO startExam(ExamStartRequest request) {
        Integer userId = tools.getCurrentUserIdInteger();
        Integer courseId = request.getCourseId();
        Integer sectionId = request.getSectionId();
        String examType = request.getExamType();

        log.info("=== 开始考试，用户ID：{}，课程ID：{}，小节ID：{}，考试类型：{}",
                userId, courseId, sectionId, examType);

        // 1. 参数校验
        if (courseId == null) {
            throw new IllegalArgumentException("课程ID不能为空");
        }
        if (examType == null || examType.isEmpty()) {
            throw new IllegalArgumentException("考试类型不能为空");
        }

        // 2. 检查是否有进行中的会话（幂等：返回已有会话）
        SectionTestSummary existing = findExistingSession(userId, courseId, sectionId, examType);
        if (existing != null) {
            log.info("=== 用户已有进行中的考试会话，返回已有会话，批次号：{}", existing.getTestBatchNo());
            return buildSessionVO(existing, examType);
        }

        // 3. 生成测试批次号（先生成，后续绑定题目）
        LocalDateTime now = LocalDateTime.now();
        String testBatchNo = examType + "_" + System.currentTimeMillis() + "_" + userId + "_"
                + UUID.randomUUID().toString().substring(0, 8);

        // 4. 查询最新的 generation batch 题目并迁移到本次考试批次
        // 对于 SECTION_PRACTICE，按 sectionId 隔离查询，防止跨小节批次污染
        // 对于 COURSE_EXAM，sectionId 传 null（课程级考试不区分小节）
        String questionCategory = "SECTION_PRACTICE".equals(examType)
                ? "SECTION_PRACTICE" : "COURSE_EXAM";
        Integer batchSectionId = "SECTION_PRACTICE".equals(examType) ? sectionId : null;
        List<Question> questions = questionService.getLatestGenBatchByCourseAndCategory(
                courseId, questionCategory, batchSectionId);
        if (questions == null || questions.isEmpty()) {
            // 兼容旧数据：回退到全量查询，同时迁移到 session batch
            log.warn("=== 未找到 generation batch 题目，回退到全量查询，courseId={}, category={}, sectionId={}",
                    courseId, questionCategory, sectionId);
            if ("SECTION_PRACTICE".equals(examType)) {
                questions = questionService.getByCourseAndSectionId(courseId, sectionId);
            } else {
                questions = questionService.getByCourseIdAndCategory(courseId, questionCategory);
            }
            // 关键修复：回退路径也必须迁移题目到 session batch，确保 getQuestions 能按 testBatchNo 查询到
            if (questions != null && !questions.isEmpty()) {
                List<Integer> questionIds = questions.stream()
                        .map(Question::getId)
                        .collect(Collectors.toList());
                questionService.assignQuestionsToSession(questionIds, testBatchNo);
                log.info("=== 回退路径题目批次迁移完成：{} 道题目 → sessionBatch={}",
                        questionIds.size(), testBatchNo);
            }
        } else {
            // 将 generation batch 题目迁移到本次考试 session batch
            String genBatchNo = questions.get(0).getTestBatchNo();
            questionService.assignQuestionsToSession(genBatchNo, testBatchNo);
            log.info("=== 题目批次迁移完成：genBatch={} → sessionBatch={}, 题目数={}",
                    genBatchNo, testBatchNo, questions.size());
        }

        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("该考试没有可用的题目");
        }

        // 5. 获取考试时长
        int examDuration = getExamDuration(courseId);

        // 6. 创建考试会话
        SectionTestSummary session = new SectionTestSummary();
        session.setUserId(userId);
        session.setCourseId(courseId);
        session.setSectionId(sectionId != null ? sectionId : 0);
        session.setTestName(buildTestName(courseId, sectionId, examType));
        session.setTestBatchNo(testBatchNo);
        session.setTotalQuestions(questions.size());
        session.setAnsweredQuestions(0);
        session.setCorrectQuestions(0);
        session.setWrongQuestions(0);
        session.setUnansweredQuestions(questions.size());
        session.setTotalScore(BigDecimal.ZERO);
        session.setAccuracyRate(BigDecimal.ZERO);
        session.setAvgTimeSpent(BigDecimal.ZERO);
        session.setTotalTimeSpent(0);
        session.setStartTime(now);
        session.setDeadline(now.plusSeconds(examDuration));
        session.setExamDuration(examDuration);
        session.setStatus("IN_PROGRESS");
        session.setCreateTime(now);
        session.setUpdateTime(now);

        summaryMapper.insert(session);
        log.info("=== 考试会话创建成功，批次号：{}，题目数：{}，时长：{}秒，截止时间：{}",
                testBatchNo, questions.size(), examDuration, session.getDeadline());

        return buildSessionVO(session, examType);
    }

    @Override
    public ExamSessionVO getActiveSession(Integer courseId, Integer sectionId) {
        Integer userId = tools.getCurrentUserIdInteger();
        log.info("=== 查询用户活跃考试会话，用户ID：{}，课程ID：{}，小节ID：{}", userId, courseId, sectionId);

        SectionTestSummary session;
        if (sectionId != null && sectionId > 0) {
            session = summaryMapper.selectInProgressByUserAndSection(userId, sectionId);
        } else {
            session = summaryMapper.selectInProgressByUserAndCourse(userId, courseId);
        }

        if (session == null) {
            log.info("=== 用户无进行中的考试会话");
            return null;
        }

        // 检查是否已超时
        if (session.getDeadline() != null && LocalDateTime.now().isAfter(session.getDeadline())) {
            log.info("=== 活跃会话已超时，批次号：{}", session.getTestBatchNo());
        }

        String examType = session.getTestBatchNo() != null
                && session.getTestBatchNo().startsWith("PRACTICE_")
                ? "SECTION_PRACTICE" : "COURSE_EXAM";

        ExamSessionVO vo = buildSessionVO(session, examType);

        // 加载已保存的答案
        List<QuestionTestRecord> records = testRecordService.getTestRecordsByBatchNo(
                session.getTestBatchNo());
        if (records != null && !records.isEmpty()) {
            vo.setAnsweredCount((int) records.stream()
                    .filter(r -> r.getUserAnswer() != null && !r.getUserAnswer().trim().isEmpty())
                    .count());
            vo.setSavedAnswers(records.stream()
                    .filter(r -> r.getUserAnswer() != null && !r.getUserAnswer().trim().isEmpty())
                    .map(r -> {
                        SavedAnswerVO a = new SavedAnswerVO();
                        a.setQuestionId(r.getQuestionId());
                        a.setUserAnswer(r.getUserAnswer());
                        return a;
                    })
                    .collect(Collectors.toList()));
        } else {
            vo.setAnsweredCount(0);
            vo.setSavedAnswers(new ArrayList<>());
        }

        return vo;
    }

    @Override
    public ExamSessionVO getSessionStatus(String testBatchNo) {
        log.info("=== 查询考试会话状态，批次号：{}", testBatchNo);

        SectionTestSummary session = summaryMapper.selectByBatchNo(testBatchNo);
        if (session == null) {
            throw new IllegalArgumentException("考试会话不存在：" + testBatchNo);
        }

        // 检查是否已超时（刚超时但还没被定时任务处理的边界情况）
        LocalDateTime now = LocalDateTime.now();
        if ("IN_PROGRESS".equals(session.getStatus())
                && session.getDeadline() != null
                && now.isAfter(session.getDeadline())) {
            log.info("=== 考试会话已超时（查询时检测到），批次号：{}", testBatchNo);
        }

        // 确定考试类型
        String examType = session.getTestBatchNo() != null
                && session.getTestBatchNo().startsWith("PRACTICE_")
                ? "SECTION_PRACTICE" : "COURSE_EXAM";

        ExamSessionVO vo = buildSessionVO(session, examType);

        // 加载已保存的答案
        List<QuestionTestRecord> records = testRecordService.getTestRecordsByBatchNo(testBatchNo);
        if (records != null && !records.isEmpty()) {
            vo.setAnsweredCount((int) records.stream()
                    .filter(r -> r.getUserAnswer() != null && !r.getUserAnswer().trim().isEmpty())
                    .count());
            vo.setSavedAnswers(records.stream()
                    .filter(r -> r.getUserAnswer() != null && !r.getUserAnswer().trim().isEmpty())
                    .map(r -> {
                        SavedAnswerVO a = new SavedAnswerVO();
                        a.setQuestionId(r.getQuestionId());
                        a.setUserAnswer(r.getUserAnswer());
                        return a;
                    })
                    .collect(Collectors.toList()));
        } else {
            vo.setAnsweredCount(0);
            vo.setSavedAnswers(new ArrayList<>());
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAnswer(SaveAnswerRequest request) {
        Integer userId = tools.getCurrentUserIdInteger();
        String testBatchNo = request.getTestBatchNo();

        log.info("=== 暂存答案，用户ID：{}，批次号：{}，答案数量：{}",
                userId, testBatchNo, request.getAnswers() != null ? request.getAnswers().size() : 0);

        // 1. 校验会话
        SectionTestSummary session = validateSession(testBatchNo, userId);

        // 2. 校验考试未超时
        if (isSessionExpired(session)) {
            throw new IllegalStateException("考试时间已到，无法保存答案");
        }

        // 3. 解析答案
        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            return;
        }

        // 4. 查询题目信息（按 testBatchNo 隔离）
        String examType = session.getTestBatchNo().startsWith("PRACTICE_")
                ? "SECTION_PRACTICE" : "COURSE_EXAM";
        List<Question> questions = getQuestions(session.getCourseId(), session.getSectionId(),
                examType, testBatchNo);
        Map<Integer, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 5. 构建测试记录（暂存状态，不判题、不评分），先删旧再插新
        List<QuestionTestRecord> existingRecords = testRecordService.getTestRecordsByBatchNo(
                testBatchNo);
        if (existingRecords != null && !existingRecords.isEmpty()) {
            Set<Integer> updatingQuestionIds = request.getAnswers().stream()
                    .map(SaveAnswerRequest.AnswerItem::getQuestionId)
                    .collect(Collectors.toSet());
            testRecordMapper.deleteByBatchNoAndQuestionIds(testBatchNo,
                    new ArrayList<>(updatingQuestionIds));
        }

        // 插入新记录
        List<QuestionTestRecord> records = new ArrayList<>();
        for (SaveAnswerRequest.AnswerItem answer : request.getAnswers()) {
            Question question = questionMap.get(answer.getQuestionId());
            if (question == null) {
                log.warn("=== 题目不存在，跳过，questionId：{}", answer.getQuestionId());
                continue;
            }

            QuestionTestRecord record = new QuestionTestRecord();
            record.setUserId(userId);
            record.setCourseId(session.getCourseId());
            record.setSectionId(session.getSectionId());
            record.setQuestionId(question.getId());
            record.setQuestionCategory(question.getQuestionCategory());
            record.setQuestionType(question.getQuestionType());
            record.setContent(question.getContent() != null ? question.getContent() : "");
            record.setKnowledgePoint(question.getKnowledgePoint() != null
                    ? question.getKnowledgePoint() : "");
            record.setCorrectAnswer(question.getAnswer());
            record.setUserAnswer(answer.getUserAnswer() != null ? answer.getUserAnswer() : "");
            record.setIsCorrect(null); // 暂存不判题
            record.setScore(BigDecimal.ZERO);
            record.setTimeSpent(0);
            record.setTestBatchNo(testBatchNo);
            record.setCreateTime(LocalDateTime.now());
            records.add(record);
        }

        if (!records.isEmpty()) {
            testRecordService.batchInsertTestRecords(records);
        }

        // 6. 更新会话的已答题数
        List<QuestionTestRecord> allRecords = testRecordService.getTestRecordsByBatchNo(
                testBatchNo);
        int answeredCount = (int) (allRecords != null ? allRecords.stream()
                .filter(r -> r.getUserAnswer() != null && !r.getUserAnswer().trim().isEmpty())
                .count() : 0);
        session.setAnsweredQuestions(answeredCount);
        session.setUnansweredQuestions(session.getTotalQuestions() - answeredCount);
        session.setUpdateTime(LocalDateTime.now());
        summaryMapper.update(session);

        log.info("=== 答案暂存成功，批次号：{}，已答题数：{}", testBatchNo, answeredCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamSubmitResultVO submitExam(ExamSubmitRequest request) {
        Integer userId = tools.getCurrentUserIdInteger();
        String testBatchNo = request.getTestBatchNo();

        log.info("=== 提交考试，用户ID：{}，批次号：{}，答案数量：{}",
                userId, testBatchNo, request.getAnswers() != null ? request.getAnswers().size() : 0);

        // 1. 校验会话
        SectionTestSummary session = validateSession(testBatchNo, userId);

        // 2. 服务端计算实际耗时（权威时间源！）
        LocalDateTime now = LocalDateTime.now();
        int actualElapsed = (int) Duration.between(session.getStartTime(), now).getSeconds();
        boolean isTimeout = session.getDeadline() != null && now.isAfter(session.getDeadline());

        log.info("=== 实际耗时：{}秒，是否超时：{}", actualElapsed, isTimeout);

        // 3. 查询题目信息（按 testBatchNo 隔离）
        String examType = session.getTestBatchNo().startsWith("PRACTICE_")
                ? "SECTION_PRACTICE" : "COURSE_EXAM";
        List<Question> questions = getQuestions(session.getCourseId(), session.getSectionId(),
                examType, testBatchNo);
        Map<Integer, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 4. 加载已有的暂存记录（保留原始 createTime，用于计算每题耗时）
        List<QuestionTestRecord> existingRecords = testRecordService.getTestRecordsByBatchNo(
                testBatchNo);
        Map<Integer, QuestionTestRecord> existingMap = new HashMap<>();
        if (existingRecords != null) {
            for (QuestionTestRecord r : existingRecords) {
                existingMap.put(r.getQuestionId(), r);
            }
        }

        // 5. 构建提交答案映射
        Map<Integer, String> submittedAnswerMap = new HashMap<>();
        if (request.getAnswers() != null) {
            for (QuestionAnswerDTO answer : request.getAnswers()) {
                submittedAnswerMap.put(answer.getQuestionId(), answer.getUserAnswer());
            }
        }

        // 6. 判题并构建测试记录
        List<QuestionTestRecord> records = new ArrayList<>();
        List<QuestionResultVO> questionResults = new ArrayList<>();
        int correctCount = 0;
        int wrongCount = 0;
        int unansweredCount = 0;
        int maxAllowedTime = session.getExamDuration() != null
                ? session.getExamDuration() : actualElapsed;

        for (Question question : questions) {
            QuestionTestRecord existingRecord = existingMap.get(question.getId());

            // 确定用户答案：优先用本次提交的，其次用已暂存的
            String userAnswer = submittedAnswerMap.get(question.getId());
            if (userAnswer == null && existingRecord != null) {
                userAnswer = existingRecord.getUserAnswer();
            }
            boolean answered = userAnswer != null && !userAnswer.trim().isEmpty();
            boolean isCorrect = false;

            if (answered) {
                isCorrect = checkAnswer(question, userAnswer);
                if (isCorrect) correctCount++;
                else wrongCount++;
            } else {
                unansweredCount++;
            }

            // 计算该题耗时：基于首次暂存时间到当前时间的差值
            int timeSpent = 0;
            LocalDateTime recordCreateTime = now;
            if (existingRecord != null && existingRecord.getCreateTime() != null) {
                int elapsed = (int) Duration.between(existingRecord.getCreateTime(), now).getSeconds();
                timeSpent = Math.min(Math.max(0, elapsed), maxAllowedTime);
                recordCreateTime = existingRecord.getCreateTime();
            } else if (answered) {
                timeSpent = Math.min(actualElapsed / Math.max(questions.size(), 1), maxAllowedTime);
            }

            QuestionTestRecord record = new QuestionTestRecord();
            record.setUserId(userId);
            record.setCourseId(session.getCourseId());
            record.setSectionId(session.getSectionId());
            record.setQuestionId(question.getId());
            record.setQuestionCategory(question.getQuestionCategory());
            record.setQuestionType(question.getQuestionType());
            record.setContent(question.getContent() != null ? question.getContent() : "");
            record.setKnowledgePoint(question.getKnowledgePoint() != null
                    ? question.getKnowledgePoint() : "未知知识点");
            record.setCorrectAnswer(question.getAnswer());
            record.setUserAnswer(answered ? userAnswer : "");
            record.setIsCorrect(answered ? isCorrect : null);
            record.setScore(isCorrect ? BigDecimal.TEN : BigDecimal.ZERO);
            record.setTimeSpent(timeSpent);
            record.setTestBatchNo(testBatchNo);
            record.setCreateTime(recordCreateTime);
            records.add(record);

            // 构建VO
            QuestionResultVO qr = new QuestionResultVO();
            qr.setQuestionId(question.getId());
            qr.setQuestionType(question.getQuestionType());
            qr.setContent(question.getContent());
            qr.setKnowledgePoint(question.getKnowledgePoint());
            qr.setCorrectAnswer(question.getAnswer());
            qr.setUserAnswer(answered ? userAnswer : "");
            qr.setIsCorrect(answered ? isCorrect : null);
            qr.setScore(isCorrect ? BigDecimal.TEN : BigDecimal.ZERO);
            qr.setTimeSpent(timeSpent);
            questionResults.add(qr);
        }

        // 7. 删除旧暂存记录，插入新记录
        if (existingRecords != null && !existingRecords.isEmpty()) {
            testRecordMapper.deleteByBatchNo(testBatchNo);
        }
        testRecordService.batchInsertTestRecords(records);

        // 8. 更新汇总
        int totalTimeSpent = Math.min(actualElapsed, maxAllowedTime);
        BigDecimal totalScore = records.stream()
                .map(r -> r.getScore() != null ? r.getScore() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int answeredCount = correctCount + wrongCount;
        BigDecimal accuracyRate = answeredCount > 0
                ? BigDecimal.valueOf(correctCount).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(answeredCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        double avgSeconds = records.isEmpty() ? 0 : (double) totalTimeSpent / records.size();
        BigDecimal avgTimeSpent = BigDecimal.valueOf(avgSeconds)
                .setScale(2, RoundingMode.HALF_UP);

        session.setEndTime(now);
        session.setTotalTimeSpent(totalTimeSpent);
        session.setTotalScore(totalScore);
        session.setAnsweredQuestions(answeredCount);
        session.setCorrectQuestions(correctCount);
        session.setWrongQuestions(wrongCount);
        session.setUnansweredQuestions(unansweredCount);
        session.setAccuracyRate(accuracyRate);
        session.setAvgTimeSpent(avgTimeSpent);
        session.setStatus(isTimeout ? "AUTO_SUBMITTED" : "COMPLETED");
        session.setUpdateTime(now);
        summaryMapper.update(session);

        // 9. 更新知识点统计
        testRecordService.updateKnowledgePointStats(userId, session.getSectionId());

        log.info("=== 考试提交完成，批次号：{}，正确：{}，错误：{}，未答：{}，超时：{}",
                testBatchNo, correctCount, wrongCount, unansweredCount, isTimeout);

        // 10. 构建返回结果
        ExamSubmitResultVO result = new ExamSubmitResultVO();
        result.setTestBatchNo(testBatchNo);
        result.setTotalQuestions(questions.size());
        result.setAnsweredQuestions(answeredCount);
        result.setCorrectQuestions(correctCount);
        result.setWrongQuestions(wrongCount);
        result.setUnansweredQuestions(unansweredCount);
        result.setTotalScore(totalScore);
        result.setAccuracyRate(accuracyRate);
        result.setTotalTimeSpent(totalTimeSpent);
        result.setStartTime(session.getStartTime());
        result.setEndTime(now);
        result.setStatus(isTimeout ? "AUTO_SUBMITTED" : "COMPLETED");
        result.setIsTimeout(isTimeout);
        result.setQuestions(questionResults);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoSubmit(SectionTestSummary session) {
        log.info("=== 自动交卷开始，批次号：{}，用户ID：{}",
                session.getTestBatchNo(), session.getUserId());

        LocalDateTime now = LocalDateTime.now();
        int actualElapsed = (int) Duration.between(session.getStartTime(), now).getSeconds();

        // 1. 查询已有暂存记录
        List<QuestionTestRecord> existingRecords = testRecordService.getTestRecordsByBatchNo(
                session.getTestBatchNo());

        // 2. 查询所有题目（按 testBatchNo 隔离），找出未作答的
        String examType = session.getTestBatchNo().startsWith("PRACTICE_")
                ? "SECTION_PRACTICE" : "COURSE_EXAM";
        List<Question> questions = getQuestions(session.getCourseId(), session.getSectionId(),
                examType, session.getTestBatchNo());

        Set<Integer> answeredQuestionIds = new HashSet<>();
        if (existingRecords != null) {
            answeredQuestionIds = existingRecords.stream()
                    .filter(r -> r.getUserAnswer() != null && !r.getUserAnswer().trim().isEmpty())
                    .map(QuestionTestRecord::getQuestionId)
                    .collect(Collectors.toSet());
        }

        // 3. 判题并更新已有记录（保留首次暂存时间，计算每题真实耗时）
        int correctCount = 0;
        int wrongCount = 0;
        int unansweredCount = 0;
        int maxAllowedTime = session.getExamDuration() != null
                ? session.getExamDuration() : actualElapsed;
        Map<Integer, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        List<QuestionTestRecord> allRecords = new ArrayList<>();

        if (existingRecords != null) {
            for (QuestionTestRecord record : existingRecords) {
                if (record.getUserAnswer() != null && !record.getUserAnswer().trim().isEmpty()) {
                    Question question = questionMap.get(record.getQuestionId());
                    boolean isCorrect = question != null && checkAnswer(question,
                            record.getUserAnswer());
                    record.setIsCorrect(isCorrect);
                    record.setScore(isCorrect ? BigDecimal.TEN : BigDecimal.ZERO);

                    if (record.getCreateTime() != null) {
                        int elapsed = (int) Duration.between(record.getCreateTime(), now).getSeconds();
                        record.setTimeSpent(Math.min(Math.max(0, elapsed), maxAllowedTime));
                    } else {
                        record.setTimeSpent(Math.min(actualElapsed
                                / Math.max(questions.size(), 1), maxAllowedTime));
                    }

                    if (isCorrect) correctCount++;
                    else wrongCount++;
                } else {
                    record.setIsCorrect(null);
                    record.setScore(BigDecimal.ZERO);
                    record.setTimeSpent(0);
                }
                allRecords.add(record);
            }
        }

        // 4. 为未作答的题目创建空记录
        for (Question question : questions) {
            if (!answeredQuestionIds.contains(question.getId())) {
                QuestionTestRecord record = new QuestionTestRecord();
                record.setUserId(session.getUserId());
                record.setCourseId(session.getCourseId());
                record.setSectionId(session.getSectionId());
                record.setQuestionId(question.getId());
                record.setQuestionCategory(question.getQuestionCategory());
                record.setQuestionType(question.getQuestionType());
                record.setContent(question.getContent() != null ? question.getContent() : "");
                record.setKnowledgePoint(question.getKnowledgePoint() != null
                        ? question.getKnowledgePoint() : "未知知识点");
                record.setCorrectAnswer(question.getAnswer());
                record.setUserAnswer("");
                record.setIsCorrect(null);
                record.setScore(BigDecimal.ZERO);
                record.setTimeSpent(0);
                record.setTestBatchNo(session.getTestBatchNo());
                record.setCreateTime(now);
                allRecords.add(record);
                unansweredCount++;
            }
        }

        // 5. 持久化：先删后插
        testRecordMapper.deleteByBatchNo(session.getTestBatchNo());
        testRecordService.batchInsertTestRecords(allRecords);

        // 6. 更新汇总
        int totalTimeSpent = Math.min(actualElapsed,
                session.getExamDuration() != null ? session.getExamDuration() : actualElapsed);
        int answeredCount = correctCount + wrongCount;
        BigDecimal totalScore = BigDecimal.valueOf(correctCount).multiply(BigDecimal.TEN);
        BigDecimal accuracyRate = answeredCount > 0
                ? BigDecimal.valueOf(correctCount).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(answeredCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        summaryMapper.updateStatusToAutoSubmitted(session.getId(), now, totalTimeSpent);

        session.setEndTime(now);
        session.setTotalTimeSpent(totalTimeSpent);
        session.setTotalScore(totalScore);
        session.setAnsweredQuestions(answeredCount);
        session.setCorrectQuestions(correctCount);
        session.setWrongQuestions(wrongCount);
        session.setUnansweredQuestions(unansweredCount);
        session.setAccuracyRate(accuracyRate);
        session.setStatus("AUTO_SUBMITTED");
        session.setUpdateTime(now);
        summaryMapper.update(session);

        // 7. 更新知识点统计
        testRecordService.updateKnowledgePointStats(session.getUserId(), session.getSectionId());

        log.info("=== 自动交卷完成，批次号：{}，正确：{}，错误：{}，未答：{}",
                session.getTestBatchNo(), correctCount, wrongCount, unansweredCount);
    }

    @Override
    public List<SectionTestSummary> getCourseExamHistory(Integer userId, Integer courseId) {
        log.info("=== 查询课程考试历史汇总，用户ID：{}，课程ID：{}", userId, courseId);

        if (userId == null || courseId == null) {
            throw new IllegalArgumentException("用户ID和课程ID不能为空");
        }

        List<SectionTestSummary> summaries = summaryMapper.selectByUserAndCourse(userId, courseId);
        log.info("=== 查询到课程考试历史汇总数量：{}", summaries != null ? summaries.size() : 0);
        return summaries != null ? summaries : new ArrayList<>();
    }

    @Override
    public TestHistoryDetailVO getCourseExamHistoryDetail(Integer userId, Integer summaryId) {
        log.info("=== 查询课程考试历史详情，用户ID：{}，summaryId：{}", userId, summaryId);

        if (userId == null || summaryId == null) {
            throw new IllegalArgumentException("用户ID和汇总记录ID不能为空");
        }

        SectionTestSummary summary = summaryMapper.selectById(summaryId.longValue());
        if (summary == null) {
            log.info("=== 未查询到指定的考试汇总记录，summaryId={}", summaryId);
            return null;
        }
        if (!summary.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权查看该考试记录");
        }

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

        List<QuestionTestRecord> records = testRecordService.getTestRecordsByBatchNo(
                summary.getTestBatchNo());
        vo.setQuestions(buildQuestionDetails(records));

        log.info("=== 查询到课程考试历史详情，summaryId={}，题目数={}",
                summaryId, records != null ? records.size() : 0);
        return vo;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 查找用户已有的进行中会话
     */
    private SectionTestSummary findExistingSession(Integer userId, Integer courseId,
                                                    Integer sectionId, String examType) {
        if ("SECTION_PRACTICE".equals(examType) && sectionId != null && sectionId > 0) {
            return summaryMapper.selectInProgressByUserAndSection(userId, sectionId);
        } else {
            return summaryMapper.selectInProgressByUserAndCourse(userId, courseId);
        }
    }

    /**
     * 校验会话合法性
     */
    private SectionTestSummary validateSession(String testBatchNo, Integer userId) {
        SectionTestSummary session = summaryMapper.selectByBatchNo(testBatchNo);
        if (session == null) {
            throw new IllegalArgumentException("考试会话不存在：" + testBatchNo);
        }
        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作该考试会话");
        }
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new IllegalStateException("考试已结束，状态：" + session.getStatus());
        }
        return session;
    }

    /**
     * 判断会话是否已过期
     */
    private boolean isSessionExpired(SectionTestSummary session) {
        return session.getDeadline() != null && LocalDateTime.now().isAfter(session.getDeadline());
    }

    /**
     * 获取考试题目（按 testBatchNo 隔离，带安全回退）
     *
     * 优先按 testBatchNo 精确查询以隔离不同批次的题目；
     * 若 batch 查询为空，回退到非 batch 查询以兼容旧数据/迁移异常等边缘场景。
     */
    private List<Question> getQuestions(Integer courseId, Integer sectionId, String examType,
                                         String testBatchNo) {
        List<Question> questions;
        if ("SECTION_PRACTICE".equals(examType)) {
            questions = questionService.getByCourseAndSectionIdWithBatchNo(courseId, sectionId,
                    testBatchNo);
        } else {
            questions = questionService.getByCourseIdAndCategoryWithBatchNo(courseId, "COURSE_EXAM",
                    testBatchNo);
        }

        // 安全回退：若 batch 隔离查询为空，尝试无 batch 过滤查询
        // 这可能发生在：旧数据无正确 batchNo、回退路径未迁移等边缘场景
        if (questions == null || questions.isEmpty()) {
            log.warn("=== 按 testBatchNo 查询题目为空，回退到无 batch 过滤查询，testBatchNo={}, courseId={}, sectionId={}",
                    testBatchNo, courseId, sectionId);
            if ("SECTION_PRACTICE".equals(examType)) {
                questions = questionService.getByCourseAndSectionId(courseId, sectionId);
            } else {
                questions = questionService.getByCourseIdAndCategory(courseId, "COURSE_EXAM");
            }
        }

        return questions;
    }

    /**
     * 获取考试时长（秒）
     */
    private int getExamDuration(Integer courseId) {
        if (courseId != null) {
            try {
                Course course = courseMapper.selectById(courseId);
                if (course != null && course.getExamDuration() != null
                        && course.getExamDuration() > 0) {
                    log.debug("=== 从课程配置读取考试时长：courseId={}, duration={}秒",
                            courseId, course.getExamDuration());
                    return course.getExamDuration();
                }
            } catch (Exception e) {
                log.warn("=== 查询课程考试时长失败，使用默认值：courseId={}, default={}秒",
                        courseId, defaultExamDuration, e);
            }
        }
        return defaultExamDuration;
    }

    /**
     * 构建考试名称
     */
    private String buildTestName(Integer courseId, Integer sectionId, String examType) {
        if ("SECTION_PRACTICE".equals(examType)) {
            return "小节练习 - " + sectionId;
        } else {
            return "课程考试 - " + courseId;
        }
    }

    /**
     * 构建会话VO
     */
    private ExamSessionVO buildSessionVO(SectionTestSummary session, String examType) {
        ExamSessionVO vo = new ExamSessionVO();
        vo.setTestBatchNo(session.getTestBatchNo());
        vo.setCourseId(session.getCourseId());
        vo.setSectionId(session.getSectionId());
        vo.setExamType(examType);
        vo.setStartTime(session.getStartTime());
        vo.setDeadline(session.getDeadline());
        vo.setExamDuration(session.getExamDuration());
        vo.setStatus(session.getStatus());

        // 计算剩余时间
        if (session.getDeadline() != null) {
            long remaining = Duration.between(LocalDateTime.now(), session.getDeadline()).getSeconds();
            vo.setRemainingSeconds(Math.max(0, remaining));
        } else {
            vo.setRemainingSeconds(0L);
        }

        vo.setTotalQuestions(session.getTotalQuestions());
        vo.setAnsweredCount(session.getAnsweredQuestions() != null
                ? session.getAnsweredQuestions() : 0);
        vo.setSavedAnswers(new ArrayList<>());

        return vo;
    }

    /**
     * 判题逻辑
     */
    private boolean checkAnswer(Question question, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return false;
        }

        String correctAnswer = question.getAnswer();
        String questionType = question.getQuestionType();

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

        return correctAnswer.trim().equalsIgnoreCase(userAnswer.trim());
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
