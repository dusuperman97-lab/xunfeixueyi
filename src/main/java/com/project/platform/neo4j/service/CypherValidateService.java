package com.project.platform.neo4j.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

/**
 * LLM Cypher 校验与修正服务接口。
 * 使用 LangChain4j @AiService 自动代理，对生成的 Cypher 进行语法/语义/安全校验和自动修正。
 */
@AiService(
        wiringMode = EXPLICIT,
        chatModel = "qwenChatModel",
        streamingChatModel = "qwenStreamingChatModel"
)
public interface CypherValidateService {

    /**
     * 对生成的 Cypher 语句进行语法正确性和逻辑合理性验证。
     *
     * @param cypherQuery 待校验的 Cypher 语句
     * @param userIntent  原始用户查询意图（用于语义理解）
     * @return JSON 格式的校验结果（不包含 markdown 代码块标记）：
     * <pre>{@code
     * {
     *   "valid": true/false,
     *   "errors": [
     *     {
     *       "errorType": "SYNTAX|SEMANTIC|LOGIC|SECURITY",
     *       "severity": "BLOCKING|WARNING",
     *       "errorLocation": "错误位置描述",
     *       "message": "错误详情",
     *       "suggestion": "修正建议"
     *     }
     *   ]
     * }
     * }</pre>
     */
    @SystemMessage("""
            你是一个 Neo4j Cypher 语句校验专家。你的任务是检查 Cypher 语句的语法正确性、逻辑合理性和安全性。

            ## 校验维度
            1. **SYNTAX**（语法）：Cypher 关键字拼写、子句顺序、括号匹配、变量引用是否正确
            2. **SEMANTIC**（语义）：MATCH 的节点标签和关系类型是否与图模型一致
            3. **LOGIC**（逻辑）：查询逻辑是否合理，能否回答用户的问题意图
            4. **SECURITY**（安全）：是否包含写操作（DELETE/DETACH DELETE/DROP/REMOVE/SET）、是否使用了参数化占位符

            ## 图模型参考
            - 节点：Course, Chapter, Section, KnowledgePoint（含 studentId 属性）
            - 关系：HAS_CHAPTER, HAS_SECTION, HAS_KNOWLEDGE_POINT, PREREQUISITE, ADVANCED, RELATED

            ## 项目约束
            - 只能使用后端会传入的参数：$studentId、$entryNodeId、$entryNodeIds
            - 匹配 Neo4j 元素 ID 必须使用 elementId(n)，不要使用 n.elementId
            - 为了前端图谱可视化，RETURN 必须包含节点、关系或路径对象，例如 RETURN n, m, r 或 RETURN n, p；只返回 name/count/collect(name) 等标量时应判为 LOGIC 错误

            ## 输出要求
            返回纯 JSON（不要包裹在 markdown 代码块 ```json 中）。
            如果 Cypher 正确无误，返回 {"valid": true, "errors": []}。
            """)
    @UserMessage("""
            请校验以下 Cypher 语句：

            ```cypher
            {{cypherQuery}}
            ```

            原始用户查询意图：{{userIntent}}

            返回 JSON 校验结果：
            """)
    String validateCypher(@V("cypherQuery") String cypherQuery,
                          @V("userIntent") String userIntent);

    /**
     * 根据校验错误信息修正 Cypher 语句。
     *
     * @param originalCypher    原始的 Cypher 语句
     * @param validationErrors  校验失败的错误信息（JSON 格式）
     * @param userIntent        原始用户查询意图
     * @param entryNodeContext  入口节点上下文信息
     * @return 修正后的 Cypher 语句（纯文本，不含 markdown 标记）
     */
    @SystemMessage("""
            你是一个 Neo4j Cypher 专家。你的任务是根据校验错误信息修正 Cypher 语句，确保修正后的语句：
            1. 语法完全正确
            2. 使用参数化占位符（$paramName）
            3. 包含 studentId 过滤条件
            4. 查询逻辑能回答用户意图
            5. 不包含任何写操作
            6. 使用 Neo4j 内置函数 elementId(n) 匹配入口节点，不要写 n.elementId
            7. 只能使用后端会传入的参数：$studentId、$entryNodeId、$entryNodeIds
            8. RETURN 必须包含节点、关系或路径对象，例如 RETURN n, m, r 或 RETURN n, p，避免只返回 name/count 等标量导致前端图谱为空

            ## 图模型
            节点：Course, Chapter, Section, KnowledgePoint（均有 studentId 属性）
            关系：HAS_CHAPTER, HAS_SECTION, HAS_KNOWLEDGE_POINT, PREREQUISITE, ADVANCED, RELATED

            ## 输出要求
            只返回修正后的纯 Cypher 语句。不要包裹在 markdown 代码块中。不要添加任何解释。
            """)
    @UserMessage("""
            原始 Cypher 语句：
            ```cypher
            {{originalCypher}}
            ```

            校验错误信息：
            {{validationErrors}}

            用户查询意图：{{userIntent}}

            入口节点上下文：{{entryNodeContext}}

            请返回修正后的 Cypher 语句：
            """)
    String correctCypher(@V("originalCypher") String originalCypher,
                         @V("validationErrors") String validationErrors,
                         @V("userIntent") String userIntent,
                         @V("entryNodeContext") String entryNodeContext);
}
