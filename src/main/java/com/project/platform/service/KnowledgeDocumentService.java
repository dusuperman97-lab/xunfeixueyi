package com.project.platform.service;

import com.project.platform.entity.KnowledgeDocument;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeDocumentService {

    /**
     * 上传文档到知识库
     */
    KnowledgeDocument uploadDocument(Long kbId, Long studentId, MultipartFile file);

    /**
     * 获取知识库的所有文档
     */
    List<KnowledgeDocument> getDocumentsByKbId(Long kbId, Long studentId);

    /**
     * 删除文档
     */
    void deleteDocument(Long documentId, Long studentId);

    /**
     * 获取用户的所有文档
     */
    List<KnowledgeDocument> getDocumentsByStudentId(Long studentId);

    /**
     * 获取文档的所有分片列表
     */
    List<TextSegment> getDocumentSegments(Long documentId, Long studentId);

    /**
     * 删除文档的单个分片
     */
    void deleteDocumentSegment(Long documentId, Long studentId, int chunkIndex);

    /**
     * 重新向量化指定文档
     *
     * @param documentId 文档ID
     * @param studentId  用户ID
     * @return 更新后的文档信息
     */
    KnowledgeDocument reembedDocument(Long documentId, Long studentId);
}
