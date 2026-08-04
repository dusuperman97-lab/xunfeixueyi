package com.project.platform.rag.enhance;

import com.project.platform.agents.ai.KnowledgeRagHelperService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * HyDE（Hypothetical Document Embeddings）服务
 * <p>
 * 核心思路：不改写问题本身，而是先让大模型"脑补"一个可能的答案，然后用这个假设答案去检索。
 * <p>
 * 为什么有效？
 * 假设答案的文本风格和知识库文档更接近。比如用户问"微服务之间怎么通信"，
 * 假设回答会包含"REST、gRPC、RabbitMQ、Kafka、解耦、削峰"等专业术语，
 * 这些词和知识库文档的用词高度重合，用假设回答的向量去检索，命中率比用原始短问题高很多。
 * <p>
 * 流程对比：
 * - 传统RAG：用户问题 → 检索 → 生成答案
 * - HyDE：  用户问题 → 生成假设答案 → 用假设答案检索 → 生成最终答案
 * <p>
 * 适用场景：用户提问很短、很模糊的场景。
 * 注意：如果用户的问题本身就很具体、很完整，HyDE反而可能引入噪音。另外会多一次LLM调用，延迟增加300-800ms。
 */
@Slf4j
@Service
public class HydeService {

    @Resource
    private KnowledgeRagHelperService knowledgeRagHelperService;

    private static final String HYDE_PROMPT = """
            请根据以下问题，生成一段可能的回答。
            这段回答不需要完全准确，但应该包含相关的专业术语和概念。
            直接输出回答内容，不要加任何前缀或解释。
            
            问题：
            """;

    public String generateHypotheticalAnswer(String question) {
        try {
            // 使用字符串拼接而非 String.format，避免 question 中含 % 字符时抛出 MissingFormatArgumentException
            String prompt = HYDE_PROMPT + question;
            String hypothetical = knowledgeRagHelperService.chat(prompt);
            if (hypothetical == null || hypothetical.isBlank()) {
                log.warn("HyDE生成结果为空，回退到原始问题");
                return question;
            }
            log.info("HyDE假设回答: 长度={}", hypothetical.length());
            return hypothetical;
        } catch (Exception e) {
            log.warn("HyDE生成失败，回退到原始问题: {}", e.getMessage());
            return question;
        }
    }

    public boolean shouldUseHyde(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        return question.length() < 15;
    }
}
