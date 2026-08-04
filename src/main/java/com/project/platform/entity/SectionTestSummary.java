package com.project.platform.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 小节测试汇总实体类
 * 用于统计每个小节的整体测试情况
 */
@Data
public class SectionTestSummary {
    private Long id;                    // 主键ID
    private Integer userId;             // 用户ID
    private Integer courseId;           // 课程ID
    private Integer sectionId;          // 小节ID
    private String testName;            // 测试名称
    private String testBatchNo;         // 测试批次号
    private Integer totalQuestions;     // 总题目数
    private Integer answeredQuestions;  // 已答题数
    private Integer correctQuestions;   // 正确题数
    private Integer wrongQuestions;     // 错误题数
    private Integer unansweredQuestions;// 未答题数
    private BigDecimal totalScore;      // 总分
    private BigDecimal accuracyRate;    // 正确率（百分比）
    private BigDecimal avgTimeSpent;    // 平均答题耗时（秒）
    private Integer totalTimeSpent;     // 总答题耗时（秒）
    private LocalDateTime startTime;    // 开始答题时间
    private LocalDateTime endTime;      // 完成答题时间
    private LocalDateTime deadline;     // 考试截止时间（startTime + 考试时长）
    private Integer examDuration;       // 考试时长（秒）
    private String status;              // 测试状态：IN_PROGRESS(进行中), COMPLETED(已完成), AUTO_SUBMITTED(自动交卷)
    private LocalDateTime createTime;   // 创建时间
    private LocalDateTime updateTime;   // 更新时间
}
