package com.project.platform.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识来源
 * 用于标识答案引用的具体文档片段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeSource {
    
    /**
     * 来源文件名称
     */
    private String fileName;
    
    /**
     * 知识库ID
     */
    private Long kbId;
    
    /**
     * 知识库名称
     */
    private String kbName;
    
    /**
     * 文档ID
     */
    private Long docId;
    
    /**
     * 相似度分数
     */
    private Double score;
    
    /**
     * 内容片段
     */
    private String snippet;

    /**
     * 分片索引
     */
    private Integer chunkIndex;
}
