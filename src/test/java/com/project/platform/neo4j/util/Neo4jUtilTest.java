package com.project.platform.neo4j.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Neo4jUtil 单元测试。
 * 测试参数化查询、标识符校验、结果构建等核心安全性功能。
 */
class Neo4jUtilTest {

    // ==================== 标识符校验 ====================

    @Test
    @DisplayName("合法标识符应通过校验")
    void testValidIdentifier() {
        assertTrue(Neo4jUtil.isValidIdentifier("Course"));
        assertTrue(Neo4jUtil.isValidIdentifier("KnowledgePoint"));
        assertTrue(Neo4jUtil.isValidIdentifier("_customLabel"));
        assertTrue(Neo4jUtil.isValidIdentifier("node_name"));
    }

    @Test
    @DisplayName("非法标识符应被拒绝")
    void testInvalidIdentifier() {
        assertFalse(Neo4jUtil.isValidIdentifier(null));
        assertFalse(Neo4jUtil.isValidIdentifier(""));
        assertFalse(Neo4jUtil.isValidIdentifier("123label"));         // 数字开头
        assertFalse(Neo4jUtil.isValidIdentifier("DROP TABLE"));       // 空格
        assertFalse(Neo4jUtil.isValidIdentifier("label; DETACH DELETE n")); // 注入攻击
        assertFalse(Neo4jUtil.isValidIdentifier("label-name"));       // 连字符
        assertFalse(Neo4jUtil.isValidIdentifier("label`--"));         // 反引号
    }

    @Test
    @DisplayName("含特殊字符的标识符应被拒绝")
    void testInjectionAttempts() {
        // SQL/Cypher 注入攻击向量
        assertFalse(Neo4jUtil.isValidIdentifier("Course)--"));
        assertFalse(Neo4jUtil.isValidIdentifier("n] DELETE n"));
        assertFalse(Neo4jUtil.isValidIdentifier("n} RETURN n"));
        assertFalse(Neo4jUtil.isValidIdentifier("' OR '1'='1"));
        assertFalse(Neo4jUtil.isValidIdentifier("\"; DROP INDEX;"));
    }

    // ==================== GraphResult 构建 ====================

    @Test
    @DisplayName("GraphResult.empty() 应返回空结果而非 null")
    void testGraphResultEmpty() {
        Neo4jUtil.GraphResult result = Neo4jUtil.GraphResult.empty();
        assertNotNull(result);
        assertNotNull(result.getNodes());
        assertNotNull(result.getRelationships());
        assertTrue(result.getNodes().isEmpty());
        assertFalse(result.isHasError());
    }

    @Test
    @DisplayName("GraphResult.error() 应标记错误状态")
    void testGraphResultError() {
        Neo4jUtil.GraphResult result = Neo4jUtil.GraphResult.error("连接超时");
        assertTrue(result.isHasError());
        assertEquals("连接超时", result.getErrorMessage());
        assertNotNull(result.getNodes());
        assertTrue(result.getNodes().isEmpty());
    }

    // ==================== BatchResult 构建 ====================

    @Test
    @DisplayName("BatchResult.success 和 failure 工厂方法")
    void testBatchResult() {
        Neo4jUtil.BatchResult success = Neo4jUtil.BatchResult.success();
        assertTrue(success.isSuccess());
        assertNull(success.getErrorMessage());

        Neo4jUtil.BatchResult failure = Neo4jUtil.BatchResult.failure("节点不存在");
        assertFalse(failure.isSuccess());
        assertEquals("节点不存在", failure.getErrorMessage());
    }

    // ==================== 创建索引标识符校验 ====================

    @Test
    @DisplayName("createIndex 应拒绝非法标签名")
    void testCreateIndexInvalidLabel() {
        assertThrows(IllegalArgumentException.class, () ->
                Neo4jUtil.createIndex("Course; DROP INDEX foo", "name"));
        assertThrows(IllegalArgumentException.class, () ->
                Neo4jUtil.createIndex("", "name"));
    }

    @Test
    @DisplayName("createIndex 应拒绝非法属性名")
    void testCreateIndexInvalidProp() {
        assertThrows(IllegalArgumentException.class, () ->
                Neo4jUtil.createIndex("Course", "name; DROP INDEX foo"));
    }
}
