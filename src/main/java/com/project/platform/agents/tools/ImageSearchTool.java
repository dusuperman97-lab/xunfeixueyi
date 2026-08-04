package com.project.platform.agents.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 图片搜索工具类（返回图片直链URL）
 * 使用 Pixabay API（免费，每日5000次请求）
 */
@Slf4j
@Component
public class ImageSearchTool {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Pixabay API Key（免费注册获取：https://pixabay.com/api/docs/）
    private static final String PIXABAY_API_KEY = "55466246-63f9dfdb62080fcf0cfc32f7c";

    @Tool(
            name = "imageSearch",
            value = "搜索技术相关的示意图、架构图、流程图。当用户需要查找学习资源配图时，必须调用此工具。" +
                    "关键词优化规则：" +
                    "1. 使用英文关键词+图表类型(diagram/architecture/flowchart/concept)" +
                    "2. 示例：'机器学习'→'machine learning algorithm diagram'、'神经网络'→'neural network architecture'、'数据库设计'→'database design flowchart'" +
                    "返回结果：图片直链URL列表（5张），用于后续生成结构化图片资源。"
    )
    public List<String> searchImage(@P(value = "搜索关键词，需优化后传入。如'机器学习'应转换为'machine learning algorithm diagram'") String keyword) {


        log.info("=== pixabay图片搜索工具被调用！keyword = [{}]", keyword);

        List<String> imageUrls = new ArrayList<>();

        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String apiUrl = "https://pixabay.com/api/?key=" + PIXABAY_API_KEY +
                    "&q=" + encodedKeyword +
                    "&image_type=photo" +
                    "&per_page=5" +
                    "&safesearch=true";

            log.info("调用 Pixabay API: {}", apiUrl);

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String jsonResponse = response.toString();
                log.info("API 响应长度: {}", jsonResponse.length());

                imageUrls = parsePixabayResponse(jsonResponse);
            } else {
                log.warn("Pixabay API 返回状态码: {}", responseCode);
            }

            conn.disconnect();

            log.info("图片搜索完成，返回 {} 个图片URL", imageUrls.size());

        } catch (Exception e) {
            log.error("图片搜索失败", e);
        }
        
        if (!imageUrls.isEmpty()) {
            String result = imageUrls.get(0);
            log.info("imageSearch搜索结果: {}", result);
        } else {
            log.warn("imageSearch未找到相关图片");
        }

        return imageUrls;
    }

    /**
     * 解析 Pixabay API 响应，提取图片URL
     */
    private List<String> parsePixabayResponse(String jsonResponse) throws Exception {
        List<String> imageUrls = new ArrayList<>();

        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode hits = root.path("hits");

        if (hits.isArray()) {
            for (JsonNode hit : hits) {
                String imageUrl = hit.path("largeImageURL").asText();

                if (!imageUrl.isEmpty()) {
                    imageUrls.add(imageUrl);
                    log.info("提取图片URL: {}", imageUrl);
                }

                if (imageUrls.size() >= 5) {
                    break;
                }
            }
        }

        return imageUrls;
    }
}
