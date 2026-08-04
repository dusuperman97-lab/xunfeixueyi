package com.project.platform.controller.knowledge;

import com.project.platform.agents.config.utils.Tools;
import com.project.platform.neo4j.dto.GraphDataVO;
import com.project.platform.neo4j.service.KnowledgeGraphService;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识图谱兼容控制器
 * 旧接口 /knowledge-graph/** 代理到新的 Neo4j 知识图谱服务
 * 新接口请使用 /api/knowledge-graph/**
 */
//@RestController
//@RequestMapping("/knowledge-graph")
@Slf4j
public class LegacyKnowledgeGraphController {

    @Resource
    private KnowledgeGraphService knowledgeGraphService;

    @Resource
    private Tools tools;

    /**
     * 生成指定知识点的知识图谱（兼容旧接口）
     */
    @PostMapping("/generate")
    public ResponseVO<GraphDataVO> generateKnowledgeGraph(@RequestParam String knowledgePoint) {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== [兼容接口] 生成知识点图谱，知识点: {}, 学生ID: {}", knowledgePoint, studentId);
        return ResponseVO.ok(knowledgeGraphService.getKnowledgePointGraph(knowledgePoint, studentId));
    }

    /**
     * 从课程构建完整知识图谱（兼容旧接口）
     */
    @PostMapping("/build-from-course")
    public ResponseVO<GraphDataVO> buildKnowledgeGraphFromCourse(@RequestParam Integer courseId) {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== [兼容接口] 从课程构建图谱，课程ID: {}, 学生ID: {}", courseId, studentId);
        return ResponseVO.ok(knowledgeGraphService.buildFromCourse(courseId, studentId));
    }

    /**
     * 查询知识点的来源（兼容旧接口）
     */
    @GetMapping("/sources")
    public ResponseVO<List<Map<String, Object>>> getKnowledgePointSources(@RequestParam String knowledgePoint) {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== [兼容接口] 查询知识点来源，知识点: {}, 学生ID: {}", knowledgePoint, studentId);
        GraphDataVO graphData = knowledgeGraphService.getKnowledgePointGraph(knowledgePoint, studentId);
        List<Map<String, Object>> sources = graphData.getNodes().stream()
                .filter(n -> "Section".equals(n.getType()) || "Chapter".equals(n.getType()))
                .map(n -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("sourceType", n.getType());
                    map.put("sourceId", n.getBusinessId());
                    map.put("sourceName", n.getName());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
        return ResponseVO.ok(sources);
    }

    /**
     * 获取知识图谱数据（兼容旧接口）
     */
    @GetMapping("/graph-data")
    public ResponseVO<GraphDataVO> getGraphData(@RequestParam String centerNodeName) {
        Integer studentId = tools.getCurrentUserIdInteger();
        log.info("=== [兼容接口] 获取图谱数据，中心节点: {}, 学生ID: {}", centerNodeName, studentId);
        return ResponseVO.ok(knowledgeGraphService.getKnowledgePointGraph(centerNodeName, studentId));
    }
}
