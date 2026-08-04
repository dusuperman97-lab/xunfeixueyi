package com.project.platform.annotation;

import com.project.platform.entity.AiToolUsage;

import java.lang.annotation.*;

/**
 * AI工具使用追踪注解
 * 用于标记需要记录使用情况的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TrackAiUsage {
    /**
     * AI工具类型
     */
    AiToolUsage.ToolType value();
}
