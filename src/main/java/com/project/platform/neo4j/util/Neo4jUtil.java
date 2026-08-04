package com.project.platform.neo4j.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.neo4j.driver.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Neo4j 工具类
 * 基于 KGBuilder 项目 Neo4jUtil 模式，提供原生 Cypher 查询能力。
 * 所有方法均为 static，通过 Spring 注入 Driver。
 *
 * <p>安全增强（v2）：
 * <ul>
 *   <li>参数化查询：使用 Neo4j 原生参数绑定 ({@code $param})，防止 Cypher 注入</li>
 *   <li>查询超时控制：通过 {@link TransactionConfig} 设置最大执行时间</li>
 *   <li>结构化结果封装：{@link GraphResult} 统一返回节点、关系、路径、标量数据</li>
 *   <li>向后兼容：原有方法标记为 {@code @Deprecated}（后续版本移除）</li>
 * </ul>
 */
@Component
@Lazy(false)
@Slf4j
public class Neo4jUtil implements AutoCloseable {

    // ==================== 配置常量 ====================

    /** 默认查询超时时间（秒） */
    public static final long DEFAULT_QUERY_TIMEOUT_SECONDS = 30;

    /** 长查询超时时间（秒） */
    public static final long LONG_QUERY_TIMEOUT_SECONDS = 120;

    private static Driver neo4jDriver;

    @Autowired
    @Lazy
    public void setNeo4jDriver(Driver neo4jDriver) {
        Neo4jUtil.neo4jDriver = neo4jDriver;
    }

    // ==================== 结构化结果 DTO ====================

    /**
     * Cypher 查询的结构化结果封装。
     * 根据返回列的类型自动归类为节点、关系、路径、标量数据。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphResult {
        /** 节点列表（已去重） */
        private List<Map<String, Object>> nodes;
        /** 关系列表（已去重） */
        private List<Map<String, Object>> relationships;
        /** 路径列表（每条路径独立） — 也展开合并到 nodes/relationships */
        private List<List<Map<String, Object>>> pathDetails;
        /** 表格/标量数据（如聚合查询结果） */
        private List<Map<String, Object>> tabularData;
        /** 是否发生错误 */
        private boolean hasError;
        /** 错误信息 */
        private String errorMessage;
        /** 查询执行耗时（毫秒） */
        private long executionTimeMs;

        public static GraphResult empty() {
            return GraphResult.builder()
                    .nodes(Collections.emptyList())
                    .relationships(Collections.emptyList())
                    .pathDetails(Collections.emptyList())
                    .tabularData(Collections.emptyList())
                    .hasError(false)
                    .build();
        }

        public static GraphResult error(String message) {
            return GraphResult.builder()
                    .nodes(Collections.emptyList())
                    .relationships(Collections.emptyList())
                    .pathDetails(Collections.emptyList())
                    .tabularData(Collections.emptyList())
                    .hasError(true)
                    .errorMessage(message)
                    .build();
        }
    }

    // ==================== 连接检测 ====================

    /**
     * 测试 Neo4j 连接是否正常
     */
    public static boolean isNeo4jOpen() {
        try (Session session = neo4jDriver.session()) {
            return session.isOpen();
        }
    }

    // ==================== 安全执行（v2 新增） ====================

    /**
     * 执行参数化 Cypher 查询（推荐方式 — 防注入）。
     * <p>用法示例：
     * <pre>{@code
     *   Map<String, Object> params = Map.of("studentId", 1, "name", "五行学说");
     *   List<Map<String, Object>> result = Neo4jUtil.runCypher(
     *       "MATCH (kp:KnowledgePoint {name: $name, studentId: $studentId}) RETURN kp", params);
     * }</pre>
     *
     * @param cypher  Cypher 语句（使用 $param 占位符）
     * @param params  参数映射
     * @return 原始 Record 列表（每条 Record 转为 Map）
     */
    public static List<Map<String, Object>> runCypher(String cypher, Map<String, Object> params) {
        return runCypher(cypher, params, DEFAULT_QUERY_TIMEOUT_SECONDS);
    }

    /**
     * 执行参数化 Cypher 查询（带超时）。
     */
    public static List<Map<String, Object>> runCypher(String cypher, Map<String, Object> params,
                                                       long timeoutSeconds) {
        long start = System.currentTimeMillis();
        List<Map<String, Object>> resultData = new ArrayList<>();

        Map<String, Object> safeParams = params != null ? params : Collections.emptyMap();
        try (Session session = neo4jDriver.session();
             Transaction tx = session.beginTransaction(txConfig(timeoutSeconds))) {
            log.debug("Cypher (timeout={}s): {}", timeoutSeconds, cypher);
            Result result = tx.run(new Query(cypher, safeParams));
            // 必须在 commit 前消费 Result，否则 commit 后 Result 关闭
            List<Record> records = result.list();
            tx.commit();
            for (Record record : records) {
                Map<String, Object> row = new HashMap<>();
                for (Pair<String, Value> pair : record.fields()) {
                    row.put(pair.key(), toPlainValue(pair.value()));
                }
                resultData.add(row);
            }
            log.debug("查询完成，耗时={}ms，结果行数={}", System.currentTimeMillis() - start, resultData.size());
        } catch (Exception e) {
            log.error("runCypher 执行失败 [{}ms]: {}", System.currentTimeMillis() - start, e.getMessage());
            throw new RuntimeException("Cypher 执行失败: " + e.getMessage(), e);
        }
        return resultData;
    }

    /**
     * 执行参数化 Cypher 并返回结构化 GraphResult。
     * 自动对返回列按类型（NODE / RELATIONSHIP / PATH / 标量）分类。
     */
    public static GraphResult runCypherStructured(String cypher, Map<String, Object> params) {
        return runCypherStructured(cypher, params, DEFAULT_QUERY_TIMEOUT_SECONDS);
    }

    /**
     * 执行参数化 Cypher 并返回结构化 GraphResult（带超时）。
     */
    public static GraphResult runCypherStructured(String cypher, Map<String, Object> params,
                                                   long timeoutSeconds) {
        long start = System.currentTimeMillis();

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> relationships = new ArrayList<>();
        List<Map<String, Object>> tabularData = new ArrayList<>();
        Set<String> nodeUuids = new HashSet<>();
        Set<String> relUuids = new HashSet<>();

        Map<String, Object> safeParams = params != null ? params : Collections.emptyMap();
        try (Session session = neo4jDriver.session();
             Transaction tx = session.beginTransaction(txConfig(timeoutSeconds))) {
            log.debug("Cypher Structured (timeout={}s): {}", timeoutSeconds, cypher);
            Result rs = tx.run(new Query(cypher, safeParams));
            // 必须在 commit 前消费 Result
            List<Record> records = rs.list();
            tx.commit();

            if (records.isEmpty()) {
                return GraphResult.builder()
                        .nodes(Collections.emptyList())
                        .relationships(Collections.emptyList())
                        .pathDetails(Collections.emptyList())
                        .tabularData(Collections.emptyList())
                        .hasError(false)
                        .executionTimeMs(System.currentTimeMillis() - start)
                        .build();
            }

            for (Record recordItem : records) {
                for (Pair<String, Value> pair : recordItem.fields()) {
                    String typeName = pair.value().type().name();
                    if ("NULL".equals(typeName)) {
                        continue;
                    }
                    if ("NODE".equals(typeName)) {
                        addNodeFromValue(pair.value().asNode(), nodes, nodeUuids);
                    } else if ("RELATIONSHIP".equals(typeName)) {
                        addRelFromValue(pair.value().asRelationship(), relationships, relUuids);
                    } else if ("PATH".equals(typeName)) {
                        Path path = pair.value().asPath();
                        for (Node nodeItem : path.nodes()) {
                            addNodeFromValue(nodeItem, nodes, nodeUuids);
                        }
                        for (Relationship relItem : path.relationships()) {
                            addRelFromValue(relItem, relationships, relUuids);
                        }
                    } else if (typeName.contains("LIST")) {
                        Iterable<Value> values = pair.value().values();
                        for (Value v : values) {
                            String subType = v.type().name();
                            if ("NODE".equals(subType)) {
                                addNodeFromValue(v.asNode(), nodes, nodeUuids);
                            } else if ("RELATIONSHIP".equals(subType)) {
                                addRelFromValue(v.asRelationship(), relationships, relUuids);
                            }
                        }
                    } else {
                        // 标量值 → 表格行
                        Map<String, Object> row = new HashMap<>();
                        row.put(pair.key(), toPlainValue(pair.value()));
                        tabularData.add(row);
                    }
                }
            }

            return GraphResult.builder()
                    .nodes(nodes)
                    .relationships(relationships)
                    .pathDetails(Collections.emptyList())
                    .tabularData(tabularData)
                    .hasError(false)
                    .executionTimeMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            log.error("runCypherStructured 执行失败 [{}ms]: {}", System.currentTimeMillis() - start, e.getMessage());
            return GraphResult.error("执行失败: " + e.getMessage());
        }
    }

    /**
     * 批量执行参数化 Cypher（事务中，带超时）。
     * 每条语句失败不影响后续，返回成功/失败状态列表。
     *
     * @param cyphersWithParams 二元组列表 (cypher, params)
     * @return 返回每条的布尔结果 + 错误信息
     */
    public static List<BatchResult> runBatchCypherWithParams(
            List<Map.Entry<String, Map<String, Object>>> cyphersWithParams) {
        return runBatchCypherWithParams(cyphersWithParams, LONG_QUERY_TIMEOUT_SECONDS);
    }

    /**
     * 批量执行参数化 Cypher（事务中，自定义超时）。
     */
    public static List<BatchResult> runBatchCypherWithParams(
            List<Map.Entry<String, Map<String, Object>>> cyphersWithParams, long timeoutSeconds) {

        List<BatchResult> results = new ArrayList<>();

        try (Session session = neo4jDriver.session();
             Transaction tx = session.beginTransaction(txConfig(timeoutSeconds))) {

            for (Map.Entry<String, Map<String, Object>> entry : cyphersWithParams) {
                try {
                    tx.run(new Query(entry.getKey(),
                            entry.getValue() != null ? entry.getValue() : Collections.emptyMap()));
                    results.add(BatchResult.success());
                } catch (Exception e) {
                    log.error("批量执行单条失败: {}", e.getMessage());
                    results.add(BatchResult.failure(e.getMessage()));
                }
            }
            tx.commit();
        } catch (Exception e) {
            log.error("批量执行事务失败: {}", e.getMessage());
            // 事务失败标记未成功的条目
            for (int i = results.size(); i < cyphersWithParams.size(); i++) {
                results.add(BatchResult.failure("事务整体失败: " + e.getMessage()));
            }
        }
        return results;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BatchResult {
        private boolean success;
        private String errorMessage;

        public static BatchResult success() {
            return new BatchResult(true, null);
        }

        public static BatchResult failure(String msg) {
            return new BatchResult(false, msg);
        }
    }

    /**
     * 执行参数化 Cypher 写语句（无返回值）。
     */
    public static void runCypherWrite(String cypher, Map<String, Object> params) {
        Map<String, Object> safeParams = params != null ? params : Collections.emptyMap();
        try (Session session = neo4jDriver.session();
             Transaction tx = session.beginTransaction()) {
            log.debug("Cypher Write: {}", cypher);
            Result result = tx.run(new Query(cypher, safeParams));
            result.consume();  // 确保消费结果后再提交
            tx.commit();
        } catch (Exception e) {
            log.error("runCypherWrite 执行失败: {}", e.getMessage());
            throw new RuntimeException("Cypher 写操作失败: " + e.getMessage(), e);
        }
    }

    // ==================== 原有写操作（保持向后兼容） ====================

    /**
     * @deprecated 请使用 {@link #runCypherWrite(String, Map)} 进行参数化查询
     */
    @Deprecated
    public static void runCypherSql(String cypherSql) {
        try (Session session = neo4jDriver.session()) {
            log.debug("Cypher: {}", cypherSql);
            session.run(cypherSql);
        }
    }

    /**
     * @deprecated 请使用 {@link #runCypherWrite(String, Map)} 进行参数化查询
     */
    @Deprecated
    public static void runCypherSql(String cypherSql, Map<String, Object> params) {
        try (Session session = neo4jDriver.session()) {
            log.debug("Cypher: {}", cypherSql);
            session.run(cypherSql, params);
        }
    }

    /**
     * @deprecated 请使用 {@link #batchRunCypherWithTx(List)} 参数化版本
     */
    @Deprecated
    public static boolean batchRunCypherWithTx(List<String> cyphers) {
        try (Session session = neo4jDriver.session();
             Transaction tx = session.beginTransaction()) {
            for (String cypher : cyphers) {
                tx.run(cypher);
            }
            tx.commit();
            return true;
        } catch (Exception e) {
            log.error("批量执行Cypher失败: {}", e.getMessage());
            return false;
        }
    }

    // ==================== 读操作 ====================

    /**
     * 执行 Cypher 查询，使用自定义映射函数转换结果。
     * @deprecated 请使用 {@link #runCypher(String, Map)} 参数化版本
     */
    @Deprecated
    public <T> List<T> readCyphers(String cypherSql, Function<Record, T> mapper) {
        try (Session session = neo4jDriver.session()) {
            log.debug("Cypher: {}", cypherSql);
            Result result = session.run(cypherSql);
            return result.list(mapper);
        }
    }

    /**
     * 获取节点集合（不保留关系）
     * @deprecated 请使用 {@link #runCypherStructured(String, Map)} 解析 GraphResult.nodes
     */
    @Deprecated
    public static List<Map<String, Object>> getGraphNode(String cypherSql) {
        List<Map<String, Object>> ents = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            log.debug("Cypher: {}", cypherSql);
            Result result = session.run(cypherSql);
            if (result.hasNext()) {
                List<Record> records = result.list();
                for (Record recordItem : records) {
                    List<Pair<String, Value>> fields = recordItem.fields();
                    for (Pair<String, Value> pair : fields) {
                        String typeName = pair.value().type().name();
                        if ("NODE".equals(typeName)) {
                            Node neo4jNode = pair.value().asNode();
                            HashMap<String, Object> rss = new HashMap<>();
                            rss.put("uuid", neo4jNode.elementId());
                            rss.put("labels", new ArrayList<>((Collection) neo4jNode.labels()));
                            for (Entry<String, Object> entry : neo4jNode.asMap().entrySet()) {
                                rss.put(entry.getKey(), entry.getValue());
                            }
                            ents.add(rss);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("getGraphNode查询失败: {}", e.getMessage());
        }
        return ents;
    }

    /**
     * @deprecated 请使用 {@link #runCypherStructured(String, Map)} 解析结果
     */
    @Deprecated
    public static HashMap<String, Object> getSingleGraphNode(String cypherSql) {
        List<Map<String, Object>> ent = getGraphNode(cypherSql);
        return ent.isEmpty() ? null : (HashMap<String, Object>) ent.get(0);
    }

    /**
     * @deprecated 请使用 {@link #runCypherStructured(String, Map)} 解析 GraphResult.relationships
     */
    @Deprecated
    public static List<Map<String, Object>> getGraphRelationShip(String cypherSql) {
        List<Map<String, Object>> ents = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            log.debug("Cypher: {}", cypherSql);
            Result result = session.run(cypherSql);
            if (result.hasNext()) {
                List<Record> records = result.list();
                for (Record recordItem : records) {
                    List<Pair<String, Value>> fields = recordItem.fields();
                    for (Pair<String, Value> pair : fields) {
                        String typeName = pair.value().type().name();
                        if ("RELATIONSHIP".equals(typeName)) {
                            Relationship rship = pair.value().asRelationship();
                            HashMap<String, Object> rss = new HashMap<>();
                            rss.put("uuid", rship.elementId());
                            rss.put("sourceId", rship.startNodeElementId());
                            rss.put("targetId", rship.endNodeElementId());
                            rss.put("type", rship.type());
                            for (Entry<String, Object> entry : rship.asMap().entrySet()) {
                                rss.put(entry.getKey(), entry.getValue());
                            }
                            ents.add(rss);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("getGraphRelationShip查询失败: {}", e.getMessage());
        }
        return ents;
    }

    /**
     * @deprecated 请使用 {@link #runCypherStructured(String, Map)}
     */
    @Deprecated
    public static HashMap<String, Object> getGraphNodeAndShip(String cypherSql) {
        HashMap<String, Object> result = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> relationships = new ArrayList<>();
        Set<String> nodeUuids = new HashSet<>();
        Set<String> relUuids = new HashSet<>();

        try (Session session = neo4jDriver.session()) {
            log.debug("Cypher: {}", cypherSql);
            Result rs = session.run(cypherSql);
            if (rs.hasNext()) {
                List<Record> records = rs.list();
                for (Record recordItem : records) {
                    for (Pair<String, Value> pair : recordItem.fields()) {
                        String typeName = pair.value().type().name();
                        if ("NULL".equals(typeName)) continue;
                        if ("NODE".equals(typeName)) {
                            addNodeFromValue(pair.value().asNode(), nodes, nodeUuids);
                        } else if ("RELATIONSHIP".equals(typeName)) {
                            addRelFromValue(pair.value().asRelationship(), relationships, relUuids);
                        } else if ("PATH".equals(typeName)) {
                            Path path = pair.value().asPath();
                            for (Node nodeItem : path.nodes()) addNodeFromValue(nodeItem, nodes, nodeUuids);
                            for (Relationship relItem : path.relationships()) addRelFromValue(relItem, relationships, relUuids);
                        } else if (typeName.contains("LIST")) {
                            Iterable<Value> values = pair.value().values();
                            for (Value v : values) {
                                String subType = v.type().name();
                                if ("NODE".equals(subType)) addNodeFromValue(v.asNode(), nodes, nodeUuids);
                                else if ("RELATIONSHIP".equals(subType)) addRelFromValue(v.asRelationship(), relationships, relUuids);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("getGraphNodeAndShip查询失败: {}", e.getMessage());
        }
        result.put("node", nodes);
        result.put("relationship", relationships);
        return result;
    }

    /**
     * @deprecated 请使用 {@link #runCypher(String, Map)}
     */
    @Deprecated
    public static List<Map<String, Object>> getGraphTable(String cypherSql) {
        List<Map<String, Object>> resultData = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            log.debug("Cypher: {}", cypherSql);
            Result result = session.run(cypherSql);
            if (result.hasNext()) {
                List<Record> records = result.list();
                for (Record recordItem : records) {
                    List<Pair<String, Value>> fields = recordItem.fields();
                    HashMap<String, Object> row = new HashMap<>();
                    for (Pair<String, Value> pair : fields) {
                        row.put(pair.key(), pair.value());
                    }
                    resultData.add(row);
                }
            }
        } catch (Exception e) {
            log.error("getGraphTable查询失败: {}", e.getMessage());
        }
        return resultData;
    }

    /**
     * @deprecated 请使用 {@link #runCypher(String, Map)} 解析聚合结果
     */
    @Deprecated
    public static long getGraphValue(String cypherSql) {
        try (Session session = neo4jDriver.session()) {
            log.debug("Cypher: {}", cypherSql);
            Result result = session.run(cypherSql);
            if (result.hasNext()) {
                Record record = result.next();
                for (Value value : record.values()) {
                    return value.asLong();
                }
            }
        } catch (Exception e) {
            log.error("getGraphValue查询失败: {}", e.getMessage());
        }
        return 0;
    }

    // ==================== 索引管理 ====================

    /**
     * 创建索引（使用参数化标签名，防注入）。
     */
    public static void createIndex(String label, String prop) {
        // 标签名和属性名做白名单校验
        if (!isValidIdentifier(label)) {
            throw new IllegalArgumentException("非法的 Neo4j 标签名: " + label);
        }
        if (!isValidIdentifier(prop)) {
            throw new IllegalArgumentException("非法的属性名: " + prop);
        }
        try (Session session = neo4jDriver.session()) {
            String cypher = String.format("CREATE INDEX IF NOT EXISTS FOR (n:`%s`) ON (n.%s)", label, prop);
            session.run(cypher);
        } catch (Exception e) {
            log.error("创建索引失败: {}", e.getMessage());
        }
    }

    /**
     * 创建全文索引（用于关键词搜索）。
     */
    public static void createFulltextIndex(String indexName, List<String> labels, List<String> properties) {
        try (Session session = neo4jDriver.session()) {
            String labelsStr = labels.stream()
                    .map(l -> "`" + l + "`")
                    .collect(Collectors.joining("|"));
            String propsStr = String.join(", ", properties);
            String cypher = String.format(
                    "CREATE FULLTEXT INDEX %s IF NOT EXISTS FOR (n:%s) ON EACH [%s]",
                    indexName, labelsStr, propsStr);
            session.run(cypher);
        } catch (Exception e) {
            log.error("创建全文索引失败: {}", e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将 Neo4j Value 转为普通 Java 对象（解决序列化问题）。
     */
    private static Object toPlainValue(Value value) {
        if (value == null || value.isNull()) return null;
        String typeName = value.type().name();
        if ("NODE".equals(typeName)) {
            Node node = value.asNode();
            Map<String, Object> map = new HashMap<>();
            map.put("_elementId", node.elementId());
            map.put("_labels", new ArrayList<>((Collection) node.labels()));
            map.putAll(node.asMap());
            return map;
        }
        if ("RELATIONSHIP".equals(typeName)) {
            Relationship rel = value.asRelationship();
            Map<String, Object> map = new HashMap<>();
            map.put("_elementId", rel.elementId());
            map.put("_type", rel.type());
            map.put("_startNodeElementId", rel.startNodeElementId());
            map.put("_endNodeElementId", rel.endNodeElementId());
            map.putAll(rel.asMap());
            return map;
        }
        if ("PATH".equals(typeName)) {
            return value.asPath().toString();
        }
        // 基础类型直接取
        try {
            return value.asObject();
        } catch (Exception e) {
            return value.toString();
        }
    }

    /**
     * 构建带超时的 TransactionConfig。
     */
    private static TransactionConfig txConfig(long timeoutSeconds) {
        return TransactionConfig.builder()
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    /**
     * 校验 Neo4j 标识符（标签名/属性名）是否合法。
     * 仅允许字母、数字、下划线，且不以数字开头。
     */
    public static boolean isValidIdentifier(String identifier) {
        return identifier != null && identifier.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }

    /**
     * 从 Neo4j Node 中提取数据并添加到节点列表（去重）。
     */
    private static void addNodeFromValue(Node neo4jNode, List<Map<String, Object>> nodes, Set<String> uuids) {
        String uuid = neo4jNode.elementId();
        if (uuids.contains(uuid)) return;
        uuids.add(uuid);
        Map<String, Object> nodeData = new HashMap<>();
        nodeData.put("uuid", uuid);
        nodeData.put("labels", new ArrayList<>((Collection) neo4jNode.labels()));
        for (Entry<String, Object> entry : neo4jNode.asMap().entrySet()) {
            nodeData.put(entry.getKey(), entry.getValue());
        }
        nodes.add(nodeData);
    }

    /**
     * 从 Neo4j Relationship 中提取数据并添加到关系列表（去重）。
     */
    private static void addRelFromValue(Relationship rship, List<Map<String, Object>> relationships,
                                         Set<String> uuids) {
        String uuid = rship.elementId();
        if (uuids.contains(uuid)) return;
        uuids.add(uuid);
        Map<String, Object> relData = new HashMap<>();
        relData.put("uuid", uuid);
        relData.put("sourceId", rship.startNodeElementId());
        relData.put("targetId", rship.endNodeElementId());
        relData.put("type", rship.type());
        for (Entry<String, Object> entry : rship.asMap().entrySet()) {
            relData.put(entry.getKey(), entry.getValue());
        }
        relationships.add(relData);
    }

    @Override
    public void close() throws Exception {
        if (neo4jDriver != null) {
            neo4jDriver.close();
        }
    }
}
