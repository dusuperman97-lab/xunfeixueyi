package com.project.platform.neo4j.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 入口节点信息 DTO。
 * 混合检索（向量 + 关键词）后返回的候选节点信息，
 * 供 Cypher 生成阶段作为上下文使用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntryNodeInfo {

    /** 节点类型：Course / Chapter / Section / KnowledgePoint */
    private String nodeType;

    /** 实体名称 */
    private String entityName;

    /** Neo4j 内部 elementId */
    private String nodeId;

    /** 节点的所有属性 */
    private Map<String, Object> properties;

    /** 节点的所有标签 */
    private List<String> labels;

    /** 检索相关度评分 (0.0 ~ 1.0) */
    private Double relevanceScore;

    /** 检索来源：VECTOR / KEYWORD / FALLBACK */
    private String source;
}
