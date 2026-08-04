package com.project.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识图谱节点实体类
 */
@Data
public class KnowledgeGraphNode {
    /*
     * 主键ID
     */
    private Long id;
    
    /*
     * 节点名称（知识点名称）
     */
    private String nodeName;
    
    /*
     * 节点类型：KNOWLEDGE_POINT(知识点), CHAPTER(章节), SECTION(小节), DOCUMENT(文档)
     */
    private String nodeType;
    
    /*
     * 关联的业务ID（chapter_id/section_id/document_id等）
     */
    private Integer nodeId;
    
    /*
     * 节点描述
     */
    private String description;
    
    /*
     * 层级：1-核心知识点，2-子知识点，3-来源
     */
    private Integer level;
    
    /*
     * 学生ID（如果是个人知识点图谱）
     */
    private Integer studentId;
    
    /*
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /*
     * 更新时间
     */
    private LocalDateTime updateTime;
}
