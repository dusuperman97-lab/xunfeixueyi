package com.project.platform.agents.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class KnowledgeSearchTool {


    private String apiKey = "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";

    private String apiUrl = "https://api.tavily.com/search";

    // 超时设置（毫秒）
    private static final int TIMEOUT = 30000;
    // 重试次数
    private static final int MAX_RETRIES = 3;

    /**
     * 结构化搜索 — 返回可编程使用的SearchResult列表
     * 供内容校验Agent等需要结构化数据的场景使用
     */
    public List<SearchResult> searchStructured(String query, int maxResults) {
        return searchStructured(query, maxResults, null);
    }

    /**
     * 结构化搜索（支持限定域名）
     * @param includeDomains 限定搜索的域名列表，如 ["douyin.com", "xiaohongshu.com"]，传 null 则不限定
     */
    public List<SearchResult> searchStructured(String query, int maxResults, List<String> includeDomains) {
        log.info("=== Tavily结构化搜索: query={}, maxResults={}, domains={}", query, maxResults, includeDomains);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("api_key", apiKey);
                paramMap.put("query", query);
                paramMap.put("search_depth", "advanced");
                paramMap.put("include_answer", false);
                paramMap.put("max_results", maxResults);
                if (includeDomains != null && !includeDomains.isEmpty()) {
                    paramMap.put("include_domains", includeDomains);
                }

                HttpResponse response = HttpRequest.post(apiUrl)
                        .header("Content-Type", "application/json")
                        .body(JSONUtil.toJsonStr(paramMap))
                        .timeout(TIMEOUT)
                        .execute();

                if (response.getStatus() == 200) {
                    JSONObject json = JSONUtil.parseObj(response.body());
                    JSONArray results = json.getJSONArray("results");
                    if (results == null || results.isEmpty()) return Collections.emptyList();

                    List<SearchResult> list = new ArrayList<>();
                    for (int i = 0; i < results.size(); i++) {
                        JSONObject item = results.getJSONObject(i);
                        SearchResult sr = new SearchResult();
                        sr.setTitle(item.getStr("title", ""));
                        sr.setUrl(item.getStr("url", ""));
                        sr.setContent(item.getStr("content", item.getStr("snippet", "")));
                        sr.setScore(item.getDouble("score", 0.0));
                        list.add(sr);
                    }
                    log.info("结构化搜索完成: query={}, results={}", query, list.size());
                    return list;
                }
                log.warn("Tavily API异常: status={}, attempt={}", response.getStatus(), attempt);
            } catch (Exception e) {
                log.warn("搜索尝试 {} 失败: {}", attempt, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try { Thread.sleep(1000L * attempt); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * 结构化搜索结果
     */
    @lombok.Data
    public static class SearchResult {
        private String title;
        private String url;
        private String content;
        private Double score;
    }

    @Tool(
            name = "knowledgeSearch",
            value = "搜索专业文章、技术博客、学术文献。当用户需要查找学习资源时，必须调用此工具获取相关文章。" +
                    "关键词优化规则：" +
                    "1. 直接使用中文关键词（Tavily支持多语言搜索）" +
                    "2. 添加限定词：教程、入门、实战、进阶、最佳实践等" +
                    "3. 示例：'机器学习入门'→'机器学习 入门 教程'、'Spring Boot实战'→'Spring Boot 实战 项目案例'" +
                    "返回结果包含：标题、摘要、来源链接和相关度评分，用于后续生成结构化文章资源列表。"
    )
    public String searchKnowledge(
            @P(value = "搜索关键词，如'机器学习 入门 教程'、'Spring Boot 实战 项目'") String query) {

        log.info("=== Tavily知识搜索工具被调用！query = [{}]", query);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.info("第 {} 次尝试调用 Tavily API: {}", attempt, apiUrl);

                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("api_key", apiKey);
                paramMap.put("query", query);
                paramMap.put("search_depth", "basic");
                paramMap.put("include_answer", true);
                paramMap.put("max_results", 5);

                HttpResponse response = HttpRequest.post(apiUrl)
                        .header("Content-Type", "application/json")
                        .body(JSONUtil.toJsonStr(paramMap))
                        .timeout(TIMEOUT)  // 设置超时
                        .execute();

                if (response.getStatus() == 200) {
                    String result = response.body();
                    log.info("Tavily搜索成功，返回结果长度: {}", result.length());
                    return parseResult(result);
                } else {
                    log.error("Tavily API返回错误状态码: {}", response.getStatus());
                    return "搜索失败，状态码: " + response.getStatus();
                }

            } catch (Exception e) {
                log.error("第 {} 次尝试失败: {}", attempt, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    log.error("Tavily知识搜索失败，query=[{}]", query, e);
                    return "搜索服务暂时不可用，请稍后重试。错误: " + e.getMessage();
                }
                // 重试前等待
                try {
                    Thread.sleep(1000 * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return "搜索失败";
    }

    private String parseResult(String jsonResult) {
        try {
            JSONObject jsonObject = JSONUtil.parseObj(jsonResult);
            
            // 提取 results 数组
            JSONArray results = jsonObject.getJSONArray("results");
            if (results == null || results.isEmpty()) {
                log.warn("未找到相关搜索结果");
                return "未找到相关知识。";
            }

            // 格式化输出多条结果
            int resultCount = Math.min(5, results.size());
            StringBuilder formattedResult = new StringBuilder();
            formattedResult.append("【Tavily知识搜索结果 - 共 ").append(resultCount).append(" 条】\n\n");

            for (int i = 0; i < resultCount; i++) {
                JSONObject item = results.getJSONObject(i);
                String title = item.getStr("title", "无标题");
                // Tavily 可能使用 content 或 snippet 字段
                String content = item.getStr("content");
                if (content == null || content.isEmpty()) {
                    content = item.getStr("snippet", "无摘要");
                }
                String url = item.getStr("url", "");
                Double score = item.getDouble("score", 0.0);

                formattedResult.append("[").append(i + 1).append("] ").append(title).append("\n");
                formattedResult.append("摘要: ").append(content).append("\n");
                formattedResult.append("来源: ").append(url).append("\n");
                if (score > 0) {
                    formattedResult.append("相关度: ").append(String.format("%.2f", score)).append("\n");
                }
                formattedResult.append("\n");
            }

            log.info("Tavily知识搜索完成，返回 {} 条结果", resultCount);
            return formattedResult.toString();
            
        } catch (Exception e) {
            log.error("解析Tavily结果失败", e);
            return jsonResult; // 降级：返回原始JSON
        }
    }
}
