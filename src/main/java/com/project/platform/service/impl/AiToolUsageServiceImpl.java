package com.project.platform.service.impl;

import com.project.platform.entity.AiToolUsage;
import com.project.platform.mapper.AiToolUsageMapper;
import com.project.platform.service.AiToolUsageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * AI工具使用情况服务实现
 */
@Slf4j
@Service
public class AiToolUsageServiceImpl implements AiToolUsageService {

    private final AiToolUsageMapper aiToolUsageMapper;

    public AiToolUsageServiceImpl(AiToolUsageMapper aiToolUsageMapper) {
        this.aiToolUsageMapper = aiToolUsageMapper;
    }

    @Override
    public AiToolUsage getUsageByUserId(Integer userId) {
        log.info("=== 查询用户AI工具使用情况，userId: {}", userId);
        AiToolUsage usage = aiToolUsageMapper.selectByUserId(userId);
        
        // 如果不存在则创建新记录
        if (usage == null) {
            log.info("=== 用户暂无AI工具使用记录，创建新记录，userId: {}", userId);
            usage = new AiToolUsage();
            usage.setUserId(userId);
            usage.setCreateTime(LocalDateTime.now());
            usage.setUpdateTime(LocalDateTime.now());
            aiToolUsageMapper.insert(usage);
            log.info("=== 创建AI工具使用情况记录成功，userId: {}, id: {}", userId, usage.getId());
        }
        
        return usage;
    }

    @Override
    public AiToolUsage saveOrUpdateUsage(AiToolUsage aiToolUsage) {
        log.info("=== 保存或更新AI工具使用情况，userId: {}", aiToolUsage.getUserId());
        
        AiToolUsage existing = aiToolUsageMapper.selectByUserId(aiToolUsage.getUserId());
        
        if (existing == null) {
            // 首次创建
            aiToolUsage.setCreateTime(LocalDateTime.now());
            aiToolUsage.setUpdateTime(LocalDateTime.now());
            aiToolUsageMapper.insert(aiToolUsage);
            log.info("=== 首次创建AI工具使用情况记录成功，userId: {}, id: {}", 
                    aiToolUsage.getUserId(), aiToolUsage.getId());
        } else {
            // 更新
            aiToolUsage.setId(existing.getId());
            aiToolUsage.setUpdateTime(LocalDateTime.now());
            aiToolUsageMapper.updateById(aiToolUsage);
            log.info("=== 更新AI工具使用情况记录成功，userId: {}, id: {}", 
                    aiToolUsage.getUserId(), aiToolUsage.getId());
        }
        
        return aiToolUsage;
    }

    @Override
    public void incrementUsageCount(Integer userId, AiToolUsage.ToolType toolType) {
        log.info("=== 增加工具使用次数，userId: {}, toolType: {}", userId, toolType);
        
        // 确保记录存在
        getUsageByUserId(userId);
        
        String columnName = getColumnNameForToolType(toolType, "count");
        aiToolUsageMapper.incrementUsageCount(userId, columnName);
        
        log.info("=== 工具使用次数增加成功，userId: {}, toolType: {}", userId, toolType);
    }

    @Override
    public void incrementUsageDuration(Integer userId, AiToolUsage.ToolType toolType, Integer minutes) {
        log.info("=== 增加工具使用时长，userId: {}, toolType: {}, minutes: {}", userId, toolType, minutes);
        
        // 确保记录存在
        getUsageByUserId(userId);
        
        String countColumn = getColumnNameForToolType(toolType, "count");
        String durationColumn = getColumnNameForToolType(toolType, "duration");
        
        aiToolUsageMapper.incrementUsageCount(userId, countColumn);
        aiToolUsageMapper.incrementUsageDuration(userId, durationColumn, minutes);
        
        log.info("=== 工具使用时长增加成功，userId: {}, toolType: {}, minutes: {}", userId, toolType, minutes);
    }

    @Override
    public void updateLastUsageTime(Integer userId, AiToolUsage.ToolType toolType) {
        log.info("=== 更新工具最近使用时间，userId: {}, toolType: {}", userId, toolType);
        
        AiToolUsage usage = getUsageByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        
        // 更新特定工具的时间
        switch (toolType) {
            case CHAT -> usage.setLastChatTime(now);
            case PPT -> usage.setLastPptTime(now);
            case MINDMAP -> usage.setLastMindmapTime(now);
            case RESOURCE -> usage.setLastResourceTime(now);
            case IMMERSIVE_COURSE -> usage.setLastCourseTime(now);
            case VIRTUAL_TEACHER -> usage.setLastTeacherTime(now);
        }
        
        // 更新总体最近使用时间
        usage.setLastUsageTime(now);
        usage.setUpdateTime(now);
        
        aiToolUsageMapper.updateById(usage);
        
        log.info("=== 工具最近使用时间更新成功，userId: {}, toolType: {}", userId, toolType);
    }

    @Override
    public void deleteUsage(Integer userId) {
        log.info("=== 删除用户AI工具使用情况记录，userId: {}", userId);
        aiToolUsageMapper.deleteByUserId(userId);
        log.info("=== 删除成功，userId: {}", userId);
    }

    @Override
    public void analyzeAndUpdateMetrics(Integer userId) {
        log.info("=== 开始分析用户AI工具使用偏好和评估指标，userId: {}", userId);
        
        AiToolUsage usage = getUsageByUserId(userId);
        
        // 1. 计算最常使用的工具
        Map<AiToolUsage.ToolType, Integer> toolUsageMap = new HashMap<>();
        toolUsageMap.put(AiToolUsage.ToolType.CHAT, usage.getChatUsageCount());
        toolUsageMap.put(AiToolUsage.ToolType.PPT, usage.getPptUsageCount());
        toolUsageMap.put(AiToolUsage.ToolType.MINDMAP, usage.getMindmapUsageCount());
        toolUsageMap.put(AiToolUsage.ToolType.RESOURCE, usage.getResourceRecommendUsageCount());
        toolUsageMap.put(AiToolUsage.ToolType.IMMERSIVE_COURSE, usage.getImmersiveCourseUsageCount());
        toolUsageMap.put(AiToolUsage.ToolType.VIRTUAL_TEACHER, usage.getVirtualTeacherUsageCount());
        
        AiToolUsage.ToolType mostUsedTool = toolUsageMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(AiToolUsage.ToolType.CHAT);
        
        usage.setMostUsedTool(mostUsedTool.getDisplayName());
        
        // 2. 计算日均使用次数
        if (usage.getCreateTime() != null && usage.getTotalUsageCount() != null) {
            long daysBetween = ChronoUnit.DAYS.between(usage.getCreateTime(), LocalDateTime.now());
            if (daysBetween > 0) {
                usage.setAvgDailyUsageCount((double) usage.getTotalUsageCount() / daysBetween);
            } else {
                usage.setAvgDailyUsageCount((double) usage.getTotalUsageCount());
            }
        }
        
        // 3. 计算平均单次使用时长
        if (usage.getTotalUsageCount() != null && usage.getTotalUsageCount() > 0 
                && usage.getTotalDurationMinutes() != null) {
            usage.setAvgSessionDurationMinutes(
                    (double) usage.getTotalDurationMinutes() / usage.getTotalUsageCount());
        }
        
        // 4. 评估学习投入度评分 (0-100)
        double engagementScore = calculateEngagementScore(usage);
        usage.setEngagementScore(engagementScore);
        
        // 5. 评估AI工具掌握水平 (0-100)
        double toolMasteryLevel = calculateToolMasteryLevel(usage);
        usage.setToolMasteryLevel(toolMasteryLevel);
        
        // 6. 推断学习风格
        String learningStyle = inferLearningStyle(usage);
        usage.setLearningStyleByAI(learningStyle);
        
        // 7. AI辅助学习效率评价
        String efficiency = evaluateAIEfficiency(usage);
        usage.setAiLearningEfficiency(efficiency);
        
        // 8. 使用模式描述
        String usagePattern = describeUsagePattern(usage);
        usage.setUsagePattern(usagePattern);
        
        // 更新记录
        usage.setUpdateTime(LocalDateTime.now());
        aiToolUsageMapper.updateById(usage);
        
        log.info("=== AI工具使用分析完成，userId: {}, engagementScore: {}, toolMastery: {}", 
                userId, engagementScore, toolMasteryLevel);
    }

    /**
     * 获取工具对应的数据库列名
     */
    private String getColumnNameForToolType(AiToolUsage.ToolType toolType, String type) {
        boolean isDuration = "duration".equals(type);
        return switch (toolType) {
            case CHAT -> isDuration ? "chat_duration_minutes" : "chat_usage_count";
            case PPT -> isDuration ? "ppt_duration_minutes" : "ppt_usage_count";
            case MINDMAP -> isDuration ? "mindmap_duration_minutes" : "mindmap_usage_count";
            case RESOURCE -> isDuration ? "resource_duration_minutes" : "resource_recommend_usage_count";
            case IMMERSIVE_COURSE -> isDuration ? "course_duration_minutes" : "immersive_course_usage_count";
            case VIRTUAL_TEACHER -> isDuration ? "teacher_duration_minutes" : "virtual_teacher_usage_count";
        };
    }

    /**
     * 计算学习投入度评分 (0-100)
     * 基于使用频次、时长、多样性等综合评估
     */
    private double calculateEngagementScore(AiToolUsage usage) {
        double score = 0.0;
        
        // 使用频次贡献 (40%)
        int totalCount = usage.getTotalUsageCount() != null ? usage.getTotalUsageCount() : 0;
        double frequencyScore = Math.min(totalCount / 100.0, 1.0) * 40;
        
        // 使用时长贡献 (40%)
        int totalDuration = usage.getTotalDurationMinutes() != null ? usage.getTotalDurationMinutes() : 0;
        double durationScore = Math.min(totalDuration / 600.0, 1.0) * 40; // 10小时满分
        
        // 工具多样性贡献 (20%)
        long usedToolsCount = java.util.Arrays.stream(AiToolUsage.ToolType.values())
                .filter(tool -> getUsageCount(usage, tool) > 0)
                .count();
        double diversityScore = (usedToolsCount / 6.0) * 20;
        
        score = frequencyScore + durationScore + diversityScore;
        
        return Math.min(Math.round(score * 10.0) / 10.0, 100.0);
    }

    /**
     * 获取特定工具的使用次数
     */
    private int getUsageCount(AiToolUsage usage, AiToolUsage.ToolType toolType) {
        return switch (toolType) {
            case CHAT -> usage.getChatUsageCount() != null ? usage.getChatUsageCount() : 0;
            case PPT -> usage.getPptUsageCount() != null ? usage.getPptUsageCount() : 0;
            case MINDMAP -> usage.getMindmapUsageCount() != null ? usage.getMindmapUsageCount() : 0;
            case RESOURCE -> usage.getResourceRecommendUsageCount() != null ? usage.getResourceRecommendUsageCount() : 0;
            case IMMERSIVE_COURSE -> usage.getImmersiveCourseUsageCount() != null ? usage.getImmersiveCourseUsageCount() : 0;
            case VIRTUAL_TEACHER -> usage.getVirtualTeacherUsageCount() != null ? usage.getVirtualTeacherUsageCount() : 0;
        };
    }

    /**
     * 计算AI工具掌握水平 (0-100)
     * 基于工具使用的深度和广度
     */
    private double calculateToolMasteryLevel(AiToolUsage usage) {
        double score = 0.0;
        
        // 总使用次数 (30%)
        int totalCount = usage.getTotalUsageCount() != null ? usage.getTotalUsageCount() : 0;
        double countScore = Math.min(totalCount / 200.0, 1.0) * 30;
        
        // 平均单次时长 (30%) - 反映使用的深度
        double avgDuration = usage.getAvgSessionDurationMinutes() != null ? usage.getAvgSessionDurationMinutes() : 0;
        double depthScore = Math.min(avgDuration / 30.0, 1.0) * 30; // 30分钟满分
        
        // 工具多样性 (40%)
        long usedToolsCount = java.util.Arrays.stream(AiToolUsage.ToolType.values())
                .filter(tool -> getUsageCount(usage, tool) > 0)
                .count();
        double breadthScore = (usedToolsCount / 6.0) * 40;
        
        score = countScore + depthScore + breadthScore;
        
        return Math.min(Math.round(score * 10.0) / 10.0, 100.0);
    }

    /**
     * 推断学习风格
     */
    private String inferLearningStyle(AiToolUsage usage) {
        int chatCount = usage.getChatUsageCount() != null ? usage.getChatUsageCount() : 0;
        int pptCount = usage.getPptUsageCount() != null ? usage.getPptUsageCount() : 0;
        int mindmapCount = usage.getMindmapUsageCount() != null ? usage.getMindmapUsageCount() : 0;
        int resourceCount = usage.getResourceRecommendUsageCount() != null ? usage.getResourceRecommendUsageCount() : 0;
        
        if (chatCount > pptCount + mindmapCount + resourceCount) {
            return "深度对话型 - 偏好通过AI对话深入探讨问题";
        } else if (pptCount > chatCount && pptCount > mindmapCount) {
            return "结构化输出型 - 偏好生成结构化的学习材料";
        } else if (mindmapCount > chatCount && mindmapCount > pptCount) {
            return "可视化思维型 - 偏好通过思维导图梳理知识";
        } else if (resourceCount > chatCount) {
            return "资源探索型 - 偏好搜索和学习外部资源";
        } else {
            return "综合平衡型 - 多样化使用各类AI工具";
        }
    }

    /**
     * 评估AI辅助学习效率
     */
    private String evaluateAIEfficiency(AiToolUsage usage) {
        double avgDuration = usage.getAvgSessionDurationMinutes() != null ? usage.getAvgSessionDurationMinutes() : 0;
        double avgDaily = usage.getAvgDailyUsageCount() != null ? usage.getAvgDailyUsageCount() : 0;
        
        if (avgDaily > 5 && avgDuration < 10) {
            return "高效型 - 高频使用且单次使用时长适中，学习效率较高";
        } else if (avgDaily < 2 && avgDuration > 30) {
            return "深度型 - 使用频次低但单次深度使用，可能在进行深度学习";
        } else if (avgDaily > 3 && avgDuration > 20) {
            return "投入型 - 高频且深度使用AI工具，学习投入度高";
        } else if (avgDaily < 1) {
            return "低活跃型 - AI工具使用频率较低，建议增加使用以提升学习效果";
        } else {
            return "正常型 - AI工具使用情况正常，保持当前学习节奏";
        }
    }

    /**
     * 描述使用模式
     */
    private String describeUsagePattern(AiToolUsage usage) {
        int totalCount = usage.getTotalUsageCount() != null ? usage.getTotalUsageCount() : 0;
        int totalDuration = usage.getTotalDurationMinutes() != null ? usage.getTotalDurationMinutes() : 0;
        
        if (totalCount == 0) {
            return "尚未开始使用AI工具";
        } else if (totalCount < 10) {
            return "探索阶段 - 刚开始尝试使用AI工具";
        } else if (totalDuration < 60 && totalCount > 10) {
            return "快速使用型 - 频繁但短暂地使用AI工具";
        } else if (totalDuration > 300) {
            return "深度依赖型 - 大量使用AI工具辅助学习";
        } else {
            return "稳定使用型 - 保持稳定的人工智能辅助学习模式";
        }
    }
}
