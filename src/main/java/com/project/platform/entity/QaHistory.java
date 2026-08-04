package com.project.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识库问答历史记录实体
 */
@Data
public class QaHistory {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 提问用户ID
     */
    private Long studentId;

    /**
     * 目标知识库ID（可为空，表示全局检索）
     */
    private Long kbId;

    /**
     * 原始问题
     */
    private String originalQuestion;

    /**
     * 改写后的问题
     */
    private String rewrittenQuestion;

    /**
     * 实际用于检索的查询文本（HyDE 或改写后）
     */
    private String actualSearchQuery;

    /**
     * AI 生成的答案
     */
    private String answer;

    /**
     * 知识来源 JSON（KnowledgeSource 列表序列化）
     */
    private String sourcesJson;

    /**
     * 引用信息 JSON
     */
    private String citationsJson;

    /**
     * 反馈：0-无反馈, 1-赞, -1-踩
     */
    private Integer feedback;

    /**
     * 反馈备注
     */
    private String feedbackComment;

    /**
     * 检索耗时（毫秒）
     */
    private Long retrievalTime;

    /**
     * 总耗时（毫秒）
     */
    private Long totalTime;

    /**
     * 检索结果数量
     */
    private Integer resultCount;

    /**
     * 是否使用了知识库
     */
    private Boolean usedKnowledge;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
