package com.project.platform.agents.config.utils;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Slf4j
public class RagTools {

    @Value("${pinecone_key}")
    String pineconeKey;

    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    @Resource
    private EmbeddingModel ollamaEmbeddingModel;

    /**
     * PineconeEmbeddingStore 实例缓存（按 namespace 隔离）
     * <p>
     * 避免每次检索请求都创建新的客户端实例，
     * 复用底层 HTTP 连接池，减少 GC 压力。
     */
    private final ConcurrentMap<String, PineconeEmbeddingStore> storeCache = new ConcurrentHashMap<>();

    public EmbeddingStore<TextSegment> getEmbeddingStore(String namespace) {
        return storeCache.computeIfAbsent(namespace, ns -> {
            log.info("创建并缓存 PineconeEmbeddingStore: namespace={}", ns);
            return PineconeEmbeddingStore.builder()
                    .apiKey(pineconeKey)
                    .index("xunfei-index")
                    .nameSpace(ns)
                    .build();
        });
    }

    /**
     * 移除缓存的 EmbeddingStore 实例
     * 用于知识库删除时清理对应 namespace 的连接
     */
    public void evictStore(String namespace) {
        PineconeEmbeddingStore removed = storeCache.remove(namespace);
        if (removed != null) {
            log.info("已清理 PineconeEmbeddingStore 缓存: namespace={}", namespace);
        }
    }
}
