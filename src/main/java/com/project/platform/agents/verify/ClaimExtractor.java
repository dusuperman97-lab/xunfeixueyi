package com.project.platform.agents.verify;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.project.platform.agents.ai.KnowledgeRagHelperService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 声明提取器 — 用LLM从课程文本中提取可独立验证的事实声明
 * <p>
 * 这是整个校验链路中唯一使用LLM的环节。
 * LLM只负责"从文本中找出哪些是事实断言"，不做真伪判断。
 * 真伪判断由 {@link FactCheckPipeline} 通过联网查证完成。
 * <p>
 * 设计原则：
 * <ul>
 *   <li>LLM输出结构化JSON，便于程序化处理</li>
 *   <li>normalizedStatement 改写为可直接用于搜索引擎的查询语句</li>
 *   <li>不提取主观评价、模糊表述、过渡语句</li>
 * </ul>
 *
 * @author duyongchao
 * @date 2026/6/17
 */
@Slf4j
@Component
public class ClaimExtractor {

    @Resource
    private KnowledgeRagHelperService chatService;

    /**
     * 从课程内容中提取可验证声明
     *
     * @param courseContent 课程全文
     * @return 声明列表（已填充claimId、originalText、searchQuery）
     */
    public List<VerificationClaim> extract(String courseContent) {
        if (courseContent == null || courseContent.isBlank()) {
            return Collections.emptyList();
        }

        log.info("[ClaimExtractor] 开始提取, contentLength={}", courseContent.length());

        String prompt = buildPrompt(courseContent);
        String response;
        try {
            response = chatService.chat(prompt);
        } catch (Exception e) {
            log.error("[ClaimExtractor] LLM调用失败", e);
            return Collections.emptyList();
        }

        List<VerificationClaim> claims = parseClaims(response);
        log.info("[ClaimExtractor] 完成, 提取{}条声明", claims.size());
        return claims;
    }

    private String buildPrompt(String content) {
        return """
            从以下课程内容中提取所有可独立验证的事实声明。

            提取范围（满足任一即可）:
            1. 定义性表述: "XX是..."、"XX指..."
            2. 包含具体数字: 年份、百分比、数量、排名
            3. 提及具体人物/机构及其成就
            4. 明确因果断言: "因为...所以..."、"导致"
            5. 案例引用: "以XX为例"、"例如XX"

            不提取:
            - 主观评价 ("很好"、"重要")
            - 过渡语句
            - 纯教学引导语 ("接下来我们学习...")

            每条声明改写searchQuery为适合搜索引擎的查询短语（15字以内）。

            输出纯JSON数组:
            [{"originalText":"原文","searchQuery":"搜索查询"}]

            课程内容:
            %s
            """.formatted(content);
    }

    private List<VerificationClaim> parseClaims(String response) {
        if (response == null || response.isBlank()) return Collections.emptyList();

        try {
            String json = extractJsonArray(response);
            if (json == null) {
                log.warn("[ClaimExtractor] 响应中无JSON数组");
                return Collections.emptyList();
            }

            JSONArray arr = JSON.parseArray(json);
            if (arr == null || arr.isEmpty()) return Collections.emptyList();

            List<VerificationClaim> claims = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String original = obj.getString("originalText");
                String query = obj.getString("searchQuery");

                if (original == null || original.isBlank()) continue;
                if (query == null || query.isBlank()) {
                    query = original.length() > 30 ? original.substring(0, 30) : original;
                }

                claims.add(VerificationClaim.builder()
                        .claimId("C" + String.format("%02d", claims.size() + 1))
                        .originalText(original.trim())
                        .searchQuery(query.trim())
                        .build());
            }
            return claims;
        } catch (Exception e) {
            log.error("[ClaimExtractor] JSON解析失败", e);
            return Collections.emptyList();
        }
    }

    private String extractJsonArray(String text) {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }
}
