package com.project.platform.rag.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 缓存配置
 * 注册 EmbeddingCache 和 RetrievalResultCache 为 Spring Bean
 */
@Configuration
public class RagCacheConfig {

    @Bean
    public EmbeddingCache embeddingCache() {
        return new EmbeddingCache(30, 10000);
    }

    @Bean
    public RetrievalResultCache retrievalResultCache() {
        return new RetrievalResultCache(5, 5000);
    }
}
