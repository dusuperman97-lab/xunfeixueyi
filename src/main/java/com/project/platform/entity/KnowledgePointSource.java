package com.project.platform.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识点来源追踪实体类
 */
@Data
public class KnowledgePointSource {
    /*
     * 主键ID
     */
    private Long id;
    
    /*
     * 知识点名称
     */
    private String knowledgePoint;
    
    /*
     * 来源类型：CHAPTER(章节), SECTION(小节), DOCUMENT(文档), QUESTION(题目)
     */
    private String sourceType;
    
    /*
     * 来源ID
     */
    private Integer sourceId;
    
    /*
     * 来源名称
     */
    private String sourceName;
    
    /*
     * 内容片段
     */
    private String contentSnippet;
    
    /*
     * 页码（如果是文档）
     */
    private Integer pageNumber;
    
    /*
     * 匹配置信度
     */
    private BigDecimal confidence;
    
    /*
     * 学生ID
     */
    private Integer studentId;
    
    /*
     * 创建时间
     */
    private LocalDateTime createTime;
}
