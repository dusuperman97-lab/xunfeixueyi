package com.project.platform.rag.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.rag.content.Content;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;

/**
 * 检索结果本地缓存
 * <p>
 * 对相同查询的检索结果进行短期缓存（5 分钟），
 * 避免高并发时重复调用 Pinecone 向量检索。
 * <p>
 * 缓存失效策略：
 * - 文档上传/删除 → 清除对应知识库的缓存
 * - 自然过期（TTL 5min）
 */
@Slf4j
public class RetrievalResultCache {

    private final Cache<String, List<Content>> cache;

    public RetrievalResultCache() {
        this(5, 5000);
    }

    public RetrievalResultCache(int ttlMinutes, int maxSize) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(ttlMinutes))
                .maximumSize(maxSize)
                .recordStats()
                .build();
    }

    /**
     * 获取缓存的检索结果
     */
    public List<Content> get(String query, Long kbId, Long studentId) {
        String key = buildKey(query, kbId, studentId);
        List<Content> result = cache.getIfPresent(key);
        if (result != null) {
            log.debug("RetrievalCache hit: query={}, kbId={}", query, kbId);
        }
        return result;
    }

    /**
     * 放入缓存
     */
    public void put(String query, Long kbId, Long studentId, List<Content> contents) {
        if (query == null || contents == null) return;
        String key = buildKey(query, kbId, studentId);
        cache.put(key, contents);
    }

    /**
     * 清除指定知识库的所有缓存（文档上传/删除时触发）
     */
    public void invalidateByKbId(Long kbId) {
        // Caffeine 不支持按模式批量删除，此处标记供调用方在文档变更时清全量
        log.info("知识库变更，清空全部检索缓存: kbId={}", kbId);
        cache.invalidateAll();
    }

    public double hitRate() {
        return cache.stats().hitRate();
    }

    public long estimatedSize() {
        return cache.estimatedSize();
    }

    private String buildKey(String query, Long kbId, Long studentId) {
        String raw = studentId + ":" + (kbId != null ? kbId : "all") + ":" + query;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return raw;
        }
    }
}
