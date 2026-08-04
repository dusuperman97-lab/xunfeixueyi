package com.project.platform.rag.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.data.embedding.Embedding;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

/**
 * Embedding 向量本地缓存
 * <p>
 * 对于相同的问题，避免重复调用 Embedding API，
 * 使用 Caffeine 本地缓存，30 分钟过期，最多 10000 条。
 */
@Slf4j
public class EmbeddingCache {

    private final Cache<String, Embedding> cache;

    public EmbeddingCache() {
        this(30, 10000);
    }

    public EmbeddingCache(int ttlMinutes, int maxSize) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(ttlMinutes))
                .maximumSize(maxSize)
                .recordStats()
                .build();
    }

    /**
     * 获取缓存的 Embedding
     *
     * @param text 原始文本
     * @return 缓存的 Embedding，不存在返回 null
     */
    public Embedding get(String text) {
        String key = hash(text);
        Embedding emb = cache.getIfPresent(key);
        if (emb != null) {
            log.debug("EmbeddingCache hit: text.length={}", text != null ? text.length() : 0);
        }
        return emb;
    }

    /**
     * 放入缓存
     */
    public void put(String text, Embedding embedding) {
        if (text == null || embedding == null) return;
        String key = hash(text);
        cache.put(key, embedding);
    }

    /**
     * 清空缓存（如切换 Embedding 模型时调用）
     */
    public void invalidateAll() {
        cache.invalidateAll();
        log.info("EmbeddingCache 已全部清空");
    }

    public long estimatedSize() {
        return cache.estimatedSize();
    }

    public double hitRate() {
        return cache.stats().hitRate();
    }

    private String hash(String text) {
        if (text == null) return "null";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(text.hashCode());
        }
    }
}
