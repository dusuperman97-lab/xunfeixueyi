package com.project.platform.controller.resource;

import cn.hutool.json.JSONUtil;
import com.project.platform.agents.ai.ResourceSearchService;
import com.project.platform.agents.config.utils.Tools;
import com.project.platform.agents.entry.ChatForm;
import com.project.platform.agents.tools.ImageSearchTool2;
import com.project.platform.agents.tools.KnowledgeSearchTool;
import com.project.platform.annotation.TrackAiUsage;
import com.project.platform.dto.ResourceSearchDTO;
import com.project.platform.entity.AiToolUsage;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 资源搜索控制器
 * 直接调用 KnowledgeSearchTool / BilibiliVideoTool / ImageSearchTool2
 * 保证每类资源都有 > 1 条结果
 */
@RestController
@RequestMapping("/xunfei/resource")
@Slf4j
public class ResourceSearchController {

    @Resource
    private ResourceSearchService resourceSearchService;

    @Resource
    private KnowledgeSearchTool knowledgeSearchTool;

    @Resource
    private ImageSearchTool2 imageSearchTool2;

    @Resource
    private Tools tools;

    @PostMapping(value = "/search")
    @TrackAiUsage(AiToolUsage.ToolType.RESOURCE)
    public Flux<String> resourceSearch(@RequestBody ChatForm chatForm, HttpServletResponse response) {
        String memoryId = chatForm.getMemoryId();
        Integer userId = tools.getCurrentUserIdInteger();

        if (chatForm.getMessage() == null || chatForm.getMessage().trim().isEmpty()) {
            log.error("=== 资源搜索失败：用户消息为空！userId: {}", userId);
            return Flux.error(new IllegalArgumentException("搜索关键词不能为空，请输入您想查找的学习资源主题"));
        }

        final String query = chatForm.getMessage().trim();

        if (memoryId == null || memoryId.isEmpty()) {
            memoryId = userId + "_resource_" + java.util.UUID.randomUUID().toString().substring(0, 8);
            log.info("=== 生成新资源搜索会话ID: {}", memoryId);
        } else {
            log.info("=== 使用已有资源搜索会话ID: {}", memoryId);
        }

        response.setHeader("X-Conversation-Id", memoryId);
        response.setHeader("Content-Type", "text/plain; charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");

        log.info("=== 资源搜索被调用! userId: {}, memoryId: {}, 关键词: {}", userId, memoryId, query);

        try {
            // 1. 搜索文章 (8-10 篇)
            List<KnowledgeSearchTool.SearchResult> articleResults =
                    searchArticles(query, 10);

            // 2. 搜索视频 (2-3 个)
            List<KnowledgeSearchTool.SearchResult> videoResults =
                    searchVideos(query, 3);

            // 2.1 并行抓取视频封面图
            java.util.Map<String, String> coverMap = fetchCovers(videoResults);

            // 3. 搜索图片 (2-3 张)
            List<String> imageUrls =
                    searchImages(query, 3);

            // 4. 组装响应
            ResourceSearchDTO dto = buildResponse(query, articleResults, videoResults, coverMap, imageUrls);
            String json = JSONUtil.toJsonStr(dto);

            log.info("=== 资源搜索完成: articles={}, videos={}, images={}",
                    dto.getResources().getArticles().size(),
                    dto.getResources().getVideos().size(),
                    dto.getResources().getImages().size());

            return Flux.just(json);

        } catch (Exception e) {
            log.error("=== 资源搜索发生错误！error: {}", e.getMessage(), e);
            return Flux.just(buildErrorResponse("抱歉，资源搜索暂时遇到问题，请稍后重试。"));
        }
    }

    /* ======================== 三类资源搜索 ======================== */

    /**
     * 搜索文章：请求 15 条，过滤后取前 10 条
     */
    private List<KnowledgeSearchTool.SearchResult> searchArticles(String query, int maxResults) {
        try {
            List<KnowledgeSearchTool.SearchResult> results =
                    knowledgeSearchTool.searchStructured(query, 15);
            if (results == null) return Collections.emptyList();

            return results.stream()
                    .filter(r -> r.getTitle() != null && !r.getTitle().isBlank())
                    .filter(r -> r.getUrl() != null && !r.getUrl().isBlank())
                    .sorted(Comparator.comparingDouble(
                            (KnowledgeSearchTool.SearchResult r) ->
                                    r.getScore() != null ? r.getScore() : 0.0
                    ).reversed())
                    .limit(maxResults)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("文章搜索失败", e);
            return Collections.emptyList();
        }
    }

    /** 视频搜索限定域名 */
    private static final List<String> VIDEO_DOMAINS = List.of("douyin.com", "xiaohongshu.com");

    /** 封面抓取线程池 */
    private static final ExecutorService coverExecutor = Executors.newFixedThreadPool(4);

    /**
     * 搜索视频：从抖音、小红书获取视频资源，并抓取封面图
     */
    private List<KnowledgeSearchTool.SearchResult> searchVideos(String query, int maxResults) {
        try {
            List<KnowledgeSearchTool.SearchResult> results =
                    knowledgeSearchTool.searchStructured(query + " 视频", 10, VIDEO_DOMAINS);
            if (results == null) return Collections.emptyList();

            return results.stream()
                    .filter(r -> r.getTitle() != null && !r.getTitle().isBlank())
                    .filter(r -> r.getUrl() != null && !r.getUrl().isBlank())
                    .sorted(Comparator.comparingDouble(
                            (KnowledgeSearchTool.SearchResult r) ->
                                    r.getScore() != null ? r.getScore() : 0.0
                    ).reversed())
                    .limit(maxResults)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("视频搜索失败", e);
            return Collections.emptyList();
        }
    }

    /** 并行抓取每个视频页面的 og:image，存入 coverMap */
    private java.util.Map<String, String> fetchCovers(List<KnowledgeSearchTool.SearchResult> videos) {
        java.util.Map<String, String> coverMap = new java.util.concurrent.ConcurrentHashMap<>();
        if (videos.isEmpty()) return coverMap;
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (KnowledgeSearchTool.SearchResult video : videos) {
            String url = video.getUrl();
            futures.add(CompletableFuture.runAsync(() -> {
                String cover = extractOgImage(url);
                if (cover != null) {
                    coverMap.put(url, cover);
                }
            }, coverExecutor));
        }
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(6, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("封面抓取超时或失败: {}", e.getMessage());
        }
        return coverMap;
    }

    /** 从页面提取封面：先试 Jsoup og:image，失败则用 Microlink 预览服务 */
    private String extractOgImage(String pageUrl) {
        // 1. 尝试 Jsoup 直接提取（适合 SSR 页面）
        try {
            Document doc = Jsoup.connect(pageUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(4000)
                    .followRedirects(true)
                    .get();

            String ogImage = doc.select("meta[property=og:image]").attr("content");
            if (!ogImage.isBlank()) return ogImage;

            String twitterImage = doc.select("meta[name=twitter:image]").attr("content");
            if (!twitterImage.isBlank()) return twitterImage;

            String poster = doc.select("video").attr("poster");
            if (!poster.isBlank()) return poster;
        } catch (Exception e) {
            log.debug("Jsoup 提取封面失败 {}: {}", pageUrl, e.getMessage());
        }

        // 2. 回退到 Microlink 预览服务（支持 JS 渲染页面）
        try {
            String api = "https://api.microlink.io/?url=" + java.net.URLEncoder.encode(pageUrl, "UTF-8");
            Document mlDoc = Jsoup.connect(api)
                    .timeout(6000)
                    .ignoreContentType(true)
                    .get();
            String json = mlDoc.text();
            // 简单解析 image.url
            int imgIdx = json.indexOf("\"url\":\"https://");
            if (imgIdx > 0) {
                imgIdx += 7;
                int imgEnd = json.indexOf("\"", imgIdx);
                if (imgEnd > imgIdx) {
                    String imgUrl = json.substring(imgIdx, imgEnd).replace("\\/", "/");
                    if (imgUrl.startsWith("https://") && imgUrl.length() > 20) {
                        return imgUrl;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Microlink 提取封面失败 {}: {}", pageUrl, e.getMessage());
        }

        return null;
    }

    /**
     * 搜索图片：请求 5 张，取前 maxResults 张
     */
    private List<String> searchImages(String query, int maxResults) {
        try {
            String imageQuery = query + " 示意图";
            List<String> results = imageSearchTool2.searchImage(imageQuery);
            if (results == null) return Collections.emptyList();
            if (results.size() > maxResults) {
                return results.subList(0, maxResults);
            }
            return results;
        } catch (Exception e) {
            log.error("图片搜索失败", e);
            return Collections.emptyList();
        }
    }

    /* ======================== 响应组装 ======================== */

    private ResourceSearchDTO buildResponse(
            String query,
            List<KnowledgeSearchTool.SearchResult> articles,
            List<KnowledgeSearchTool.SearchResult> videos,
            java.util.Map<String, String> coverMap,
            List<String> imageUrls) {

        List<ResourceSearchDTO.ArticleResource> articleList = articles.stream()
                .map(this::toArticle)
                .collect(Collectors.toList());

        List<ResourceSearchDTO.VideoResource> videoList = videos.stream()
                .map(sr -> toVideo(sr, coverMap))
                .collect(Collectors.toList());

        List<ResourceSearchDTO.ImageResource> imageList = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            ResourceSearchDTO.ImageResource img = new ResourceSearchDTO.ImageResource();
            img.setUrl(imageUrls.get(i));
            img.setTitle(query + " 相关示意图 " + (i + 1));
            img.setSource("百度图片");
            imageList.add(img);
        }

        ResourceSearchDTO dto = new ResourceSearchDTO();
        dto.setMessage("为你整理了关于「" + query + "」的 "
                + articleList.size() + " 篇文章、"
                + videoList.size() + " 个视频、"
                + imageList.size() + " 张图片：");

        ResourceSearchDTO.Resources resources = new ResourceSearchDTO.Resources();
        resources.setArticles(articleList);
        resources.setVideos(videoList);
        resources.setImages(imageList);
        dto.setResources(resources);

        return dto;
    }

    private ResourceSearchDTO.ArticleResource toArticle(KnowledgeSearchTool.SearchResult sr) {
        ResourceSearchDTO.ArticleResource article = new ResourceSearchDTO.ArticleResource();
        article.setTitle(sr.getTitle());
        article.setUrl(sr.getUrl());
        article.setSnippet(truncate(sr.getContent(), 120));
        article.setSource(extractSource(sr.getUrl()));
        return article;
    }

    private ResourceSearchDTO.VideoResource toVideo(KnowledgeSearchTool.SearchResult sr,
                                                     java.util.Map<String, String> coverMap) {
        ResourceSearchDTO.VideoResource video = new ResourceSearchDTO.VideoResource();
        video.setTitle(sr.getTitle());
        video.setUrl(sr.getUrl());
        video.setCover(coverMap.get(sr.getUrl()));
        video.setSource(extractSource(sr.getUrl()));
        return video;
    }

    /* ======================== 工具方法 ======================== */

    private String truncate(String text, int maxLen) {
        if (text == null || text.isBlank()) return "";
        String cleaned = text.replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= maxLen) return cleaned;
        return cleaned.substring(0, maxLen) + "...";
    }

    private String extractSource(String url) {
        try {
            String host = new URI(url).getHost();
            if (host == null) return "未知来源";
            host = host.replace("www.", "").toLowerCase();
            if (host.contains("csdn")) return "CSDN";
            if (host.contains("zhihu")) return "知乎";
            if (host.contains("juejin")) return "掘金";
            if (host.contains("cnblogs")) return "博客园";
            if (host.contains("github")) return "GitHub";
            if (host.contains("bilibili")) return "B站";
            if (host.contains("douyin")) return "抖音";
            if (host.contains("xiaohongshu")) return "红书";
            if (host.contains("jianshu")) return "简书";
            if (host.contains("segmentfault")) return "SegmentFault";
            if (host.contains("toutiao")) return "今日头条";
            if (host.contains("sohu")) return "搜狐";
            if (host.contains("163")) return "网易";
            if (host.contains("tencent")) return "腾讯云";
            if (host.contains("aliyun")) return "阿里云";
            if (host.contains("huawei")) return "华为云";
            return host;
        } catch (Exception e) {
            return "网络来源";
        }
    }

    private String buildErrorResponse(String message) {
        ResourceSearchDTO dto = new ResourceSearchDTO();
        dto.setMessage(message);
        ResourceSearchDTO.Resources resources = new ResourceSearchDTO.Resources();
        resources.setImages(Collections.emptyList());
        resources.setVideos(Collections.emptyList());
        resources.setArticles(Collections.emptyList());
        dto.setResources(resources);
        return JSONUtil.toJsonStr(dto);
    }
}
