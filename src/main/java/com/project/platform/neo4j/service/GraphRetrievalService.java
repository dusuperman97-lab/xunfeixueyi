package com.project.platform.neo4j.service;

import com.project.platform.neo4j.dto.EntryNodeInfo;

import java.util.List;

/**
 * 知识图谱混合检索服务接口。
 * 结合向量相似度检索（Pinecone）与关键词检索（Neo4j CONTAINS），
 * 从图谱中查找与查询最相关的入口节点。
 */
public interface GraphRetrievalService {

    /**
     * 混合检索入口节点。
     * 向量检索 + 关键词检索 → 去重合并 → 按相关度排序。
     *
     * @param nodeType   目标节点类型（如 KnowledgePoint、Course），null 表示不限类型
     * @param entityName 实体名称
     * @param studentId  学生 ID（租户隔离）
     * @param maxResults 最大返回数
     * @return 候选入口节点列表（按 relevanceScore 降序）
     */
    List<EntryNodeInfo> hybridSearch(String nodeType, String entityName,
                                      Integer studentId, int maxResults);

    /**
     * 降级检索：当混合检索无结果时触发。
     * 策略：去掉类型限制 → 模糊匹配 → 返回同类节点。
     *
     * @param nodeType   原始目标节点类型
     * @param keyword    搜索关键词
     * @param studentId  学生 ID
     * @return 兜底检索结果
     */
    List<EntryNodeInfo> fallbackSearch(String nodeType, String keyword, Integer studentId);

    /**
     * 纯关键词检索（Neo4j CONTAINS）。
     */
    List<EntryNodeInfo> keywordSearch(String nodeType, String keyword, Integer studentId, int limit);

    /**
     * 纯向量检索（Pinecone 语义相似度）。
     */
    List<EntryNodeInfo> vectorSearch(String nodeType, String query, int maxResults);
}
