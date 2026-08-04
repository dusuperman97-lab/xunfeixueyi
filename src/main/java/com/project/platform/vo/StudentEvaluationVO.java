package com.project.platform.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 学生效果评估综合数据 VO
 * 聚合 AI 工具使用、练习测试、学习时长、问答反馈等多维度统计数据
 */
@Data
public class StudentEvaluationVO {

    private OverviewStats overview;
    private List<ToolUsageStat> toolUsage;
    private List<TestTrendPoint> testTrend;
    private List<KnowledgeMastery> knowledgeMastery;
    private List<DailyStudy> dailyStudy;
    private QaFeedback qaFeedback;
    private List<HourActivity> hourActivity;
    private List<CourseProgress> courseProgress;
    private List<CourseStudyTime> courseStudyTime;

    @Data
    public static class OverviewStats {
        private Integer totalStudyMinutes;
        private Integer totalTestCount;
        private Double avgAccuracy;
        private Integer totalQaCount;
        private Integer totalResourcesSaved;
        private Integer maxConsecutiveDays;
        /** 综合投入度评分 0-100 */
        private Integer engagementScore;
    }

    @Data
    public static class ToolUsageStat {
        private String toolKey;
        private String toolLabel;
        private String icon;
        private Integer usageCount;
        private Integer durationMinutes;
    }

    @Data
    public static class TestTrendPoint {
        private String date;
        private Integer testCount;
        private Double avgAccuracy;
        private Integer avgDuration; // seconds
    }

    @Data
    public static class KnowledgeMastery {
        private String knowledgePoint;
        private BigDecimal accuracyRate;
        private String masteryLevel;
        private Integer totalCount;
    }

    @Data
    public static class DailyStudy {
        private String date;
        private Integer durationMinutes;
    }

    @Data
    public static class QaFeedback {
        private Integer total;
        private Integer thumbsUp;
        private Integer thumbsDown;
        private Integer noFeedback;
    }

    @Data
    public static class HourActivity {
        private Integer hour;
        private Integer sessionCount;
        private Integer totalMinutes;
    }

    @Data
    public static class CourseProgress {
        private Integer courseId;
        private String courseName;
        private Integer totalSections;
        private Integer completedSections;
        private Double completionRate; // 0-100
        private String status; // NOT_STARTED / IN_PROGRESS / COMPLETED
    }

    @Data
    public static class CourseStudyTime {
        private Integer courseId;
        private String courseName;
        private Integer totalMinutes;
    }
}
