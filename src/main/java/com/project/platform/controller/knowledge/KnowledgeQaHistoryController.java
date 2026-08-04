package com.project.platform.controller.knowledge;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.entity.QaHistory;
import com.project.platform.service.QaHistoryService;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库问答历史控制器
 */
@RestController
@RequestMapping("/knowledge/rag/history")
@Slf4j
public class KnowledgeQaHistoryController {

    @Resource
    private QaHistoryService qaHistoryService;

    @Resource
    private Tools tools;

    /**
     * 获取用户问答历史列表
     *
     * @param kbId     知识库ID（可选，不传则查询全部知识库）
     * @param page     页码（从1开始，默认1）
     * @param pageSize 每页数量（默认10）
     */
    @GetMapping
    public ResponseVO<List<QaHistory>> list(
            @RequestParam(required = false) Long kbId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Long studentId = Long.valueOf(tools.getCurrentUserId());
            log.info("查询问答历史: studentId={}, kbId={}, page={}, pageSize={}",
                    studentId, kbId, page, pageSize);

            List<QaHistory> list;
            if (kbId != null) {
                list = qaHistoryService.getHistoryByStudentAndKb(studentId, kbId, page, pageSize);
            } else {
                list = qaHistoryService.getHistoryByStudent(studentId, page, pageSize);
            }
            return ResponseVO.ok(list);
        } catch (Exception e) {
            log.error("查询问答历史失败", e);
            return ResponseVO.error(e.getMessage());
        }
    }

    /**
     * 提交答案反馈
     *
     * @param historyId 问答记录ID
     * @param feedback  反馈类型：1-赞, -1-踩
     * @param comment   反馈备注（可选）
     */
    @PostMapping("/{historyId}/feedback")
    public ResponseVO<Void> feedback(
            @PathVariable Long historyId,
            @RequestParam Integer feedback,
            @RequestParam(required = false) String comment) {
        try {
            Long studentId = Long.valueOf(tools.getCurrentUserId());
            log.info("提交问答反馈: historyId={}, studentId={}, feedback={}", historyId, studentId, feedback);

            qaHistoryService.submitFeedback(historyId, studentId, feedback, comment);
            return ResponseVO.ok();
        } catch (Exception e) {
            log.error("提交问答反馈失败", e);
            return ResponseVO.error(e.getMessage());
        }
    }

    /**
     * 删除问答记录
     */
    @DeleteMapping("/{historyId}")
    public ResponseVO<Void> delete(@PathVariable Long historyId) {
        try {
            Long studentId = Long.valueOf(tools.getCurrentUserId());
            log.info("删除问答历史: historyId={}, studentId={}", historyId, studentId);

            qaHistoryService.deleteHistory(historyId, studentId);
            return ResponseVO.ok();
        } catch (Exception e) {
            log.error("删除问答历史失败", e);
            return ResponseVO.error(e.getMessage());
        }
    }
}
