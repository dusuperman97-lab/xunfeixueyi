package com.project.platform.neo4j.dto;

import lombok.Data;

import java.util.List;

/**
 * 图谱节点数据传输对象
 * 用于前端可视化的节点数据
 */
@Data
public class GraphNodeVO {

    /**
     * Neo4j 内部元素ID
     */
    private String uuid;

    /**
     * 节点标签列表（如 ["Course"]、["KnowledgePoint"]）
     */
    private List<String> labels;

    /**
     * 业务ID（courseId / chapterId / sectionId / kpId）
     */
    private Integer businessId;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点类型标签（取主标签：Course/Chapter/Section/KnowledgePoint）
     */
    private String type;

    /**
     * 节点描述
     */
    private String description;

    /**
     * 学生ID
     */
    private Integer studentId;

    /**
     * 是否为中心节点
     */
    private Boolean isCenter;
}
