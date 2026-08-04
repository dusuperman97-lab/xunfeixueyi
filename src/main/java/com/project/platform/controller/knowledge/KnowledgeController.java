package com.project.platform.controller.knowledge;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.entity.KnowledgeBase;
import com.project.platform.service.KnowledgeBaseService;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 个人知识库管理接口
 * <p>
 * 安全策略：所有接口从 CurrentUserThreadLocal 获取当前登录用户 ID，
 * 不信任前端传入的 studentId，防止横向越权访问。
 */
@RestController
@RequestMapping("/knowledge")
@Slf4j
public class KnowledgeController {

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private Tools tools;

    /**
     * 创建知识库
     */
    @PostMapping("/create")
    public ResponseVO<KnowledgeBase> create(
            @RequestParam String name,
            @RequestParam(required = false) String description) {
        try {
            Long studentId = Long.valueOf(tools.getCurrentUserId());
            KnowledgeBase kb = knowledgeBaseService.createKnowledgeBase(studentId, name, description);
            return ResponseVO.ok(kb);
        } catch (Exception e) {
            log.error("创建知识库失败", e);
            return ResponseVO.error(e.getMessage());
        }
    }

    /**
     * 更新知识库
     */
    @PutMapping("/update/{id}")
    public ResponseVO<Void> update(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description) {
        try {
            Long studentId = Long.valueOf(tools.getCurrentUserId());
            knowledgeBaseService.updateKnowledgeBase(id, studentId, name, description);
            return ResponseVO.ok();
        } catch (Exception e) {
            log.error("更新知识库失败", e);
            return ResponseVO.error(e.getMessage());
        }
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/delete/{id}")
    public ResponseVO<Void> delete(@PathVariable Long id) {
        try {
            Long studentId = Long.valueOf(tools.getCurrentUserId());
            knowledgeBaseService.deleteKnowledgeBase(id, studentId);
            return ResponseVO.ok();
        } catch (Exception e) {
            log.error("删除知识库失败", e);
            return ResponseVO.error(e.getMessage());
        }
    }

    /**
     * 获取用户的所有知识库
     */
    @GetMapping("/list")
    public ResponseVO<List<KnowledgeBase>> list() {
        try {
            Long studentId = Long.valueOf(tools.getCurrentUserId());
            List<KnowledgeBase> list = knowledgeBaseService.getKnowledgeBases(studentId);
            return ResponseVO.ok(list);
        } catch (Exception e) {
            log.error("获取知识库列表失败", e);
            return ResponseVO.error(e.getMessage());
        }
    }

    /**
     * 获取知识库详情
     */
    @GetMapping("/detail/{id}")
    public ResponseVO<KnowledgeBase> detail(@PathVariable Long id) {
        try {
            Long studentId = Long.valueOf(tools.getCurrentUserId());
            KnowledgeBase kb = knowledgeBaseService.getKnowledgeBaseById(id, studentId);
            if (kb != null) {
                return ResponseVO.ok(kb);
            }
            return ResponseVO.error("知识库不存在");
        } catch (Exception e) {
            log.error("获取知识库详情失败", e);
            return ResponseVO.error(e.getMessage());
        }
    }
}
