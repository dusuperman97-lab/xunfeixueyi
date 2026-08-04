package com.project.platform.neo4j.controller;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.neo4j.dto.GraphDataVO;
import com.project.platform.neo4j.dto.GraphQueryResultVO;
import com.project.platform.neo4j.service.KnowledgeGraphQueryService;
import com.project.platform.neo4j.service.KnowledgeGraphService;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 知识图谱控制器
 * 统一入口：/api/knowledge-graph/**
 *
 * 图模型设计:
 *   节点: Course(课程), Chapter(章节), Section(小节), KnowledgePoint(知识点)
 *   关系: HAS_CHAPTER, HAS_SECTION, HAS_KNOWLEDGE_POINT, PREREQUISITE, ADVANCED, RELATED
 */
@RestController
@RequestMapping("/api/knowledge-graph")
@Slf4j
public class KnowledgeGraphController {

    @Resource
    private KnowledgeGraphService knowledgeGraphService;

    @Resource
    private KnowledgeGraphQueryService knowledgeGraphQueryService;

    @Resource
    private Tools tools;

    // ==================== 图谱构建 ====================

    /**
     * 从MySQL课程数据同步构建Neo4j知识图谱
     * POST /api/knowledge-graph/build?courseId=1
     */
    @PostMapping("/build")
    public ResponseVO<GraphDataVO> buildFromCourse(@RequestParam Integer courseId) {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== 构建课程知识图谱，courseId={}, studentId={}", courseId, studentId);
        return ResponseVO.ok(knowledgeGraphService.buildFromCourse(courseId, studentId));
    }

    // ==================== 图谱查询 ====================

    /**
     * 获取课程的完整知识图谱（课程-章节-小节-知识点层级关系）
     * GET /api/knowledge-graph/course?courseId=1
     */
    @GetMapping("/course")
    public ResponseVO<GraphDataVO> getCourseGraph(@RequestParam Integer courseId) {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== 获取课程图谱，courseId={}, studentId={}", courseId, studentId);
        return ResponseVO.ok(knowledgeGraphService.getCourseGraph(courseId, studentId));
    }

    /**
     * 获取小节的知识点图谱
     * GET /api/knowledge-graph/section?sectionId=1
     */
    @GetMapping("/section")
    public ResponseVO<GraphDataVO> getSectionKnowledgeGraph(@RequestParam Integer sectionId) {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== 获取小节知识点图谱，sectionId={}, studentId={}", sectionId, studentId);
        return ResponseVO.ok(knowledgeGraphService.getSectionKnowledgeGraph(sectionId, studentId));
    }

    /**
     * 获取知识点为中心的扩展图谱
     * GET /api/knowledge-graph/knowledge-point?name=五行学说
     */
    @GetMapping("/knowledge-point")
    public ResponseVO<GraphDataVO> getKnowledgePointGraph(@RequestParam String name) {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== 获取知识点图谱，name={}, studentId={}", name, studentId);
        return ResponseVO.ok(knowledgeGraphService.getKnowledgePointGraph(name, studentId));
    }

    /**
     * 获取课程间关系图谱
     * GET /api/knowledge-graph/course-relations
     */
    @GetMapping("/course-relations")
    public ResponseVO<GraphDataVO> getCourseRelationGraph() {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== 获取课程关系图谱，studentId={}", studentId);
        return ResponseVO.ok(knowledgeGraphService.getCourseRelationGraph(studentId));
    }

    // ==================== 搜索 ====================

    /**
     * 搜索知识点
     * GET /api/knowledge-graph/search?keyword=五行
     */
    @GetMapping("/search")
    public ResponseVO<List<Map<String, Object>>> searchKnowledgePoints(@RequestParam String keyword) {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== 搜索知识点，keyword={}, studentId={}", keyword, studentId);
        return ResponseVO.ok(knowledgeGraphService.searchKnowledgePoints(keyword, studentId));
    }

    // ==================== 关系创建 ====================

    /**
     * 创建知识点间的关系
     * POST /api/knowledge-graph/kp-relation?fromName=xxx&toName=xxx&relationType=PREREQUISITE
     */
    @PostMapping("/kp-relation")
    public ResponseVO<String> createKnowledgePointRelation(
            @RequestParam String fromName,
            @RequestParam String toName,
            @RequestParam String relationType) {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== 创建知识点关系: {} -[{}]-> {}, studentId={}", fromName, relationType, toName, studentId);
        knowledgeGraphService.createKnowledgePointRelation(fromName, toName, relationType, studentId);
        return ResponseVO.ok("关系创建成功");
    }

    /**
     * 创建课程间的关系
     * POST /api/knowledge-graph/course-relation?fromCourseId=1&toCourseId=2&relationType=PREREQUISITE
     */
    @PostMapping("/course-relation")
    public ResponseVO<String> createCourseRelation(
            @RequestParam Integer fromCourseId,
            @RequestParam Integer toCourseId,
            @RequestParam String relationType) {
        log.info("=== 创建课程关系: {} -[{}]-> {}", fromCourseId, relationType, toCourseId);
        knowledgeGraphService.createCourseRelation(fromCourseId, toCourseId, relationType);
        return ResponseVO.ok("课程关系创建成功");
    }

    // ==================== 统计 & 管理 ====================

    /**
     * 获取图谱统计信息
     * GET /api/knowledge-graph/stats
     */
    @GetMapping("/stats")
    public ResponseVO<Map<String, Object>> getGraphStats() {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== 获取图谱统计信息，studentId={}", studentId);
        return ResponseVO.ok(knowledgeGraphService.getGraphStats(studentId));
    }

    /**
     * 删除学生的所有图谱数据
     * DELETE /api/knowledge-graph/student-graph
     */
    @DeleteMapping("/student-graph")
    public ResponseVO<String> deleteStudentGraph() {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== 删除学生图谱数据，studentId={}", studentId);
        knowledgeGraphService.deleteStudentGraph(studentId);
        return ResponseVO.ok("图谱数据删除成功");
    }

    // ==================== 智能查询（LLM 自然语言 → Cypher） ====================

    /**
     * 自然语言知识图谱智能查询（同步，6 步流程）。
     *
     * <p>请求示例：
     * <pre>{@code
     * POST /api/knowledge-graph/smart-query
     * Content-Type: application/json
     * { "query": "五行学说有哪些前置知识点？" }
     * }</pre>
     */
    @PostMapping("/smart-query")
    public ResponseVO<GraphQueryResultVO> smartQuery(@RequestBody Map<String, String> body) {
        Integer studentId = tools.getCurrentUserIdInteger();
        String query = body.get("query");
        if (query == null || query.isBlank()) {
            return ResponseVO.fail(400, "查询内容不能为空");
        }
        log.info("=== 智能图谱查询开始 === studentId={}, query={}", studentId, query);
        GraphQueryResultVO result = knowledgeGraphQueryService.query(query, studentId);
        log.info("=== 智能图谱查询完成 === success={}, time={}ms",
                result.isSuccess(), result.getTotalExecutionTimeMs());
        return ResponseVO.ok(result);
    }

    /**
     * 自然语言知识图谱智能查询（流式 SSE）。
     *
     * <p>请求示例：
     * <pre>{@code
     * POST /api/knowledge-graph/smart-query/stream
     * Content-Type: application/json
     * { "query": "中医基础理论包含哪些章节？" }
     * }</pre>
     */
    @PostMapping(value = "/smart-query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> smartQueryStream(@RequestBody Map<String, String> body) {
        Integer studentId = tools.getCurrentUserIdInteger();
        String query = body.get("query");
        if (query == null || query.isBlank()) {
            return Flux.just("event: error\ndata: 查询内容不能为空\n\n",
                    "event: done\ndata: [DONE]\n\n");
        }
        log.info("=== 流式智能图谱查询开始 === studentId={}, query={}", studentId, query);
        return knowledgeGraphQueryService.queryStreaming(query, studentId);
    }
}
