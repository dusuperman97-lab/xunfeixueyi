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

/**
 * 知识搜索工具 - 用于搜索专业知识、学术内容等
 */
@Slf4j
@Component
public class KnowledgeSearchTool3 {

    // 博查 Web Search API
    private static final String BOCHA_SEARCH_API_URL = "https://api.bochaai.com/v1/web-search";

    // API Key（生产环境建议从配置文件读取）
    private static final String API_KEY = "sk-c260a40d87164408a15f83cab3f3df32";

    private static final int RESULT_COUNT = 5;
    private static final int TIMEOUT = 30000;

    private final String apiUrl;
    private final String apiKey;

    public KnowledgeSearchTool3() {
        this(BOCHA_SEARCH_API_URL, API_KEY);
    }

    KnowledgeSearchTool3(String apiUrl, String apiKey) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    @Tool(
            name = "knowledgeSearch",
            value = "搜索专业文章、技术博客、学术文献。当用户需要查找学习资源时，必须调用此工具获取相关文章。" +
                    "关键词优化规则：" +
                    "1. 直接使用中文关键词（博查搜索支持中文内容检索）" +
                    "2. 添加限定词：教程、入门、实战、进阶、最佳实践等" +
                    "3. 示例：'机器学习入门'→'机器学习 入门 教程'、'Spring Boot实战'→'Spring Boot 实战 项目案例'" +
                    "返回结果包含：标题、摘要、来源链接，用于后续生成结构化文章资源列表。"
    )
    public String searchKnowledge(
            @P(value = "搜索关键词，如'机器学习 入门 教程'、'Spring Boot 实战 项目'") String query) {

        log.info("=== 知识搜索工具被调用！query = [{}]", query);

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.set("query", query);
            requestBody.set("summary", true);
            requestBody.set("count", RESULT_COUNT);

            String response = postJson(apiUrl, apiKey, requestBody.toString());
            return parseBochaResult(query, response);

        } catch (Exception e) {
            log.error("知识搜索失败", e);
            return "Error searching knowledge: " + e.getMessage();
        }
    }

    String postJson(String url, String apiKey, String body) {
        HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body)
                .timeout(TIMEOUT)
                .execute();

        if (response.getStatus() != 200) {
            throw new IllegalStateException("博查 API 返回错误状态码: " + response.getStatus());
        }

        return response.body();
    }

    private String parseBochaResult(String query, String response) {
        JSONObject jsonObject = JSONUtil.parseObj(response);

        Integer code = jsonObject.getInt("code");
        Boolean success = jsonObject.getBool("success");
        if ((code != null && code != 200) || Boolean.FALSE.equals(success)) {
            String message = jsonObject.getStr("msg", "未知错误");
            log.warn("博查 API 返回错误: code={}, msg={}", code, message);
            return "搜索失败，博查 API 返回错误: " + message;
        }

        JSONObject data = jsonObject.getJSONObject("data");
        JSONObject webPages = data == null ? jsonObject.getJSONObject("webPages") : data.getJSONObject("webPages");
        JSONArray results = webPages == null ? null : webPages.getJSONArray("value");

        if (results == null || results.isEmpty()) {
            log.warn("未找到相关搜索结果");
            return "未找到与 '" + query + "' 相关的专业知识。";
        }

        int resultCount = Math.min(RESULT_COUNT, results.size());
        StringBuilder formattedResult = new StringBuilder();
        formattedResult.append("【博查知识搜索结果 - 共 ").append(resultCount).append(" 条】\n\n");

        for (int i = 0; i < resultCount; i++) {
            JSONObject item = results.getJSONObject(i);
            String title = item.getStr("name", "无标题");
            String summary = item.getStr("summary");
            if (summary == null || summary.isBlank()) {
                summary = item.getStr("snippet", "无摘要");
            }
            String url = item.getStr("url", "");

            formattedResult.append("[").append(i + 1).append("] ").append(title).append("\n");
            formattedResult.append("摘要: ").append(summary).append("\n");
            formattedResult.append("来源: ").append(url).append("\n\n");
        }

        log.info("知识搜索完成，返回 {} 条结果", resultCount);
        return formattedResult.toString();
    }
}
