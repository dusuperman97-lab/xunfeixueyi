package com.project.platform.aspect;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.annotation.TrackAiUsage;
import com.project.platform.entity.AiToolUsage;
import com.project.platform.service.AiToolUsageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * AI工具使用统计切面
 * 自动记录AI工具的使用次数和时间
 */
@Aspect
@Component
@Slf4j
public class AiUsageTrackingAspect {

    @Resource
    private AiToolUsageService aiToolUsageService;

    @Resource
    private Tools tools;

    /**
     * 定义切点：所有标记了@TrackAiUsage注解的方法
     */
    @Pointcut("@annotation(trackAiUsage)")
    public void aiUsagePointcut(TrackAiUsage trackAiUsage) {
    }

    /**
     * 在方法成功返回后记录使用情况
     */
    @AfterReturning(pointcut = "aiUsagePointcut(trackAiUsage)", returning = "result")
    public void trackUsage(JoinPoint joinPoint, TrackAiUsage trackAiUsage, Object result) {
        try {
            Integer userId = tools.getCurrentUserIdInteger();
            AiToolUsage.ToolType toolType = trackAiUsage.value();

            // 记录使用次数
            aiToolUsageService.incrementUsageCount(userId, toolType);
            // 更新最后使用时间
            aiToolUsageService.updateLastUsageTime(userId, toolType);

            log.debug("=== AI工具使用记录成功！userId: {}, toolType: {}", userId, toolType);
        } catch (Exception e) {
            log.error("=== 记录AI工具使用情况失败", e);
        }
    }
}
