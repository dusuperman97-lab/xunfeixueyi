package com.project.platform.service.impl;

import com.project.platform.entity.KnowledgeBase;
import com.project.platform.mapper.KnowledgeBaseMapper;
import com.project.platform.mapper.KnowledgeDocumentMapper;
import com.project.platform.rag.store.VectorCleanupService;
import com.project.platform.service.KnowledgeBaseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Resource
    private VectorCleanupService vectorCleanupService;

    @Override
    @Transactional
    public KnowledgeBase createKnowledgeBase(Long studentId, String name, String description) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setStudentId(studentId);
        kb.setName(name);
        kb.setDescription(description);
        kb.setDocumentCount(0);
        kb.setStatus(1);
        kb.setCreatedAt(LocalDateTime.now());
        kb.setUpdatedAt(LocalDateTime.now());

        // 为每个知识库生成唯一的 Pinecone namespace，隔离不同用户的数据
        kb.setPineconeNamespace("student_" + studentId + "_kb_" + UUID.randomUUID().toString().substring(0, 8));
        
        knowledgeBaseMapper.insert(kb);
        log.info("创建知识库成功: id={}, name={}, namespace={}", kb.getId(), name, kb.getPineconeNamespace());
        return kb;
    }

    /**
     * 将知识库名称转换为合法的 Pinecone namespace 片段
     * 规则：只保留字母、数字、下划线、连字符，中文转下划线
     */
    private String sanitizeNamespaceName(String name) {
        if (name == null || name.isEmpty()) {
            return "unnamed";
        }
        
        // 替换所有非字母数字字符为下划线
        String sanitized = name.replaceAll("[^a-zA-Z0-9_-]", "_")
                               .replaceAll("_+", "_")  // 合并连续下划线
                               .toLowerCase();
        
        // 去除首尾下划线
        sanitized = sanitized.replaceAll("^_+|_+$", "");
        
        // 限制长度（预留空间给前缀和UUID）
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        
        // 如果处理后为空，使用默认值
        return sanitized.isEmpty() ? "unnamed" : sanitized;
    }

    @Override
    @Transactional
    public void updateKnowledgeBase(Long id, Long studentId, String name, String description) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb == null || !kb.getStudentId().equals(studentId)) {
            throw new RuntimeException("知识库不存在或无权限");
        }
        
        kb.setName(name);
        kb.setDescription(description);
        knowledgeBaseMapper.update(kb);
        log.info("更新知识库成功: id={}", id);
    }

    @Override
    @Transactional
    public void deleteKnowledgeBase(Long id, Long studentId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb == null || !kb.getStudentId().equals(studentId)) {
            throw new RuntimeException("知识库不存在或无权限");
        }

        // 1. 清理 Pinecone 中的向量数据 — 所有关联文档的分片
        try {
            vectorCleanupService.deleteVectorsByKbId(id, kb.getPineconeNamespace());
            // 额外确保 namespace 下无残留
            vectorCleanupService.deleteAllInNamespace(kb.getPineconeNamespace());
            log.info("知识库向量数据清理完成: kbId={}, namespace={}", id, kb.getPineconeNamespace());
        } catch (Exception e) {
            log.error("清理知识库向量数据失败: kbId={}", id, e);
            // 清理失败不阻塞数据库删除，但记录日志供人工处理
        }

        // 2. 逻辑删除关联的文档记录
        List<com.project.platform.entity.KnowledgeDocument> docs =
                knowledgeDocumentMapper.selectByKbId(id);
        for (com.project.platform.entity.KnowledgeDocument doc : docs) {
            knowledgeDocumentMapper.deleteById(doc.getId());
        }

        // 3. 删除知识库记录
        knowledgeBaseMapper.deleteById(id);
        log.info("删除知识库成功: id={}", id);
    }

    @Override
    public List<KnowledgeBase> getKnowledgeBases(Long studentId) {
        return knowledgeBaseMapper.selectByStudentId(studentId);
    }

    @Override
    public KnowledgeBase getKnowledgeBaseById(Long id, Long studentId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        if (kb != null && kb.getStudentId().equals(studentId)) {
            // 实时更新文档数量
            int count = knowledgeDocumentMapper.countByKbId(id);
            kb.setDocumentCount(count);
            return kb;
        }
        return null;
    }
}
