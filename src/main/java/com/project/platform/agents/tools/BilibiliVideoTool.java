package com.project.platform.agents.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class BilibiliVideoTool {

    private static final int DEFAULT_MAX_RESULTS = 3;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Tool(
            name = "bilibiliVideoSearch",
            value = "搜索B站教学视频。当用户需要查找技术学习资源时，必须调用此工具，并从返回的多条结果中选择多个真实视频链接。" +
                    "关键词优化规则：" +
                    "1. 使用中文关键词，用空格分隔核心概念" +
                    "2. 添加'教程'、'入门'、'实战'等后缀" +
                    "3. 示例：'机器学习'→'机器学习 入门 教程'、'Spring Boot'→'Spring Boot 实战 项目'、'Python数据分析'→'Python 数据分析 完整教程'" +
                    "返回结果包含多条视频的标题、链接、时长、UP主和简介。"
    )
    public String searchVideos(@P(value = "搜索关键词，如'机器学习 入门 教程'、'Spring Boot 实战'") String keyword) {
        log.info("=== B站视频搜索工具被调用！keyword = [{}]", keyword);

        try {
            List<VideoResult> videos = searchStructured(keyword, DEFAULT_MAX_RESULTS);
            String result = formatVideoResults(videos);
            log.info("bilibiliVideoSearch搜索结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("B站搜索失败", e);
            return "B站搜索失败，可手动打开搜索页: " + buildSearchPageUrl(keyword);
        }
    }

    /**
     * 结构化搜索B站视频，供需要直接消费多条视频结果的服务复用。
     */
    public List<VideoResult> searchStructured(String keyword, int maxResults) throws Exception {
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String apiUrl = "https://api.bilibili.com/x/web-interface/search/all/v2"
                + "?keyword=" + encodedKeyword
                + "&search_type=video"
                + "&page=1"
                + "&page_size=" + Math.max(maxResults, DEFAULT_MAX_RESULTS);

        Document doc = Jsoup.connect(apiUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .header("Referer", "https://www.bilibili.com/")
                .header("Cookie", "buvid3=guest; _uuid=guest; buvid4=guest")
                .header("Accept", "application/json")
                .timeout(10000)
                .ignoreContentType(true)
                .get();

        String json = doc.text();
        log.info("B站API返回: {}", json.substring(0, Math.min(200, json.length())));
        return parseVideosFromJson(json, maxResults);
    }

    List<VideoResult> parseVideosFromJson(String json, int maxResults) throws Exception {
        List<VideoResult> results = new ArrayList<>();
        if (json == null || json.isBlank() || maxResults <= 0) {
            return results;
        }

        JsonNode root = OBJECT_MAPPER.readTree(json);
        int code = root.path("code").asInt(0);
        if (code != 0) {
            log.warn("B站API业务错误: code={}, message={}", code, root.path("message").asText(""));
            return results;
        }

        JsonNode resultArr = root.path("data").path("result");
        if (resultArr.isArray()) {
            for (JsonNode typeBlock : resultArr) {
                String type = typeBlock.path("result_type").asText("");
                if (!"video".equals(type)) {
                    continue;
                }
                JsonNode dataList = typeBlock.path("data");
                if (dataList.isArray()) {
                    results.addAll(parseVideoDataList(dataList, maxResults - results.size()));
                }
                if (results.size() >= maxResults) {
                    break;
                }
            }
            return results;
        }

        JsonNode directData = root.path("data");
        if (directData.isArray()) {
            return parseVideoDataList(directData, maxResults);
        }

        log.warn("B站API返回格式异常: data.result 不是数组");
        return results;
    }

    private List<VideoResult> parseVideoDataList(JsonNode dataList, int maxResults) {
        List<VideoResult> results = new ArrayList<>();
        Set<String> seenBvid = new LinkedHashSet<>();

        for (JsonNode item : dataList) {
            if (results.size() >= maxResults) {
                break;
            }

            String bvid = item.path("bvid").asText("").trim();
            String title = cleanHtml(item.path("title").asText(""));
            if (bvid.isBlank() || title.isBlank() || !seenBvid.add(bvid)) {
                continue;
            }

            VideoResult video = new VideoResult();
            video.setTitle(title);
            video.setUrl("https://www.bilibili.com/video/" + bvid);
            video.setDuration(item.path("duration").asText(""));
            video.setAuthor(item.path("author").asText(""));
            video.setDescription(truncate(item.path("description").asText(""), 160));
            video.setSource("B站");
            results.add(video);
        }

        return results;
    }

    private String formatVideoResults(List<VideoResult> videos) {
        if (videos == null || videos.isEmpty()) {
            return "未找到相关视频。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【B站视频搜索结果 - 共 ").append(videos.size()).append(" 条】\n\n");
        for (int i = 0; i < videos.size(); i++) {
            VideoResult video = videos.get(i);
            sb.append("[").append(i + 1).append("] 标题: ").append(video.getTitle()).append("\n");
            sb.append("链接: ").append(video.getUrl()).append("\n");
            appendIfPresent(sb, "时长", video.getDuration());
            appendIfPresent(sb, "UP主", video.getAuthor());
            appendIfPresent(sb, "简介", video.getDescription());
            sb.append("来源: ").append(video.getSource()).append("\n\n");
        }
        return sb.toString();
    }

    private void appendIfPresent(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(": ").append(value.trim()).append("\n");
        }
    }

    private String cleanHtml(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Jsoup.parse(value).text().trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private String buildSearchPageUrl(String keyword) {
        return "https://search.bilibili.com/all?keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
    }

    @Data
    public static class VideoResult {
        private String title;
        private String url;
        private String duration;
        private String author;
        private String description;
        private String source;
    }
}
