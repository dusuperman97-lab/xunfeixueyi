package com.project.platform.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;

import java.util.List;

/**
 * 知识库检索服务
 * 负责从向量数据库中检索与用户问题相关的文档片段
 */
public interface KnowledgeRetrieverService {
    
    /**
     * 在指定知识库中检索相关内容
     * 
     * @param studentId 用户ID
     * @param kbId 知识库ID
     * @param query 用户查询
     * @param maxResults 最大返回结果数
     * @param minScore 最小相似度分数
     * @return 相关内容列表
     */
    List<Content> retrieveFromKnowledgeBase(Long studentId, Long kbId, String query, int maxResults, double minScore);
    
    /**
     * 在用户的所有知识库中检索相关内容
     * 
     * @param studentId 用户ID
     * @param query 用户查询
     * @param maxResults 最大返回结果数
     * @param minScore 最小相似度分数
     * @return 相关内容列表
     */
    List<Content> retrieveFromAllKnowledgeBases(Long studentId, String query, int maxResults, double minScore);
    
    /**
     * 格式化检索结果为上下文字符串
     * 
     * @param contents 检索到的内容
     * @return 格式化后的上下文字符串
     */
    String formatContext(List<Content> contents);
}
