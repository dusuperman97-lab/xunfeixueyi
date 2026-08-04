package com.project.platform.service;

import com.project.platform.entity.KnowledgePointStats;

import java.util.List;

/**
 * 知识点统计服务接口
 */
public interface KnowledgePointStatsService {
    
    /**
     * 根据用户ID和小节ID查询知识点统计
     */
    List<KnowledgePointStats> getKnowledgePointStats(Integer userId, Integer sectionId);
    
    /**
     * 根据用户ID查询所有知识点统计
     */
    List<KnowledgePointStats> getAllKnowledgePointStats(Integer userId);
    
    /**
     * 根据掌握程度查询知识点
     */
    List<KnowledgePointStats> getKnowledgePointsByMasteryLevel(Integer userId, String masteryLevel);
}
