package com.project.platform.neo4j.service;

import com.project.platform.agents.config.utils.RagTools;
import com.project.platform.neo4j.dto.EntryNodeInfo;
import com.project.platform.neo4j.util.Neo4jUtil;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.project.platform.neo4j.util.Neo4jUtil.isValidIdentifier;

/**
 * 知识图谱混合检索实现。
 *
 * <p>检索流程：
 * <ol>
 *   <li>向量检索：Embedding → Pinecone 语义搜索 → 提取 elementId → Neo4j 回查完整节点</li>
 *   <li>关键词检索：Neo4j CONTAINS 精确/模糊匹配</li>
 *   <li>结果融合：去重 → 加权排序（向量:0.7, 关键词:0.3）</li>
 *   <li>降级策略：扩大范围 → 移除类型限制 → 模糊 LIKE 匹配</li>
 * </ol>
 */
@Slf4j
@Service
public class GraphRetrievalServiceImpl implements GraphRetrievalService {

    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    @Resource
    private RagTools ragTools;

    /** 图节点向量索引的 Pinecone namespace 前缀 */
    private static final String GRAPH_NODE_NAMESPACE = "graph-nodes";

    /** 向量检索分数权重 */
    private static final double VECTOR_WEIGHT = 0.7;

    /** 关键词检索分数权重 */
    private static final double KEYWORD_WEIGHT = 0.3;

    // ==================== 混合检索 ====================

    @Override
    public List<EntryNodeInfo> hybridSearch(String nodeType, String entityName,
                                             Integer studentId, int maxResults) {
        log.info("混合检索: nodeType={}, entity={}, studentId={}, max={}",
                nodeType, entityName, studentId, maxResults);

        List<EntryNodeInfo> allResults = new ArrayList<>();

        // 1. 向量检索
        try {
            List<EntryNodeInfo> vectorResults = vectorSearch(nodeType, entityName, maxResults);
            allResults.addAll(vectorResults);
            log.info("向量检索结果: {} 条", vectorResults.size());
        } catch (Exception e) {
            log.warn("向量检索异常，跳过: {}", e.getMessage());
        }

        // 2. 关键词检索
        try {
            List<EntryNodeInfo> keywordResults = keywordSearch(nodeType, entityName, studentId, maxResults * 2);
            allResults.addAll(keywordResults);
            log.info("关键词检索结果: {} 条", keywordResults.size());
        } catch (Exception e) {
            log.warn("关键词检索异常，跳过: {}", e.getMessage());
        }

        // 3. 去重合并 + 加权排序
        List<EntryNodeInfo> merged = mergeAndRank(allResults, maxResults);
        log.info("混合检索合并后: {} 条", merged.size());
        return merged;
    }

    // ==================== 关键词检索 ====================

    @Override
    public List<EntryNodeInfo> keywordSearch(String nodeType, String keyword,
                                              Integer studentId, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }

        // 构建安全的参数化查询
        StringBuilder cypher = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", keyword);
        params.put("studentId", studentId);
        params.put("limit", limit);

        if (nodeType != null && !nodeType.isBlank() && isValidIdentifier(nodeType)) {
            // 指定节点类型
            cypher.append("MATCH (n:`").append(nodeType).append("`) ");
            cypher.append("WHERE n.studentId = $studentId ");
            cypher.append("AND (n.name CONTAINS $keyword ");
            cypher.append("  OR n.description CONTAINS $keyword ");
            cypher.append("  OR n.courseName CONTAINS $keyword ");
            cypher.append("  OR n.chapterName CONTAINS $keyword ");
            cypher.append("  OR n.sectionName CONTAINS $keyword) ");
        } else {
            // 不限类型，全图搜索
            cypher.append("MATCH (n) ");
            cypher.append("WHERE n.studentId = $studentId ");
            cypher.append("AND (n.name CONTAINS $keyword ");
            cypher.append("  OR n.description CONTAINS $keyword ");
            cypher.append("  OR n.courseName CONTAINS $keyword ");
            cypher.append("  OR n.chapterName CONTAINS $keyword ");
            cypher.append("  OR n.sectionName CONTAINS $keyword) ");
        }
        cypher.append("RETURN n, labels(n) AS lbls LIMIT $limit");

        try {
            List<Map<String, Object>> rows = Neo4jUtil.runCypher(cypher.toString(), params);
            return rowsToEntryNodes(rows, "KEYWORD");
        } catch (Exception e) {
            log.error("关键词检索执行失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== 向量检索 ====================

    @Override
    public List<EntryNodeInfo> vectorSearch(String nodeType, String query, int maxResults) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        try {
            // 1. 向量化查询
            Embedding queryEmbedding = qwenEmbeddingModel.embed(query).content();

            // 2. 从 Pinecone 检索
            EmbeddingStore<TextSegment> store = ragTools.getEmbeddingStore(GRAPH_NODE_NAMESPACE);
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(maxResults)
                    .minScore(0.6)  // 语义匹配最低阈值
                    .build();
            EmbeddingSearchResult<TextSegment> result = store.search(request);

            // 3. 提取 elementId 并从 Neo4j 回查完整节点
            List<String> elementIds = new ArrayList<>();
            Map<String, Double> scoreMap = new HashMap<>();

            for (EmbeddingMatch<TextSegment> match : result.matches()) {
                TextSegment segment = match.embedded();
                String elementId = segment.metadata() != null
                        ? segment.metadata().getString("elementId")
                        : null;
                if (elementId != null) {
                    elementIds.add(elementId);
                    scoreMap.put(elementId, match.score());
                }
            }

            if (elementIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 4. 从 Neo4j 批量获取节点详情
            List<EntryNodeInfo> nodes = fetchNodesByIds(elementIds, scoreMap, "VECTOR");

            // 如果有指定类型，过滤
            if (nodeType != null && !nodeType.isBlank()) {
                nodes = nodes.stream()
                        .filter(n -> n.getLabels() != null && n.getLabels().contains(nodeType))
                        .collect(Collectors.toList());
            }

            return nodes;
        } catch (Exception e) {
            log.warn("向量检索执行失败 (可能 Pinecone 未就绪): {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== 降级检索 ====================

    @Override
    public List<EntryNodeInfo> fallbackSearch(String nodeType, String keyword, Integer studentId) {
        log.info("触发降级检索: nodeType={}, keyword={}, studentId={}", nodeType, keyword, studentId);

        // 策略 1：移除类型限制，全图搜索
        List<EntryNodeInfo> results = keywordSearch(null, keyword, studentId, 20);
        if (!results.isEmpty()) {
            log.info("降级策略1（全图搜索）: {} 条", results.size());
            return results;
        }

        // 策略 2：模糊匹配（部分关键词）
        String shortKeyword = keyword.length() > 1 ? keyword.substring(0, keyword.length() / 2) : keyword;
        if (!shortKeyword.equals(keyword)) {
            results = keywordSearch(null, shortKeyword, studentId, 20);
            if (!results.isEmpty()) {
                log.info("降级策略2（短关键词）: {} 条", results.size());
                return results;
            }
        }

        // 策略 3：返回同类型下的所有节点（作为兜底）
        if (nodeType != null && !nodeType.isBlank() && isValidIdentifier(nodeType)) {
            String cypher = String.format(
                    "MATCH (n:`%s`) WHERE n.studentId = $studentId RETURN n, labels(n) AS lbls LIMIT $limit",
                    nodeType);
            Map<String, Object> params = new HashMap<>();
            params.put("studentId", studentId);
            params.put("limit", 20);
            try {
                List<Map<String, Object>> rows = Neo4jUtil.runCypher(cypher, params);
                results = rowsToEntryNodes(rows, "FALLBACK");
                log.info("降级策略3（同类节点）: {} 条", results.size());
                return results;
            } catch (Exception e) {
                log.error("降级策略3失败: {}", e.getMessage());
            }
        }

        log.warn("所有降级策略均无结果");
        return Collections.emptyList();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 去重合并 + 加权排序。
     * 按 nodeId 去重，向量来源权重 0.7，关键词来源权重 0.3。
     */
    private List<EntryNodeInfo> mergeAndRank(List<EntryNodeInfo> allResults, int maxResults) {
        Map<String, EntryNodeInfo> merged = new LinkedHashMap<>();

        for (EntryNodeInfo info : allResults) {
            String key = info.getNodeId();
            if (merged.containsKey(key)) {
                // 已存在：累加分数
                EntryNodeInfo existing = merged.get(key);
                double newScore = existing.getRelevanceScore() + info.getRelevanceScore() * 0.5;
                merged.put(key, EntryNodeInfo.builder()
                        .nodeId(key)
                        .nodeType(existing.getNodeType())
                        .entityName(existing.getEntityName())
                        .properties(existing.getProperties())
                        .labels(existing.getLabels())
                        .relevanceScore(Math.min(newScore, 1.0))
                        .source(existing.getSource() + "+" + info.getSource())
                        .build());
            } else {
                // 根据来源调整权重
                double weight = "VECTOR".equals(info.getSource()) ? VECTOR_WEIGHT : KEYWORD_WEIGHT;
                info.setRelevanceScore(info.getRelevanceScore() * weight);
                merged.put(key, info);
            }
        }

        // 按分数降序排列
        return merged.values().stream()
                .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    /**
     * 将 Neo4j 原始查询结果转换为 EntryNodeInfo 列表。
     */
    private List<EntryNodeInfo> rowsToEntryNodes(List<Map<String, Object>> rows, String source) {
        List<EntryNodeInfo> results = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            @SuppressWarnings("unchecked")
            Map<String, Object> nodeData = (Map<String, Object>) row.get("n");
            if (nodeData == null) continue;

            @SuppressWarnings("unchecked")
            List<String> labels;
            Object rawLabels = row.getOrDefault("lbls",
                    nodeData.getOrDefault("_labels", Collections.emptyList()));
            if (rawLabels instanceof List<?> list) {
                labels = list.stream().map(Object::toString).collect(Collectors.toList());
            } else if (rawLabels instanceof String s) {
                labels = List.of(s);
            } else {
                labels = Collections.emptyList();
            }

            String nodeId = (String) nodeData.getOrDefault("_elementId", nodeData.get("uuid"));
            String nodeType = (labels != null && !labels.isEmpty()) ? labels.get(0) : "Unknown";
            String entityName = (String) nodeData.getOrDefault("name",
                    nodeData.getOrDefault("courseName",
                            nodeData.getOrDefault("chapterName",
                                    nodeData.getOrDefault("sectionName", ""))));

            EntryNodeInfo info = EntryNodeInfo.builder()
                    .nodeType(nodeType)
                    .entityName(entityName)
                    .nodeId(nodeId)
                    .properties(nodeData)
                    .labels(labels)
                    .relevanceScore(source.equals("VECTOR") ? 0.8 : 0.6)
                    .source(source)
                    .build();
            results.add(info);
        }
        return results;
    }

    /**
     * 根据 elementId 列表从 Neo4j 批量获取节点详情。
     */
    private List<EntryNodeInfo> fetchNodesByIds(List<String> elementIds,
                                                  Map<String, Double> scoreMap,
                                                  String source) {
        // 使用 elementId() 函数进行精确查找
        StringBuilder cypher = new StringBuilder("UNWIND $ids AS id ");
        cypher.append("MATCH (n) WHERE elementId(n) = id ");
        cypher.append("RETURN n, labels(n) AS lbls");

        Map<String, Object> params = Map.of("ids", elementIds);

        try {
            List<Map<String, Object>> rows = Neo4jUtil.runCypher(cypher.toString(), params);
            List<EntryNodeInfo> nodes = rowsToEntryNodes(rows, source);

            // 回填向量搜索的分数
            for (EntryNodeInfo node : nodes) {
                if (scoreMap.containsKey(node.getNodeId())) {
                    node.setRelevanceScore(scoreMap.get(node.getNodeId()));
                }
            }
            return nodes;
        } catch (Exception e) {
            log.error("根据 elementId 回查节点失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
