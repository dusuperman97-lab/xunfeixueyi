package com.project.platform.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG Pipeline 执行结果
 * <p>
 * 包含完整的RAG流水线执行结果，方便调试和追踪每一步的效果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagPipelineResult {

    private String originalQuestion;
    private String rewrittenQuestion;
    private String hypotheticalAnswer;
    private String actualSearchQuery;

    private int initialResultCount;
    private int filteredResultCount;
    private int rerankedResultCount;

    private List<KnowledgeSource> sources;
    private String formattedContext;
    private List<String> citations;
    private boolean usedKnowledge;

    private long rewriteTime;
    private long hydeTime;
    private long retrievalTime;
    private long filterTime;
    private long rerankTime;
    private long totalTime;

    private RagPipelineConfig config;

    public String getTimeSummary() {
        return String.format("总耗时:%dms [改写:%dms, HyDE:%dms, 检索:%dms, 过滤:%dms, 重排:%dms]",
                totalTime, rewriteTime, hydeTime, retrievalTime, filterTime, rerankTime);
    }
}
