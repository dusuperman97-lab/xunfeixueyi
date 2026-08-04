package com.project.platform.service.impl;

import com.project.platform.dto.QuestionTestRecordDTO;
import com.project.platform.entity.KnowledgePointStats;
import com.project.platform.entity.SectionTestSummary;
import com.project.platform.entity.QuestionTestRecord;
import com.project.platform.entity.Section;
import com.project.platform.mapper.KnowledgePointStatsMapper;
import com.project.platform.mapper.QuestionTestRecordMapper;
import com.project.platform.mapper.SectionTestSummaryMapper;
import com.project.platform.service.QuestionTestRecordService;
import com.project.platform.service.SectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 题目测试记录服务实现类
 */
@Service
public class QuestionTestRecordServiceImpl implements QuestionTestRecordService {

    @Autowired
    private QuestionTestRecordMapper testRecordMapper;

    @Autowired
    private KnowledgePointStatsMapper knowledgePointStatsMapper;

    @Autowired
    private SectionTestSummaryMapper sectionTestSummaryMapper;

    @Autowired
    private SectionService sectionService;

    /**
     * 批量保存测试记录并更新统计
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTestRecords(List<QuestionTestRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        // 1. 批量插入测试记录
        testRecordMapper.batchInsert(records);

        // 2. 获取第一个记录的批次信息
        String testBatchNo = records.get(0).getTestBatchNo();
        Integer userId = records.get(0).getUserId();
        Integer sectionId = records.get(0).getSectionId();

        // 3. 更新知识点统计
        updateKnowledgePointStatsInternal(userId, sectionId);

        // 4. 更新小节测试汇总
        updateSectionTestSummary(userId, sectionId, testBatchNo, records);
    }

    /**
     * 根据用户ID和小节ID查询测试记录
     */
    @Override
    public List<QuestionTestRecord> getTestRecords(Integer userId, Integer sectionId) {
        return testRecordMapper.selectByUserAndSection(userId, sectionId);
    }

    /**
     * 根据测试批次号查询测试记录
     */
    @Override
    public List<QuestionTestRecord> getTestRecordsByBatchNo(String testBatchNo) {
        return testRecordMapper.selectByBatchNo(testBatchNo);
    }

    /**
     * 获取知识点正确率统计
     */
    @Override
    public List<Map<String, Object>> getKnowledgePointAccuracy(Integer userId, Integer sectionId) {
        return testRecordMapper.selectAccuracyByKnowledgePoint(userId, sectionId);
    }

    /**
     * 获取测试历史
     */
    @Override
    public List<Map<String, Object>> getTestHistory(Integer userId, Integer sectionId) {
        return testRecordMapper.selectTestHistory(userId, sectionId);
    }

    /**
     * 获取练习情况记录（按测试批次聚合）
     */
    @Override
    public List<QuestionTestRecordDTO> getPracticeRecords(Integer userId) {
        List<Map<String, Object>> records = testRecordMapper.selectPracticeRecords(userId);
        List<QuestionTestRecordDTO> dtoList = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> record = records.get(i);
            QuestionTestRecordDTO dto = new QuestionTestRecordDTO();
            dto.setRowNumber(i + 1); // 设置序号

            // 设置练习时长（秒转分钟显示）
            Object totalTimeSpent = record.get("totalTimeSpent");
            if (totalTimeSpent != null) {
                dto.setTotalTimeSpent(((Number) totalTimeSpent).intValue());
            }

            // 设置练习名称（去掉前面的序号，如 "3.2 Java开发环境搭建" -> "Java开发环境搭建"）
            String sectionName = (String) record.get("sectionName");
            if (sectionName != null && sectionName.matches("^\\d+\\.\\d+\\s+.*")) {
                // 匹配格式：数字.数字 空格 内容，去掉前面的序号部分
                sectionName = sectionName.replaceFirst("^\\d+\\.\\d+\\s+", "");
            }
            dto.setSectionName(sectionName);

            // 设置练习创建时间
            Object createTime = record.get("createTime");
            if (createTime != null) {
                if (createTime instanceof java.sql.Timestamp) {
                    dto.setCreateTime(((java.sql.Timestamp) createTime).toLocalDateTime());
                } else if (createTime instanceof LocalDateTime) {
                    dto.setCreateTime((LocalDateTime) createTime);
                }
            }

            // 设置正确率
            Object accuracyRate = record.get("accuracyRate");
            if (accuracyRate != null) {
                dto.setAccuracyRate(accuracyRate.toString());
            }

            dtoList.add(dto);
        }

        return dtoList;
    }

    /**
     * 查询用户的错题记录
     */
    @Override
    public List<QuestionTestRecord> getWrongQuestions(Integer userId, Integer sectionId) {
        return testRecordMapper.selectWrongQuestions(userId, sectionId);
    }

    /**
     * 更新知识点统计
     */
    public void updateKnowledgePointStats2(Integer userId, Integer sectionId) {
        // 按知识点分组统计
        List<Map<String, Object>> statsList = testRecordMapper.selectAccuracyByKnowledgePoint(userId, sectionId);

        for (Map<String, Object> stat : statsList) {
            KnowledgePointStats stats = new KnowledgePointStats();
            stats.setUserId(userId);
            stats.setSectionId(sectionId);
            stats.setKnowledgePoint((String) stat.get("knowledgePoint"));
            stats.setTotalCount(((Number) stat.get("totalCount")).intValue());
            stats.setCorrectCount(((Number) stat.get("correctCount")).intValue());
            stats.setWrongCount(((Number) stat.get("wrongCount")).intValue());
            Object accuracyObj = stat.get("accuracyRate");
            stats.setAccuracyRate(accuracyObj != null ? new BigDecimal(accuracyObj.toString()) : BigDecimal.ZERO);
            Object avgTimeObj = stat.get("avgTimeSpent");
            stats.setAvgTimeSpent(avgTimeObj != null ? new BigDecimal(avgTimeObj.toString()) : BigDecimal.ZERO);
            stats.setLastTestTime(LocalDateTime.now());

            // 计算掌握程度
            stats.setMasteryLevel(calculateMasteryLevel(stats.getAccuracyRate()));

            // 插入或更新
            knowledgePointStatsMapper.upsert(stats);
        }
    }

    /**
     * 更新小节测试汇总
     */
    private void updateSectionTestSummary(Integer userId, Integer sectionId,
                                          String testBatchNo, List<QuestionTestRecord> records) {
        // 查询是否已存在汇总记录
        SectionTestSummary existingSummary = sectionTestSummaryMapper.selectByBatchNo(testBatchNo);

        // 统计数据
        int totalQuestions = records.size();
        long answeredQuestions = records.stream().filter(r -> r.getUserAnswer() != null).count();
        long correctQuestions = records.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
        long wrongQuestions = records.stream().filter(r -> Boolean.FALSE.equals(r.getIsCorrect())).count();
        long unansweredQuestions = totalQuestions - answeredQuestions;

        BigDecimal totalScore = records.stream()
                .map(r -> r.getScore() != null ? r.getScore() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal accuracyRate = answeredQuestions > 0
                ? BigDecimal.valueOf(correctQuestions).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(answeredQuestions), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 1. 先计算平均值（double 类型），如果没有数据则默认为 0.0
        double avgSeconds = records.stream()
                .filter(r -> r.getTimeSpent() != null)
                .mapToInt(QuestionTestRecord::getTimeSpent)
                .average()
                .orElse(0.0);

        // 2. 再转换为 BigDecimal 并保留两位小数
        BigDecimal avgTimeSpent = BigDecimal.valueOf(avgSeconds).setScale(2, RoundingMode.HALF_UP);


        int totalTimeSpent = records.stream()
                .mapToInt(r -> r.getTimeSpent() != null ? r.getTimeSpent() : 0)
                .sum();

        LocalDateTime startTime = records.stream()
                .map(QuestionTestRecord::getCreateTime)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        LocalDateTime endTime = LocalDateTime.now();

        if (existingSummary != null) {
            // 更新现有记录
            existingSummary.setAnsweredQuestions((int) answeredQuestions);
            existingSummary.setCorrectQuestions((int) correctQuestions);
            existingSummary.setWrongQuestions((int) wrongQuestions);
            existingSummary.setUnansweredQuestions((int) unansweredQuestions);
            existingSummary.setTotalScore(totalScore);
            existingSummary.setAccuracyRate(accuracyRate);
            existingSummary.setAvgTimeSpent(avgTimeSpent);
            existingSummary.setTotalTimeSpent(totalTimeSpent);
            existingSummary.setEndTime(endTime);
            existingSummary.setStatus("COMPLETED");

            sectionTestSummaryMapper.update(existingSummary);
        } else {
            // 创建新记录
            SectionTestSummary summary = new SectionTestSummary();
            summary.setUserId(userId);
            summary.setSectionId(sectionId);

            // 获取小节名称作为测试名称
            Section section = sectionService.selectById(sectionId);
            if (section != null) {
                summary.setTestName(section.getSectionName());
            }

            summary.setTestBatchNo(testBatchNo);
            summary.setTotalQuestions(totalQuestions);
            summary.setAnsweredQuestions((int) answeredQuestions);
            summary.setCorrectQuestions((int) correctQuestions);
            summary.setWrongQuestions((int) wrongQuestions);
            summary.setUnansweredQuestions((int) unansweredQuestions);
            summary.setTotalScore(totalScore);
            summary.setAccuracyRate(accuracyRate);
            summary.setAvgTimeSpent(avgTimeSpent);
            summary.setTotalTimeSpent(totalTimeSpent);
            summary.setStartTime(startTime);
            summary.setEndTime(endTime);
            summary.setStatus("COMPLETED");

            sectionTestSummaryMapper.insert(summary);
        }
    }

    /**
     * 根据正确率计算掌握程度
     */
    private String calculateMasteryLevel(BigDecimal accuracyRate) {
        if (accuracyRate == null) {
            return "BEGINNER";
        }

        double rate = accuracyRate.doubleValue();
        if (rate >= 90) {
            return "MASTER";      // 精通：正确率>=90%
        } else if (rate >= 75) {
            return "ADVANCED";    // 高级：正确率>=75%
        } else if (rate >= 60) {
            return "INTERMEDIATE";// 中级：正确率>=60%
        } else {
            return "BEGINNER";    // 初级：正确率<60%
        }
    }

    /**
     * 批量插入测试记录（不更新统计）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchInsertTestRecords(List<QuestionTestRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        // 只批量插入测试记录，不更新统计
        testRecordMapper.batchInsert(records);
    }

    /**
     * 更新知识点统计（公开方法）
     */
    @Override
    public void updateKnowledgePointStats(Integer userId, Integer sectionId) {
        // 调用私有方法
        updateKnowledgePointStatsInternal(userId, sectionId);
    }

    /**
     * 更新知识点统计（内部方法）
     */
    private void updateKnowledgePointStatsInternal(Integer userId, Integer sectionId) {
        // 按知识点分组统计
        List<Map<String, Object>> statsList = testRecordMapper.selectAccuracyByKnowledgePoint(userId, sectionId);

        for (Map<String, Object> stat : statsList) {
            KnowledgePointStats stats = new KnowledgePointStats();
            stats.setUserId(userId);
            stats.setSectionId(sectionId);
            stats.setKnowledgePoint((String) stat.get("knowledgePoint"));
            stats.setTotalCount(((Number) stat.get("totalCount")).intValue());
            stats.setCorrectCount(((Number) stat.get("correctCount")).intValue());
            stats.setWrongCount(((Number) stat.get("wrongCount")).intValue());
            Object accuracyObj = stat.get("accuracyRate");
            stats.setAccuracyRate(accuracyObj != null ? new BigDecimal(accuracyObj.toString()) : BigDecimal.ZERO);
            Object avgTimeObj = stat.get("avgTimeSpent");
            stats.setAvgTimeSpent(avgTimeObj != null ? new BigDecimal(avgTimeObj.toString()) : BigDecimal.ZERO);
            stats.setLastTestTime(LocalDateTime.now());

            // 计算掌握程度
            stats.setMasteryLevel(calculateMasteryLevel(stats.getAccuracyRate()));

            // 插入或更新
            knowledgePointStatsMapper.upsert(stats);
        }
    }
}
