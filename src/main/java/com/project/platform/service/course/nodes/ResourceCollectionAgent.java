package com.project.platform.service.course.nodes;

import com.project.platform.agents.tools.BilibiliVideoTool;
import com.project.platform.agents.tools.ImageSearchTool2;
import com.project.platform.agents.tools.KnowledgeSearchTool;
import com.project.platform.dto.courseworkflow.CourseWorkflowSnapshotDTO;
import com.project.platform.dto.courseworkflow.CourseWorkflowState;
import com.project.platform.mapper.CourseWorkflowResourceMapper;
import com.project.platform.service.course.CourseWorkflowTraceService;
import com.project.platform.service.course.WorkflowSseEmitterService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ResourceCollectionAgent {

    @Resource
    private KnowledgeSearchTool knowledgeSearchTool;

    @Resource
    private BilibiliVideoTool bilibiliVideoTool;

    @Resource(name = "imageSearchTool2")
    private ImageSearchTool2 imageSearchTool;

    @Resource
    private CourseWorkflowTraceService traceService;

    @Resource
    private CourseWorkflowResourceMapper resourceMapper;

    @Resource
    private WorkflowSseEmitterService sseEmitterService;

    private static final Pattern BILIBILI_URL_PATTERN =
            Pattern.compile("https?://www\\.bilibili\\.com/video/[A-Za-z0-9]+");

    public CourseWorkflowState execute(CourseWorkflowState state) {
        String taskId = state.getTaskId();
        log.info("[资源搜集] 开始: taskId={}, queries={}", taskId,
                state.getSearchQueries() != null ? state.getSearchQueries().size() : 0);

        traceService.bind(taskId);
        traceService.startStep(taskId, "resource_collection", "正在多维度搜索学习资源...");
        List<String> queries = state.getSearchQueries();
        traceService.logActivity(taskId, "resource_collection", "任务调度师", "任务派遣",
                "dispatch", "多源调度",
                "派遣4位搜索专家: 图片猎手(百度) + 视频猎手(B站) + 网页检索师(Tavily) + 社区探勘师(知乎/GitHub)，共 " +
                (queries != null ? queries.size() : 1) + " 个搜索维度。");
        pushSnapshot(taskId);

        try {
            List<CourseWorkflowSnapshotDTO.Evidence> allResources =
                    searchInParallel(state, taskId);
            state.setResources(allResources);
            state.getStageStatus().put("resource_collection", "completed");

            traceService.completeStep(taskId, "resource_collection",
                    "已搜集 " + allResources.size() + " 条学习资源");
            traceService.logActivity(taskId, "resource_collection", "任务调度师",
                    "搜索完成", "result", "资源汇总",
                    "4位搜索专家搜索完成。共获取 " + allResources.size() + " 条候选资源。");

            log.info("[资源搜集] 完成: taskId={}, resources={}", taskId, allResources.size());
        } catch (Exception e) {
            log.error("[资源搜集] 失败: taskId={}", taskId, e);
            state.getStageStatus().put("resource_collection", "failed");
            throw new RuntimeException("资源搜集失败", e);
        }

        return state;
    }

    private List<CourseWorkflowSnapshotDTO.Evidence> searchInParallel(
            CourseWorkflowState state, String taskId) {
        List<String> queryList = state.getSearchQueries();
        if (queryList == null || queryList.isEmpty()) {
            queryList = List.of(state.getCourseName() + " 学习资源");
        }
        final List<String> queries = queryList;

        final String primaryQuery = queries.get(0);
        List<CourseWorkflowSnapshotDTO.Evidence> allResources = new ArrayList<>();

        // 并行执行四类搜索：网页 + 视频 + 图片 + 社区
        ExecutorService executor = Executors.newFixedThreadPool(4);

        CompletableFuture<List<CourseWorkflowSnapshotDTO.Evidence>> webFuture =
                CompletableFuture.supplyAsync(
                        () -> searchWeb(taskId, queries), executor);

        CompletableFuture<List<CourseWorkflowSnapshotDTO.Evidence>> videoFuture =
                CompletableFuture.supplyAsync(
                        () -> searchVideos(taskId, primaryQuery), executor);

        CompletableFuture<List<CourseWorkflowSnapshotDTO.Evidence>> imageFuture =
                CompletableFuture.supplyAsync(
                        () -> searchImages(taskId, primaryQuery), executor);

        CompletableFuture<List<CourseWorkflowSnapshotDTO.Evidence>> communityFuture =
                CompletableFuture.supplyAsync(
                        () -> searchCommunity(taskId, primaryQuery), executor);

        try {
            CompletableFuture.allOf(webFuture, videoFuture, imageFuture, communityFuture)
                    .get(90, TimeUnit.SECONDS);

            allResources.addAll(webFuture.get());
            allResources.addAll(videoFuture.get());
            allResources.addAll(imageFuture.get());
            allResources.addAll(communityFuture.get());
        } catch (Exception e) {
            log.warn("部分搜索维度超时或失败: {}", e.getMessage());
            // 尝试获取已完成的结果
            try { allResources.addAll(webFuture.get()); } catch (Exception ignored) {}
            try { allResources.addAll(videoFuture.get()); } catch (Exception ignored) {}
            try { allResources.addAll(imageFuture.get()); } catch (Exception ignored) {}
            try { allResources.addAll(communityFuture.get()); } catch (Exception ignored) {}
        } finally {
            executor.shutdownNow();
        }

        return deduplicate(allResources);
    }

    // ==================== 网页搜索 ====================

    private List<CourseWorkflowSnapshotDTO.Evidence> searchWeb(
            String taskId, List<String> queries) {
        List<CourseWorkflowSnapshotDTO.Evidence> results = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (String query : queries) {
            try {
                log.info("[网页搜索] query={}", query);
                traceService.logActivity(taskId, "resource_collection",
                        "网页检索师", "工具调用",
                        "search", "Tavily 搜索",
                        "调用 Tavily API 搜索: \"" + query + "\"");
                pushSnapshotToClient(taskId);

                List<KnowledgeSearchTool.SearchResult> searchResults =
                        knowledgeSearchTool.searchStructured(query, 5);

                traceService.logActivity(taskId, "resource_collection",
                        "网页检索师", "搜索结果",
                        "search", "搜索完成",
                        "Tavily 搜索 \"" + query + "\" 返回 " + searchResults.size() + " 条结果");

                for (KnowledgeSearchTool.SearchResult sr : searchResults) {
                    CourseWorkflowSnapshotDTO.Evidence ev = new CourseWorkflowSnapshotDTO.Evidence();
                    ev.setId(UUID.randomUUID().toString());
                    ev.setKind("web");
                    ev.setTitle(sr.getTitle());
                    ev.setUrl(sr.getUrl());
                    ev.setSource("Tavily 网页搜索");
                    ev.setSnippet(truncate(sr.getContent(), 200));
                    ev.setScore(sr.getScore());
                    ev.setCredibilityLabel("pending");
                    results.add(ev);

                    traceService.addEvidence(taskId, "web", ev.getTitle(), ev.getUrl(),
                            ev.getSource(), ev.getSnippet(), ev.getScore(), "pending");
                    sseEmitterService.sendEvidence(taskId, ev);
                    persistResource(taskId, ev, now);
                    progressiveDelay();
                }
            } catch (Exception e) {
                log.warn("网页搜索失败: query={}, error={}", query, e.getMessage());
            }
            if (results.size() >= 15) break;
        }
        return results;
    }

    // ==================== 视频搜索 ====================

    private List<CourseWorkflowSnapshotDTO.Evidence> searchVideos(
            String taskId, String query) {
        List<CourseWorkflowSnapshotDTO.Evidence> results = new ArrayList<>();
        long now = System.currentTimeMillis();

        try {
            String keyword = query.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z\\s]", "") + " 教程";
            log.info("[视频搜索] keyword={}", keyword);
            traceService.logActivity(taskId, "resource_collection",
                    "视频猎手", "工具调用",
                    "search", "B站搜索",
                    "调用 Bilibili API 搜索视频: \"" + keyword + "\"");
            pushSnapshot(taskId);

            String searchResult = bilibiliVideoTool.searchVideos(keyword);
            Matcher matcher = BILIBILI_URL_PATTERN.matcher(searchResult);
            int count = 0;
            while (matcher.find() && count < 3) {
                String videoUrl = matcher.group();
                CourseWorkflowSnapshotDTO.Evidence ev = new CourseWorkflowSnapshotDTO.Evidence();
                ev.setId(UUID.randomUUID().toString());
                ev.setKind("video");
                ev.setTitle("B站教学视频 - " + query);
                ev.setUrl(videoUrl);
                ev.setSource("B站（Bilibili）");
                ev.setSnippet("搜索结果匹配: " + keyword);
                ev.setScore(0.7);
                ev.setCredibilityLabel("pending");
                results.add(ev);

                traceService.addEvidence(taskId, "video", ev.getTitle(), ev.getUrl(),
                        ev.getSource(), ev.getSnippet(), ev.getScore(), "pending");
                sseEmitterService.sendEvidence(taskId, ev);
                persistResource(taskId, ev, now);
                progressiveDelay();
                count++;
            }
            log.info("[视频搜索] 找到 {} 个视频", count);
        } catch (Exception e) {
            log.warn("视频搜索失败: query={}, error={}", query, e.getMessage());
        }
        return results;
    }

    // ==================== 图片搜索 ====================

    private List<CourseWorkflowSnapshotDTO.Evidence> searchImages(
            String taskId, String query) {
        List<CourseWorkflowSnapshotDTO.Evidence> results = new ArrayList<>();
        long now = System.currentTimeMillis();

        try {
            String keyword = query.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z\\s]", "");
            log.info("[图片搜索] keyword={}", keyword);
            traceService.logActivity(taskId, "resource_collection",
                    "图片猎手", "工具调用",
                    "search", "百度搜图",
                    "调用百度图片 API 搜索: \"" + keyword + "\"");
            pushSnapshot(taskId);

            List<String> imageUrls = imageSearchTool.searchImage(keyword);

            traceService.logActivity(taskId, "resource_collection",
                    "图片猎手", "搜索结果",
                    "search", "图片结果",
                    "百度图片搜索 \"" + keyword + "\" 找到 " + imageUrls.size() + " 张图片");
            for (int i = 0; i < Math.min(imageUrls.size(), 5); i++) {
                String imgUrl = imageUrls.get(i);
                CourseWorkflowSnapshotDTO.Evidence ev = new CourseWorkflowSnapshotDTO.Evidence();
                ev.setId(UUID.randomUUID().toString());
                ev.setKind("image");
                ev.setTitle(query + " - 示意图" + (i + 1));
                ev.setUrl(imgUrl);
                ev.setSource("百度图片");
                ev.setSnippet("关键词: " + keyword);
                ev.setScore(0.6);
                ev.setCredibilityLabel("pending");
                results.add(ev);

                traceService.addEvidence(taskId, "image", ev.getTitle(), ev.getUrl(),
                        ev.getSource(), ev.getSnippet(), ev.getScore(), "pending");
                sseEmitterService.sendEvidence(taskId, ev);
                persistResource(taskId, ev, now);
            }
            log.info("[图片搜索] 找到 {} 张图片", results.size());
        } catch (Exception e) {
            log.warn("图片搜索失败: query={}, error={}", query, e.getMessage());
        }
        return results;
    }

    // ==================== 社区搜索（社区探勘师） ====================

    private List<CourseWorkflowSnapshotDTO.Evidence> searchCommunity(
            String taskId, String query) {
        List<CourseWorkflowSnapshotDTO.Evidence> results = new ArrayList<>();
        long now = System.currentTimeMillis();

        try {
            String keyword = query.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z\\s]", "");
            traceService.logActivity(taskId, "resource_collection",
                    "社区探勘师", "工具调用",
                    "search", "社区挖掘",
                    "搜索社区经验帖: \"" + keyword + " 经验 实战 踩坑\"");
            pushSnapshot(taskId);

            // 用 Tavily 搜索社区内容 (zhihu, csdn, github 等)
            String communityQuery = keyword + " site:zhihu.com OR site:csdn.net OR site:github.com 经验";
            List<KnowledgeSearchTool.SearchResult> searchResults =
                    knowledgeSearchTool.searchStructured(communityQuery, 3);

            traceService.logActivity(taskId, "resource_collection",
                    "社区探勘师", "搜索结果",
                    "search", "社区发现",
                    "社区探勘完成: \"" + keyword + "\" 找到 " + searchResults.size() + " 条经验帖");

            for (KnowledgeSearchTool.SearchResult sr : searchResults) {
                CourseWorkflowSnapshotDTO.Evidence ev = new CourseWorkflowSnapshotDTO.Evidence();
                ev.setId(UUID.randomUUID().toString());
                ev.setKind("web");
                ev.setTitle("[社区经验] " + sr.getTitle());
                ev.setUrl(sr.getUrl());
                ev.setSource("社区探勘 · 知乎/CSDN/GitHub");
                ev.setSnippet(truncate(sr.getContent(), 200));
                ev.setScore(sr.getScore());
                ev.setCredibilityLabel("pending");
                results.add(ev);

                traceService.addEvidence(taskId, "web", ev.getTitle(), ev.getUrl(),
                        ev.getSource(), ev.getSnippet(), ev.getScore(), "pending");
                sseEmitterService.sendEvidence(taskId, ev);
                persistResource(taskId, ev, now);
                progressiveDelay();
            }
        } catch (Exception e) {
            log.warn("社区搜索失败: query={}, error={}", query, e.getMessage());
        }
        return results;
    }

    // ==================== 工具方法 ====================

    private void persistResource(String taskId, CourseWorkflowSnapshotDTO.Evidence ev,
                                  long now) {
        try {
            com.project.platform.entity.CourseWorkflowResource resource =
                    new com.project.platform.entity.CourseWorkflowResource();
            resource.setTaskId(taskId);
            resource.setStageId("resource_collection");
            resource.setKind(ev.getKind());
            resource.setTitle(ev.getTitle());
            resource.setUrl(ev.getUrl());
            resource.setSource(ev.getSource());
            resource.setSnippet(ev.getSnippet());
            resource.setScore(ev.getScore());
            resource.setCredibilityLabel(ev.getCredibilityLabel());
            resource.setCreatedAt(now);
            resourceMapper.insert(resource);
        } catch (Exception e) {
            log.debug("持久化资源失败(非关键): {}", e.getMessage());
        }
    }

    private List<CourseWorkflowSnapshotDTO.Evidence> deduplicate(
            List<CourseWorkflowSnapshotDTO.Evidence> list) {
        List<CourseWorkflowSnapshotDTO.Evidence> deduped = new ArrayList<>();
        for (CourseWorkflowSnapshotDTO.Evidence ev : list) {
            boolean dup = deduped.stream().anyMatch(
                    d -> d.getUrl() != null && d.getUrl().equals(ev.getUrl()));
            if (!dup) {
                deduped.add(ev);
            }
        }
        return deduped;
    }

    private String truncate(String text, int max) {
        if (text == null || text.length() <= max) return text != null ? text : "";
        return text.substring(0, max) + "...";
    }

    private void pushSnapshot(String taskId) {
        var s = traceService.getSnapshot(taskId);
        if (s != null) sseEmitterService.sendSnapshot(taskId, s);
    }

    private void pushSnapshotToClient(String taskId) {
        pushSnapshot(taskId);
    }

    private void progressiveDelay() {
        try { Thread.sleep(350); } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
