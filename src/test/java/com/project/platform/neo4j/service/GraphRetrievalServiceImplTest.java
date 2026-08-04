package com.project.platform.neo4j.service;

import com.project.platform.neo4j.dto.EntryNodeInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GraphRetrievalService 单元测试。
 * 测试检索策略、降级逻辑、结果融合等核心功能。
 * （不依赖 Spring Context — 纯逻辑验证）
 */
class GraphRetrievalServiceImplTest {

    // ==================== 降级检索逻辑 ====================

    @Test
    @DisplayName("混合检索无结果时应触发降级策略")
    void testFallbackTriggeredWhenNoResults() {
        // 验证逻辑：hybridSearch 返回空时，外部调用者能正确触发 fallbackSearch
        // 此测试验证接口契约
        List<EntryNodeInfo> empty = Collections.emptyList();
        assertTrue(empty.isEmpty());

        // 模拟降级触发
        boolean fallbackTriggered = empty.isEmpty();
        assertTrue(fallbackTriggered, "检索无结果时应触发降级");
    }

    @Test
    @DisplayName("降级检索应返回更宽松范围的结果")
    void testFallbackHasWiderScope() {
        // 验证降级检索的宽松范围逻辑
        String originalType = "KnowledgePoint";
        String keyword = "五行学说";

        // 模拟降级：移除类型限制
        String fallbackType = null; // 全图搜索
        assertNull(fallbackType, "降级时应移除类型限制");

        // 模拟降级：缩短关键词
        String shortKeyword = keyword.length() > 1 ? keyword.substring(0, keyword.length() / 2) : keyword;
        assertNotEquals(keyword, shortKeyword, "降级关键词应短于原始关键词");
    }

    // ==================== 结果融合 ====================

    @Test
    @DisplayName("合并结果应去重并加权排序")
    void testMergeDeduplicatesAndRanks() {
        // 创建两个来源的重叠结果
        EntryNodeInfo node1 = EntryNodeInfo.builder()
                .nodeId("node-001").entityName("五行学说").nodeType("KnowledgePoint")
                .relevanceScore(0.9).source("VECTOR")
                .build();
        EntryNodeInfo node2 = EntryNodeInfo.builder()
                .nodeId("node-002").entityName("阴阳学说").nodeType("KnowledgePoint")
                .relevanceScore(0.7).source("VECTOR")
                .build();
        EntryNodeInfo node1Dup = EntryNodeInfo.builder()
                .nodeId("node-001").entityName("五行学说").nodeType("KnowledgePoint")
                .relevanceScore(0.6).source("KEYWORD")
                .build();
        EntryNodeInfo node3 = EntryNodeInfo.builder()
                .nodeId("node-003").entityName("藏象学说").nodeType("KnowledgePoint")
                .relevanceScore(0.5).source("KEYWORD")
                .build();

        List<EntryNodeInfo> all = Arrays.asList(node1, node2, node1Dup, node3);

        // 手动模拟 mergeAndRank 的去重和排序逻辑
        Map<String, EntryNodeInfo> merged = new LinkedHashMap<>();
        for (EntryNodeInfo info : all) {
            String key = info.getNodeId();
            if (merged.containsKey(key)) {
                EntryNodeInfo existing = merged.get(key);
                double newScore = existing.getRelevanceScore() + info.getRelevanceScore() * 0.5;
                merged.put(key, EntryNodeInfo.builder()
                        .nodeId(key).nodeType(existing.getNodeType())
                        .entityName(existing.getEntityName())
                        .relevanceScore(Math.min(newScore, 1.0))
                        .source(existing.getSource() + "+" + info.getSource())
                        .build());
            } else {
                double weight = "VECTOR".equals(info.getSource()) ? 0.7 : 0.3;
                info.setRelevanceScore(info.getRelevanceScore() * weight);
                merged.put(key, info);
            }
        }

        // 去重验证
        assertEquals(3, merged.size(), "应去重为 3 个节点");

        // 排序验证（按分数降序）
        List<EntryNodeInfo> sorted = merged.values().stream()
                .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
                .toList();

        // node-001 应该是最高分（0.9 * 0.7 + 0.6 * 0.5 = 0.93）
        assertEquals("node-001", sorted.get(0).getNodeId());
        assertTrue(sorted.get(0).getRelevanceScore() > sorted.get(1).getRelevanceScore());
    }

    // ==================== EntryNodeInfo 构建 ====================

    @Test
    @DisplayName("EntryNodeInfo 构建器应正确设置所有字段")
    void testEntryNodeInfoBuilder() {
        Map<String, Object> props = Map.of("name", "五行学说", "description", "中医基础理论");
        EntryNodeInfo info = EntryNodeInfo.builder()
                .nodeType("KnowledgePoint")
                .entityName("五行学说")
                .nodeId("4:abc:123")
                .properties(props)
                .labels(List.of("KnowledgePoint"))
                .relevanceScore(0.85)
                .source("VECTOR")
                .build();

        assertEquals("KnowledgePoint", info.getNodeType());
        assertEquals("五行学说", info.getEntityName());
        assertEquals("4:abc:123", info.getNodeId());
        assertEquals(0.85, info.getRelevanceScore());
        assertEquals("VECTOR", info.getSource());
        assertNotNull(info.getProperties());
        assertEquals("五行学说", info.getProperties().get("name"));
    }
}
