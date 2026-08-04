package com.project.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeBase {
    /*
     * 主键ID
     */
    private Long id;
    
    /*
     * 用户ID
     */
    private Long studentId;
    
    /*
     * 知识库名称
     */
    private String name;
    
    /*
     * 知识库描述
     */
    private String description;
    
    /*
     * 文档数量
     */
    private Integer documentCount;
    
    /*
     * 状态：0-禁用，1-启用
     */
    private Integer status;
    
    /*
     * Pinecone向量数据库命名空间
     */
    private String pineconeNamespace;
    
    /*
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /*
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
