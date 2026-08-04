package com.project.platform.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识图谱关系实体类
 */
@Data
public class KnowledgeGraphEdge {
    /*
     * 主键ID
     */
    private Long id;
    
    /*
     * 源节点ID
     */
    private Long sourceNodeId;
    
    /*
     * 目标节点ID
     */
    private Long targetNodeId;
    
    /*
     * 关系类型：CONTAINS(包含), PREREQUISITE(前置), FOLLOW_UP(后续), RELATED(相关), SOURCE(来源)
     */
    private String relationshipType;
    
    /*
     * 关系标签（显示在连线上）
     */
    private String relationshipLabel;
    
    /*
     * 关系权重
     */
    private BigDecimal weight;
    
    /*
     * 学生ID（如果是个人知识点图谱）
     */
    private Integer studentId;
    
    /*
     * 创建时间
     */
    private LocalDateTime createTime;
}
