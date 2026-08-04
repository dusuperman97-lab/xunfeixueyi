package com.project.platform.service;

import com.project.platform.entity.AiToolUsage;

/**
 * AI工具使用情况服务接口
 */
public interface AiToolUsageService {

    /**
     * 根据用户ID获取AI工具使用情况
     * @param userId 用户ID
     * @return AI工具使用情况，如果不存在则创建新记录
     */
    AiToolUsage getUsageByUserId(Integer userId);

    /**
     * 保存或更新AI工具使用情况
     * @param aiToolUsage AI工具使用情况数据
     * @return 保存后的数据
     */
    AiToolUsage saveOrUpdateUsage(AiToolUsage aiToolUsage);

    /**
     * 增加指定工具的使用次数
     * @param userId 用户ID
     * @param toolType 工具类型
     */
    void incrementUsageCount(Integer userId, AiToolUsage.ToolType toolType);

    /**
     * 增加指定工具的使用时长
     * @param userId 用户ID
     * @param toolType 工具类型
     * @param minutes 时长（分钟）
     */
    void incrementUsageDuration(Integer userId, AiToolUsage.ToolType toolType, Integer minutes);

    /**
     * 更新工具的最近使用时间
     * @param userId 用户ID
     * @param toolType 工具类型
     */
    void updateLastUsageTime(Integer userId, AiToolUsage.ToolType toolType);

    /**
     * 删除用户的AI工具使用情况记录
     * @param userId 用户ID
     */
    void deleteUsage(Integer userId);

    /**
     * 计算并更新使用偏好和评估指标
     * @param userId 用户ID
     */
    void analyzeAndUpdateMetrics(Integer userId);
}
