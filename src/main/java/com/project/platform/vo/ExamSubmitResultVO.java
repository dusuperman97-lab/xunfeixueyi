package com.project.platform.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试提交结果VO
 */
@Data
public class ExamSubmitResultVO {

    /** 测试批次号 */
    private String testBatchNo;

    /** 总题目数 */
    private Integer totalQuestions;

    /** 已答题数 */
    private Integer answeredQuestions;

    /** 正确题数 */
    private Integer correctQuestions;

    /** 错误题数 */
    private Integer wrongQuestions;

    /** 未答题数 */
    private Integer unansweredQuestions;

    /** 总分 */
    private BigDecimal totalScore;

    /** 正确率 */
    private BigDecimal accuracyRate;

    /** 总耗时（秒） */
    private Integer totalTimeSpent;

    /** 考试开始时间 */
    private LocalDateTime startTime;

    /** 交卷时间 */
    private LocalDateTime endTime;

    /** 考试状态：COMPLETED(正常交卷) / AUTO_SUBMITTED(超时自动交卷) */
    private String status;

    /** 是否超时交卷 */
    private Boolean isTimeout;

    /** 逐题答题详情 */
    private List<QuestionResultVO> questions;
}
