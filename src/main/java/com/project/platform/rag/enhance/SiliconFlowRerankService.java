package com.project.platform.rag.enhance;

import cn.hutool.core.collection.CollectionUtil;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于SiliconFlow Rerank API实现的重排序服务
 * <p>
 * SiliconFlow的 /v1/rerank 接口兼容多种Reranker模型，
 * 如 BAAI/bge-reranker-v2-m3、Qwen/Qwen3-Reranker-8B 等，
 * 通过配置 rerank.model 即可切换，不用改代码。
 * <p>
 * 设计要点：
 * 1. 调用外部Rerank API做精排，精度远高于向量相似度
 * 2. API调用失败时降级返回原始顺序的前topN个（不影响主流程）
 * 3. 把rerank分数写入metadata，方便后续排查或展示
 */
@Slf4j
@Service
public class SiliconFlowRerankService implements RerankService {

    private static final String RERANK_URL = "https://api.siliconflow.cn/v1/rerank";

    private final RestTemplate restTemplate;

    private final String apiKey;
    private final String model;
    private final int defaultTopN;

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 30000;

    public SiliconFlowRerankService(
            @Value("${rag.rerank.api-key:}") String apiKey,
            @Value("${rag.rerank.model:BAAI/bge-reranker-v2-m3}") String model,
            @Value("${rag.rerank.top-n:3}") int defaultTopN) {
        this.apiKey = apiKey;
        this.model = model;
        this.defaultTopN = defaultTopN;

        // 配置超时，防止API无响应时线程永久阻塞
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Content> rerank(String query, List<Content> contents, int topN) {
        if (CollectionUtil.isEmpty(contents)) {
            return contents;
        }

        if (!isEnabled()) {
            log.debug("Rerank未启用（缺少API Key），跳过重排序");
            return contents.stream().limit(topN).collect(Collectors.toList());
        }

        List<String> texts = contents.stream()
                .map(content -> {
                    TextSegment seg = content.textSegment();
                    return seg != null && seg.text() != null ? seg.text() : "";
                })
                .filter(text -> !text.isEmpty())  // 过滤空文本，避免API报错
                .collect(Collectors.toList());

        if (texts.isEmpty()) {
            log.warn("所有候选内容文本为空，跳过Rerank");
            return contents.stream().limit(topN).collect(Collectors.toList());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model != null ? model : "BAAI/bge-reranker-v2-m3");
        body.put("query", query);
        body.put("documents", texts);
        body.put("top_n", Math.min(topN, contents.size()));
        body.put("return_documents", false);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    RERANK_URL, new HttpEntity<>(body, headers), Map.class);

            Map responseBody = response.getBody();
            if (responseBody == null) {
                log.warn("Rerank API返回空响应体，降级返回原始顺序");
                return contents.stream().limit(topN).collect(Collectors.toList());
            }

            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) responseBody.get("results");

            if (results == null || results.isEmpty()) {
                log.warn("Rerank API返回空结果列表，降级返回原始顺序");
                return contents.stream().limit(topN).collect(Collectors.toList());
            }

            // SiliconFlow rerank API 默认已按 relevance_score 降序排列，此处保留排序以确保兼容性
            return results.stream()
                    .sorted(Comparator.comparingDouble(
                            r -> -((Number) r.get("relevance_score")).doubleValue()))
                    .map(r -> {
                        Object indexObj = r.get("index");
                        Object scoreObj = r.get("relevance_score");

                        if (indexObj == null || scoreObj == null) {
                            log.warn("Rerank结果缺少index或relevance_score字段，跳过该条目");
                            return null;
                        }

                        int index = ((Number) indexObj).intValue();
                        double score = ((Number) scoreObj).doubleValue();

                        // 越界检查：防止API返回非法index导致崩溃
                        if (index < 0 || index >= contents.size()) {
                            log.warn("Rerank返回的index越界: index={}, contents.size={}", index, contents.size());
                            return null;
                        }

                        Content originalContent = contents.get(index);
                        TextSegment originalSegment = originalContent.textSegment();

                        // 通过copy避免修改原始metadata（防止副作用）
                        var metadataCopy = originalSegment.metadata() != null
                                ? originalSegment.metadata().copy()
                                : new dev.langchain4j.data.document.Metadata();
                        metadataCopy.put("rerank_score", String.valueOf(score));

                        String segText = originalSegment.text();
                        log.debug("Rerank | score={} | text={}...",
                                String.format("%.4f", score),
                                segText != null ? segText.substring(0, Math.min(50, segText.length())) : "(empty)");

                        return Content.from(TextSegment.from(segText, metadataCopy));
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Reranker调用失败，降级返回原始顺序前{}条: {}", topN, e.getMessage());
            return contents.stream().limit(topN).collect(Collectors.toList());
        }
    }

    public List<Content> rerank(String query, List<Content> contents) {
        return rerank(query, contents, defaultTopN);
    }
}
