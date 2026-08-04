package com.project.platform.service;

import com.project.platform.entity.KnowledgeBase;

import java.util.List;

public interface KnowledgeBaseService {
    
    /**
     * 创建知识库
     */
    KnowledgeBase createKnowledgeBase(Long studentId, String name, String description);
    
    /**
     * 更新知识库
     */
    void updateKnowledgeBase(Long id, Long studentId, String name, String description);
    
    /**
     * 删除知识库
     */
    void deleteKnowledgeBase(Long id, Long studentId);
    
    /**
     * 获取用户的所有知识库
     */
    List<KnowledgeBase> getKnowledgeBases(Long studentId);
    
    /**
     * 获取知识库详情
     */
    KnowledgeBase getKnowledgeBaseById(Long id, Long studentId);
}
