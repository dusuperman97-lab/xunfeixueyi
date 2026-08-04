package com.project.platform.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识点统计实体类
 * 用于统计每个知识点的掌握情况
 */
@Data
public class KnowledgePointStats {
    private Long id;                    // 主键ID
    private Integer userId;             // 用户ID
    private Integer sectionId;          // 小节ID
    private String knowledgePoint;      // 知识点名称
    private Integer totalCount;         // 总答题次数
    private Integer correctCount;       // 正确次数
    private Integer wrongCount;         // 错误次数
    private BigDecimal accuracyRate;    // 正确率（百分比）
    private BigDecimal avgTimeSpent;    // 平均答题耗时（秒）
    private LocalDateTime lastTestTime; // 最后测试时间
    private String masteryLevel;        // 掌握程度：BEGINNER(初级), INTERMEDIATE(中级), ADVANCED(高级), MASTER(精通)
    private LocalDateTime updateTime;   // 更新时间
}
