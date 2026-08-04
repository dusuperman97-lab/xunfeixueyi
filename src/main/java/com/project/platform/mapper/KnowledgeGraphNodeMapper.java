package com.project.platform.mapper;

import com.project.platform.entity.KnowledgeGraphNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识图谱节点Mapper接口
 */
@Mapper
public interface KnowledgeGraphNodeMapper {

    /**
     * 插入节点
     */
    int insert(KnowledgeGraphNode node);

    /**
     * 批量插入节点
     */
    int batchInsert(@Param("nodes") List<KnowledgeGraphNode> nodes);

    /**
     * 根据ID查询节点
     */
    KnowledgeGraphNode selectById(@Param("id") Long id);

    /**
     * 根据节点名称和类型查询
     */
    KnowledgeGraphNode selectByNodeNameAndType(@Param("nodeName") String nodeName,
                                                @Param("nodeType") String nodeType,
                                                @Param("studentId") Integer studentId);

    /**
     * 查询知识图谱的所有节点
     */
    List<KnowledgeGraphNode> selectNodesByGraph(@Param("centerNodeName") String centerNodeName,
                                                 @Param("studentId") Integer studentId);

    /**
     * 根据学生ID查询所有节点
     */
    List<KnowledgeGraphNode> selectByStudentId(@Param("studentId") Integer studentId);

    /**
     * 根据节点类型查询
     */
    List<KnowledgeGraphNode> selectByNodeType(@Param("nodeType") String nodeType,
                                               @Param("studentId") Integer studentId);

    /**
     * 更新节点
     */
    int update(KnowledgeGraphNode node);

    /**
     * 删除节点
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据学生ID删除所有节点
     */
    int deleteByStudentId(@Param("studentId") Integer studentId);
}
