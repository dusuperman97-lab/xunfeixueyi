package com.project.platform.neo4j.dto;

import lombok.Data;

import java.util.List;

/**
 * 知识图谱可视化数据传输对象
 * 统一返回给前端的图谱数据格式
 */
@Data
public class GraphDataVO {

    /**
     * 节点列表
     */
    private List<GraphNodeVO> nodes;

    /**
     * 边列表
     */
    private List<GraphEdgeVO> edges;

    /**
     * 中心节点名称（可选）
     */
    private String centerNode;

    /**
     * 节点总数
     */
    private Integer totalNodes;

    /**
     * 边总数
     */
    private Integer totalEdges;
}
