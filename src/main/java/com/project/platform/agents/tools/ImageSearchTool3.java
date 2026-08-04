package com.project.platform.agents.tools;


import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 图片搜索工具类（基于 Pexels API）
 * 根据关键词搜索相关知识图片，返回单张高清图片链接
 */
@Slf4j
@Component
public class ImageSearchTool3 {

    private static final String API_KEY = "kffeEAtTZiiiyRu3kuFNiwFDAoq2VVfNNrphpfs3SZZr5Kn2JtAWVZW9";
    private static final String API_URL = "https://api.pexels.com/v1/search";

    @Tool(
            name = "imageSearch",
            value = "根据关键词搜索相关知识图片，返回一张高清图片链接。当用户需要查看示意图、示例图时调用此工具。" +
                    "参数说明：" +
                    "query: 搜索关键词，如'Java编程'、'Spring框架'、'数据库架构图'"
    )
    public String searchImage(@P(value = "搜索关键词，描述需要查找的图片内容，如'Java编程'、'Spring框架'") String query) {
        log.info("===  Pexels图片搜索工具被调用！query = [{}]", query);

        try {
            String imageUrl = searchSingleImage(query);

            if (StrUtil.isNotBlank(imageUrl)) {
                log.info("图片搜索完成，返回图片URL: {}", imageUrl);
                return imageUrl;
            } else {
                return "未找到相关图片，请尝试其他关键词";
            }

        } catch (Exception e) {
            log.error("图片搜索失败", e);
            return "图片搜索失败: " + e.getMessage();
        }
    }

    /**
     * 搜索单张高清图片
     * @param query 搜索关键词
     * @return 高清图片URL，如果未找到则返回null
     */
    private String searchSingleImage(String query) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", API_KEY);

        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        params.put("per_page", 1); // ✅ 只请求1张图片
        params.put("page", 1);

        log.info("调用 Pexels API: {}, query={}", API_URL, query);

        String response = HttpUtil.createGet(API_URL)
                .addHeaders(headers)
                .form(params)
                .timeout(10000)
                .execute()
                .body();

        log.info("API 响应: {}", response.substring(0, Math.min(200, response.length())));

        JSONObject jsonObject = JSONUtil.parseObj(response);

        if (!jsonObject.containsKey("photos") || jsonObject.getJSONArray("photos").isEmpty()) {
            log.warn("未找到相关图片");
            return null;
        }

        JSONObject firstPhoto = jsonObject.getJSONArray("photos").getJSONObject(0);
        JSONObject src = firstPhoto.getJSONObject("src");

        // ✅ 优先使用原图（original），其次是大图（large2x），最后是中等图（medium）
        String imageUrl = src.getStr("original");
        if (StrUtil.isBlank(imageUrl)) {
            imageUrl = src.getStr("large2x");
        }
        if (StrUtil.isBlank(imageUrl)) {
            imageUrl = src.getStr("large");
        }

        log.info("提取图片URL: {}", imageUrl);
        return imageUrl;
    }
}
