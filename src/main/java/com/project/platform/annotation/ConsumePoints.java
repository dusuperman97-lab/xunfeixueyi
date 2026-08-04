package com.project.platform.annotation;

import java.lang.annotation.*;

/**
 * 积分消耗注解
 * <p>
 * 标记在 Controller 方法上，表示调用该方法需要消耗用户积分。
 * AOP 切面会在方法执行前校验余额并扣减积分，余额不足时抛出异常阻止方法执行。
 * </p>
 *
 * <pre>
 * 使用示例：
 * &#64;PostMapping("/generate-smart")
 * &#64;ConsumePoints(value = 10, bizType = "COURSE")
 * public ResponseVO generateCourseSmart(...) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConsumePoints {

    /**
     * 消耗积分数
     */
    int value();

    /**
     * 业务类型，用于积分流水追踪
     * <p>可选值：COURSE, PPT, MINDMAP, CHAT, EXAM, VERIFY</p>
     */
    String bizType();
}
