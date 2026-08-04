package com.project.platform.neo4j.dto;

import lombok.Data;

/**
 * 图谱边数据传输对象
 * 用于前端可视化的关系数据
 */
@Data
public class GraphEdgeVO {

    /**
     * 关系唯一标识
     */
    private String uuid;

    /**
     * 起始节点元素ID
     */
    private String sourceId;

    /**
     * 目标节点元素ID
     */
    private String targetId;

    /**
     * 关系类型（如 HAS_CHAPTER / HAS_SECTION / HAS_KNOWLEDGE_POINT / PREREQUISITE / RELATED）
     */
    private String type;

    /**
     * 关系标签（中文显示，如 "包含章节" / "包含知识点" / "前置" / "相关"）
     */
    private String label;

    /**
     * 关系权重
     */
    private Double weight;
}
