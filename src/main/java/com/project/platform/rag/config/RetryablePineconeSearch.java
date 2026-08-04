package com.project.platform.rag.config;

import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * 带重试的 Pinecone 检索封装
 * <p>
 * 对瞬时故障（网络抖动、Pinecone 短暂不可用）自动重试 3 次，
 * 指数退避：200ms → 400ms → 800ms，总等待不超过 1.4s。
 */
@Slf4j
@Component
public class RetryablePineconeSearch {

    /**
     * 带重试的 Pinecone 向量检索
     *
     * @param store   向量存储
     * @param request 检索请求
     * @return 检索结果
     */
    @Retryable(
            retryFor = {IOException.class, RuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000)
    )
    public EmbeddingSearchResult<TextSegment> searchWithRetry(
            EmbeddingStore<TextSegment> store,
            EmbeddingSearchRequest request) {
        log.debug("执行 Pinecone 检索: maxResults={}, minScore={}",
                request.maxResults(), request.minScore());
        return store.search(request);
    }
}
