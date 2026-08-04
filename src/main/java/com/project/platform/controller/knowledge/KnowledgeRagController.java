package com.project.platform.controller.knowledge;

import com.project.platform.agents.ai.KnowledgeRagAssistant;
import com.project.platform.agents.config.utils.Tools;
import com.project.platform.dto.KnowledgeAnswerDTO;
import com.project.platform.dto.KnowledgeQueryDTO;
import com.project.platform.vo.ResponseVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 知识库 RAG 问答接口
 * <p>
 * 支持三种模式：
 * 1. 默认模式 - 根据请求参数灵活控制各增强环节
 * 2. 快速模式 - 关闭耗时环节，优先响应速度
 * 3. 高质量模式 - 开启所有增强环节，优先答案质量
 * <p>
 * 安全策略：所有接口从 CurrentUserThreadLocal 获取当前登录用户 ID，
 * 不信任前端传入的 studentId，防止横向越权访问。
 */
@RestController
@RequestMapping("/knowledge/rag")
@Slf4j
public class KnowledgeRagController {

    @Resource
    private KnowledgeRagAssistant knowledgeRagAssistant;

    @Resource
    private Tools tools;

    /**
     * 知识库问答（默认模式，支持通过参数控制增强环节）
     */
    @PostMapping("/query")
    public ResponseVO<KnowledgeAnswerDTO> query(
            @RequestBody KnowledgeQueryDTO queryDTO) {
        try {
            Long studentId = Long.valueOf(tools.getCurrentUserId());
            log.info("收到知识库问答请求: studentId={}, question={}", studentId, queryDTO.getQuestion());

            KnowledgeAnswerDTO result = knowledgeRagAssistant.answerQuestion(studentId, queryDTO);
            return ResponseVO.ok(result);
        } catch (Exception e) {
            log.error("知识库问答失败", e);
            return ResponseVO.error(e.getMessage());
        }
    }

    /**
     * 知识库问答（流式输出）
     */
    @PostMapping("/stream")
    public Flux<String> queryStream(
            @RequestBody KnowledgeQueryDTO queryDTO) {
        try {
            Long studentId = Long.valueOf(tools.getCurrentUserId());
            log.info("收到知识库流式问答请求: studentId={}, question={}", studentId, queryDTO.getQuestion());

            return knowledgeRagAssistant.answerQuestionStreaming(studentId, queryDTO);
        } catch (Exception e) {
            log.error("知识库流式问答失败", e);
            return Flux.error(e);
        }
    }

    /**
     * 知识库问答 - 快速模式
     * 关闭查询改写、HyDE、Rerank等耗时环节，优先响应速度
     */
    @PostMapping("/query/fast")
    public ResponseVO<KnowledgeAnswerDTO> queryFast(
            @RequestBody KnowledgeQueryDTO queryDTO) {
        try {
            Long studentId = Long.valueOf(tools.getCurrentUserId());
            log.info("收到知识库快速问答请求: studentId={}, question={}", studentId, queryDTO.getQuestion());

            KnowledgeAnswerDTO result = knowledgeRagAssistant.answerQuestionFast(studentId, queryDTO);
            return ResponseVO.ok(result);
        } catch (Exception e) {
            log.error("知识库快速问答失败", e);
            return ResponseVO.error(e.getMessage());
        }
    }

    /**
     * 知识库问答 - 高质量模式
     * 开启查询改写、HyDE、Rerank等所有增强环节，优先答案质量
     */
    @PostMapping("/query/high-quality")
    public ResponseVO<KnowledgeAnswerDTO> queryHighQuality(
            @RequestBody KnowledgeQueryDTO queryDTO) {
        try {
            Long studentId = Long.valueOf(tools.getCurrentUserId());
            log.info("收到知识库高质量问答请求: studentId={}, question={}", studentId, queryDTO.getQuestion());

            KnowledgeAnswerDTO result = knowledgeRagAssistant.answerQuestionHighQuality(studentId, queryDTO);
            return ResponseVO.ok(result);
        } catch (Exception e) {
            log.error("知识库高质量问答失败", e);
            return ResponseVO.error(e.getMessage());
        }
    }

    /**
     * 测试知识库检索（仅检索，不生成答案）
     */
    @PostMapping("/test-retrieve")
    public ResponseVO<KnowledgeAnswerDTO> testRetrieve(
            @RequestBody KnowledgeQueryDTO queryDTO) {
        try {
            Long studentId = Long.valueOf(tools.getCurrentUserId());
            log.info("收到知识库检索测试请求: studentId={}, question={}", studentId, queryDTO.getQuestion());

            KnowledgeAnswerDTO result = knowledgeRagAssistant.answerQuestion(studentId, queryDTO);

            // 只返回检索结果，不返回生成的答案
            result.setAnswer("(测试模式：仅返回检索结果)");

            return ResponseVO.ok(result);
        } catch (Exception e) {
            log.error("知识库检索测试失败", e);
            return ResponseVO.error(e.getMessage());
        }
    }
}
