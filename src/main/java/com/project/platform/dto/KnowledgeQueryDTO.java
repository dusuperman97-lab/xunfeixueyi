package com.project.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库问答请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeQueryDTO {
    
    /**
     * 用户问题
     */
    private String question;
    
    /**
     * 知识库ID（可选，不传则检索所有知识库）
     */
    private Long kbId;
    
    /**
     * 最大返回结果数（默认5）
     */
    private Integer maxResults = 5;
    
    /**
     * 最小相似度分数（默认0.7）
     */
    private Double minScore = 0.7;

    // ========== RAG Pipeline 增强参数 ==========

    /**
     * 是否启用查询改写（默认true）
     */
    private Boolean queryRewriteEnabled = true;

    /**
     * 是否启用HyDE（默认false）
     */
    private Boolean hydeEnabled = false;

    /**
     * 是否启用Rerank重排序（默认true）
     */
    private Boolean rerankEnabled = true;

    /**
     * 是否启用权限过滤（默认true）
     */
    private Boolean permissionFilterEnabled = true;

    /**
     * 是否启用引用生成（默认true）
     */
    private Boolean citationEnabled = true;

    /**
     * Rerank后返回的topN数（默认3）
     */
    private Integer rerankTopN = 3;

    /**
     * 会话ID（用于查询改写缓存）
     */
    private String sessionId;

    /**
     * 对话历史（用于查询改写的上下文补全，格式："用户：xxx\n助手：xxx"）
     */
    private String chatHistory;
}
