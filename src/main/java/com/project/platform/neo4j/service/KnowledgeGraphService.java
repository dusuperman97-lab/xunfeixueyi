package com.project.platform.neo4j.service;

import com.project.platform.neo4j.dto.GraphDataVO;

import java.util.List;
import java.util.Map;

/**
 * 知识图谱服务接口
 * 基于Neo4j图数据库，提供课程-知识点、课程-课程的关系图谱
 *
 * 图模型设计:
 * 节点标签: Course(课程), Chapter(章节), Section(小节), KnowledgePoint(知识点)
 * 关系类型:
 *   (Course)-[:HAS_CHAPTER]->(Chapter)       课程包含章节
 *   (Chapter)-[:HAS_SECTION]->(Section)      章节包含小节
 *   (Section)-[:HAS_KNOWLEDGE_POINT]->(KP)   小节包含知识点
 *   (Course)-[:PREREQUISITE]->(Course)       前置课程
 *   (Course)-[:ADVANCED]->(Course)           进阶课程
 *   (KnowledgePoint)-[:PREREQUISITE]->(KP)   前置知识点
 *   (KnowledgePoint)-[:RELATED]->(KP)        相关知识点
 */
public interface KnowledgeGraphService {

    /**
     * 从MySQL课程数据同步构建Neo4j知识图谱
     * 创建课程、章节、小节、知识点的完整图谱结构
     *
     * @param courseId 课程ID
     * @param studentId 学生ID
     * @return 图谱可视化数据
     */
    GraphDataVO buildFromCourse(Integer courseId, Integer studentId);

    /**
     * 获取课程的知识图谱（课程-章节-小节-知识点层级关系）
     *
     * @param courseId 课程ID
     * @param studentId 学生ID
     * @return 图谱可视化数据
     */
    GraphDataVO getCourseGraph(Integer courseId, Integer studentId);

    /**
     * 获取小节的知识点图谱
     * 展示小节与其知识点的直接关系，以及知识点间的关联
     *
     * @param sectionId 小节ID
     * @param studentId 学生ID
     * @return 图谱可视化数据
     */
    GraphDataVO getSectionKnowledgeGraph(Integer sectionId, Integer studentId);

    /**
     * 获取知识点为中心的扩展图谱
     * 从某个知识点出发，展示其前置/后续/相关知识点
     *
     * @param knowledgePoint 知识点名称
     * @param studentId 学生ID
     * @return 图谱可视化数据
     */
    GraphDataVO getKnowledgePointGraph(String knowledgePoint, Integer studentId);

    /**
     * 获取课程间的关系图谱
     * 展示课程之间的前置/进阶关系
     *
     * @param studentId 学生ID
     * @return 图谱可视化数据
     */
    GraphDataVO getCourseRelationGraph(Integer studentId);

    /**
     * 搜索知识点
     *
     * @param keyword 搜索关键词
     * @param studentId 学生ID
     * @return 匹配的知识点节点列表
     */
    List<Map<String, Object>> searchKnowledgePoints(String keyword, Integer studentId);

    /**
     * 创建知识点间的关系
     *
     * @param fromKpName 起始知识点名称
     * @param toKpName 目标知识点名称
     * @param relationType 关系类型（PREREQUISITE/RELATED）
     * @param studentId 学生ID
     */
    void createKnowledgePointRelation(String fromKpName, String toKpName,
                                      String relationType, Integer studentId);

    /**
     * 创建课程间的关系
     *
     * @param fromCourseId 起始课程ID
     * @param toCourseId 目标课程ID
     * @param relationType 关系类型（PREREQUISITE/ADVANCED）
     */
    void createCourseRelation(Integer fromCourseId, Integer toCourseId, String relationType);

    /**
     * 获取图谱统计信息
     *
     * @param studentId 学生ID
     * @return 统计数据
     */
    Map<String, Object> getGraphStats(Integer studentId);

    /**
     * 删除指定学生的所有图谱数据
     *
     * @param studentId 学生ID
     */
    void deleteStudentGraph(Integer studentId);
}