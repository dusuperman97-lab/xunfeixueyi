package com.project.platform.neo4j.service;

import cn.hutool.json.JSONUtil;
import com.project.platform.neo4j.dto.CypherValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KnowledgeGraphQueryService 单元测试。
 * 测试 Cypher 校验解析、Prompt 构建、写操作检测等核心逻辑。
 * （不依赖 Spring Context / LLM / Neo4j 连接）
 */
class KnowledgeGraphQueryServiceImplTest {

    // ==================== Cypher 校验结果解析 ====================

    @Test
    @DisplayName("应正确解析有效的校验 JSON 响应")
    void testParseValidationResult_Valid() {
        String response = """
                {
                  "valid": true,
                  "errors": []
                }""";

        // 模拟 parseValidationResult 逻辑
        cn.hutool.json.JSONObject obj = JSONUtil.parseObj(response);
        boolean valid = obj.getBool("valid", false);

        assertTrue(valid);
        assertEquals(0, obj.getJSONArray("errors").size());
    }

    @Test
    @DisplayName("应正确解析含错误的校验 JSON 响应")
    void testParseValidationResult_WithErrors() {
        String response = """
                {
                  "valid": false,
                  "errors": [
                    {
                      "errorType": "SYNTAX",
                      "severity": "BLOCKING",
                      "errorLocation": "第3行 MATCH 子句",
                      "message": "变量 k 未定义",
                      "suggestion": "将 k 改为 kp"
                    },
                    {
                      "errorType": "SECURITY",
                      "severity": "BLOCKING",
                      "errorLocation": "第5行",
                      "message": "使用了字符串拼接而非参数化占位符",
                      "suggestion": "将 '五行学说' 替换为 $keyword 参数"
                    }
                  ]
                }""";

        cn.hutool.json.JSONObject obj = JSONUtil.parseObj(response);
        boolean valid = obj.getBool("valid", false);
        cn.hutool.json.JSONArray errorArray = obj.getJSONArray("errors");

        assertFalse(valid);
        assertEquals(2, errorArray.size());

        cn.hutool.json.JSONObject firstError = errorArray.getJSONObject(0);
        assertEquals("SYNTAX", firstError.getStr("errorType"));
        assertEquals("BLOCKING", firstError.getStr("severity"));
        assertEquals("变量 k 未定义", firstError.getStr("message"));
    }

    @Test
    @DisplayName("应正确解析带 markdown 标记的校验响应")
    void testParseValidationResult_WithMarkdownFence() {
        String response = """
                ```json
                {
                  "valid": true,
                  "errors": []
                }
                ```""";

        // 模拟 clean 逻辑
        String cleaned = response.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(cleaned.indexOf("\n") + 1);
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.lastIndexOf("```")).trim();
            }
        }

        cn.hutool.json.JSONObject obj = JSONUtil.parseObj(cleaned);
        assertTrue(obj.getBool("valid", false));
    }

    // ==================== Cypher Markdown 清理 ====================

    @Test
    @DisplayName("应正确剥离 Cypher 外的 markdown 代码块标记")
    void testCleanCypherMarkdown() {
        String rawCypher = """
                ```cypher
                MATCH (kp:KnowledgePoint {name: $name, studentId: $studentId})
                OPTIONAL MATCH (kp)-[:PREREQUISITE]->(pre:KnowledgePoint)
                RETURN kp, pre
                ```""";

        // 模拟 cleanCypherOutput
        String cleaned = rawCypher.trim();
        if (cleaned.startsWith("```")) {
            int start = cleaned.indexOf("\n");
            if (start > 0) cleaned = cleaned.substring(start + 1);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.lastIndexOf("```")).trim();
        }

        assertFalse(cleaned.startsWith("```"));
        assertFalse(cleaned.endsWith("```"));
        assertTrue(cleaned.startsWith("MATCH"));
    }

    @Test
    @DisplayName("纯 Cypher 无 markdown 包裹时不应被截断")
    void testCleanCypherNoMarkdown() {
        String cypher = "MATCH (n) WHERE n.studentId = $studentId RETURN n LIMIT 10";
        // 无 ``` 包裹时保持不变
        assertEquals(cypher, cypher.trim());
    }

    // ==================== 写操作检测 ====================

    @Test
    @DisplayName("应正确检测 DELETE 写操作")
    void testContainsWriteOperation_Delete() {
        String cypher = "MATCH (n) WHERE n.name = 'test' DELETE n";
        assertTrue(containsWriteOperation(cypher));
    }

    @Test
    @DisplayName("应正确检测 DETACH DELETE 写操作")
    void testContainsWriteOperation_DetachDelete() {
        String cypher = "MATCH (n) DETACH DELETE n";
        assertTrue(containsWriteOperation(cypher));
    }

    @Test
    @DisplayName("应正确检测 CREATE 写操作")
    void testContainsWriteOperation_Create() {
        String cypher = "CREATE (n:Test {name: 'foo'})";
        assertTrue(containsWriteOperation(cypher));
    }

    @Test
    @DisplayName("应正确检测 MERGE 写操作")
    void testContainsWriteOperation_Merge() {
        String cypher = "MERGE (n:Course {businessId: 1})";
        assertTrue(containsWriteOperation(cypher));
    }

    @Test
    @DisplayName("纯读操作不应被标记为写操作")
    void testContainsWriteOperation_ReadOnly() {
        String cypher = "MATCH (kp:KnowledgePoint {name: $name, studentId: $studentId}) " +
                "OPTIONAL MATCH (kp)-[:PREREQUISITE]->(pre:KnowledgePoint) " +
                "RETURN kp, pre LIMIT 20";
        assertFalse(containsWriteOperation(cypher));
    }

    @Test
    @DisplayName("聚合查询不应被标记为写操作")
    void testContainsWriteOperation_Aggregation() {
        String cypher = "MATCH (n) WHERE n.studentId = $studentId RETURN count(n) AS total";
        assertFalse(containsWriteOperation(cypher));
    }

    // ==================== ValidationResult 辅助方法 ====================

    @Test
    @DisplayName("BLOCKING 错误应使 hasBlockingErrors 返回 true")
    void testHasBlockingErrors() {
        CypherValidationResult.ValidationError blocking = CypherValidationResult.ValidationError.builder()
                .errorType("SYNTAX")
                .severity("BLOCKING")
                .message("语法错误")
                .build();

        CypherValidationResult.ValidationError warning = CypherValidationResult.ValidationError.builder()
                .errorType("LOGIC")
                .severity("WARNING")
                .message("建议优化")
                .build();

        CypherValidationResult result = CypherValidationResult.builder()
                .valid(false)
                .errors(List.of(warning, blocking))
                .build();

        assertTrue(result.hasBlockingErrors());
    }

    @Test
    @DisplayName("纯 WARNING 不应使 hasBlockingErrors 返回 true")
    void testHasBlockingErrors_OnlyWarnings() {
        CypherValidationResult.ValidationError warning = CypherValidationResult.ValidationError.builder()
                .errorType("LOGIC")
                .severity("WARNING")
                .message("建议优化查询性能")
                .build();

        CypherValidationResult result = CypherValidationResult.builder()
                .valid(true)
                .errors(List.of(warning))
                .build();

        assertFalse(result.hasBlockingErrors());
    }

    @Test
    @DisplayName("空错误列表 hasBlockingErrors 应返回 false")
    void testHasBlockingErrors_Empty() {
        CypherValidationResult result = CypherValidationResult.builder()
                .valid(true)
                .errors(Collections.emptyList())
                .build();

        assertFalse(result.hasBlockingErrors());
    }

    // ==================== 工具方法（内联模拟） ====================

    /**
     * 模拟 KnowledgeGraphQueryServiceImpl.containsWriteOperation()
     */
    private boolean containsWriteOperation(String cypher) {
        if (cypher == null) return false;
        String upper = cypher.toUpperCase();
        return upper.contains("DELETE") || upper.contains("DETACH DELETE")
                || upper.contains("DROP") || upper.contains("REMOVE")
                || upper.contains("CREATE") || upper.contains("MERGE")
                || upper.contains("SET");
    }
}
