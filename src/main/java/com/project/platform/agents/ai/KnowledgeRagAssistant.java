package com.project.platform.agents.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.platform.dto.KnowledgeAnswerDTO;
import com.project.platform.dto.KnowledgeQueryDTO;
import com.project.platform.entity.QaHistory;
import com.project.platform.rag.RagPipeline;
import com.project.platform.rag.RagPipelineConfig;
import com.project.platform.rag.RagPipelineResult;
import com.project.platform.service.QaHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 知识库 RAG 助手服务
 * 实现检索增强生成（Retrieval-Augmented Generation）
 * <p>
 * 底层检索统一委托给 {@link RagPipeline}，确保查询经过
 * 查询改写 → HyDE → 向量检索 → 权限过滤 → 重排序 → 引用生成 的完整流程。
 * <p>
 * 每次问答完成后自动记录到问答历史。
 */
@Slf4j
@Service
public class KnowledgeRagAssistant {

    @Resource
    private KnowledgeRagHelperService knowledgeRagHelperService;

    @Resource
    private RagPipeline ragPipeline;

    @Resource
    private QaHistoryService qaHistoryService;

    private static final ObjectMapper objectMapper = new ObjectMapper();


    /**
     * 知识库问答（默认模式，经由完整 RagPipeline）
     *
     * @param studentId 用户ID
     * @param queryDTO  查询请求
     * @return 问答结果
     */
    public KnowledgeAnswerDTO answerQuestion(Long studentId, KnowledgeQueryDTO queryDTO) {
        long startTime = System.currentTimeMillis();

        log.info("=== 知识库问答开始（via RagPipeline）=== studentId={}, question={}, kbId={}",
                studentId, queryDTO.getQuestion(), queryDTO.getKbId());

        // 1. 从 DTO 构建 RagPipelineConfig
        RagPipelineConfig config = buildPipelineConfig(queryDTO);

        // 2. 执行完整的 RAG Pipeline（查询改写 → HyDE → 检索 → 过滤 → 重排 → 引用）
        RagPipelineResult pipelineResult = ragPipeline.execute(
                studentId, queryDTO.getKbId(), queryDTO.getQuestion(), config);

        // 3. 构建 RAG Prompt（使用 Pipeline 格式化后的上下文）
        String ragPrompt = buildRagPrompt(queryDTO.getQuestion(), pipelineResult.getFormattedContext());

        // 4. 调用大模型生成答案
        String answer = generateAnswer(ragPrompt);

        // 5. 构建响应
        KnowledgeAnswerDTO result = new KnowledgeAnswerDTO();
        result.setAnswer(answer);
        result.setSources(pipelineResult.getSources());
        result.setUsedKnowledge(pipelineResult.isUsedKnowledge());
        result.setRetrievalTime(pipelineResult.getRetrievalTime());

        // Pipeline 增强字段
        result.setOriginalQuestion(pipelineResult.getOriginalQuestion());
        result.setRewrittenQuestion(pipelineResult.getRewrittenQuestion());
        result.setActualSearchQuery(pipelineResult.getActualSearchQuery());
        result.setCitations(pipelineResult.getCitations());
        result.setTimeSummary(pipelineResult.getTimeSummary());

        // 6. 记录问答历史
        saveQaHistory(studentId, queryDTO, pipelineResult, answer);

        log.info("=== 知识库问答完成 === answer_length={}, sources={}, {}",
                answer != null ? answer.length() : 0,
                pipelineResult.getSources().size(),
                pipelineResult.getTimeSummary());
        return result;
    }

    /**
     * 流式知识库问答（返回 SSE 格式）
     *
     * @param studentId 用户ID
     * @param queryDTO  查询请求
     * @return 流式答案
     */
    public Flux<String> answerQuestionStreaming(Long studentId, KnowledgeQueryDTO queryDTO) {
        log.info("=== 知识库流式问答开始（via RagPipeline）=== studentId={}, question={}, kbId={}",
                studentId, queryDTO.getQuestion(), queryDTO.getKbId());

        // 1. 从 DTO 构建 RagPipelineConfig
        RagPipelineConfig config = buildPipelineConfig(queryDTO);

        // 2. 执行完整的 RAG Pipeline
        RagPipelineResult pipelineResult = ragPipeline.execute(
                studentId, queryDTO.getKbId(), queryDTO.getQuestion(), config);

        // 3. 构建 RAG Prompt 并流式生成
        String ragPrompt = buildRagPrompt(queryDTO.getQuestion(), pipelineResult.getFormattedContext());
        return generateAnswerStreaming(ragPrompt);
    }

    /**
     * 快速问答（委托给 RagPipeline.executeFast）
     */
    public KnowledgeAnswerDTO answerQuestionFast(Long studentId, KnowledgeQueryDTO queryDTO) {
        long startTime = System.currentTimeMillis();
        log.info("=== 知识库快速问答（via RagPipeline.executeFast）=== studentId={}, question={}",
                studentId, queryDTO.getQuestion());

        // 委托给 RagPipeline 快速模式（关闭查询改写、HyDE、Rerank）
        RagPipelineResult pipelineResult = ragPipeline.executeFast(
                studentId, queryDTO.getKbId(), queryDTO.getQuestion());

        String ragPrompt = buildRagPrompt(queryDTO.getQuestion(), pipelineResult.getFormattedContext());
        String answer = generateAnswer(ragPrompt);

        KnowledgeAnswerDTO result = new KnowledgeAnswerDTO();
        result.setAnswer(answer);
        result.setSources(pipelineResult.getSources());
        result.setUsedKnowledge(pipelineResult.isUsedKnowledge());
        result.setRetrievalTime(pipelineResult.getRetrievalTime());
        result.setOriginalQuestion(pipelineResult.getOriginalQuestion());
        result.setRewrittenQuestion(pipelineResult.getRewrittenQuestion());
        result.setActualSearchQuery(pipelineResult.getActualSearchQuery());
        result.setCitations(pipelineResult.getCitations());
        result.setTimeSummary(pipelineResult.getTimeSummary());

        log.info("=== 知识库快速问答完成 === answer_length={}, sources={}",
                answer != null ? answer.length() : 0, pipelineResult.getSources().size());
        return result;
    }

    /**
     * 高质量问答（委托给 RagPipeline.executeHighQuality）
     */
    public KnowledgeAnswerDTO answerQuestionHighQuality(Long studentId, KnowledgeQueryDTO queryDTO) {
        long startTime = System.currentTimeMillis();
        log.info("=== 知识库高质量问答（via RagPipeline.executeHighQuality）=== studentId={}, question={}",
                studentId, queryDTO.getQuestion());

        // 委托给 RagPipeline 高质量模式（开启所有增强环节）
        RagPipelineResult pipelineResult = ragPipeline.executeHighQuality(
                studentId, queryDTO.getKbId(), queryDTO.getQuestion());

        String ragPrompt = buildRagPrompt(queryDTO.getQuestion(), pipelineResult.getFormattedContext());
        String answer = generateAnswer(ragPrompt);

        KnowledgeAnswerDTO result = new KnowledgeAnswerDTO();
        result.setAnswer(answer);
        result.setSources(pipelineResult.getSources());
        result.setUsedKnowledge(pipelineResult.isUsedKnowledge());
        result.setRetrievalTime(pipelineResult.getRetrievalTime());
        result.setOriginalQuestion(pipelineResult.getOriginalQuestion());
        result.setRewrittenQuestion(pipelineResult.getRewrittenQuestion());
        result.setActualSearchQuery(pipelineResult.getActualSearchQuery());
        result.setCitations(pipelineResult.getCitations());
        result.setTimeSummary(pipelineResult.getTimeSummary());

        log.info("=== 知识库高质量问答完成 === answer_length={}, sources={}",
                answer != null ? answer.length() : 0, pipelineResult.getSources().size());
        return result;
    }

    /**
     * 记录问答历史
     *
     * @param studentId      用户ID
     * @param queryDTO       查询请求
     * @param pipelineResult Pipeline 结果
     * @param answer         最终答案
     */
    private void saveQaHistory(Long studentId, KnowledgeQueryDTO queryDTO,
                                RagPipelineResult pipelineResult, String answer) {
        try {
            QaHistory history = new QaHistory();
            history.setStudentId(studentId);
            history.setKbId(queryDTO.getKbId());
            history.setOriginalQuestion(pipelineResult.getOriginalQuestion());
            history.setRewrittenQuestion(pipelineResult.getRewrittenQuestion());
            history.setActualSearchQuery(pipelineResult.getActualSearchQuery());
            history.setAnswer(answer);
            history.setSourcesJson(toJson(pipelineResult.getSources()));
            history.setCitationsJson(toJson(pipelineResult.getCitations()));
            history.setFeedback(0);
            history.setRetrievalTime(pipelineResult.getRetrievalTime());
            history.setTotalTime(pipelineResult.getTotalTime());
            history.setResultCount(pipelineResult.getRerankedResultCount());
            history.setUsedKnowledge(pipelineResult.isUsedKnowledge());

            qaHistoryService.saveHistory(history);
        } catch (Exception e) {
            log.warn("问答历史记录保存失败（不影响主流程）: {}", e.getMessage());
        }
    }

    private String toJson(Object obj) {
        try {
            return obj != null ? objectMapper.writeValueAsString(obj) : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 从 KnowledgeQueryDTO 构建 RagPipelineConfig
     */
    private RagPipelineConfig buildPipelineConfig(KnowledgeQueryDTO dto) {
        return RagPipelineConfig.builder()
                .queryRewriteEnabled(dto.getQueryRewriteEnabled() != null ? dto.getQueryRewriteEnabled() : true)
                .hydeEnabled(dto.getHydeEnabled() != null ? dto.getHydeEnabled() : false)
                .rerankEnabled(dto.getRerankEnabled() != null ? dto.getRerankEnabled() : true)
                .permissionFilterEnabled(dto.getPermissionFilterEnabled() != null ? dto.getPermissionFilterEnabled() : true)
                .citationEnabled(dto.getCitationEnabled() != null ? dto.getCitationEnabled() : true)
                .maxResults(dto.getMaxResults() != null ? dto.getMaxResults() : 5)
                .minScore(dto.getMinScore() != null ? dto.getMinScore() : 0.7)
                .rerankTopN(dto.getRerankTopN() != null ? dto.getRerankTopN() : 3)
                .sessionId(dto.getSessionId())
                .chatHistory(dto.getChatHistory())
                .build();
    }

    /**
     * 构建 RAG Prompt
     */
    private String buildRagPrompt(String question, String context) {
        log.info("=== 构建 RAG Prompt === question={}, context_length={}", question, context.length());

        boolean hasKnowledge = !context.contains("未找到相关知识库内容");

        String promptTemplate;
        if (hasKnowledge) {
            promptTemplate = """
                    你是一位专业的AI学习助手，正在帮助学生解答问题。以下是从学生个人知识库中检索到的相关资料：

                    --- 参考资料 ---
                    %s

                    --- 学生问题 ---
                    %s

                    --- 回答指南 ---
                    • 像一位耐心的老师那样自然流畅地回答问题
                    • 充分运用上述资料中的信息，但不要用"根据资料1"、"根据文档"等机械化的表述
                    • 将知识融会贯通，用通俗易懂的语言解释，就像在面对面讲解
                    • 如果资料中的信息不够完整，可以补充你的专业知识，但要明确区分
                    • 语气亲切自然，避免过于正式或生硬的表达
                    • 如果涉及专业术语，记得用简单的语言解释一下

                    请开始回答：
                    """;

            return String.format(promptTemplate, context, question);
        } else {
            promptTemplate = """
                    你是一位专业的AI学习助手。学生在个人知识库中提问，但暂时没有检索到直接相关的内容。

                    --- 学生问题 ---
                    %s

                    --- 回答指南 ---
                    • 友好地告知学生知识库中暂时没有找到相关内容
                    • 基于你的专业知识提供帮助性的解答
                    • 建议学生上传相关学习资料到知识库，这样能获得更精准的答案
                    • 语气要鼓励和支持，让学生感受到帮助

                    请开始回答：
                    """;

            return String.format(promptTemplate, question);
        }
    }

    /**
     * 调用大模型生成答案
     */
    private String generateAnswer(String prompt) {
        try {
            return knowledgeRagHelperService.chat(prompt);
        } catch (Exception e) {
            log.error("调用大模型失败", e);
            return "抱歉，系统繁忙，请稍后再试。";
        }
    }

    /**
     * 调用大模型生成答案（流式输出）
     */
    private Flux<String> generateAnswerStreaming(String prompt) {
        try {
            return knowledgeRagHelperService.streamChat(prompt);
        } catch (Exception e) {
            log.error("调用大模型失败", e);
            return Flux.just("抱歉，系统繁忙，请稍后再试。");
        }
    }
}
