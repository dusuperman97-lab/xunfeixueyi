package com.project.platform.neo4j.dto;

import com.project.platform.neo4j.util.Neo4jUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 知识图谱智能查询的统一结果 DTO。
 * 封装 6 步流程的全部中间产出和最终结果，方便前端展示和问题排查。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphQueryResultVO {

    // ========== Step 1 产出 ==========
    /** 识别的入口节点类型 */
    private String identifiedNodeType;
    /** 识别的实体名称 */
    private String identifiedEntityName;

    // ========== Step 2 产出 ==========
    /** 混合检索得到的候选入口节点列表 */
    private List<EntryNodeInfo> entryNodes;
    /** 是否触发了降级检索 */
    private boolean fallbackTriggered;

    // ========== Step 3-5 产出 ==========
    /** 最终生成的 Cypher 语句（已通过校验和修正） */
    private String generatedCypher;
    /** 最后一次校验结果（如通过则为 valid=true） */
    private CypherValidationResult validationResult;
    /** 修正重试次数 */
    private int retryCount;

    // ========== Step 6 产出 ==========
    /** 图谱结构化数据（节点 + 关系） */
    private Neo4jUtil.GraphResult graphData;
    /** LLM 自然语言总结（可选，在最终结果中附加回答） */
    private String naturalAnswer;

    // ========== 元数据 ==========
    /** 全部流程总耗时（毫秒） */
    private long totalExecutionTimeMs;
    /** 各阶段耗时详情 */
    private Map<String, Long> stepTimings;
    /** 是否成功 */
    private boolean success;
    /** 错误信息（如失败） */
    private String errorMessage;
}
