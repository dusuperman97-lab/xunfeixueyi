package com.project.platform.service;

import com.project.platform.agents.tools.BilibiliVideoTool;
import com.project.platform.dto.LearningPathDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 学习路线视频填充服务
 * 为学习路线中的课程自动搜索并填充B站视频地址
 */
@Slf4j
@Service
public class LearningPathVideoService {

    @Resource
    private BilibiliVideoTool bilibiliVideoTool;

    // 创建线程池用于并行搜索视频（最多5个并发）
    private static final ExecutorService videoSearchExecutor = Executors.newFixedThreadPool(5);

    // 用于从B站搜索结果中提取视频URL的正则表达式
    private static final Pattern VIDEO_URL_PATTERN = Pattern.compile("https://www\\.bilibili\\.com/video/[A-Za-z0-9]+");

    /**
     * 为学习路线中的所有课程填充视频URL
     * 
     * @param learningPathDTO 学习路线DTO
     * @return 填充了视频URL的学习路线DTO
     */
    public LearningPathDTO fillVideoUrls(LearningPathDTO learningPathDTO) {
        if (learningPathDTO == null || learningPathDTO.getYearPlans() == null) {
            return learningPathDTO;
        }

        log.info("=== 开始为学习路线填充视频URL，学年计划数量: {}", learningPathDTO.getYearPlans().size());

        // 收集所有需要搜索视频的课程
        for (LearningPathDTO.YearPlan yearPlan : learningPathDTO.getYearPlans()) {
            if (yearPlan.getCourses() == null) {
                continue;
            }

            for (LearningPathDTO.CoursePlan course : yearPlan.getCourses()) {
                // 如果videoUrl为空，则搜索视频
                if (course.getVideoUrl() == null || course.getVideoUrl().trim().isEmpty()) {
                    String videoUrl = searchVideoForCourse(course.getCourseName(), course.getDescription());
                    course.setVideoUrl(videoUrl);
                    log.info("课程 [{}] 的视频URL: {}", course.getCourseName(), videoUrl);
                }
            }
        }

        log.info("=== 学习路线视频URL填充完成");
        return learningPathDTO;
    }

    /**
     * 异步为学习路线填充视频URL（不阻塞主流程）
     * 
     * @param learningPathDTO 学习路线DTO
     * @return CompletableFuture，完成后返回填充了视频URL的学习路线DTO
     */
    public CompletableFuture<LearningPathDTO> fillVideoUrlsAsync(LearningPathDTO learningPathDTO) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return fillVideoUrls(learningPathDTO);
            } catch (Exception e) {
                log.error("异步填充视频URL失败", e);
                return learningPathDTO; // 失败时返回原始数据
            }
        }, videoSearchExecutor);
    }

    /**
     * 为单个课程搜索视频
     * 
     * @param courseName 课程名称
     * @param description 课程描述（可选，用于优化搜索关键词）
     * @return 视频URL，如果未找到则返回空字符串
     */
    private String searchVideoForCourse(String courseName, String description) {
        if (courseName == null || courseName.trim().isEmpty()) {
            return "";
        }

        try {
            // 构建搜索关键词：课程名称 + 教程
            String keyword = buildSearchKeyword(courseName, description);
            
            log.info("正在为课程 [{}] 搜索视频，关键词: [{}]", courseName, keyword);

            // 调用B站搜索工具
            String searchResult = bilibiliVideoTool.searchVideos(keyword);

            // 从搜索结果中提取视频URL
            String videoUrl = extractVideoUrl(searchResult);

            if (videoUrl != null && !videoUrl.isEmpty()) {
                log.info("成功为课程 [{}] 找到视频: {}", courseName, videoUrl);
                return videoUrl;
            } else {
                log.warn("未能为课程 [{}] 找到视频", courseName);
                return "";
            }

        } catch (Exception e) {
            log.error("为课程 [{}] 搜索视频失败", courseName, e);
            return "";
        }
    }

    /**
     * 构建搜索关键词
     * 
     * @param courseName 课程名称
     * @param description 课程描述
     * @return 优化后的搜索关键词
     */
    private String buildSearchKeyword(String courseName, String description) {
        // 清理课程名称
        String cleanName = courseName.trim();

        // 如果课程名称已经包含"教程"、"入门"等词，直接使用
        if (cleanName.matches(".*(?:教程|入门|实战|基础|完整).*")) {
            return cleanName;
        }

        // 否则添加"教程"后缀
        return cleanName + " 教程";
    }

    /**
     * 从搜索结果中提取视频URL
     * 
     * @param searchResult 搜索结果字符串
     * @return 视频URL，如果未找到则返回空字符串
     */
    private String extractVideoUrl(String searchResult) {
        if (searchResult == null || searchResult.isEmpty()) {
            return "";
        }

        // 使用正则表达式提取B站视频URL
        Matcher matcher = VIDEO_URL_PATTERN.matcher(searchResult);
        if (matcher.find()) {
            return matcher.group();
        }

        // 如果正则匹配失败，尝试其他格式
        if (searchResult.contains("bilibili.com/video/")) {
            int start = searchResult.indexOf("https://www.bilibili.com/video/");
            if (start == -1) {
                start = searchResult.indexOf("http://www.bilibili.com/video/");
            }
            if (start != -1) {
                int end = searchResult.indexOf(" ", start);
                if (end == -1) {
                    end = searchResult.length();
                }
                return searchResult.substring(start, end);
            }
        }

        return "";
    }
}
