package com.project.platform.mapper;

import com.project.platform.entity.KnowledgePointStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识点统计Mapper接口
 */
@Mapper
public interface KnowledgePointStatsMapper {
    
    /**
     * 插入或更新知识点统计
     */
    int upsert(KnowledgePointStats stats);
    
    /**
     * 根据用户ID和小节ID查询知识点统计
     */
    List<KnowledgePointStats> selectByUserAndSection(@Param("userId") Integer userId, 
                                                      @Param("sectionId") Integer sectionId);
    
    /**
     * 根据用户ID查询所有知识点统计
     */
    List<KnowledgePointStats> selectByUserId(@Param("userId") Integer userId);
    
    /**
     * 根据掌握程度查询知识点
     */
    List<KnowledgePointStats> selectByMasteryLevel(@Param("userId") Integer userId, 
                                                    @Param("masteryLevel") String masteryLevel);
}
