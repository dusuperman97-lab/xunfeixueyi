package com.project.platform.controller.exam;


import com.project.platform.entity.SectionTestSummary;
import com.project.platform.service.SectionService;
import com.project.platform.service.SectionTestSummaryService;
import com.project.platform.vo.ResponseVO;
import com.project.platform.vo.TestHistoryDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 小节测试汇总控制器
 * 提供测试汇总查询和管理接口
 */
@RestController
@RequestMapping("/api/section-test-summary")
@CrossOrigin
@Slf4j
public class SectionTestSummaryController {

    @Autowired
    private SectionTestSummaryService sectionTestSummaryService;

    @Autowired
    private SectionService sectionService;

    /**
     * 创建或更新测试汇总记录
     */
    @PostMapping("/save")
    public ResponseVO<Void> saveTestSummary(@RequestBody SectionTestSummary summary) {
        try {
            sectionTestSummaryService.createOrUpdateSummary(summary);
            sectionService.learnComplete(summary.getUserId(), summary.getSectionId());
            return ResponseVO.ok(null);
        } catch (Exception e) {
            return ResponseVO.error("保存测试汇总失败：" + e.getMessage());
        }
    }

    /**
     * 根据用户ID和小节ID查询测试汇总
     */
    @GetMapping("/summaries")
    public ResponseVO<List<SectionTestSummary>> getTestSummaries(
            @RequestParam Integer userId,
            @RequestParam Integer sectionId) {
        try {
            List<SectionTestSummary> summaries = sectionTestSummaryService.getTestSummaries(userId, sectionId);
            return ResponseVO.ok(summaries);
        } catch (Exception e) {
            return ResponseVO.error("查询测试汇总失败：" + e.getMessage());
        }
    }

    /**
     * 根据测试批次号查询测试汇总
     */
    @GetMapping("/summary/batch/{testBatchNo}")
    public ResponseVO<SectionTestSummary> getTestSummaryByBatchNo(
            @PathVariable String testBatchNo) {
        try {
            SectionTestSummary summary = sectionTestSummaryService.getTestSummaryByBatchNo(testBatchNo);
            return ResponseVO.ok(summary);
        } catch (Exception e) {
            return ResponseVO.error("查询测试汇总失败：" + e.getMessage());
        }
    }

    /**
     * 查询用户的测试历史汇总
     */
    @GetMapping("/history")
    public ResponseVO<List<SectionTestSummary>> getTestHistory(
            @RequestParam Integer userId) {
        try {
            List<SectionTestSummary> history = sectionTestSummaryService.getTestHistory(userId);
            return ResponseVO.ok(history);
        } catch (Exception e) {
            return ResponseVO.error("查询测试历史失败：" + e.getMessage());
        }
    }

    /**
     * 查询随堂测试历史详情(包含题目列表)
     * @param userId 用户ID
     * @param sectionId 小节ID
     */
    @GetMapping("/section/history")
    public ResponseVO<List<TestHistoryDetailVO>> getSectionTestHistoryDetail(
            @RequestParam Integer userId,
            @RequestParam Integer sectionId) {
        try {
            log.info("=== 接收到随堂测试历史查询请求，用户ID：{}，小节ID：{}", userId, sectionId);
            List<TestHistoryDetailVO> history = sectionTestSummaryService.getSectionTestHistoryDetail(userId, sectionId);
            return ResponseVO.ok(history);
        } catch (Exception e) {
            log.error("=== 查询随堂测试历史失败，用户ID：{}，小节ID：{}", userId, sectionId, e);
            return ResponseVO.error("查询随堂测试历史失败：" + e.getMessage());
        }
    }

    /**
     * 查询课程考试历史详情(包含题目列表)
     * @param userId 用户ID
     * @param courseId 课程ID
     */
    @GetMapping("/course/history")
    public ResponseVO<List<TestHistoryDetailVO>> getCourseExamHistoryDetail(
            @RequestParam Integer userId,
            @RequestParam Integer courseId) {
        try {
            log.info("=== 接收到课程考试历史查询请求，用户ID：{}，课程ID：{}", userId, courseId);
            List<TestHistoryDetailVO> history = sectionTestSummaryService.getCourseExamHistoryDetail(userId, courseId);
            return ResponseVO.ok(history);
        } catch (Exception e) {
            log.error("=== 查询课程考试历史失败，用户ID：{}，课程ID：{}", userId, courseId, e);
            return ResponseVO.error("查询课程考试历史失败：" + e.getMessage());
        }
    }

}
