package com.project.platform.service.impl;

import com.project.platform.entity.SectionTestSummary;
import com.project.platform.mapper.SectionTestSummaryMapper;
import com.project.platform.service.ExamSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 考试超时自动交卷定时任务
 * <p>
 * 定期扫描 status=IN_PROGRESS 且 deadline < NOW() 的考试会话，
 * 自动执行交卷操作：保存已有答案、未答题标记为超时未答。
 */
@Slf4j
@Component
public class ExamTimeoutTask {

    @Autowired
    private SectionTestSummaryMapper summaryMapper;

    @Autowired
    private ExamSessionService examSessionService;

    /** 超时扫描间隔（毫秒），从配置文件读取 */
    @Value("${exam.timeout-check-interval:30000}")
    private long checkInterval;

    /**
     * 定时扫描过期考试会话并自动交卷
     * 默认每30秒执行一次
     */
    @Scheduled(fixedDelayString = "${exam.timeout-check-interval:30000}")
    public void autoSubmitExpiredExams() {
        log.debug("=== 开始扫描过期考试会话");

        List<SectionTestSummary> expiredSessions;
        try {
            expiredSessions = summaryMapper.selectExpiredInProgress();
        } catch (Exception e) {
            log.error("=== 查询过期考试会话失败", e);
            return;
        }

        if (expiredSessions == null || expiredSessions.isEmpty()) {
            log.debug("=== 没有过期的考试会话");
            return;
        }

        log.info("=== 发现 {} 个过期考试会话，开始自动交卷", expiredSessions.size());

        int successCount = 0;
        int failCount = 0;

        for (SectionTestSummary session : expiredSessions) {
            try {
                examSessionService.autoSubmit(session);
                successCount++;
                log.info("=== 自动交卷成功：batchNo={}, userId={}, courseId={}",
                        session.getTestBatchNo(), session.getUserId(), session.getCourseId());
            } catch (Exception e) {
                failCount++;
                log.error("=== 自动交卷失败：batchNo={}, userId={}",
                        session.getTestBatchNo(), session.getUserId(), e);
            }
        }

        log.info("=== 过期考试扫描完成，成功：{}，失败：{}", successCount, failCount);
    }
}
