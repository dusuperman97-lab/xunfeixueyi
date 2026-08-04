package com.project.platform.controller.stats;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.service.StudentEvaluationService;
import com.project.platform.vo.ResponseVO;
import com.project.platform.vo.StudentEvaluationVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 学生效果评估控制器
 * 提供学习行为分析、练习测试统计、资源使用反馈等综合评估数据的查询接口
 */
@RestController
@RequestMapping("/api/evaluation")
@CrossOrigin
@Slf4j
public class StudentEvaluationController {

    @Resource
    private StudentEvaluationService studentEvaluationService;

    @Resource
    private Tools tools;

    /**
     * 获取当前学生的效果评估仪表盘数据
     * 聚合 AI 工具使用、练习测试趋势、知识点掌握、学习时长、问答反馈等多维度数据
     *
     * @param courseId 课程ID（可选，不传则统计所有课程）
     * @param days     统计近N天（默认30）
     */
    @GetMapping("/dashboard")
    public ResponseVO<StudentEvaluationVO> getDashboard(
            @RequestParam(required = false) Integer courseId,
            @RequestParam(defaultValue = "30") Integer days) {
        Integer userId = tools.getCurrentUserIdInteger();
        log.info("=== 获取学生效果评估数据，userId：{}，courseId：{}，days：{}", userId, courseId, days);
        try {
            StudentEvaluationVO vo = studentEvaluationService.getDashboard(userId, courseId, days);
            return ResponseVO.ok(vo);
        } catch (Exception e) {
            log.error("=== 获取效果评估数据失败，userId：{}", userId, e);
            return ResponseVO.error("获取效果评估数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取指定学生的效果评估仪表盘数据（管理员视角）
     */
    @GetMapping("/dashboard/{userId}")
    public ResponseVO<StudentEvaluationVO> getDashboardByUser(
            @PathVariable Integer userId,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(defaultValue = "30") Integer days) {
        log.info("=== 管理员查询学生效果评估数据，userId：{}，courseId：{}，days：{}", userId, courseId, days);
        try {
            StudentEvaluationVO vo = studentEvaluationService.getDashboard(userId, courseId, days);
            return ResponseVO.ok(vo);
        } catch (Exception e) {
            log.error("=== 获取效果评估数据失败，userId：{}", userId, e);
            return ResponseVO.error("获取效果评估数据失败：" + e.getMessage());
        }
    }
}
