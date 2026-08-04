package com.project.platform.neo4j.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.project.platform.agents.ai.KnowledgeRagHelperService;
import com.project.platform.neo4j.dto.CypherValidationResult;
import com.project.platform.neo4j.dto.EntryNodeInfo;
import com.project.platform.neo4j.dto.GraphQueryResultVO;
import com.project.platform.neo4j.exception.CypherValidationException;
import com.project.platform.neo4j.exception.GraphQueryException;
import com.project.platform.neo4j.util.Neo4jUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 知识图谱智能查询编排实现。
 *
 * <p>6 步流程编排，含重试、降级、异常处理。
 */
@Slf4j
@Service
public class KnowledgeGraphQueryServiceImpl implements KnowledgeGraphQueryService {

    /** Cypher 校验 + 修正的最大重试次数 */
    private static final int MAX_RETRY = 3;

    /** 入口节点识别 LLM 超时（秒） */
    private static final long IDENTIFY_TIMEOUT = 15;

    /** Cypher 生成 LLM 超时（秒） */
    private static final long GENERATE_TIMEOUT = 20;

    @Resource
    private KnowledgeRagHelperService llmService;

    @Resource
    private CypherGenerateService cypherGenerateService;

    @Resource
    private CypherValidateService cypherValidateService;

    @Resource
    private GraphRetrievalService graphRetrievalService;

    // ==================== 同步查询 ====================

    @Override
    public GraphQueryResultVO query(String userQuery, Integer studentId) {
        long totalStart = System.currentTimeMillis();
        Map<String, Long> stepTimings = new ConcurrentHashMap<>();
        int retryCount = 0;

        try {
            // ===== Step 1: 入口节点识别 =====
            long stepStart = System.currentTimeMillis();
            Step1Result step1 = identifyEntryNode(userQuery);
            stepTimings.put("step1_identify", System.currentTimeMillis() - stepStart);
            log.info("[Step1] 入口节点识别: type={}, entity={}, confidence={}",
                    step1.nodeType, step1.entityName, step1.confidence);

            // ===== Step 2: 混合检索入口节点 =====
            stepStart = System.currentTimeMillis();
            List<EntryNodeInfo> entryNodes = retrieveEntryNodes(
                    step1.nodeType, step1.entityName, studentId);
            boolean fallbackTriggered = false;

            if (entryNodes.isEmpty()) {
                log.warn("[Step2] 混合检索无结果，触发降级策略");
                entryNodes = graphRetrievalService.fallbackSearch(
                        step1.nodeType, step1.entityName, studentId);
                fallbackTriggered = true;
            }
            stepTimings.put("step2_retrieve", System.currentTimeMillis() - stepStart);
            log.info("[Step2] 检索完成: {} 个候选节点, fallback={}", entryNodes.size(), fallbackTriggered);

            // ===== Step 3: Cypher 生成 =====
            stepStart = System.currentTimeMillis();
            String entryNodeContext = buildEntryNodeContext(entryNodes);
            String cypher = generateCypher(userQuery, entryNodeContext);
            stepTimings.put("step3_generate", System.currentTimeMillis() - stepStart);
            log.info("[Step3] Cypher 生成完成: {}", cypher);

            // ===== Step 4-5: 校验 + 修正循环 =====
            stepStart = System.currentTimeMillis();
            CypherValidationResult validationResult;
            try {
                String finalCypher = validateAndCorrect(cypher, userQuery, entryNodeContext);
                // 最终再校验一次
                validationResult = parseValidationResult(
                        cypherValidateService.validateCypher(finalCypher, userQuery));
                cypher = finalCypher;
            } catch (CypherValidationException e) {
                // 重试耗尽仍失败：使用原始 Cypher 尝试执行
                validationResult = CypherValidationResult.builder()
                        .valid(false)
                        .errors(e.getErrors())
                        .build();
                log.warn("[Step4-5] 校验修正失败，尝试使用原始 Cypher 执行");
            }
            stepTimings.put("step4_5_validate_correct", System.currentTimeMillis() - stepStart);

            // ===== Step 6: 执行查询 =====
            stepStart = System.currentTimeMillis();
            Neo4jUtil.GraphResult graphData = executeQuery(cypher, studentId, entryNodes);
            stepTimings.put("step6_execute", System.currentTimeMillis() - stepStart);
            log.info("[Step6] 查询执行完成: nodes={}, rels={}, error={}",
                    graphData.getNodes().size(), graphData.getRelationships().size(), graphData.isHasError());

            return GraphQueryResultVO.builder()
                    .identifiedNodeType(step1.nodeType)
                    .identifiedEntityName(step1.entityName)
                    .entryNodes(entryNodes)
                    .fallbackTriggered(fallbackTriggered)
                    .generatedCypher(cypher)
                    .validationResult(validationResult)
                    .retryCount(retryCount)
                    .graphData(graphData)
                    .totalExecutionTimeMs(System.currentTimeMillis() - totalStart)
                    .stepTimings(stepTimings)
                    .success(!graphData.isHasError())
                    .build();

        } catch (GraphQueryException e) {
            log.error("知识图谱查询失败 [phase={}]: {}", e.getPhase(), e.getMessage());
            return GraphQueryResultVO.builder()
                    .success(false)
                    .errorMessage("查询失败 [" + e.getPhase() + "]: " + e.getMessage())
                    .totalExecutionTimeMs(System.currentTimeMillis() - totalStart)
                    .stepTimings(stepTimings)
                    .build();
        } catch (Exception e) {
            log.error("知识图谱查询异常", e);
            return GraphQueryResultVO.builder()
                    .success(false)
                    .errorMessage("系统异常: " + e.getMessage())
                    .totalExecutionTimeMs(System.currentTimeMillis() - totalStart)
                    .stepTimings(stepTimings)
                    .build();
        }
    }

    // ==================== 流式查询 ====================

    @Override
    public Flux<String> queryStreaming(String userQuery, Integer studentId) {
        return Mono.fromCallable(() -> {
                    // 同步执行完整流程
                    GraphQueryResultVO result = query(userQuery, studentId);
                    // 转为 SSE 事件
                    JSONObject json = JSONUtil.parseObj(result);
                    return json.toStringPretty();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofSeconds(120))
                .flatMapMany(result -> Flux.just(
                        "event: result\ndata: " + result + "\n\n",
                        "event: done\ndata: [DONE]\n\n"
                ))
                .onErrorResume(e -> Flux.just(
                        "event: error\ndata: " + e.getMessage() + "\n\n",
                        "event: done\ndata: [DONE]\n\n"
                ));
    }

    // ==================== 6 步流程拆解 ====================

    /**
     * Step 1: 入口节点识别。
     * 调用 LLM 分析用户输入，确定查询知识图谱所需的入口节点类型和实体名称。
     */
    private Step1Result identifyEntryNode(String userQuery) {
        String prompt = buildEntryNodeIdentifyPrompt(userQuery);
        String response;
        try {
            response = llmService.chat(prompt);
        } catch (Exception e) {
            throw new GraphQueryException("IDENTIFY", "LLM 入口识别调用失败", e);
        }
        return parseStep1Response(response, userQuery);
    }

    /**
     * Step 2: 混合检索入口节点。
     */
    private List<EntryNodeInfo> retrieveEntryNodes(String nodeType, String entityName, Integer studentId) {
        try {
            return graphRetrievalService.hybridSearch(nodeType, entityName, studentId, 10);
        } catch (Exception e) {
            throw new GraphQueryException("RETRIEVE", "混合检索失败: " + e.getMessage(), e);
        }
    }

    /**
     * Step 3: 生成 Cypher。
     */
    private String generateCypher(String userQuery, String entryNodeContext) {
        try {
            String cypher = cypherGenerateService.generateCypher(userQuery, entryNodeContext);
            // 清理 LLM 输出中可能残留的 markdown 标记，并修正常见的 elementId 写法
            return normalizeGeneratedCypher(cleanCypherOutput(cypher));
        } catch (Exception e) {
            throw new GraphQueryException("GENERATE", "Cypher 生成失败", e);
        }
    }

    /**
     * Step 4-5: 校验 + 自动修正循环。
     */
    private String validateAndCorrect(String cypher, String userQuery, String entryNodeContext) {
        String currentCypher = cypher;

        for (int retry = 0; retry < MAX_RETRY; retry++) {
            String validationResponse;
            try {
                validationResponse = cypherValidateService.validateCypher(currentCypher, userQuery);
            } catch (Exception e) {
                log.warn("Cypher 校验 LLM 调用失败 (retry={}): {}", retry, e.getMessage());
                // LLM 调用失败时，如果不涉及写操作，可尝试直接执行
                if (!containsWriteOperation(currentCypher)) {
                    log.info("校验 LLM 不可用但 Cypher 不含写操作，跳过校验直接执行");
                    return currentCypher;
                }
                throw new GraphQueryException("VALIDATE", "Cypher 校验 LLM 不可用且可能含写操作", e);
            }

            CypherValidationResult result = parseValidationResult(validationResponse);

            if (result.isValid()) {
                log.info("Cypher 校验通过 (retry={})", retry);
                return currentCypher;
            }

            log.warn("Cypher 校验未通过 (retry={}): {} 个错误", retry + 1, result.getErrors().size());
            for (CypherValidationResult.ValidationError err : result.getErrors()) {
                log.warn("  - [{}] {}: {}", err.getErrorType(), err.getErrorLocation(), err.getMessage());
            }

            if (retry < MAX_RETRY - 1) {
                // Step 5: 修正
                try {
                    String errorsJson = JSONUtil.toJsonStr(result.getErrors());
                    currentCypher = cypherValidateService.correctCypher(
                            currentCypher, errorsJson, userQuery, entryNodeContext);
                    currentCypher = normalizeGeneratedCypher(cleanCypherOutput(currentCypher));
                    log.info("Cypher 修正完成 (retry={}): {}", retry + 1, currentCypher);
                } catch (Exception e) {
                    log.warn("Cypher 修正 LLM 调用失败 (retry={}): {}", retry, e.getMessage());
                }
            }
        }

        // 重试耗尽
        throw new CypherValidationException(
                "Cypher 校验失败超过最大重试次数 (" + MAX_RETRY + ")",
                parseValidationResult(cypherValidateService.validateCypher(currentCypher, userQuery)).getErrors(),
                currentCypher);
    }

    /**
     * Step 6: 执行查询。
     */
    private Neo4jUtil.GraphResult executeQuery(String cypher, Integer studentId, List<EntryNodeInfo> entryNodes) {
        Map<String, Object> params = buildQueryParams(studentId, entryNodes);
        try {
            return Neo4jUtil.runCypherStructured(cypher, params);
        } catch (Exception e) {
            throw new GraphQueryException("EXECUTE", "Cypher 执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建生成 Cypher 可使用的参数。
     */
    private Map<String, Object> buildQueryParams(Integer studentId, List<EntryNodeInfo> entryNodes) {
        Map<String, Object> params = new HashMap<>();
        params.put("studentId", studentId);

        List<String> entryNodeIds = entryNodes == null ? Collections.emptyList() : entryNodes.stream()
                .map(EntryNodeInfo::getNodeId)
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .collect(Collectors.toList());

        params.put("entryNodeIds", entryNodeIds);
        params.put("entryNodeId", entryNodeIds.isEmpty() ? "" : entryNodeIds.get(0));
        return params;
    }

    // ==================== Prompt 构建 ====================

    /**
     * 构建入口节点识别 Prompt。
     */
    private String buildEntryNodeIdentifyPrompt(String userQuery) {
        return """
                你是一个知识图谱查询分析专家。请根据用户的自然语言问题，分析并确定查询知识图谱所需的入口信息。

                ## 图模型
                - 节点类型：Course（课程）、Chapter（章节）、Section（小节）、KnowledgePoint（知识点）
                - 关系类型：HAS_CHAPTER、HAS_SECTION、HAS_KNOWLEDGE_POINT、PREREQUISITE、ADVANCED、RELATED

                ## 用户问题
                %s

                ## 输出要求
                返回 JSON 格式（不要包含 markdown 代码块标记）：
                {
                  "nodeType": "KnowledgePoint|Course|Chapter|Section",
                  "entityName": "识别的实体名称",
                  "confidence": 0.0-1.0,
                  "reasoning": "识别理由简述"
                }

                如果问题不需要特定入口节点（如统计、全局查询），nodeType 和 entityName 设为空字符串。
                """.formatted(userQuery);
    }

    /**
     * 构建入口节点上下文字符串（供 Cypher 生成使用）。
     */
    private String buildEntryNodeContext(List<EntryNodeInfo> entryNodes) {
        if (entryNodes == null || entryNodes.isEmpty()) {
            return "（未找到特定的入口节点，请基于图模型生成通用查询）";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("共找到 ").append(entryNodes.size()).append(" 个候选入口节点：\n\n");

        for (int i = 0; i < entryNodes.size(); i++) {
            EntryNodeInfo node = entryNodes.get(i);
            sb.append("--- 节点 ").append(i + 1).append(" ---\n");
            sb.append("元素ID: ").append(node.getNodeId()).append("\n");
            sb.append("标签: ").append(node.getLabels()).append("\n");
            sb.append("类型: ").append(node.getNodeType()).append("\n");
            sb.append("名称: ").append(node.getEntityName()).append("\n");
            if (node.getProperties() != null) {
                sb.append("属性: ").append(toJsonSafe(node.getProperties())).append("\n");
            }
            sb.append("相关度: ").append(String.format("%.2f", node.getRelevanceScore())).append("\n\n");
        }
        return sb.toString();
    }

    // ==================== 响应解析 ====================

    /**
     * 解析 Step 1 LLM 响应。
     */
    private Step1Result parseStep1Response(String response, String userQuery) {
        try {
            // 清理可能的 markdown 代码块标记
            String json = response.trim();
            if (json.startsWith("```")) {
                json = json.substring(json.indexOf("\n") + 1);
                if (json.endsWith("```")) {
                    json = json.substring(0, json.lastIndexOf("```")).trim();
                }
            }

            JSONObject obj = JSONUtil.parseObj(json);
            return new Step1Result(
                    obj.getStr("nodeType", ""),
                    obj.getStr("entityName", ""),
                    obj.getDouble("confidence", 0.5),
                    obj.getStr("reasoning", "")
            );
        } catch (Exception e) {
            log.warn("解析 Step 1 响应失败，使用默认值: {}", e.getMessage());
            return new Step1Result("", userQuery, 0.5, "解析失败，使用原始查询");
        }
    }

    /**
     * 解析校验 LLM 返回的 JSON 为 CypherValidationResult。
     */
    private CypherValidationResult parseValidationResult(String response) {
        try {
            String json = response.trim();
            if (json.startsWith("```")) {
                json = json.substring(json.indexOf("\n") + 1);
                if (json.endsWith("```")) {
                    json = json.substring(0, json.lastIndexOf("```")).trim();
                }
            }

            JSONObject obj = JSONUtil.parseObj(json);
            boolean valid = obj.getBool("valid", false);

            List<CypherValidationResult.ValidationError> errors = new ArrayList<>();
            JSONArray errorArray = obj.getJSONArray("errors");
            if (errorArray != null) {
                for (int i = 0; i < errorArray.size(); i++) {
                    JSONObject errObj = errorArray.getJSONObject(i);
                    errors.add(CypherValidationResult.ValidationError.builder()
                            .errorType(errObj.getStr("errorType", "UNKNOWN"))
                            .severity(errObj.getStr("severity", "BLOCKING"))
                            .errorLocation(errObj.getStr("errorLocation", ""))
                            .message(errObj.getStr("message", ""))
                            .suggestion(errObj.getStr("suggestion", ""))
                            .build());
                }
            }

            return CypherValidationResult.builder()
                    .valid(valid)
                    .errors(errors)
                    .build();
        } catch (Exception e) {
            log.warn("解析校验结果 JSON 失败: {}", e.getMessage());
            // 降级：认为通过校验（容错）
            return CypherValidationResult.builder().valid(true).errors(Collections.emptyList()).build();
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 清理 LLM 输出的 Cypher 中的 markdown 标记。
     */
    private String cleanCypherOutput(String cypher) {
        if (cypher == null) return "";
        String cleaned = cypher.trim();
        if (cleaned.startsWith("```")) {
            int start = cleaned.indexOf("\n");
            if (start > 0) cleaned = cleaned.substring(start + 1);
            else cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.lastIndexOf("```")).trim();
        }
        return cleaned.trim();
    }

    /**
     * 修正模型偶尔生成的属性式 elementId 写法。
     */
    private String normalizeGeneratedCypher(String cypher) {
        if (cypher == null || cypher.isBlank()) return cypher;
        return cypher.replaceAll("(?i)\\b([A-Za-z_][A-Za-z0-9_]*)\\.elementId\\b", "elementId($1)");
    }

    /**
     * 检测 Cypher 是否包含写操作。
     */
    private boolean containsWriteOperation(String cypher) {
        if (cypher == null) return false;
        String upper = cypher.toUpperCase();
        return upper.contains("DELETE") || upper.contains("DETACH DELETE")
                || upper.contains("DROP") || upper.contains("REMOVE")
                || upper.contains("CREATE") || upper.contains("MERGE")
                || upper.contains("SET");
    }

    /**
     * 安全的 JSON 序列化。
     */
    private String toJsonSafe(Object obj) {
        try {
            return JSONUtil.toJsonStr(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    // ==================== 内部类 ====================

    /**
     * Step 1 识别结果。
     */
    private record Step1Result(String nodeType, String entityName, double confidence, String reasoning) {
    }
}
