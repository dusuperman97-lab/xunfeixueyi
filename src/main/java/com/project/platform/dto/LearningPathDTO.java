package com.project.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 学习路线DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathDTO {
    private Integer id;
    private String pathType;                // 路线类型：major-专业推荐, personal-个性化
    private String pathName;                // 路线名称
    private String aiAnalysis;              // AI分析说明
    private List<YearPlan> yearPlans;       // 学年计划列表

    /**
     * 学年计划
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YearPlan {
        private String year;                // 学年（如："大一上学期"）
        private String description;         // 该学年学习目标
        private List<CoursePlan> courses;   // 课程列表
    }

    /**
     * 课程计划
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoursePlan {
        private String courseName;          // 课程名称
        private String description;         // 课程描述
        private String videoUrl;            // 课程视频地址
        private String priority;            // 优先级：high-高, medium-中, low-低
        private String reason;              // 推荐理由
    }
}
