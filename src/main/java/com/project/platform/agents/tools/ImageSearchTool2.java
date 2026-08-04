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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class ImageSearchTool2 {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Tool(
            name = "baiduImageSearch",
            value = "搜索中文技术示意图、架构图、流程图。关键词用中文，返回5张图片直链。"
    )
    public List<String> searchImage(
            @P(value = "中文关键词，如：微服务架构图、Redis集群示意图") String keyword) {

        log.info("=== 百度图片搜索被调用！keyword = [{}]", keyword);

        Set<String> imageUrlSet = new LinkedHashSet<>();

        HttpURLConnection conn = null;

        try {
            String encodedWord = URLEncoder.encode(keyword, StandardCharsets.UTF_8);

            String apiUrl = "https://image.baidu.com/search/acjson"
                    + "?tn=resultjson_com"
                    + "&logid=undefined"
                    + "&ipn=rj"
                    + "&ct=201326592"
                    + "&is="
                    + "&fp=result"
                    + "&queryWord=" + encodedWord
                    + "&cl=2"
                    + "&lm=-1"
                    + "&ie=utf-8"
                    + "&oe=utf-8"
                    + "&adpicid="
                    + "&st=-1"
                    + "&z="
                    + "&ic=0"
                    + "&hd="
                    + "&latest="
                    + "&copyright="
                    + "&word=" + encodedWord
                    + "&s="
                    + "&se="
                    + "&tab="
                    + "&width="
                    + "&height="
                    + "&face=0"
                    + "&istype=2"
                    + "&qc="
                    + "&nc=1"
                    + "&fr="
                    + "&expermode="
                    + "&force="
                    + "&pn=0"
                    + "&rn=30"
                    + "&gsm=1e";

            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);

            // 尽量模拟真实浏览器请求
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                            + "AppleWebKit/537.36 (KHTML, like Gecko) "
                            + "Chrome/120.0.0.0 Safari/537.36");

            conn.setRequestProperty("Accept",
                    "application/json, text/javascript, */*; q=0.01");

            conn.setRequestProperty("Accept-Language",
                    "zh-CN,zh;q=0.9,en;q=0.8");

            conn.setRequestProperty("Referer",
                    "https://image.baidu.com/search/index?tn=baiduimage&word=" + encodedWord);

            conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");

            int responseCode = conn.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.warn("百度图片接口状态码: {}", responseCode);

                String errorBody = readErrorBody(conn);
                log.warn("百度图片接口错误响应: {}", errorBody);

                return new ArrayList<>();
            }

            String json = readResponse(conn);
            
            if (json == null || json.isBlank()) {
                log.warn("百度图片接口返回为空");
                return new ArrayList<>();
            }
            
            // 百度有时会在 JSON 前面加脏字符,截取第一个 {
            int jsonStart = json.indexOf("{");
            if (jsonStart > 0) {
                json = json.substring(jsonStart);
            }
            
            // 清理JSON中的非法控制字符(保留合法的空白字符: \t, \n, \r)
            json = cleanJsonString(json);
            
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.path("data");

            if (!data.isArray()) {
                log.warn("百度图片接口 data 不是数组，原始响应: {}", json);
                return new ArrayList<>();
            }

            for (JsonNode item : data) {
                if (imageUrlSet.size() >= 5) {
                    break;
                }

                // 每个 item 只取一张最佳质量的图片，避免同图不同尺寸都加入
                String bestUrl = pickBestUrl(item);
                addIfValid(imageUrlSet, bestUrl);
            }

        } catch (Exception e) {
            log.error("百度图片搜索失败，keyword={}", keyword, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        List<String> imageUrls = new ArrayList<>(imageUrlSet);

        if (imageUrls.size() > 5) {
            imageUrls = imageUrls.subList(0, 5);
        }

        log.info("返回 {} 张图片: {}", imageUrls.size(), imageUrls);

        return imageUrls;
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            return sb.toString();
        }
    }

    private String readErrorBody(HttpURLConnection conn) {
        try {
            if (conn.getErrorStream() == null) {
                return "";
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                return sb.toString();
            }
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 清理JSON字符串中的非法控制字符
     * 保留合法的空白字符: tab(\t), newline(\n), carriage return(\r)
     * 移除其他控制字符(ASCII 0-31中除了9,10,13之外的字符)
     */
    private String cleanJsonString(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        StringBuilder cleaned = new StringBuilder(json.length());
        boolean inString = false; // 是否在字符串内部
        boolean escaped = false;  // 是否遇到转义符

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            // 处理转义字符
            if (escaped) {
                cleaned.append(c);
                escaped = false;
                continue;
            }

            // 检测到转义符
            if (c == '\\' && inString) {
                cleaned.append(c);
                escaped = true;
                continue;
            }

            // 检测字符串边界
            if (c == '"') {
                inString = !inString;
                cleaned.append(c);
                continue;
            }

            // 如果在字符串内部,需要特殊处理控制字符
            if (inString) {
                // 合法的控制字符: tab(9), newline(10), carriage return(13)
                if (c == '\t' || c == '\n' || c == '\r') {
                    // 将这些字符转义
                    if (c == '\t') {
                        cleaned.append("\\t");
                    } else if (c == '\n') {
                        cleaned.append("\\n");
                    } else {
                        cleaned.append("\\r");
                    }
                } else if (c < 32) {
                    // 其他控制字符,替换为空格或Unicode转义
                    log.debug("发现非法控制字符(ASCII {}),已替换为空格", (int) c);
                    cleaned.append(' ');
                } else {
                    cleaned.append(c);
                }
            } else {
                // 在字符串外部,直接移除控制字符(除了合法空白)
                if (c >= 32 || c == '\t' || c == '\n' || c == '\r') {
                    cleaned.append(c);
                } else {
                    log.debug("移除非字符串区域的非法控制字符(ASCII {})", (int) c);
                }
            }
        }

        return cleaned.toString();
    }

    /** 从百度图片结果中选最佳质量的 URL：objURL > replaceUrl.ObjURL > middleURL > thumbURL */
    private String pickBestUrl(JsonNode item) {
        String obj = item.path("objURL").asText("");
        if (isValidImageUrl(obj)) return obj;

        JsonNode replaceArr = item.path("replaceUrl");
        if (replaceArr.isArray()) {
            for (JsonNode r : replaceArr) {
                String rObj = r.path("ObjURL").asText("");
                if (isValidImageUrl(rObj)) return rObj;
            }
        }

        String middle = item.path("middleURL").asText("");
        if (isValidImageUrl(middle)) return middle;

        String thumb = item.path("thumbURL").asText("");
        if (isValidImageUrl(thumb)) return thumb;

        return "";
    }

    private boolean isValidImageUrl(String url) {
        if (url == null || url.isBlank()) return false;
        String trimmed = url.trim();
        return (trimmed.startsWith("http://") || trimmed.startsWith("https://"))
                && !trimmed.contains("baidu.com/link?url=");
    }

    private void addIfValid(Set<String> imageUrlSet, String imageUrl) {
        if (imageUrlSet.size() >= 5) {
            return;
        }

        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        imageUrl = imageUrl.trim();

        // 只保留 http/https 图片地址
        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            return;
        }

        // 过滤一些明显不是图片直链的地址
        if (imageUrl.contains("baidu.com/link?url=")) {
            return;
        }

        imageUrlSet.add(imageUrl);
    }
}
