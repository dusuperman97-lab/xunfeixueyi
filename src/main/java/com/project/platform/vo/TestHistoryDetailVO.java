package com.project.platform.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 测试历史详情VO
 * 用于返回完整的测试历史记录,包含汇总信息和题目详情
 */
@Data
public class TestHistoryDetailVO {
    
    // ========== 测试汇总信息 ==========
    private Long summaryId;               // 汇总记录ID
    private String testName;              // 测试名称
    private String testBatchNo;           // 测试批次号
    private Integer totalQuestions;       // 总题目数
    private Integer answeredQuestions;    // 已答题数
    private Integer correctQuestions;     // 正确题数
    private Integer wrongQuestions;       // 错误题数
    private Integer unansweredQuestions;  // 未答题数
    private BigDecimal totalScore;        // 总分
    private BigDecimal accuracyRate;      // 正确率(百分比)
    private Integer totalTimeSpent;       // 总答题耗时(秒)
    private LocalDateTime startTime;      // 开始答题时间
    private LocalDateTime endTime;        // 完成答题时间
    private String status;                // 测试状态:IN_PROGRESS(进行中), COMPLETED(已完成)
    
    // ========== 题目详情列表 ==========
    private List<QuestionDetailVO> questions;  // 题目详情列表
}
