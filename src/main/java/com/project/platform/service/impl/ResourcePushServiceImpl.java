package com.project.platform.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.platform.agents.tools.ImageSearchTool2;
import com.project.platform.agents.tools.KnowledgeSearchTool;
import com.project.platform.agents.tools.KnowledgeSearchTool.SearchResult;
import com.project.platform.entity.Chapter;
import com.project.platform.entity.Course;
import com.project.platform.entity.ResourceContextMetadata;
import com.project.platform.entity.ResourcePush;
import com.project.platform.entity.Section;
import com.project.platform.mapper.ChapterMapper;
import com.project.platform.mapper.CourseMapper;
import com.project.platform.mapper.ResourcePushMapper;
import com.project.platform.mapper.SectionMapper;
import com.project.platform.service.LearningWorkspaceService;
import com.project.platform.service.ResourcePushService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ResourcePushServiceImpl implements ResourcePushService {

    @Resource
    private ResourcePushMapper resourcePushMapper;

    @Resource
    private KnowledgeSearchTool knowledgeSearchTool;

    @Resource
    private ImageSearchTool2 imageSearchTool2;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private ChapterMapper chapterMapper;

    @Resource
    private SectionMapper sectionMapper;

    @Resource
    private LearningWorkspaceService learningWorkspaceService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

    // B站 guest cookie (无需登录，降低反爬概率)
    private static final String BILIBILI_COOKIE =
            "buvid3=guest; _uuid=guest; buvid4=guest";

    @Override
    public List<ResourcePush> searchAndPush(Integer courseId, Integer chapterId,
                                             Integer sectionId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            log.warn("搜索关键词为空，无法执行资源推送");
            return Collections.emptyList();
        }

        List<ResourcePush> existingResources = resourcePushMapper.selectByCourseId(courseId);
        if (existingResources != null && !existingResources.isEmpty()) {
            log.info("=== 课程已有推送资源，跳过联网搜索: courseId={}, existing={}", courseId, existingResources.size());
            return existingResources;
        }

        long now = System.currentTimeMillis();
        List<ResourcePush> newResources = new ArrayList<>();

        log.info("=== 开始资源推送: courseId={}, keyword={}", courseId, keyword);

        // 1. 搜索 B 站视频
        List<ResourcePush> bilibiliVideos = searchBilibiliVideos(courseId, chapterId, sectionId, keyword, now);
        log.info("B站视频: 搜索到 {} 个", bilibiliVideos.size());
        newResources.addAll(saveNewResources(bilibiliVideos, courseId));

        // 2. 搜索抖音视频
        List<ResourcePush> douyinVideos = searchDouyinVideos(courseId, chapterId, sectionId, keyword, now);
        log.info("抖音视频: 搜索到 {} 个", douyinVideos.size());
        newResources.addAll(saveNewResources(douyinVideos, courseId));

        // 3. 搜索文章 / 教材 / 文献
        List<ResourcePush> articles = searchArticles(courseId, chapterId, sectionId, keyword, now);
        log.info("文章/教材/文献: 搜索到 {} 个", articles.size());
        newResources.addAll(saveNewResources(articles, courseId));

        log.info("=== 资源推送完成: courseId={}, 合计新增 {} 个资源", courseId, newResources.size());
        return newResources;
    }

    @Override
    public List<ResourcePush> listByCourseId(Integer courseId) {
        return resourcePushMapper.selectByCourseId(courseId);
    }

    @Override
    public List<ResourcePush> listByCourseIdAndType(Integer courseId, String resourceType) {
        return resourcePushMapper.selectByCourseIdAndType(courseId, resourceType);
    }

    @Override
    public List<ResourcePush> listContextualResources(Integer studentId, Integer courseId, Integer chapterId,
                                                      Integer sectionId, String activeKnowledgePoint) {
        List<ResourcePush> resources = Optional.ofNullable(resourcePushMapper.selectByCourseId(courseId))
                .orElse(List.of());
        List<String> weakKnowledgePoints = learningWorkspaceService == null || sectionId == null
                ? List.of()
                : learningWorkspaceService.listWeakKnowledgePoints(studentId, sectionId);

        Map<String, ScoredResource> deduped = new LinkedHashMap<>();
        int order = 0;
        for (ResourcePush resource : resources) {
            if (resource.getUrl() == null || resource.getUrl().isBlank()) {
                continue;
            }
            ResourceContextMetadata metadata = buildMetadata(studentId, resource, chapterId, sectionId,
                    activeKnowledgePoint, weakKnowledgePoints);
            resource.setContextMetadata(metadata);
            ScoredResource scored = new ScoredResource(resource, metadata.getRelevanceScore(), order++);
            deduped.merge(resource.getUrl(), scored,
                    (left, right) -> left.score() >= right.score() ? left : right);
        }
        return deduped.values().stream()
                .sorted(Comparator.comparingDouble(ScoredResource::score).reversed()
                        .thenComparing(ScoredResource::originalOrder))
                .map(ScoredResource::resource)
                .collect(Collectors.toList());
    }

    @Override
    public String buildContextualSearchKeyword(Integer courseId, Integer chapterId, Integer sectionId,
                                               String activeKnowledgePoint) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        Course course = courseId == null || courseMapper == null ? null : courseMapper.selectById(courseId);
        Chapter chapter = chapterId == null || chapterMapper == null ? null : chapterMapper.selectById(chapterId);
        Section section = sectionId == null || sectionMapper == null ? null : sectionMapper.selectById(sectionId);
        addText(tokens, course == null ? null : course.getCourseName());
        addText(tokens, chapter == null ? null : chapter.getChapterName());
        addText(tokens, section == null ? null : section.getSectionName());
        addText(tokens, activeKnowledgePoint);
        return String.join(" ", tokens);
    }

    @Override
    public void removeByIds(List<Integer> ids) {
        resourcePushMapper.removeByIds(ids);
    }

    // ==================== B 站视频搜索 ====================

    private List<ResourcePush> searchBilibiliVideos(Integer courseId, Integer chapterId,
                                                     Integer sectionId, String keyword, long now) {
        List<ResourcePush> results = new ArrayList<>();
        try {
            String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String apiUrl = "https://api.bilibili.com/x/web-interface/search/all/v2"
                    + "?keyword=" + encoded
                    + "&search_type=video"
                    + "&page=1"
                    + "&page_size=8";

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                    .header("Referer", "https://www.bilibili.com/")
                    .header("Cookie", BILIBILI_COOKIE)
                    .header("Accept", "application/json")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("B站API返回非200: code={}", response.code());
                    return results;
                }
                String body = response.body() != null ? response.body().string() : "";
                if (body.isBlank()) return results;

                log.debug("B站API响应前200字符: {}", body.substring(0, Math.min(200, body.length())));

                JsonNode root = objectMapper.readTree(body);
                int code = root.path("code").asInt(-1);
                if (code != 0) {
                    log.warn("B站API业务错误: code={}, message={}", code, root.path("message").asText(""));
                    return results;
                }

                // 新版 v2 API: data.result 是数组，每项有 result_type + data
                JsonNode resultArr = root.path("data").path("result");
                if (!resultArr.isArray()) {
                    // 兼容旧版: data 直接就是视频列表
                    JsonNode directData = root.path("data");
                    if (directData.isArray()) {
                        return parseBilibiliDataList(directData, courseId, chapterId, sectionId, now);
                    }
                    log.warn("B站API返回格式异常: result 不是数组");
                    return results;
                }

                for (JsonNode typeBlock : resultArr) {
                    String type = typeBlock.path("result_type").asText("");
                    if (!"video".equals(type)) continue;

                    JsonNode dataList = typeBlock.path("data");
                    if (!dataList.isArray()) continue;

                    results = parseBilibiliDataList(dataList, courseId, chapterId, sectionId, now);
                }
            }
        } catch (Exception e) {
            log.error("B站视频搜索异常: keyword={}, error={}", keyword, e.getMessage());
        }
        return results;
    }

    private List<ResourcePush> parseBilibiliDataList(JsonNode dataList, Integer courseId,
                                                      Integer chapterId, Integer sectionId, long now) {
        List<ResourcePush> results = new ArrayList<>();
        int count = 0;
        for (JsonNode item : dataList) {
            if (count >= 6) break;

            String bvid = item.path("bvid").asText("");
            String title = cleanHtml(item.path("title").asText(""));
            String duration = item.path("duration").asText("");
            String author = item.path("author").asText("");
            String desc = item.path("description").asText("");

            if (title.isBlank() || bvid.isBlank()) continue;

            // 百度图片搜索获取封面（平台 CDN 封面易被防盗链拦截）
            String coverUrl = searchCoverImage(title, "视频封面");

            ResourcePush rp = new ResourcePush();
            rp.setCourseId(courseId);
            rp.setChapterId(chapterId);
            rp.setSectionId(sectionId);
            rp.setResourceType("VIDEO");
            rp.setTitle(title);
            rp.setDescription(truncate(desc, 200));
            rp.setUrl("https://www.bilibili.com/video/" + bvid);
            rp.setEmbedUrl("//player.bilibili.com/player.html?bvid=" + bvid + "&page=1&high_quality=1");
            rp.setCoverUrl(coverUrl);
            rp.setSource("B站");
            rp.setDuration(duration);
            rp.setAuthor(author);
            rp.setPushTime(now);
            rp.setIsNew(1);
            rp.setSortOrder(count);
            rp.setCreatedAt(now);
            rp.setUpdatedAt(now);

            results.add(rp);
            count++;
        }
        return results;
    }

    // ==================== 抖音视频搜索 ====================

    private List<ResourcePush> searchDouyinVideos(Integer courseId, Integer chapterId,
                                                   Integer sectionId, String keyword, long now) {
        List<ResourcePush> results = new ArrayList<>();
        try {
            // 抖音没有公开 API，使用聚合 web 搜索获取抖音视频链接
            String query = keyword + " 教程 site:douyin.com";
            List<SearchResult> searchResults = knowledgeSearchTool.searchStructured(query, 4);

            int count = 0;
            for (SearchResult sr : searchResults) {
                if (count >= 4) break;
                String url = sr.getUrl();
                if (url == null || !url.contains("douyin.com")) continue;

                String title = sr.getTitle();
                if (title == null || title.isBlank()) continue;

                // 清理抖音标题中的多余信息
                title = title.replace("抖音", "").replace(" - 抖音", "").trim();
                if (title.length() > 80) title = title.substring(0, 80);

                String coverUrl = searchCoverImage(title + " 封面", "视频封面");

                ResourcePush rp = new ResourcePush();
                rp.setCourseId(courseId);
                rp.setChapterId(chapterId);
                rp.setSectionId(sectionId);
                rp.setResourceType("VIDEO");
                rp.setTitle(title);
                rp.setDescription(truncate(sr.getContent(), 200));
                rp.setUrl(url);
                rp.setEmbedUrl(url); // 抖音视频直接打开页面
                rp.setCoverUrl(coverUrl);
                rp.setSource("抖音");
                rp.setPushTime(now);
                rp.setIsNew(1);
                rp.setSortOrder(count);
                rp.setCreatedAt(now);
                rp.setUpdatedAt(now);

                results.add(rp);
                count++;
            }
            log.info("抖音视频搜索完成: keyword={}, 找到 {} 个", keyword, results.size());
        } catch (Exception e) {
            log.error("抖音视频搜索异常: keyword={}, error={}", keyword, e.getMessage());
        }
        return results;
    }

    // ==================== 文章/教材/文献搜索 ====================

    private List<ResourcePush> searchArticles(Integer courseId, Integer chapterId,
                                               Integer sectionId, String keyword, long now) {
        List<ResourcePush> results = new ArrayList<>();
        try {
            // 搜索文章 (Tavily)
            List<SearchResult> articleResults = knowledgeSearchTool.searchStructured(keyword, 5);

            // 搜索教材
            List<SearchResult> bookResults =
                    knowledgeSearchTool.searchStructured(keyword + " 教材 推荐书", 3);

            // 搜索学术文献
            List<SearchResult> litResults =
                    knowledgeSearchTool.searchStructured(keyword + " 学术论文 研究", 3);

            int sort = 0;

            // 教材
            for (SearchResult sr : bookResults) {
                String url = sr.getUrl();
                if (url == null || isVideoPlatform(url)) continue;
                ResourcePush rp = buildArticleResource(courseId, chapterId, sectionId,
                        sr, "TEXTBOOK", now, sort++);
                if (rp != null) {
                    rp.setCoverUrl(searchCoverImage(rp.getTitle(), "教材 封面"));
                    results.add(rp);
                }
            }

            // 文献
            for (SearchResult sr : litResults) {
                String url = sr.getUrl();
                if (url == null || isVideoPlatform(url)) continue;
                ResourcePush rp = buildArticleResource(courseId, chapterId, sectionId,
                        sr, "LITERATURE", now, sort++);
                if (rp != null) {
                    rp.setCoverUrl(searchCoverImage(rp.getTitle(), "学术 论文"));
                    results.add(rp);
                }
            }

            // 普通文章 (去重)
            for (SearchResult sr : articleResults) {
                String url = sr.getUrl();
                if (url == null || isVideoPlatform(url)) continue;
                boolean dup = results.stream().anyMatch(r -> url.equals(r.getUrl()));
                if (dup) continue;

                ResourcePush rp = buildArticleResource(courseId, chapterId, sectionId,
                        sr, "TEXTBOOK", now, sort++);
                if (rp != null) {
                    rp.setCoverUrl(searchCoverImage(rp.getTitle(), "学习 资料"));
                    results.add(rp);
                }
            }

            log.info("文章搜索完成: keyword={}, 找到 {} 个资源", keyword, results.size());
        } catch (Exception e) {
            log.error("文章搜索失败: keyword={}, error={}", keyword, e.getMessage());
        }
        return results;
    }

    private ResourcePush buildArticleResource(Integer courseId, Integer chapterId,
                                               Integer sectionId, SearchResult sr,
                                               String type, long now, int sort) {
        String title = sr.getTitle();
        String url = sr.getUrl();
        if (title == null || title.isBlank()) return null;
        if (url == null || url.isBlank()) return null;

        ResourcePush rp = new ResourcePush();
        rp.setCourseId(courseId);
        rp.setChapterId(chapterId);
        rp.setSectionId(sectionId);
        rp.setResourceType(type);
        rp.setTitle(title);
        rp.setDescription(truncate(sr.getContent(), 250));
        rp.setUrl(url);
        rp.setSource(guessSource(url));
        rp.setPushTime(now);
        rp.setIsNew(1);
        rp.setSortOrder(sort);
        rp.setCreatedAt(now);
        rp.setUpdatedAt(now);
        return rp;
    }

    // ==================== 封面图片搜索 (保证每个资源都有封面) ====================

    /**
     * 多轮尝试搜索封面图片，保证每个资源都有封面
     * 策略: 依次尝试不同查询词 → 都失败则用备用占位图
     */
    private String searchCoverImage(String title, String suffix) {
        String shortTitle = title.length() > 35 ? title.substring(0, 35) : title;

        // 轮次1: 标题 + 后缀
        String cover = trySearchImage(shortTitle + " " + suffix);
        if (cover != null) return cover;

        // 轮次2: 只用标题
        cover = trySearchImage(shortTitle);
        if (cover != null) return cover;

        // 轮次3: 标题前15字 + 后缀
        if (shortTitle.length() > 15) {
            cover = trySearchImage(shortTitle.substring(0, 15) + " " + suffix);
            if (cover != null) return cover;
        }

        // 兜底: 根据资源类型返回占位图 (避免前端显示空白)
        log.info("封面图片未搜到，使用兜底占位图: title={}", title);
        return getFallbackCover(suffix);
    }

    private String trySearchImage(String query) {
        try {
            List<String> images = imageSearchTool2.searchImage(query);
            if (images != null && !images.isEmpty()) {
                return images.get(0);
            }
        } catch (Exception e) {
            // 单次失败继续尝试
        }
        return null;
    }

    /** 兜底封面: 按资源类型返回不同颜色的 SVG 内嵌图 */
    private String getFallbackCover(String suffix) {
        String base64;
        if (suffix.contains("教材") || suffix.contains("书")) {
            base64 = encodeSvg("教材", "f59e0b", "d97706");
        } else if (suffix.contains("论文") || suffix.contains("学术")) {
            base64 = encodeSvg("文献", "8b5cf6", "6d28d9");
        } else {
            base64 = encodeSvg("学习", "6366f1", "4f46e5");
        }
        return "data:image/svg+xml;base64," + base64;
    }

    private String encodeSvg(String label, String color1, String color2) {
        StringBuilder sb = new StringBuilder();
        sb.append("<svg xmlns='http://www.w3.org/2000/svg' width='400' height='250'>");
        sb.append("<defs><linearGradient id='g' x1='0%' y1='0%' x2='100%' y2='100%'>");
        sb.append("<stop offset='0%' style='stop-color:#").append(color1).append("'/>");
        sb.append("<stop offset='100%' style='stop-color:#").append(color2).append("'/>");
        sb.append("</linearGradient></defs>");
        sb.append("<rect width='400' height='250' fill='url(#g)' rx='12'/>");
        sb.append("<text x='200' y='135' text-anchor='middle' fill='rgba(255,255,255,0.85)' ");
        sb.append("font-size='36' font-family='sans-serif' font-weight='600'>").append(label).append("</text>");
        sb.append("</svg>");
        return java.util.Base64.getEncoder().encodeToString(
                sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    // ==================== 存储逻辑 (追加模式，不覆盖) ====================

    /**
     * 只插入新资源 (根据 courseId + url 去重)，不覆盖已有资源
     */
    private List<ResourcePush> saveNewResources(List<ResourcePush> resources, Integer courseId) {
        List<ResourcePush> saved = new ArrayList<>();
        for (ResourcePush rp : resources) {
            String url = rp.getUrl();
            if (url == null || url.isBlank()) continue;

            int exists = resourcePushMapper.countByCourseIdAndUrl(courseId, url);
            if (exists > 0) {
                log.info("资源已存在，跳过去重: courseId={}, url={}", courseId, url);
                continue;
            }
            resourcePushMapper.insert(rp);
            saved.add(rp);
            log.info("新增资源: type={}, title={}", rp.getResourceType(), rp.getTitle());
        }
        return saved;
    }

    private void addText(LinkedHashSet<String> parts, String value) {
        if (hasText(value)) {
            parts.add(value.trim());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private ResourceContextMetadata buildMetadata(Integer studentId, ResourcePush resource,
                                                  Integer chapterId, Integer sectionId,
                                                  String activeKnowledgePoint,
                                                  List<String> weakKnowledgePoints) {
        ResourceContextMetadata metadata = new ResourceContextMetadata();
        metadata.setResourceId(resource.getId());
        metadata.setCourseId(resource.getCourseId());
        metadata.setChapterId(resource.getChapterId());
        metadata.setSectionId(resource.getSectionId());
        metadata.setLearningStatus(learningWorkspaceService == null
                ? "NOT_STARTED"
                : learningWorkspaceService.resolveResourceLearningStatus(studentId, resource));

        String matchedWeakPoint = firstMatchingText(resource, weakKnowledgePoints);
        String matchedActivePoint = firstMatchingText(resource,
                activeKnowledgePoint == null ? List.of() : List.of(activeKnowledgePoint));
        boolean remedial = hasText(matchedWeakPoint);
        metadata.setKnowledgePoint(remedial ? matchedWeakPoint : matchedActivePoint);
        metadata.setRemedial(remedial);
        metadata.setPurpose(remedial ? "REMEDIAL" : "CONTEXT_REVIEW");
        metadata.setDifficulty(resolveDifficulty(resource));
        metadata.setCredibilityStatus(resolveCredibility(resource));
        metadata.setRecommendationReason(remedial
                ? "Prioritized for remedial review of weak knowledge point: " + matchedWeakPoint
                : recommendationReason(resource, sectionId, activeKnowledgePoint));
        metadata.setRelevanceScore(calculateResourceScore(resource, chapterId, sectionId,
                activeKnowledgePoint, weakKnowledgePoints));
        return metadata;
    }

    private String recommendationReason(ResourcePush resource, Integer sectionId, String activeKnowledgePoint) {
        if (hasText(activeKnowledgePoint) && containsText(resource, activeKnowledgePoint)) {
            return "Related to active knowledge point: " + activeKnowledgePoint;
        }
        if (sectionId != null && sectionId.equals(resource.getSectionId())) {
            return "Related to current section.";
        }
        return "Related to current learning context.";
    }

    private double calculateResourceScore(ResourcePush resource, Integer chapterId, Integer sectionId,
                                          String activeKnowledgePoint, List<String> weakKnowledgePoints) {
        double score = 0;
        if (containsAnyText(resource, weakKnowledgePoints)) {
            score += 100;
        }
        if (hasText(activeKnowledgePoint) && containsText(resource, activeKnowledgePoint)) {
            score += 40;
        }
        if (sectionId != null && sectionId.equals(resource.getSectionId())) {
            score += 20;
        }
        if (chapterId != null && chapterId.equals(resource.getChapterId())) {
            score += 10;
        }
        return score;
    }

    private String firstMatchingText(ResourcePush resource, List<String> values) {
        if (values == null) {
            return null;
        }
        return values.stream()
                .filter(this::hasText)
                .filter(value -> containsText(resource, value))
                .findFirst()
                .orElse(null);
    }

    private boolean containsAnyText(ResourcePush resource, List<String> values) {
        return values != null && values.stream().anyMatch(value -> containsText(resource, value));
    }

    private boolean containsText(ResourcePush resource, String value) {
        return hasText(value) && resourceText(resource).contains(value.toLowerCase(Locale.ROOT));
    }

    private String resourceText(ResourcePush resource) {
        return String.join(" ",
                safe(resource.getTitle()),
                safe(resource.getDescription()),
                safe(resource.getSource()))
                .toLowerCase(Locale.ROOT);
    }

    private String resolveDifficulty(ResourcePush resource) {
        String text = resourceText(resource);
        if (text.contains("advanced") || text.contains("challenge") || text.contains("deep")) {
            return "ADVANCED";
        }
        if (text.contains("basic") || text.contains("intro") || text.contains("beginner")) {
            return "ENTRY";
        }
        return "UNKNOWN";
    }

    private String resolveCredibility(ResourcePush resource) {
        String text = (safe(resource.getSource()) + " " + safe(resource.getUrl())).toLowerCase(Locale.ROOT);
        if (text.contains("official") || text.contains("github.com")
                || text.contains("arxiv.org") || text.contains("ieee.org")
                || text.contains("acm.org")) {
            return "VERIFIED";
        }
        return "UNKNOWN";
    }

    private record ScoredResource(ResourcePush resource, double score, int originalOrder) {
    }

    // ==================== 工具方法 ====================

    private boolean isVideoPlatform(String url) {
        return url.contains("bilibili.com") || url.contains("douyin.com")
                || url.contains("youtube.com") || url.contains("ixigua.com");
    }

    private String cleanHtml(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]+>", "")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&#39;", "'")
                .trim();
    }

    private String truncate(String text, int maxLen) {
        if (text == null || text.isBlank()) return null;
        String cleaned = cleanHtml(text);
        return cleaned.length() > maxLen ? cleaned.substring(0, maxLen) + "..." : cleaned;
    }

    private String guessSource(String url) {
        if (url == null) return "未知";
        String lower = url.toLowerCase();
        if (lower.contains("zhihu.com")) return "知乎";
        if (lower.contains("csdn.net")) return "CSDN";
        if (lower.contains("juejin.cn")) return "掘金";
        if (lower.contains("cnblogs.com")) return "博客园";
        if (lower.contains("github.com")) return "GitHub";
        if (lower.contains("bilibili.com")) return "B站";
        if (lower.contains("douyin.com")) return "抖音";
        if (lower.contains("cnki.net") || lower.contains("wanfangdata")) return "知网";
        if (lower.contains("arxiv.org")) return "arXiv";
        if (lower.contains("springer.com") || lower.contains("ieee.org") || lower.contains("acm.org")) return "学术期刊";
        return "网页";
    }
}
