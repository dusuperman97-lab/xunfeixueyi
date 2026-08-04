package com.project.platform.dto;

import com.project.platform.rag.KnowledgeSource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 知识库问答响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeAnswerDTO {
    
    /**
     * AI 生成的答案
     */
    private String answer;

    private Flux<String> streamAnswer;
    
    /**
     * 检索到的知识来源列表
     */
    private List<KnowledgeSource> sources;
    
    /**
     * 是否使用了知识库
     */
    private Boolean usedKnowledge;
    
    /**
     * 检索耗时（毫秒）
     */
    private Long retrievalTime;

    // ========== RAG Pipeline 增强字段 ==========

    /**
     * 原始问题
     */
    private String originalQuestion;

    /**
     * 改写后的问题
     */
    private String rewrittenQuestion;

    /**
     * 实际用于检索的查询文本
     */
    private String actualSearchQuery;

    /**
     * 引用信息列表
     */
    private List<String> citations;

    /**
     * 各阶段耗时摘要
     */
    private String timeSummary;
}
