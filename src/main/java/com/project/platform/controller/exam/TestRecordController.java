package com.project.platform.controller.exam;

import com.project.platform.dto.QuestionTestRecordDTO;
import com.project.platform.entity.KnowledgePointStats;
import com.project.platform.entity.QuestionTestRecord;
import com.project.platform.service.KnowledgePointStatsService;
import com.project.platform.service.QuestionTestRecordService;
import com.project.platform.vo.ResponseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 测试记录控制器
 * 提供测试记录查询和统计分析接口
 */
@RestController
@RequestMapping("/api/test-record")
@CrossOrigin
public class TestRecordController {

    @Autowired
    private QuestionTestRecordService testRecordService;

    @Autowired
    private KnowledgePointStatsService knowledgePointStatsService;

    /**
     * 批量保存测试记录
     */
    @PostMapping("/save")
    public ResponseVO<Void> saveTestRecords(@RequestBody List<QuestionTestRecord> records) {
        try {
            testRecordService.saveTestRecords(records);
            return ResponseVO.ok(null);
        } catch (Exception e) {
            return ResponseVO.error("保存测试记录失败：" + e.getMessage());
        }
    }

    /**
     * 根据用户ID和小节ID查询测试记录
     */
    @GetMapping("/records")
    public ResponseVO<List<QuestionTestRecord>> getTestRecords(
            @RequestParam Integer userId,
            @RequestParam Integer sectionId) {
        try {
            List<QuestionTestRecord> records = testRecordService.getTestRecords(userId, sectionId);
            return ResponseVO.ok(records);
        } catch (Exception e) {
            return ResponseVO.error("查询测试记录失败：" + e.getMessage());
        }
    }

    /**
     * 根据测试批次号查询测试记录
     */
    @GetMapping("/records/batch/{testBatchNo}")
    public ResponseVO<List<QuestionTestRecord>> getTestRecordsByBatchNo(
            @PathVariable String testBatchNo) {
        try {
            List<QuestionTestRecord> records = testRecordService.getTestRecordsByBatchNo(testBatchNo);
            return ResponseVO.ok(records);
        } catch (Exception e) {
            return ResponseVO.error("查询测试记录失败：" + e.getMessage());
        }
    }

    /**
     * 获取知识点正确率统计
     */
    @GetMapping("/knowledge-point/accuracy")
    public ResponseVO<List<Map<String, Object>>> getKnowledgePointAccuracy(
            @RequestParam Integer userId,
            @RequestParam Integer sectionId) {
        try {
            List<Map<String, Object>> accuracyList = testRecordService.getKnowledgePointAccuracy(userId, sectionId);
            return ResponseVO.ok(accuracyList);
        } catch (Exception e) {
            return ResponseVO.error("查询知识点正确率失败：" + e.getMessage());
        }
    }

    /**
     * 获取测试历史
     */
    @GetMapping("/history")
    public ResponseVO<List<Map<String, Object>>> getTestHistory(
            @RequestParam Integer userId,
            @RequestParam(required = false) Integer sectionId) {
        try {
            List<Map<String, Object>> history = testRecordService.getTestHistory(userId, sectionId);
            return ResponseVO.ok(history);
        } catch (Exception e) {
            return ResponseVO.error("查询测试历史失败：" + e.getMessage());
        }
    }

    /**
     * 根据用户ID和小节ID查询知识点统计
     */
    @GetMapping("/knowledge-point/stats")
    public ResponseVO<List<KnowledgePointStats>> getKnowledgePointStats(
            @RequestParam Integer userId,
            @RequestParam Integer sectionId) {
        try {
            List<KnowledgePointStats> stats = knowledgePointStatsService.getKnowledgePointStats(userId, sectionId);
            return ResponseVO.ok(stats);
        } catch (Exception e) {
            return ResponseVO.error("查询知识点统计失败：" + e.getMessage());
        }
    }

    /**
     * 根据用户ID查询所有知识点统计
     */
    @GetMapping("/knowledge-point/all")
    public ResponseVO<List<KnowledgePointStats>> getAllKnowledgePointStats(
            @RequestParam Integer userId) {
        try {
            List<KnowledgePointStats> stats = knowledgePointStatsService.getAllKnowledgePointStats(userId);
            return ResponseVO.ok(stats);
        } catch (Exception e) {
            return ResponseVO.error("查询知识点统计失败：" + e.getMessage());
        }
    }

    /**
     * 根据掌握程度查询知识点
     */
    @GetMapping("/knowledge-point/mastery")
    public ResponseVO<List<KnowledgePointStats>> getKnowledgePointsByMasteryLevel(
            @RequestParam Integer userId,
            @RequestParam String masteryLevel) {
        try {
            List<KnowledgePointStats> stats = knowledgePointStatsService.getKnowledgePointsByMasteryLevel(userId, masteryLevel);
            return ResponseVO.ok(stats);
        } catch (Exception e) {
            return ResponseVO.error("查询知识点统计失败：" + e.getMessage());
        }
    }


    /**
     * 查询用户的练习情况记录
     */
    @GetMapping("/practice-records")
    public ResponseVO<List<QuestionTestRecordDTO>> getPracticeRecords(
            @RequestParam Integer userId) {
        try {
            List<QuestionTestRecordDTO> records = testRecordService.getPracticeRecords(userId);
            return ResponseVO.ok(records);
        } catch (Exception e) {
            return ResponseVO.error("查询练习情况记录失败：" + e.getMessage());
        }
    }

    /**
     * 查询用户的错题记录
     */
    @GetMapping("/wrong-questions")
    public ResponseVO<List<QuestionTestRecord>> getWrongQuestions(
            @RequestParam Integer userId,
            @RequestParam(required = false) Integer sectionId) {
        try {
            List<QuestionTestRecord> wrongQuestions = testRecordService.getWrongQuestions(userId, sectionId);
            return ResponseVO.ok(wrongQuestions);
        } catch (Exception e) {
            return ResponseVO.error("查询错题记录失败：" + e.getMessage());
        }
    }

}
