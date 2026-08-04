package com.project.platform.controller.knowledge;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.entity.KnowledgeDocument;
import com.project.platform.service.KnowledgeDocumentService;
import com.project.platform.vo.ResponseVO;
import dev.langchain4j.data.segment.TextSegment;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识文档管理接口
 * <p>
 * 安全策略：所有接口从 CurrentUserThreadLocal 获取当前登录用户 ID，
 * 不信任前端传入的 studentId，防止横向越权访问。
 */
@RestController
@RequestMapping("/knowledge/document")
@Slf4j
public class KnowledgeDocumentController {

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Resource
    private Tools tools;


    /**
     * 上传文档到知识库
     */
    @PostMapping("/upload/{kbId}")
    public ResponseVO<KnowledgeDocument> upload(
            @PathVariable Long kbId,
            @RequestParam MultipartFile file) {
        try {
            Long studentId = Long.valueOf(tools.getCurrentUserId());
            KnowledgeDocument doc = knowledgeDocumentService.uploadDocument(kbId, studentId, file);
            return ResponseVO.ok(doc);
        } catch (Exception e) {
            log.error("上传文档失败", e);
            return ResponseVO.error(e.getMessage());
        }
    }

    /**
     * 获取知识库的所有文档
     */
    @GetMapping("/list/{kbId}")
    public ResponseVO<List<KnowledgeDocument>> list(@PathVariable Long kbId) {
        try {
            Long studentId = Long.valueOf(tools.getCurrentUserId());
            List<KnowledgeDocument> list = knowledgeDocumentService.getDocumentsByKbId(kbId, studentId);
            return ResponseVO.ok(list);
        } catch (Exception e) {
            log.error("获取文档列表失败", e);
            return ResponseVO.error(e.getMessage());
        }
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/delete/{id}")
    public ResponseVO<Void> delete(@PathVariable Long id) {
        try {
            Long studentId = Long.valueOf(tools.getCurrentUserId());
            knowledgeDocumentService.deleteDocument(id, studentId);
            return ResponseVO.ok();
        } catch (Exception e) {
            log.error("删除文档失败", e);
            return ResponseVO.error(e.getMessage());
        }
    }

    /**
     * 获取用户的所有文档（跨知识库）
     */
    @GetMapping("/all")
    public ResponseVO<List<KnowledgeDocument>> all() {
        try {
            Long studentId = Long.valueOf(tools.getCurrentUserId());
            List<KnowledgeDocument> list = knowledgeDocumentService.getDocumentsByStudentId(studentId);
            return ResponseVO.ok(list);
        } catch (Exception e) {
            log.error("获取所有文档失败", e);
            return ResponseVO.error(e.getMessage());
        }
    }

    /**
     * 查看文档的所有分片
     */
    @GetMapping("/{id}/segments")
    public ResponseVO<List<TextSegment>> getSegments(@PathVariable Long id) {
        try {
            Long studentId = Long.valueOf(tools.getCurrentUserId());
            List<TextSegment> segments = knowledgeDocumentService.getDocumentSegments(id, studentId);
            return ResponseVO.ok(segments);
        } catch (Exception e) {
            log.error("获取文档分片失败", e);
            return ResponseVO.error(e.getMessage());
        }
    }

    /**
     * 删除文档的指定分片
     */
    @DeleteMapping("/{id}/segment/{chunkIndex}")
    public ResponseVO<Void> deleteSegment(
            @PathVariable Long id,
            @PathVariable int chunkIndex) {
        try {
            Long studentId = Long.valueOf(tools.getCurrentUserId());
            knowledgeDocumentService.deleteDocumentSegment(id, studentId, chunkIndex);
            return ResponseVO.ok();
        } catch (Exception e) {
            log.error("删除文档分片失败", e);
            return ResponseVO.error(e.getMessage());
        }
    }

    /**
     * 重新向量化文档
     * 先清理旧的向量数据，再重新解析、分片、向量化
     */
    @PostMapping("/{id}/reembed")
    public ResponseVO<KnowledgeDocument> reembed(@PathVariable Long id) {
        try {
            Long studentId = Long.valueOf(tools.getCurrentUserId());
            log.info("收到文档重新向量化请求: docId={}, studentId={}", id, studentId);
            KnowledgeDocument doc = knowledgeDocumentService.reembedDocument(id, studentId);
            return ResponseVO.ok(doc);
        } catch (Exception e) {
            log.error("文档重新向量化失败", e);
            return ResponseVO.error(e.getMessage());
        }
    }
}
