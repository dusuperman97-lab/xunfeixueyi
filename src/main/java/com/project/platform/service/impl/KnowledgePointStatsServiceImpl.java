package com.project.platform.service.impl;

import com.project.platform.entity.KnowledgePointStats;
import com.project.platform.mapper.KnowledgePointStatsMapper;
import com.project.platform.service.KnowledgePointStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识点统计服务实现类
 */
@Service
public class KnowledgePointStatsServiceImpl implements KnowledgePointStatsService {
    
    @Autowired
    private KnowledgePointStatsMapper knowledgePointStatsMapper;
    
    /**
     * 根据用户ID和小节ID查询知识点统计
     */
    @Override
    public List<KnowledgePointStats> getKnowledgePointStats(Integer userId, Integer sectionId) {
        return knowledgePointStatsMapper.selectByUserAndSection(userId, sectionId);
    }
    
    /**
     * 根据用户ID查询所有知识点统计
     */
    @Override
    public List<KnowledgePointStats> getAllKnowledgePointStats(Integer userId) {
        return knowledgePointStatsMapper.selectByUserId(userId);
    }
    
    /**
     * 根据掌握程度查询知识点
     */
    @Override
    public List<KnowledgePointStats> getKnowledgePointsByMasteryLevel(Integer userId, String masteryLevel) {
        return knowledgePointStatsMapper.selectByMasteryLevel(userId, masteryLevel);
    }
}
