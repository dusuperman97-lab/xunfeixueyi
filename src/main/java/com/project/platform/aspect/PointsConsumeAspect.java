package com.project.platform.aspect;

import com.project.platform.annotation.ConsumePoints;
import com.project.platform.exception.CustomException;
import com.project.platform.service.MemberPointsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 积分消耗切面
 * <p>
 * 拦截所有标注了 {@link ConsumePoints} 注解的方法，
 * 在方法执行前校验用户积分余额并执行扣减。
 * 余额不足时抛出 {@link CustomException}，阻止方法执行。
 * 业务方法执行失败时，自动退还已扣减的积分。
 * </p>
 */
@Aspect
@Component
@Slf4j
public class PointsConsumeAspect {

    @Resource
    private MemberPointsService memberPointsService;

    /**
     * 切点：所有标记了 @ConsumePoints 注解的方法
     */
    @Pointcut("@annotation(com.project.platform.annotation.ConsumePoints)")
    public void consumePointsPointcut() {
    }

    /**
     * 环绕通知：方法执行前校验并扣减积分，方法失败时退还积分
     */
    @Around("consumePointsPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        ConsumePoints consumePoints = method.getAnnotation(ConsumePoints.class);

        int pointsToConsume = consumePoints.value();
        String bizType = consumePoints.bizType();

        log.info("=== 积分扣减校验开始！消耗积分: {}, 业务类型: {}", pointsToConsume, bizType);

        // 2. 扣减积分（内部会校验余额，不足时抛出异常）
        try {
            memberPointsService.deductPoints(pointsToConsume, bizType);
            log.info("=== 积分扣减成功！消耗积分: {}, 业务类型: {}", pointsToConsume, bizType);
        } catch (CustomException e) {
            log.warn("=== 积分扣减失败：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("=== 积分扣减异常", e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "积分扣减失败，请稍后重试");
        }

        // 3. 执行原方法（失败时自动退还积分）
        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            // 业务执行失败，退还积分
            try {
                memberPointsService.refundPoints(pointsToConsume, bizType, e.getMessage());
                log.warn("=== 业务执行失败，已退还积分: {}, 业务类型: {}, 原因: {}",
                        pointsToConsume, bizType, e.getMessage());
            } catch (Exception refundEx) {
                log.error("=== 积分退还失败！userId 可能损失: {} 积分, 原因: {}",
                        pointsToConsume, refundEx.getMessage());
            }
            throw e;
        }
    }
}
