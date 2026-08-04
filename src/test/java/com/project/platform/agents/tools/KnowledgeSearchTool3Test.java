package com.project.platform.agents.tools;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeSearchTool3Test {

    @Test
    void searchKnowledgeUsesBochaWebSearchAndFormatsResults() {
        KnowledgeSearchTool3 tool = new KnowledgeSearchTool3(
                "https://api.bochaai.com/v1/web-search",
                "test-api-key"
        ) {
            @Override
            String postJson(String url, String apiKey, String body) {
                assertEquals("https://api.bochaai.com/v1/web-search", url);
                assertEquals("test-api-key", apiKey);

                JSONObject request = JSONUtil.parseObj(body);
                assertEquals("阿里巴巴 ESG", request.getStr("query"));
                assertEquals(Boolean.TRUE, request.getBool("summary"));
                assertEquals(5, request.getInt("count"));

                return """
                        {
                          "webPages": {
                            "value": [
                              {
                                "name": "阿里巴巴发布2024年ESG报告",
                                "url": "https://example.com/esg",
                                "summary": "阿里巴巴持续推进减碳举措。",
                                "snippet": "备用摘要"
                              }
                            ]
                          }
                        }
                        """;
            }
        };

        String result = tool.searchKnowledge("阿里巴巴 ESG");

        assertTrue(result.contains("【博查知识搜索结果 - 共 1 条】"));
        assertTrue(result.contains("[1] 阿里巴巴发布2024年ESG报告"));
        assertTrue(result.contains("摘要: 阿里巴巴持续推进减碳举措。"));
        assertTrue(result.contains("来源: https://example.com/esg"));
    }

    @Test
    void searchKnowledgeParsesBochaDataWrappedResults() {
        KnowledgeSearchTool3 tool = new KnowledgeSearchTool3(
                "https://api.bochaai.com/v1/web-search",
                "test-api-key"
        ) {
            @Override
            String postJson(String url, String apiKey, String body) {
                return """
                        {
                          "code": 200,
                          "log_id": "test-log-id",
                          "msg": null,
                          "data": {
                            "_type": "SearchResponse",
                            "webPages": {
                              "value": [
                                {
                                  "name": "java文献 - 示例",
                                  "url": "https://example.com/java",
                                  "summary": "Java 相关文献摘要。"
                                }
                              ]
                            }
                          }
                        }
                        """;
            }
        };

        String result = tool.searchKnowledge("java相关文献");

        assertTrue(result.contains("【博查知识搜索结果 - 共 1 条】"));
        assertTrue(result.contains("[1] java文献 - 示例"));
        assertTrue(result.contains("摘要: Java 相关文献摘要。"));
        assertTrue(result.contains("来源: https://example.com/java"));
    }
}
