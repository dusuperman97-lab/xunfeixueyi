package com.project.platform.rag;

import com.project.platform.rag.enhance.CitationGenerator;
import com.project.platform.rag.enhance.HydeService;
import com.project.platform.rag.enhance.PermissionFilter;
import com.project.platform.rag.enhance.QueryRewriteService;
import com.project.platform.rag.enhance.SiliconFlowRerankService;
import com.project.platform.service.KnowledgeRetrieverService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG Pipeline 主编排器
 * <p>
 * 企业级RAG增强检索流水线，编排以下环节：
 * <p>
 * 1. 查询改写（Query Rewrite）- 指代消解 + 上下文补全 + 口语转书面
 * 2. HyDE（假设性文档嵌入）- 用假设答案代替原始问题去检索
 * 3. 向量检索（Vector Retrieval）- 从知识库检索相关内容
 * 4. 权限过滤（Permission Filter）- 确保用户只能看到有权限的内容
 * 5. 重排序（Rerank）- 用Cross-Encoder做精排，提高精度
 * 6. 引用生成（Citation）- 生成结构化引用信息
 * 7. 上下文格式化（Context Format）- 把检索结果格式化为LLM可用的Prompt
 * <p>
 * 每个环节都可以通过 RagPipelineConfig 独立开关控制，
 * 方便在不同场景下平衡效果和延迟。
 */
@Slf4j
@Service
public class RagPipeline {

    @Resource
    private QueryRewriteService queryRewriteService;

    @Resource
    private HydeService hydeService;

    @Resource
    private KnowledgeRetrieverService knowledgeRetrieverService;

    @Resource
    private PermissionFilter permissionFilter;

    @Resource
    private SiliconFlowRerankService rerankService;

    /**
     * 执行完整的RAG Pipeline
     * <p>
     * 流程：查询改写 → HyDE → 向量检索 → 权限过滤 → 重排序 → 引用生成 → 上下文格式化
     */
    public RagPipelineResult execute(Long studentId, Long kbId, String question, RagPipelineConfig config) {
        long totalStartTime = System.currentTimeMillis();

        // 输入参数校验
        if (question == null || question.isBlank()) {
            log.warn("RAG Pipeline收到空问题，直接返回空结果: studentId={}", studentId);
            return RagPipelineResult.builder()
                    .originalQuestion(question)
                    .config(config)
                    .formattedContext("未找到相关知识库内容。")
                    .usedKnowledge(false)
                    .build();
        }
        if (config == null) {
            config = RagPipelineConfig.defaultConfig();
        }
        config.validate();

        log.info("=== RAG Pipeline 开始 === studentId={}, question={}, kbId={}", studentId, question, kbId);

        RagPipelineResult.RagPipelineResultBuilder resultBuilder = RagPipelineResult.builder()
                .originalQuestion(question)
                .config(config);

        // ========== 阶段1：查询改写 ==========
        long startTime = System.currentTimeMillis();
        String searchQuery = question;
        String rewrittenQuestion = null;

        if (config.isQueryRewriteEnabled()) {
            try {
                if (config.getSessionId() != null) {
                    rewrittenQuestion = queryRewriteService.rewriteWithCache(
                            config.getSessionId(), question, config.getChatHistory());
                } else {
                    rewrittenQuestion = queryRewriteService.safeRewrite(question, config.getChatHistory());
                }
                searchQuery = rewrittenQuestion;
                log.info("查询改写: [{}] -> [{}]", question, rewrittenQuestion);
            } catch (Exception e) {
                log.warn("查询改写异常，使用原始问题: {}", e.getMessage());
                searchQuery = question;
            }
        }
        long rewriteTime = System.currentTimeMillis() - startTime;
        resultBuilder.rewrittenQuestion(rewrittenQuestion).rewriteTime(rewriteTime);

        // ========== 阶段2：HyDE（假设性文档嵌入）==========
        startTime = System.currentTimeMillis();
        String hypotheticalAnswer = null;

        if (config.isHydeEnabled() && hydeService.shouldUseHyde(searchQuery)) {
            try {
                hypotheticalAnswer = hydeService.generateHypotheticalAnswer(searchQuery);
                searchQuery = hypotheticalAnswer;
                log.info("HyDE: 使用假设回答进行检索, 长度={}", hypotheticalAnswer.length());
            } catch (Exception e) {
                log.warn("HyDE异常，使用改写后的问题: {}", e.getMessage());
            }
        }
        long hydeTime = System.currentTimeMillis() - startTime;
        resultBuilder.hypotheticalAnswer(hypotheticalAnswer).hydeTime(hydeTime);

        resultBuilder.actualSearchQuery(searchQuery);

        // ========== 阶段3：向量检索 ==========
        startTime = System.currentTimeMillis();
        List<Content> contents;
        try {
            if (kbId != null) {
                contents = knowledgeRetrieverService.retrieveFromKnowledgeBase(
                        studentId, kbId, searchQuery, config.getMaxResults(), config.getMinScore());
            } else {
                contents = knowledgeRetrieverService.retrieveFromAllKnowledgeBases(
                        studentId, searchQuery, config.getMaxResults(), config.getMinScore());
            }
        } catch (Exception e) {
            log.error("向量检索异常，返回空结果: {}", e.getMessage(), e);
            contents = new ArrayList<>();
        }
        // 防御性检查：确保contents不为null
        if (contents == null) {
            contents = new ArrayList<>();
        }
        long retrievalTime = System.currentTimeMillis() - startTime;
        int initialResultCount = contents.size();
        resultBuilder.initialResultCount(initialResultCount).retrievalTime(retrievalTime);
        log.info("向量检索完成: 结果数={}, 耗时={}ms", initialResultCount, retrievalTime);

        // ========== 阶段4：权限过滤 ==========
        startTime = System.currentTimeMillis();
        if (config.isPermissionFilterEnabled()) {
            contents = permissionFilter.filter(contents, studentId, kbId);
        }
        long filterTime = System.currentTimeMillis() - startTime;
        int filteredResultCount = contents.size();
        resultBuilder.filteredResultCount(filteredResultCount).filterTime(filterTime);

        // ========== 阶段5：重排序 ==========
        startTime = System.currentTimeMillis();
        boolean rerankExecuted = false;
        if (config.isRerankEnabled() && rerankService.isEnabled() && !contents.isEmpty()) {
            try {
                contents = rerankService.rerank(question, contents, config.getRerankTopN());
                rerankExecuted = true;
                log.info("Rerank完成: 结果数={}", contents.size());
            } catch (Exception e) {
                log.warn("Rerank异常，降级为截断取前{}条: {}", config.getRerankTopN(), e.getMessage());
                contents = contents.stream().limit(config.getRerankTopN()).collect(Collectors.toList());
            }
        }
        // 未执行rerank时，也需要按topN截断以控制上下文长度
        if (!rerankExecuted && !contents.isEmpty()) {
            contents = contents.stream().limit(config.getRerankTopN()).collect(Collectors.toList());
        }
        long rerankTime = System.currentTimeMillis() - startTime;
        int rerankedResultCount = contents.size();
        resultBuilder.rerankedResultCount(rerankedResultCount).rerankTime(rerankTime);

        // ========== 阶段6：构建知识来源和引用 ==========
        List<KnowledgeSource> sources = buildKnowledgeSources(contents);
        List<String> citations = new ArrayList<>();
        if (config.isCitationEnabled()) {
            for (int i = 0; i < contents.size(); i++) {
                citations.add(CitationGenerator.generateCitation(contents.get(i), i + 1));
            }
        }

        // ========== 阶段7：上下文格式化 ==========
        String formattedContext = formatContextWithCitations(contents, citations, config);

        // ========== 构建最终结果 ==========
        long totalTime = System.currentTimeMillis() - totalStartTime;
        boolean usedKnowledge = !contents.isEmpty();

        RagPipelineResult result = resultBuilder
                .sources(sources)
                .formattedContext(formattedContext)
                .citations(citations)
                .usedKnowledge(usedKnowledge)
                .totalTime(totalTime)
                .build();

        log.info("=== RAG Pipeline 完成 === {}", result.getTimeSummary());
        return result;
    }

    public RagPipelineResult executeFast(Long studentId, Long kbId, String question) {
        return execute(studentId, kbId, question, RagPipelineConfig.fastConfig());
    }

    public RagPipelineResult executeHighQuality(Long studentId, Long kbId, String question) {
        return execute(studentId, kbId, question, RagPipelineConfig.highQualityConfig());
    }

    private List<KnowledgeSource> buildKnowledgeSources(List<Content> contents) {
        List<KnowledgeSource> sources = new ArrayList<>();

        for (Content content : contents) {
            if (content == null) continue;

            TextSegment segment = content.textSegment();
            if (segment == null) continue;

            KnowledgeSource.KnowledgeSourceBuilder builder = KnowledgeSource.builder();

            if (segment.metadata() != null) {
                builder.fileName(segment.metadata().getString("file_name"));

                String kbIdStr = segment.metadata().getString("kb_id");
                if (kbIdStr != null) {
                    try {
                        builder.kbId(Long.parseLong(kbIdStr));
                    } catch (NumberFormatException e) {
                        log.warn("kb_id解析失败: {}", kbIdStr);
                    }
                }

                String kbName = segment.metadata().getString("kb_name");
                if (kbName != null) {
                    builder.kbName(kbName);
                }

                String rerankScoreStr = segment.metadata().getString("rerank_score");
                String scoreStr = segment.metadata().getString("score");
                if (rerankScoreStr != null) {
                    try {
                        builder.score(Double.parseDouble(rerankScoreStr));
                    } catch (NumberFormatException e) {
                        log.warn("rerank_score解析失败: {}", rerankScoreStr);
                    }
                } else if (scoreStr != null) {
                    try {
                        builder.score(Double.parseDouble(scoreStr));
                    } catch (NumberFormatException e) {
                        log.warn("score解析失败: {}", scoreStr);
                    }
                }

                String docIdStr = segment.metadata().getString("doc_id");
                if (docIdStr != null) {
                    try {
                        builder.docId(Long.parseLong(docIdStr));
                    } catch (NumberFormatException e) {
                        log.warn("doc_id解析失败: {}", docIdStr);
                    }
                }

                String chunkIndex = segment.metadata().getString("chunk_index");
                if (chunkIndex != null) {
                    try {
                        builder.chunkIndex(Integer.parseInt(chunkIndex));
                    } catch (NumberFormatException e) {
                        log.warn("chunk_index解析失败: {}", chunkIndex);
                    }
                }
            }

            String text = segment.text();
            String snippet = text != null && text.length() > 100 ? text.substring(0, 100) + "..." : text;
            builder.snippet(snippet);

            sources.add(builder.build());
        }

        return sources;
    }

    private String formatContextWithCitations(List<Content> contents, List<String> citations, RagPipelineConfig config) {
        if (contents == null || contents.isEmpty()) {
            return "未找到相关知识库内容。";
        }

        StringBuilder context = new StringBuilder();
        context.append("以下是从知识库中检索到的相关资料：\n\n");

        for (int i = 0; i < contents.size(); i++) {
            Content content = contents.get(i);
            if (content == null) continue;

            TextSegment segment = content.textSegment();
            if (segment == null) continue;

            if (config.isCitationEnabled() && citations != null && i < citations.size()) {
                context.append(citations.get(i)).append("\n");
            } else {
                context.append("--- 资料 ").append(i + 1).append(" ---\n");
            }

            context.append(segment.text()).append("\n\n");
        }

        return context.toString();
    }
}
