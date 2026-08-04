package com.project.platform.neo4j.service;

import com.project.platform.neo4j.dto.GraphQueryResultVO;
import reactor.core.publisher.Flux;

/**
 * 知识图谱智能查询编排服务接口。
 *
 * <p>基于 LLM 的 6 步自然语言查询流程：
 * <ol>
 *   <li>入口节点识别：LLM 分析用户输入，确定节点类型和实体名称</li>
 *   <li>混合检索入口节点：向量相似度 + 关键词检索，获取候选节点</li>
 *   <li>Cypher 生成：LLM 结合用户意图和入口节点上下文生成 Cypher</li>
 *   <li>Cypher 校验：LLM 检查语法/语义/逻辑/安全性</li>
 *   <li>Cypher 自动修正：校验失败时 LLM 修正后重新校验（最多 3 次）</li>
 *   <li>执行查询：通过 Neo4jUtil.runCypherStructured 执行并返回结果</li>
 * </ol>
 */
public interface KnowledgeGraphQueryService {

    /**
     * 执行 6 步知识图谱智能查询（同步）。
     *
     * @param userQuery 用户自然语言查询（如 "五行学说有哪些前置知识点？"）
     * @param studentId 学生 ID（租户隔离）
     * @return 完整查询结果（含入口节点、Cypher、图谱数据、耗时等）
     */
    GraphQueryResultVO query(String userQuery, Integer studentId);

    /**
     * 执行 6 步知识图谱智能查询（异步流式 SSE）。
     * 逐步推送各阶段的进度和中间结果。
     *
     * @param userQuery 用户自然语言查询
     * @param studentId 学生 ID
     * @return SSE 事件流
     */
    Flux<String> queryStreaming(String userQuery, Integer studentId);
}
