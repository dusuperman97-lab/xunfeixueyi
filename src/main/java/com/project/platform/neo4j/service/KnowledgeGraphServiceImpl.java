package com.project.platform.neo4j.service;

import com.project.platform.entity.Chapter;
import com.project.platform.entity.Course;
import com.project.platform.entity.Question;
import com.project.platform.entity.Section;
import com.project.platform.mapper.ChapterMapper;
import com.project.platform.mapper.CourseMapper;
import com.project.platform.mapper.QuestionMapper;
import com.project.platform.mapper.SectionMapper;
import com.project.platform.neo4j.dto.GraphDataVO;
import com.project.platform.neo4j.dto.GraphEdgeVO;
import com.project.platform.neo4j.dto.GraphNodeVO;
import com.project.platform.neo4j.util.Neo4jUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识图谱服务实现类
 * 使用 Neo4jUtil 原生 Cypher 查询，语义化节点标签和关系类型
 *
 * 图模型:
 *   (:Course {businessId, name, description, studentId})
 *   (:Chapter {businessId, name, studentId})
 *   (:Section  {businessId, name, studentId})
 *   (:KnowledgePoint {name, description, studentId})
 *
 *   (Course)-[:HAS_CHAPTER]->(Chapter)
 *   (Chapter)-[:HAS_SECTION]->(Section)
 *   (Section)-[:HAS_KNOWLEDGE_POINT]->(KnowledgePoint)
 *   (Course)-[:PREREQUISITE]->(Course)
 *   (Course)-[:ADVANCED]->(Course)
 *   (KnowledgePoint)-[:PREREQUISITE]->(KnowledgePoint)
 *   (KnowledgePoint)-[:RELATED]->(KnowledgePoint)
 */
@Slf4j
@Service
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private ChapterMapper chapterMapper;

    @Autowired
    private SectionMapper sectionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    // ==================== 构建图谱 ====================

    @Override
    public GraphDataVO buildFromCourse(Integer courseId, Integer studentId) {
        log.info("=== 从课程构建Neo4j知识图谱，courseId={}, studentId={}", courseId, studentId);

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            log.warn("课程不存在: {}", courseId);
            return buildEmptyResult();
        }

        int courseNodeCount = 0, chapterCount = 0, sectionCount = 0, kpCount = 0;

        // 1. MERGE 课程节点（参数化查询）
        Map<String, Object> courseParams = new HashMap<>();
        courseParams.put("businessId", courseId);
        courseParams.put("studentId", studentId);
        courseParams.put("name", course.getCourseName());
        courseParams.put("description", course.getDescription() != null ? course.getDescription() : "");
        Neo4jUtil.runCypherWrite(
                "MERGE (c:Course {businessId: $businessId, studentId: $studentId}) " +
                "SET c.name = $name, c.description = $description",
                courseParams);
        courseNodeCount++;

        // 2. 遍历章节
        List<Chapter> chapters = chapterMapper.listByCourseId(courseId);
        log.info("课程共 {} 个章节", chapters.size());

        for (Chapter chapter : chapters) {
            chapterCount++;
            // MERGE 章节节点 + 课程->章节关系
            Map<String, Object> chParams = new HashMap<>();
            chParams.put("businessId", chapter.getId());
            chParams.put("studentId", studentId);
            chParams.put("name", chapter.getChapterName());
            chParams.put("courseBusinessId", courseId);
            Neo4jUtil.runCypherWrite(
                    "MERGE (ch:Chapter {businessId: $businessId, studentId: $studentId}) " +
                    "SET ch.name = $name " +
                    "WITH ch " +
                    "MATCH (c:Course {businessId: $courseBusinessId, studentId: $studentId}) " +
                    "MERGE (c)-[:HAS_CHAPTER]->(ch)",
                    chParams);

            // 3. 遍历小节
            List<Section> sections = sectionMapper.listByChapterId(chapter.getId());
            log.info("  章节「{}」共 {} 个小节", chapter.getChapterName(), sections.size());

            for (Section section : sections) {
                sectionCount++;
                // MERGE 小节节点 + 章节->小节关系
                Map<String, Object> secParams = new HashMap<>();
                secParams.put("businessId", section.getId());
                secParams.put("studentId", studentId);
                secParams.put("name", section.getSectionName());
                secParams.put("chapterBusinessId", chapter.getId());
                Neo4jUtil.runCypherWrite(
                        "MERGE (s:Section {businessId: $businessId, studentId: $studentId}) " +
                        "SET s.name = $name " +
                        "WITH s " +
                        "MATCH (ch:Chapter {businessId: $chapterBusinessId, studentId: $studentId}) " +
                        "MERGE (ch)-[:HAS_SECTION]->(s)",
                        secParams);

                // 4. 从题目中提取知识点
                List<Question> questions = questionMapper.selectBySectionId(section.getId());
                Set<String> knowledgePoints = questions.stream()
                        .map(Question::getKnowledgePoint)
                        .filter(kp -> kp != null && !kp.isEmpty())
                        .collect(Collectors.toSet());

                if (!knowledgePoints.isEmpty()) {
                    log.info("    小节「{}」有 {} 个知识点: {}",
                            section.getSectionName(), knowledgePoints.size(), knowledgePoints);
                }

                for (String kp : knowledgePoints) {
                    kpCount++;
                    // MERGE 知识点节点 + 小节->知识点关系
                    Map<String, Object> kpParams = new HashMap<>();
                    kpParams.put("name", kp);
                    kpParams.put("studentId", studentId);
                    kpParams.put("description", "知识点: " + kp);
                    kpParams.put("sectionBusinessId", section.getId());
                    Neo4jUtil.runCypherWrite(
                            "MERGE (kp:KnowledgePoint {name: $name, studentId: $studentId}) " +
                            "SET kp.description = $description " +
                            "WITH kp " +
                            "MATCH (s:Section {businessId: $sectionBusinessId, studentId: $studentId}) " +
                            "MERGE (s)-[:HAS_KNOWLEDGE_POINT]->(kp)",
                            kpParams);
                }
            }
        }

        log.info("=== 课程知识图谱构建完成：Course=1, Chapter={}, Section={}, KnowledgePoint={}",
                chapterCount, sectionCount, kpCount);

        // 返回构建后的图谱数据
        return getCourseGraph(courseId, studentId);
    }

    // ==================== 查询图谱 ====================

    @Override
    public GraphDataVO getCourseGraph(Integer courseId, Integer studentId) {
        log.info("=== 获取课程知识图谱，courseId={}, studentId={}", courseId, studentId);

        // 查询课程下的完整层级路径，每条关系都绑定变量以便 Neo4jUtil 提取
        String cypher =
                "MATCH (c:Course {businessId: $courseId, studentId: $studentId}) " +
                "OPTIONAL MATCH (c)-[r_has_ch:HAS_CHAPTER]->(ch:Chapter {studentId: $studentId}) " +
                "OPTIONAL MATCH (ch)-[r_has_sec:HAS_SECTION]->(sec:Section {studentId: $studentId}) " +
                "OPTIONAL MATCH (sec)-[r_has_kp:HAS_KNOWLEDGE_POINT]->(kp:KnowledgePoint {studentId: $studentId}) " +
                "RETURN c, ch, sec, kp, r_has_ch, r_has_sec, r_has_kp";

        Map<String, Object> params = Map.of("courseId", courseId, "studentId", studentId);
        Neo4jUtil.GraphResult result = Neo4jUtil.runCypherStructured(cypher, params);

        List<Map<String, Object>> nodeList = result.getNodes();
        List<Map<String, Object>> relList = result.getRelationships();

        // 如果没有层级节点，尝试单独查课程节点
        if (nodeList.isEmpty()) {
            String singleCypher = "MATCH (c:Course {businessId: $courseId, studentId: $studentId}) RETURN c";
            List<Map<String, Object>> singleResult = Neo4jUtil.runCypher(singleCypher, params);
            nodeList = singleResult.stream()
                    // .map(m -> m) — Map类型一致，无需转换
                    .collect(Collectors.toList());
        }

        Course course = courseMapper.selectById(courseId);
        return convertToGraphDataVO(nodeList, relList,
                course != null ? course.getCourseName() : null);
    }

    @Override
    public GraphDataVO getSectionKnowledgeGraph(Integer sectionId, Integer studentId) {
        log.info("=== 获取小节知识点图谱，sectionId={}, studentId={}", sectionId, studentId);

        Map<String, Object> params = Map.of(
                "sectionId", sectionId,
                "studentId", studentId);

        String cypher =
                "MATCH (s:Section {businessId: $sectionId, studentId: $studentId}) " +
                "OPTIONAL MATCH (s)-[r_has_kp:HAS_KNOWLEDGE_POINT]->(kp:KnowledgePoint {studentId: $studentId}) " +
                "OPTIONAL MATCH (kp)-[r_kp_rel:PREREQUISITE|RELATED]-(kp2:KnowledgePoint {studentId: $studentId}) " +
                "RETURN s, kp, kp2, r_has_kp, r_kp_rel";

        Neo4jUtil.GraphResult result = Neo4jUtil.runCypherStructured(cypher, params);
        List<Map<String, Object>> nodeList = result.getNodes();
        List<Map<String, Object>> relList = result.getRelationships();

        Section section = sectionMapper.selectById(sectionId);
        return convertToGraphDataVO(nodeList, relList, section != null ? section.getSectionName() : null);
    }

    @Override
    public GraphDataVO getKnowledgePointGraph(String knowledgePoint, Integer studentId) {
        log.info("=== 获取知识点扩展图谱，knowledgePoint={}, studentId={}", knowledgePoint, studentId);

        Map<String, Object> params = Map.of(
                "name", knowledgePoint,
                "studentId", studentId);

        String cypher =
                "MATCH (kp:KnowledgePoint {name: $name, studentId: $studentId}) " +
                "OPTIONAL MATCH (kp)-[r_kp_rel:PREREQUISITE|RELATED*1..2]-(other:KnowledgePoint {studentId: $studentId}) " +
                "RETURN kp, other, r_kp_rel";

        Neo4jUtil.GraphResult result = Neo4jUtil.runCypherStructured(cypher, params);
        List<Map<String, Object>> nodeList = result.getNodes();
        List<Map<String, Object>> relList = result.getRelationships();

        return convertToGraphDataVO(nodeList, relList, knowledgePoint);
    }

    @Override
    public GraphDataVO getCourseRelationGraph(Integer studentId) {
        log.info("=== 获取课程关系图谱，studentId={}", studentId);

        Map<String, Object> params = Map.of("studentId", studentId);

        String cypher =
                "MATCH (c:Course {studentId: $studentId}) " +
                "OPTIONAL MATCH (c)-[r_course_rel:PREREQUISITE|ADVANCED]-(other:Course {studentId: $studentId}) " +
                "RETURN c, other, r_course_rel";

        Neo4jUtil.GraphResult result = Neo4jUtil.runCypherStructured(cypher, params);
        List<Map<String, Object>> nodeList = result.getNodes();
        List<Map<String, Object>> relList = result.getRelationships();

        return convertToGraphDataVO(nodeList, relList, null);
    }

    // ==================== 搜索 ====================

    @Override
    public List<Map<String, Object>> searchKnowledgePoints(String keyword, Integer studentId) {
        log.info("=== 搜索知识点，keyword={}, studentId={}", keyword, studentId);

        String cypher =
                "MATCH (kp:KnowledgePoint) WHERE kp.name CONTAINS $keyword AND kp.studentId = $studentId " +
                "RETURN kp.name AS name, kp.description AS description, kp.studentId AS studentId " +
                "LIMIT 20";

        Map<String, Object> params = Map.of(
                "keyword", keyword,
                "studentId", studentId);
        return Neo4jUtil.runCypher(cypher, params);
    }

    // ==================== 创建关系 ====================

    @Override
    public void createKnowledgePointRelation(String fromKpName, String toKpName,
                                              String relationType, Integer studentId) {
        log.info("=== 创建知识点关系: {} -[{}]-> {}, studentId={}", fromKpName, relationType, toKpName, studentId);

        // 校验关系类型（白名单，防止注入）
        if (!"PREREQUISITE".equals(relationType) && !"RELATED".equals(relationType)) {
            throw new IllegalArgumentException("不支持的关系类型: " + relationType + "，仅支持 PREREQUISITE / RELATED");
        }

        String label = "PREREQUISITE".equals(relationType) ? "前置" : "相关";
        Map<String, Object> params = new HashMap<>();
        params.put("fromName", fromKpName);
        params.put("toName", toKpName);
        params.put("studentId", studentId);
        params.put("label", label);
        // 关系类型无法参数化，但已通过白名单校验
        String cypher =
                "MATCH (a:KnowledgePoint {name: $fromName, studentId: $studentId}) " +
                "MATCH (b:KnowledgePoint {name: $toName, studentId: $studentId}) " +
                "MERGE (a)-[:" + relationType + " {label: $label}]->(b)";

        Neo4jUtil.runCypherWrite(cypher, params);
    }

    @Override
    public void createCourseRelation(Integer fromCourseId, Integer toCourseId, String relationType) {
        log.info("=== 创建课程关系: {} -[{}]-> {}", fromCourseId, relationType, toCourseId);

        if (!"PREREQUISITE".equals(relationType) && !"ADVANCED".equals(relationType)) {
            throw new IllegalArgumentException("不支持的关系类型: " + relationType + "，仅支持 PREREQUISITE / ADVANCED");
        }

        String label = "PREREQUISITE".equals(relationType) ? "前置课程" : "进阶课程";
        Map<String, Object> params = new HashMap<>();
        params.put("fromId", fromCourseId);
        params.put("toId", toCourseId);
        params.put("label", label);
        // 关系类型无法参数化，但已通过白名单校验
        String cypher =
                "MATCH (a:Course {businessId: $fromId}) " +
                "MATCH (b:Course {businessId: $toId}) " +
                "MERGE (a)-[:" + relationType + " {label: $label}]->(b)";

        Neo4jUtil.runCypherWrite(cypher, params);
    }

    // ==================== 统计 & 删除 ====================

    @Override
    public Map<String, Object> getGraphStats(Integer studentId) {
        log.info("=== 获取图谱统计信息，studentId={}", studentId);

        Map<String, Object> params = Map.of("studentId", studentId);

        String nodeCypher = "MATCH (n) WHERE n.studentId = $studentId RETURN count(n) AS cnt";
        List<Map<String, Object>> nodeResult = Neo4jUtil.runCypher(nodeCypher, params);
        long nodeCount = nodeResult.isEmpty() ? 0 :
                ((Number) nodeResult.get(0).get("cnt")).longValue();

        String edgeCypher = "MATCH (n)-[r]->(m) WHERE n.studentId = $studentId AND m.studentId = $studentId RETURN count(DISTINCT r) AS cnt";
        List<Map<String, Object>> edgeResult = Neo4jUtil.runCypher(edgeCypher, params);
        long edgeCount = edgeResult.isEmpty() ? 0 :
                ((Number) edgeResult.get(0).get("cnt")).longValue();

        Map<String, Object> stats = new HashMap<>();
        stats.put("nodes", nodeCount);
        stats.put("edges", edgeCount);
        return stats;
    }

    @Override
    public void deleteStudentGraph(Integer studentId) {
        log.info("=== 删除学生图谱数据，studentId={}", studentId);

        Map<String, Object> params = Map.of("studentId", studentId);
        Neo4jUtil.runCypherWrite(
                "MATCH (n) WHERE n.studentId = $studentId DETACH DELETE n", params);

        log.info("=== 学生图谱数据删除完成");
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将 Neo4jUtil 返回的原始 HashMap 数据转换为前端 DTO
     */
    private GraphDataVO convertToGraphDataVO(List<Map<String, Object>> nodeList,
                                              List<Map<String, Object>> relList,
                                              String centerNodeName) {
        GraphDataVO vo = new GraphDataVO();

        List<GraphNodeVO> nodes = new ArrayList<>();
        for (Map<String, Object> rawNode : nodeList) {
            GraphNodeVO nodeVO = new GraphNodeVO();
            nodeVO.setUuid((String) rawNode.get("uuid"));
            nodeVO.setLabels((List<String>) rawNode.get("labels"));
            nodeVO.setName((String) rawNode.get("name"));
            nodeVO.setDescription((String) rawNode.get("description"));
            nodeVO.setStudentId(rawNode.get("studentId") != null ? ((Number) rawNode.get("studentId")).intValue() : null);
            nodeVO.setBusinessId(rawNode.get("businessId") != null ? ((Number) rawNode.get("businessId")).intValue() : null);

            // 取第一个标签作为类型
            if (nodeVO.getLabels() != null && !nodeVO.getLabels().isEmpty()) {
                nodeVO.setType(nodeVO.getLabels().get(0));
            }

            // 判断是否为中心节点
            if (centerNodeName != null && centerNodeName.equals(nodeVO.getName())) {
                nodeVO.setIsCenter(true);
            } else {
                nodeVO.setIsCenter(false);
            }

            nodes.add(nodeVO);
        }

        List<GraphEdgeVO> edges = new ArrayList<>();
        for (Map<String, Object> rawRel : relList) {
            GraphEdgeVO edgeVO = new GraphEdgeVO();
            edgeVO.setUuid((String) rawRel.get("uuid"));
            edgeVO.setSourceId((String) rawRel.get("sourceId"));
            edgeVO.setTargetId((String) rawRel.get("targetId"));
            edgeVO.setType((String) rawRel.get("type"));
            edgeVO.setLabel((String) rawRel.get("label"));
            edgeVO.setWeight(rawRel.get("weight") != null ? ((Number) rawRel.get("weight")).doubleValue() : 1.0);
            edges.add(edgeVO);
        }

        vo.setNodes(nodes);
        vo.setEdges(edges);
        vo.setCenterNode(centerNodeName);
        vo.setTotalNodes(nodes.size());
        vo.setTotalEdges(edges.size());

        return vo;
    }

    /**
     * 构建空结果
     */
    private GraphDataVO buildEmptyResult() {
        GraphDataVO vo = new GraphDataVO();
        vo.setNodes(new ArrayList<>());
        vo.setEdges(new ArrayList<>());
        vo.setTotalNodes(0);
        vo.setTotalEdges(0);
        return vo;
    }

    /**
     * 转义Cypher字符串中的特殊字符
     */
    private String escapeCypher(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"");
    }
}
