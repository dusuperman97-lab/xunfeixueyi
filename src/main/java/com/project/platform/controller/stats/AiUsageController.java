package com.project.platform.controller.stats;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.entity.AiToolUsage;
import com.project.platform.service.AiToolUsageService;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI工具使用统计控制器
 * 处理AI工具使用情况的查询和分析
 */
@RestController
@RequestMapping("/xunfei/ai-usage")
@Slf4j
public class AiUsageController {

    @Resource
    private AiToolUsageService aiToolUsageService;

    @Resource
    private Tools tools;

    /**
     * 获取用户AI工具使用情况统计
     * 返回各种AI工具的使用次数、时长等信息，用于雷达图展示和学习效果分析
     */
    @GetMapping("/get-usage")
    public ResponseVO<AiToolUsage> getAiToolUsage() {
        Integer userId = tools.getCurrentUserIdInteger();
        log.info("=== 获取用户AI工具使用情况！userId: {}", userId);

        try {
            AiToolUsage usage = aiToolUsageService.getUsageByUserId(userId);

            // 自动分析和更新使用指标
            aiToolUsageService.analyzeAndUpdateMetrics(userId);

            // 重新获取更新后的数据
            usage = aiToolUsageService.getUsageByUserId(userId);

            log.info("=== AI工具使用情况获取成功！userId: {}, 总使用次数: {}, 投入度评分: {}",
                    userId, usage.getTotalUsageCount(), usage.getEngagementScore());

            return ResponseVO.ok(usage);
        } catch (Exception e) {
            log.error("获取AI工具使用情况失败", e);
            return ResponseVO.fail(500, "获取失败：" + e.getMessage());
        }
    }

    /**
     * 记录AI工具使用时长（由前端页面定时上报）
     * <p>
     * 前端在使用AI工具页面时，定时（每2-5分钟）将使用时长上报到此接口。
     * 工具键映射：chat→大模型对话, ppt→PPT生成, mindmap→思维导图,
     * resource→资源推荐, course→沉浸式课程, teacher→虚拟教师
     *
     * @param toolKey 工具键名
     * @param durationMinutes 使用时长（分钟），支持小数
     */
    @PostMapping("/record-duration")
    public ResponseVO<Void> recordDuration(@RequestParam String toolKey,
                                           @RequestParam Integer durationMinutes) {
        Integer userId = tools.getCurrentUserIdInteger();
        log.info("=== 记录AI工具使用时长，userId：{}，toolKey：{}，minutes：{}", userId, toolKey, durationMinutes);

        try {
            AiToolUsage.ToolType toolType = resolveToolType(toolKey);
            if (toolType == null) {
                return ResponseVO.fail(400, "未知的工具类型：" + toolKey);
            }
            if (durationMinutes == null || durationMinutes <= 0) {
                return ResponseVO.fail(400, "使用时长必须大于0");
            }
            aiToolUsageService.incrementUsageDuration(userId, toolType, durationMinutes);
            aiToolUsageService.updateLastUsageTime(userId, toolType);
            return ResponseVO.ok(null);
        } catch (Exception e) {
            log.error("记录AI工具使用时长失败，userId：{}，toolKey：{}", userId, toolKey, e);
            return ResponseVO.error("记录失败：" + e.getMessage());
        }
    }

    private AiToolUsage.ToolType resolveToolType(String toolKey) {
        return switch (toolKey) {
            case "chat" -> AiToolUsage.ToolType.CHAT;
            case "ppt" -> AiToolUsage.ToolType.PPT;
            case "mindmap" -> AiToolUsage.ToolType.MINDMAP;
            case "resource" -> AiToolUsage.ToolType.RESOURCE;
            case "course" -> AiToolUsage.ToolType.IMMERSIVE_COURSE;
            case "teacher" -> AiToolUsage.ToolType.VIRTUAL_TEACHER;
            default -> null;
        };
    }

    /**
     * 分析并更新用户AI工具使用情况
     * 手动触发分析，更新使用偏好和评估指标
     */
    @PostMapping("/analyze")
    public ResponseVO<AiToolUsage> analyzeAiToolUsage() {
        Integer userId = tools.getCurrentUserIdInteger();
        log.info("=== 手动分析用户AI工具使用情况！userId: {}", userId);

        try {
            // 执行分析
            aiToolUsageService.analyzeAndUpdateMetrics(userId);

            // 获取更新后的数据
            AiToolUsage usage = aiToolUsageService.getUsageByUserId(userId);

            log.info("=== AI工具使用情况分析完成！userId: {}, 学习风格: {}, 使用模式: {}",
                    userId, usage.getLearningStyleByAI(), usage.getUsagePattern());

            return ResponseVO.ok(usage);
        } catch (Exception e) {
            log.error("分析AI工具使用情况失败", e);
            return ResponseVO.fail(500, "分析失败：" + e.getMessage());
        }
    }
}
