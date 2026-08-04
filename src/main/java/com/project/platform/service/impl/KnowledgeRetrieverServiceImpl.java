package com.project.platform.service.impl;

import com.project.platform.agents.config.utils.RagTools;
import com.project.platform.agents.config.utils.Tools;
import com.project.platform.entity.KnowledgeBase;
import com.project.platform.mapper.KnowledgeBaseMapper;
import com.project.platform.service.KnowledgeRetrieverService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeRetrieverServiceImpl implements KnowledgeRetrieverService {

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    @Resource
    private EmbeddingModel ollamaEmbeddingModel;

    @Resource
    private RagTools ragTools;

    @Value("${rag.global-knowledge.namespace:global-knowledge}")
    private String globalKnowledgeNamespace;

    @Value("${rag.global-knowledge.enabled:true}")
    private boolean globalKnowledgeEnabled;

    /**
     * 并行检索线程池 — 固定大小，避免用户知识库多时线程溢出
     */
    private final ExecutorService retrieveExecutor = Executors.newFixedThreadPool(8, r -> {
        Thread t = new Thread(r, "rag-retrieve");
        t.setDaemon(true);
        return t;
    });

    private static final long RETRIEVE_TIMEOUT_MS = 5000;


    @Override
    public List<Content> retrieveFromKnowledgeBase(Long studentId, Long kbId, String query, int maxResults, double minScore) {
        log.info("🔍 [个人知识库检索] 开始检索: studentId={}, kbId={}, query={}", studentId, kbId, query);
        
        // 1. 验证知识库权限
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || !kb.getStudentId().equals(studentId)) {
            log.warn("知识库不存在或无权限: kbId={}, studentId={}", kbId, studentId);
            return new ArrayList<>();
        }

        // 2. 将查询文本转换为向量
        Embedding queryEmbedding = qwenEmbeddingModel.embed(query).content();

        // 3. 构建针对特定知识库 namespace 的检索请求
        // 注意：PineconeEmbeddingStore 需要使用特定 namespace 的实例
        EmbeddingStore<TextSegment> kbEmbeddingStore = ragTools.getEmbeddingStore(kb.getPineconeNamespace());

        // 4. 构建搜索请求（通过 metadata 过滤）
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(minScore)
                .filter(buildMetadataFilter(kb.getId()))
                .build();

        // 5. 执行检索
        EmbeddingSearchResult<TextSegment> searchResult = kbEmbeddingStore.search(searchRequest);

        // 6. 转换为 Content 列表
        List<Content> contents = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
            // 将相似度分数添加到 metadata 中
            TextSegment segment = match.embedded();
            segment.metadata().put("score", String.valueOf(match.score()));
            contents.add(Content.from(segment));
        }

        log.info("知识库检索完成: kbId={}, query={}, 结果数={}", kbId, query, contents.size());
        return contents;
    }

    @Override
    public List<Content> retrieveFromAllKnowledgeBases(Long studentId, String query, int maxResults, double minScore) {
        log.info("🔍 [全库检索] 开始检索: studentId={}, query={}", studentId, query);
        
        // 2. 将查询文本转换为向量
        Embedding queryEmbedding = qwenEmbeddingModel.embed(query).content();

        // 3. 在所有知识库中检索（包括全局知识库和个人知识库）
        List<EmbeddingMatch<TextSegment>> allMatches = new ArrayList<>();

        // 3.1 先检索全局知识库（如果启用）
        if (globalKnowledgeEnabled) {
            try {
                log.info("开始检索全局知识库: namespace={}", globalKnowledgeNamespace);
                EmbeddingStore<TextSegment> globalStore = ragTools.getEmbeddingStore(globalKnowledgeNamespace);

                EmbeddingSearchRequest globalSearchRequest = EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(maxResults)
                        .minScore(minScore)
                        .build();

                EmbeddingSearchResult<TextSegment> globalSearchResult = globalStore.search(globalSearchRequest);
                allMatches.addAll(globalSearchResult.matches());

                log.info("🔍 [全库检索] 全局知识库检索到 {} 条结果", globalSearchResult.matches().size());
            } catch (Exception e) {
                log.error("全局知识库检索失败", e);
            }
        }

        // 3.2 并行检索所有个人知识库
        List<KnowledgeBase> knowledgeBases = knowledgeBaseMapper.selectByStudentId(studentId);
        if (!knowledgeBases.isEmpty()) {
            // 并行提交每个知识库的检索任务
            List<CompletableFuture<List<EmbeddingMatch<TextSegment>>>> futures = knowledgeBases.stream()
                    .map(kb -> CompletableFuture.supplyAsync(() -> {
                        try {
                            EmbeddingStore<TextSegment> kbEmbeddingStore =
                                    ragTools.getEmbeddingStore(kb.getPineconeNamespace());

                            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                                    .queryEmbedding(queryEmbedding)
                                    .maxResults(maxResults)
                                    .minScore(minScore)
                                    .filter(buildMetadataFilter(kb.getId()))
                                    .build();

                            EmbeddingSearchResult<TextSegment> searchResult =
                                    kbEmbeddingStore.search(searchRequest);

                            log.debug("🔍 [全库检索] 个人知识库 {} 检索到 {} 条结果",
                                    kb.getName(), searchResult.matches().size());
                            return searchResult.matches();
                        } catch (Exception e) {
                            log.error("个人知识库检索失败: kbId={}, name={}", kb.getId(), kb.getName(), e);
                            return List.<EmbeddingMatch<TextSegment>>of();
                        }
                    }, retrieveExecutor)
                    .orTimeout(RETRIEVE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .exceptionally(ex -> {
                        log.warn("知识库检索超时或异常: kbId={}, name={}", kb.getId(), kb.getName());
                        return List.of();
                    }))
                    .toList();

            // 等待所有并行检索完成，合并结果
            for (CompletableFuture<List<EmbeddingMatch<TextSegment>>> future : futures) {
                try {
                    List<EmbeddingMatch<TextSegment>> kbMatches = future.join();
                    allMatches.addAll(kbMatches);
                } catch (Exception e) {
                    log.warn("合并知识库检索结果时异常: {}", e.getMessage());
                }
            }
        } else {
            log.info("🔍 [全库检索] 用户没有可用的个人知识库: studentId={}", studentId);
        }

        // 4. 按相似度分数排序，取前 maxResults 个
        allMatches.sort((a, b) -> Double.compare(b.score(), a.score()));
        List<EmbeddingMatch<TextSegment>> topMatches = allMatches.subList(0, Math.min(maxResults, allMatches.size()));

        // 5. 转换为 Content 列表
        List<Content> contents = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : topMatches) {
            // 将相似度分数添加到 metadata 中
            TextSegment segment = match.embedded();
            segment.metadata().put("score", String.valueOf(match.score()));
            contents.add(Content.from(segment));
        }

        log.info("✅ [全库检索完成] studentId={}, query={}, 最终结果数={}", studentId, query, contents.size());
        return contents;
    }

    @Override
    public String formatContext(List<Content> contents) {
        if (contents == null || contents.isEmpty()) {
            return "未找到相关知识库内容。";
        }

        StringBuilder context = new StringBuilder();
        context.append("以下是从知识库中检索到的相关信息：\n\n");

        for (int i = 0; i < contents.size(); i++) {
            Content content = contents.get(i);
            TextSegment segment = content.textSegment();
            
            context.append("--- 资料 ").append(i + 1).append(" ---\n");
            
            // 添加文件名信息
            if (segment.metadata() != null && segment.metadata().containsKey("file_name")) {
                context.append("来源文件: ").append(segment.metadata().getString("file_name")).append("\n");
            }
            
            // 添加内容
            context.append("内容: ").append(segment.text()).append("\n\n");
        }

        return context.toString();
    }

    /**
     * 构建 metadata 过滤条件
     * 注意：由于每个知识库使用独立的 Pinecone namespace，数据已经天然隔离
     * 这里不需要额外的 metadata 过滤，直接返回 null
     */
    private Filter buildMetadataFilter(Long kbId) {
        return null;
    }
}
