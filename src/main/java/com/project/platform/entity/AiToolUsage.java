package com.project.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI工具使用情况实体 - 用于记录和分析学生对平台各种AI工具的使用情况
 * 用于评估学生的学习效果和学习行为分析
 */
@Data
public class AiToolUsage {
    private Integer id;
    private Integer userId;                      // 用户ID
    
    // ============ AI工具使用次数统计 ============
    private Integer chatUsageCount;              // 大模型对话使用次数
    private Integer pptUsageCount;               // PPT生成使用次数
    private Integer mindmapUsageCount;           // 思维导图生成使用次数
    private Integer resourceRecommendUsageCount; // 资源推荐/搜索使用次数
    private Integer immersiveCourseUsageCount;   // 沉浸式课程学习使用次数
    private Integer virtualTeacherUsageCount;    // 虚拟数字人教师使用次数
    
    // ============ 使用时长统计（分钟） ============
    private Integer chatDurationMinutes;         // 大模型对话累计时长
    private Integer pptDurationMinutes;          // PPT生成累计时长
    private Integer mindmapDurationMinutes;      // 思维导图生成累计时长
    private Integer resourceDurationMinutes;     // 资源推荐累计时长
    private Integer courseDurationMinutes;       // 沉浸式课程累计时长
    private Integer teacherDurationMinutes;      // 虚拟教师累计时长
    
    // ============ 使用频率统计 ============
    private Integer totalUsageCount;             // 总使用次数（所有工具合计）
    private Integer totalDurationMinutes;        // 总使用时长（所有工具合计）
    private Double avgDailyUsageCount;           // 日均使用次数
    private Double avgSessionDurationMinutes;    // 平均单次使用时长
    
    // ============ 最近使用记录 ============
    private LocalDateTime lastChatTime;          // 最近一次大模型对话时间
    private LocalDateTime lastPptTime;           // 最近一次PPT生成时间
    private LocalDateTime lastMindmapTime;       // 最近一次思维导图生成时间
    private LocalDateTime lastResourceTime;      // 最近一次资源推荐时间
    private LocalDateTime lastCourseTime;        // 最近一次沉浸式课程时间
    private LocalDateTime lastTeacherTime;       // 最近一次虚拟教师时间
    private LocalDateTime lastUsageTime;         // 最近一次使用任何AI工具的时间
    
    // ============ 使用偏好分析 ============
    private String mostUsedTool;                 // 最常使用的工具类型
    private String usagePattern;                 // 使用模式描述（如：深度对话型、快速生成型等）
    private String learningStyleByAI;            // 基于AI使用行为推断的学习风格
    
    // ============ 学习效果评估指标 ============
    private Double engagementScore;              // 学习投入度评分（基于AI工具使用频率和时长）
    private Double toolMasteryLevel;             // AI工具掌握水平（0-100）
    private String aiLearningEfficiency;         // AI辅助学习效率评价
    
    // ============ 元数据 ============
    private LocalDateTime createTime;            // 记录创建时间
    private LocalDateTime updateTime;            // 最后更新时间
    
    /**
     * AI工具类型枚举
     */
    public enum ToolType {
        CHAT("大模型对话"),
        PPT("PPT生成"),
        MINDMAP("思维导图生成"),
        RESOURCE("资源推荐"),
        IMMERSIVE_COURSE("沉浸式课程学习"),
        VIRTUAL_TEACHER("虚拟数字人教师");
        
        private final String displayName;
        
        ToolType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
