package com.project.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeDocument {
    /*
     * 主键ID
     */
    private Long id;
    
    /*
     * 知识库ID
     */
    private Long kbId;
    
    /*
     * 用户ID
     */
    private Long studentId;
    
    /*
     * 文件名称
     */
    private String fileName;
    
    /*
     * 文件存储路径
     */
    private String filePath;
    
    /*
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /*
     * 文件类型：pdf, docx, txt, md
     */
    private String fileType;
    
    /*
     * 内容摘要
     */
    private String contentSummary;
    
    /*
     * 文档分片数量
     */
    private Integer segmentCount;
    
    /*
     * 向量化状态：0-未处理，1-处理中，2-已完成，3-失败
     */
    private Integer embeddingStatus;

    /*
     * 处理失败时的错误信息
     */
    private String errorMessage;
    
    /*
     * 状态：0-删除，1-正常
     */
    private Integer status;
    
    /*
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /*
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
