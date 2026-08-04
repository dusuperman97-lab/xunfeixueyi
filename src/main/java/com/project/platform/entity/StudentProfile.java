package com.project.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 学生画像实体 - 基于前端页面展示需求构建的多维度学习特征画像
 */
@Data
public class StudentProfile {
    private Integer id;
    private Integer studentId;              // 学生ID

    // ============ 基本信息维度 ============
    private String username;                // 用户名
    private String nickname;                // 昵称
    private String email;                   // 邮箱
    private String major;                   // 专业方向
    private String grade;                   // 年级（如：2021级本科生）
    private String learningGoal;            // 学习目标
    private String learningStyle;           // 学习风格
    private String weaknessPoints;          // 薄弱知识点（JSON数组）
    private String preferredStyle;          // 偏好学习风格

    // ============ 知识基础维度 (0-100分) ============
    private Integer conceptClarity;         // 概念清晰度
    private Integer memoryStability;        // 记忆稳固性
    private Integer transferAbility;        // 迁移能力
    private Integer structuralIntegrity;    // 结构完整性
    private Integer knowledgeDepth;         // 知识深度

    // ============ 认知风格维度 (0-100分，六维雷达图) ============
    private Integer informationProcessing;  // 信息加工（整体加工/序列加工）
    private Integer knowledgeRepresentation;// 知识表征（视觉倾向/言语倾向）
    private Integer learningInitiative;     // 学习主动性（主动建构/被动接受）
    private Integer cognitiveControl;       // 认知控制（反思型/冲动型）
    private Integer practiceOrientation;    // 实践导向（工程实践/理论导向）
    private Integer taskManagement;         // 任务管理（规划型/随机型）
    private String cognitiveStyleSummary;   // 认知风格总结描述

    // ============ 学习投入度维度 ============
    private String weeklyStudyData;         // 周学习数据（JSON格式：每日视频学习时长、练习时长、专注度）
    private Double avgDailyStudyMinutes;    // 日均学习时长（分钟）
    private Double avgConcentration;        // 平均专注度（0-100分）

    // ============ 易错偏好维度 (JSON格式存储) ============
    private String errorPreferences;        // 易错类型偏好（JSON数组：包含错误类型、频率、出现率、描述、标签等）
    private Integer errorTypeCount;         // 偏好错误类型数量
    private Integer highFrequencyErrorCount;// 高频次错误数量
    private Double avgErrorRate;            // 平均出现率

    // ============ 知识获取偏好维度 (0-100%) ============
    private Integer textPreference;         // 偏文字
    private Integer imagePreference;        // 偏图像
    private Integer examplePreference;      // 偏示例
    private Integer videoPreference;        // 偏视频

    // ============ 学科兴趣领域维度 ============
    private String subjectInterests;        // 学科兴趣数据（JSON格式：包含领域名称、分数、等级、图标等）
    private String interestSummary;         // 兴趣领域总结描述
    private String recommendedPath;         // 推荐学习路径（JSON格式：包含阶段、标题、描述）

    // ============ 元数据 ============
    private Integer version;                // 版本号（随学习递增）
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
