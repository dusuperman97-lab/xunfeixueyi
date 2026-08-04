package com.project.platform.mapper;

import com.project.platform.entity.KnowledgePointSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识点来源追踪Mapper接口
 */
@Mapper
public interface KnowledgePointSourceMapper {

    /**
     * 插入来源记录
     */
    int insert(KnowledgePointSource source);

    /**
     * 批量插入来源记录
     */
    int batchInsert(@Param("sources") List<KnowledgePointSource> sources);

    /**
     * 根据知识点名称查询来源
     */
    List<KnowledgePointSource> selectByKnowledgePoint(@Param("knowledgePoint") String knowledgePoint,
                                                       @Param("studentId") Integer studentId);

    /**
     * 根据学生ID查询所有来源
     */
    List<KnowledgePointSource> selectByStudentId(@Param("studentId") Integer studentId);

    /**
     * 根据来源类型和ID查询
     */
    List<KnowledgePointSource> selectBySource(@Param("sourceType") String sourceType,
                                               @Param("sourceId") Integer sourceId);

    /**
     * 更新来源记录
     */
    int update(KnowledgePointSource source);

    /**
     * 删除来源记录
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据学生ID删除所有来源
     */
    int deleteByStudentId(@Param("studentId") Integer studentId);
}
