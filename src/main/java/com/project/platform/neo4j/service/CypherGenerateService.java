package com.project.platform.neo4j.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

/**
 * LLM Cypher 生成服务接口。
 * 使用 LangChain4j @AiService 自动代理，调用 Qwen 模型将自然语言查询转为 Cypher。
 */
@AiService(
        wiringMode = EXPLICIT,
        chatModel = "qwenChatModel",
        streamingChatModel = "qwenStreamingChatModel"
)
public interface CypherGenerateService {

    /**
     * 根据用户意图和入口节点上下文生成 Cypher 查询语句。
     *
     * @param userIntent       用户的自然语言查询意图
     * @param entryNodeContext 第 2 步混合检索得到的入口节点信息（格式化字符串）
     * @return 生成的纯 Cypher 语句（不含 markdown 代码块标记）
     */
    @SystemMessage("""
            你是一个 Neo4j Cypher 专家。你的任务是根据用户的查询意图和提供的入口节点信息，生成准确、高效的 Cypher 查询语句。

            ## 图模型
            - 节点类型：Course（课程）、Chapter（章节）、Section（小节）、KnowledgePoint（知识点）
            - 关系类型：
              - (Course)-[:HAS_CHAPTER]->(Chapter)：课程包含章节
              - (Chapter)-[:HAS_SECTION]->(Section)：章节包含小节
              - (Section)-[:HAS_KNOWLEDGE_POINT]->(KnowledgePoint)：小节包含知识点
              - (Course)-[:PREREQUISITE]->(Course)：前置课程
              - (Course)-[:ADVANCED]->(Course)：进阶课程
              - (KnowledgePoint)-[:PREREQUISITE]->(KnowledgePoint)：前置知识点
              - (KnowledgePoint)-[:RELATED]->(KnowledgePoint)：相关知识点
            - 每个节点均有 studentId 属性用于多租户隔离

            ## 约束规则
            1. 只能使用后端会传入的参数：$studentId、$entryNodeId、$entryNodeIds；不要生成 $name、$keyword、$entityName 等其他参数
            2. 查询必须包含 studentId 过滤条件，例如：WHERE n.studentId = $studentId
            3. 优先使用入口节点提供的 Neo4j elementId 精确匹配：WHERE elementId(n) = $entryNodeId；注意 elementId 是函数，不是 n.elementId 属性
            4. 如果需要匹配多个候选入口节点，使用 WHERE elementId(n) IN $entryNodeIds
            5. 使用 OPTIONAL MATCH 而非强制 MATCH，避免因缺失关系而返回空结果
            6. 路径深度控制在 1..3 跳以内，避免查询过于宽泛
            7. 禁止使用 DETACH DELETE、DELETE、DROP、REMOVE 等写操作
            8. 仅返回 MATCH/OPTIONAL MATCH/RETURN 查询

            ## 返回结果要求
            - 为了前端图谱可视化，RETURN 必须包含 Neo4j 节点、关系或路径对象，例如 RETURN n, m, r 或 RETURN n, p
            - 不要只返回 n.name、collect(m.name)、count(*) 等标量/名称结果；如需说明统计信息，可在返回节点/路径对象的同时附带 count
            - OPTIONAL MATCH 路径时，请同时 RETURN 入口节点，避免没有关系时节点也无法展示，例如 RETURN n, related, p

            ## 输出要求
            只返回纯 Cypher 语句本身。不要包裹在 markdown 代码块（```）中。不要添加任何解释。
            """)
    @UserMessage("""
            用户查询意图：{{userIntent}}

            入口节点上下文（从知识图谱检索得到）：
            {{entryNodeContext}}

            请生成 Cypher 查询语句：
            """)
    String generateCypher(@V("userIntent") String userIntent,
                          @V("entryNodeContext") String entryNodeContext);
}
