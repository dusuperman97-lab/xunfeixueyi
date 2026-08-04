package com.project.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 学习路线实体
 */
@Data
public class LearningPath {
    private Integer id;
    private Integer userId;                 // 用户ID
    private String pathType;                // 路线类型：major-专业推荐, personal-个性化
    private String major;                   // 专业（专业推荐时使用）
    private String pathName;                // 路线名称
    private String pathContent;             // 路线内容（JSON格式：包含学年、课程列表等）
    private String aiAnalysis;              // AI分析说明
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
