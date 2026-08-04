package com.project.platform.mapper;

import com.project.platform.entity.KnowledgeGraphEdge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识图谱关系Mapper接口
 */
@Mapper
public interface KnowledgeGraphEdgeMapper {

    /**
     * 插入关系
     */
    int insert(KnowledgeGraphEdge edge);

    /**
     * 批量插入关系
     */
    int batchInsert(@Param("edges") List<KnowledgeGraphEdge> edges);

    /**
     * 根据ID查询关系
     */
    KnowledgeGraphEdge selectById(@Param("id") Long id);

    /**
     * 查询知识图谱的所有边
     */
    List<KnowledgeGraphEdge> selectEdgesByGraph(@Param("centerNodeName") String centerNodeName,
                                                 @Param("studentId") Integer studentId);

    /**
     * 根据源节点ID查询
     */
    List<KnowledgeGraphEdge> selectBySourceNodeId(@Param("sourceNodeId") Long sourceNodeId);

    /**
     * 根据目标节点ID查询
     */
    List<KnowledgeGraphEdge> selectByTargetNodeId(@Param("targetNodeId") Long targetNodeId);

    /**
     * 更新关系
     */
    int update(KnowledgeGraphEdge edge);

    /**
     * 删除关系
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据学生ID删除所有关系
     */
    int deleteByStudentId(@Param("studentId") Integer studentId);
}
