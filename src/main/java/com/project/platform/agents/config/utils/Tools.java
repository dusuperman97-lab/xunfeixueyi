package com.project.platform.agents.config.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.platform.dto.CurrentUserDTO;
import com.project.platform.entity.Question;
import com.project.platform.entity.QuestionTestRecord;
import com.project.platform.entity.Section;
import com.project.platform.entity.SectionTestSummary;
import com.project.platform.mapper.SectionTestSummaryMapper;
import com.project.platform.utils.CurrentUserThreadLocal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class Tools {

    @Value("${files.uploads.path:uploads}")
    private String uploadsPath;

    @Value("${files.uploads.baseUrl}")
    private String serverDomain;

    @Value("${files.mindmap.path:uploads/mindmap}")
    private String mindmapPath;

    @Value("${files.mindmap.baseUrl:http://localhost:1000/file/}")
    private String mindmapBaseUrl;

    @Value("${files.image.path:uploads/image}")
    private String iamgePath;

    @Value("${files.image.baseUrl}")
    private String  iamgeBaseUrl;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MultimodalFileHandler multimodalFileHandler;

    @Autowired
    private SectionTestSummaryMapper sectionTestSummaryMapper;




    /**
     * 解析AI返回的JSON响应并映射为Section实体对象
     * <p>
     * 该方法负责将AI生成的结构化JSON数据解析为数据库实体对象，支持两种JSON格式：
     * 1. JSON数组格式：提取第一个元素作为目标数据
     * 2. JSON对象格式：直接解析对象内容
     * <p>
     * 字段映射策略：
     * - sectionName：优先使用AI生成的值，若缺失则保留原始值
     * - videoUrl：直接使用AI生成的视频链接
     * - duration：直接使用AI生成的学习时长
     * - content：临时存储在contentUrl字段，后续会保存到文件
     * - id和chapterId：始终从原始Section对象继承
     * - sortOrder：使用传入的顺序参数
     * - createTime：统一设置为当前时间
     *
     * @param response AI返回的原始响应字符串（可能包含JSON数组或对象）
     * @param originalSection 原始小节对象，用于继承id、chapterId及sectionName兜底
     * @param sortOrder 小节的排序序号，通常由循环索引+1得出
     * @return 解析并映射完成的Section对象，若解析失败则返回包含原始数据的降级对象
     */
    public Section parseAndMapSection(String response, Section originalSection, int sortOrder) {

        // 配置宽容的 ObjectMapper（允许未转义换行符、非法转义字符等）
        ObjectMapper lenientMapper = new ObjectMapper()
                .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true)
                .configure(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER.mappedFeature(), true)
                .registerModule(new JavaTimeModule());
        try {
            String cleanJson = response.trim();
            // 移除 Markdown 代码块标记 (```json ... ```)
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.replaceAll("^```\\w*\\s*", "");
                cleanJson = cleanJson.replaceAll("\\s*```$", "");
                cleanJson = cleanJson.trim();
            }
            // 移除 BOM 头
            if (cleanJson.startsWith("\uFEFF")) {
                cleanJson = cleanJson.substring(1);
            }
            JsonNode rootNode = lenientMapper.readTree(cleanJson);
            JsonNode targetNode = rootNode.isArray() ? rootNode.get(0) : rootNode;

            Section section = new Section();
            section.setId(originalSection.getId());
            section.setChapterId(originalSection.getChapterId());

            if (targetNode.has("sectionName")) {
                section.setSectionName(targetNode.get("sectionName").asText());
            } else {
                section.setSectionName(originalSection.getSectionName());
            }

            if (targetNode.has("videoUrl")) {
                section.setVideoUrl(targetNode.get("videoUrl").asText());
            }
            if (targetNode.has("imageUrl")) {
                section.setImagUrl(targetNode.get("imageUrl").asText());
            }

            if (targetNode.has("duration")) {
                section.setDuration(targetNode.get("duration").asInt());
            }

            section.setSortOrder(sortOrder);

            section.setCreateTime(LocalDateTime.now());

            return section;

        } catch (Exception e) {
            log.error("解析小节JSON失败，使用原始数据", e);
            Section fallback = new Section();
            fallback.setId(originalSection.getId());
            fallback.setChapterId(originalSection.getChapterId());
            fallback.setSectionName(originalSection.getSectionName());
            fallback.setSortOrder(sortOrder);
            fallback.setCreateTime(LocalDateTime.now());
            return fallback;
        }
    }

    /**
     * 清理AI返回的响应
     * @param response
     * @return
     */
    public String cleanAiResponse(String response) {
        if (response == null || response.isEmpty()) {
            return "";
        }

        try {
            String clean = response.trim();

            // 1. 去掉 ```json ``` 包裹（大模型常见）
            if (clean.startsWith("```")) {
                clean = clean.replaceAll("^```\\w*\\n?", "");
                clean = clean.replaceAll("\\n?```$", "");
            }

            // 2. 优先用“字符串方式提取 content（最稳）”
            String content = extractContentRaw(clean);
            if (content != null) {
                return content;
            }

            // 3. 兜底：JSON解析（防止格式规范时）
            JsonNode jsonNode = objectMapper.readTree(clean);

            if (jsonNode.isArray() && jsonNode.size() > 0) {
                JsonNode first = jsonNode.get(0);
                if (first.has("content")) {
                    return first.get("content").asText();
                }
            }

            if (jsonNode.has("content")) {
                return jsonNode.get("content").asText();
            }

            return response;

        } catch (Exception e) {
            // ❗ 解析失败直接返回原文（保证不丢数据）
            return response;
        }
    }
    /**
     * 提取AI返回的响应中的内容
     * @param text
     * @return
     */
    public String extractContentRaw(String text) {
        try {
            // 找到 "content": 起始位置
            int start = text.indexOf("\"content\"");
            if (start == -1) return null;

            // 找到第一个冒号
            int colon = text.indexOf(":", start);
            if (colon == -1) return null;

            // 找到第一个引号（内容开始）
            int firstQuote = text.indexOf("\"", colon + 1);
            if (firstQuote == -1) return null;

            StringBuilder content = new StringBuilder();
            boolean escape = false;

            // 从内容开始逐字符解析（避免JSON破坏）
            for (int i = firstQuote + 1; i < text.length(); i++) {
                char c = text.charAt(i);

                if (escape) {
                    // 处理转义字符
                    switch (c) {
                        case 'n': content.append('\n'); break;
                        case 't': content.append('\t'); break;
                        case 'r': content.append('\r'); break;
                        case '"': content.append('"'); break;
                        case '\\': content.append('\\'); break;
                        default: content.append(c);
                    }
                    escape = false;
                } else {
                    if (c == '\\') {
                        escape = true;
                    } else if (c == '"') {
                        // 结束
                        return content.toString();
                    } else {
                        content.append(c);
                    }
                }
            }

        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * 将内容保存到文件
     */
    public String saveContentToFile(String content) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String fileName = "section_" + timestamp + "_" + uniqueId + ".md";

        String filePath = Paths.get(uploadsPath, fileName).toString();

        File directory = new File(uploadsPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
        }

        log.info("内容已保存到文件：{}", filePath);
        return serverDomain + fileName;
    }


    /**
     * 根据内容URL读取小节内容文件
     * @param contentUrl 文件URL（HTTP URL或相对路径）
     * @return 文件内容字符串，读取失败返回空字符串
     */
    public String readContentFromFile(String contentUrl) {
        if (contentUrl == null || contentUrl.isEmpty()) {
            return "";
        }

        try {
            log.info("=== 小节内容URL：{}", contentUrl);

            // 提取文件名（contentUrl 可能是 HTTP URL，需要转换为本地路径）
            String fileName;
            if (contentUrl.startsWith("http://") || contentUrl.startsWith("https://")) {
                // 从 URL 中提取文件名：http://localhost:1000/file/section_xxx.md -> section_xxx.md
                fileName = contentUrl.substring(contentUrl.lastIndexOf("/") + 1);
            } else {
                // 已经是相对路径或文件名
                fileName = contentUrl.startsWith("/") ? contentUrl.substring(1) : contentUrl;
            }

            // 构建本地文件路径
            String contentPath = Paths.get(uploadsPath, fileName).toString();
            log.info("=== 本地文件路径：{}", contentPath);

            File contentFile = new File(contentPath);
            if (contentFile.exists()) {
                String content = new String(java.nio.file.Files.readAllBytes(contentFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
                log.info("=== 读取小节内容文件成功，长度：{} 字符", content.length());
                return content;
            } else {
                log.warn("=== 文件不存在：{}", contentPath);
                return "";
            }
        } catch (Exception e) {
            log.warn("读取小节内容文件失败：{}", contentUrl, e);
            return "";
        }
    }

    /**
     * 将更新后的内容写回文件
     * @param contentUrl 文件URL（HTTP URL或相对路径）
     * @param content 更新后的内容
     * @return 是否写入成功
     */
    public boolean writeContentToFile(String contentUrl, String content) {
        if (contentUrl == null || contentUrl.isEmpty()) {
            log.warn("=== 内容URL为空，无法写入文件");
            return false;
        }
        if (content == null) {
            log.warn("=== 内容为空，无法写入文件");
            return false;
        }

        try {
            log.info("=== 准备写入文件：{}", contentUrl);

            // 提取文件名（contentUrl 可能是 HTTP URL，需要转换为本地路径）
            String fileName;
            if (contentUrl.startsWith("http://") || contentUrl.startsWith("https://")) {
                // 从 URL 中提取文件名：http://localhost:1000/file/section_xxx.md -> section_xxx.md
                fileName = contentUrl.substring(contentUrl.lastIndexOf("/") + 1);
            } else {
                // 已经是相对路径或文件名
                fileName = contentUrl.startsWith("/") ? contentUrl.substring(1) : contentUrl;
            }

            // 构建本地文件路径
            String contentPath = Paths.get(uploadsPath, fileName).toString();
            log.info("=== 本地文件路径：{}", contentPath);

            File contentFile = new File(contentPath);
            if (!contentFile.exists()) {
                log.warn("=== 文件不存在，将创建新文件：{}", contentPath);
                contentFile.getParentFile().mkdirs();
            }

            // 写入文件
            java.nio.file.Files.write(contentFile.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            log.info("=== 文件写入成功：{}，内容长度：{} 字符", contentPath, content.length());
            return true;
        } catch (Exception e) {
            log.error("写入文件失败：{}", contentUrl, e);
            return false;
        }
    }

    /**
     * 获取当前用户ID
     */
    public String getCurrentUserId() {
        try {
            CurrentUserDTO user = CurrentUserThreadLocal.getCurrentUser();
            if (user != null) {
                return user.getId().toString();
            }
        } catch (Exception e) {
            log.warn("获取当前用户失败", e);
        }
        return "default_user";
    }

    /**
     * 获取当前用户ID(Integer)
     */
    public Integer getCurrentUserIdInteger() {
        try {
            CurrentUserDTO user = CurrentUserThreadLocal.getCurrentUser();
            if (user != null) {
                return user.getId();
            }
        } catch (Exception e) {
            log.warn("获取当前用户失败", e);
        }
        return 0;
    }

    /**
     * 从 memoryId 中提取 userId
     * 格式: userId_UUID -> 提取 userId
     */
    public String extractUserIdFromMemoryId(String memoryId) {
        if (memoryId == null || memoryId.isEmpty()) {
            return "default_user";
        }

        int underscoreIndex = memoryId.indexOf("_");
        if (underscoreIndex > 0) {
            return memoryId.substring(0, underscoreIndex);
        }

        return memoryId; // 如果没有下划线，整个字符串就是 userId
    }

    /**
     * 从章节名称中提取图片搜索关键词
     */
    public String extractImageKeyword(String sectionName) {
        // 去除编号，提取核心概念
        String cleaned = sectionName.replaceAll("^[\\d.]+\\s*", "");
        // 添加可视化相关的后缀
        if (cleaned.contains("结构")) {
            return cleaned + "图";
        } else if (cleaned.contains("原理")) {
            return cleaned + "示意图";
        } else if (cleaned.contains("流程")) {
            return cleaned + "流程图";
        } else {
            return cleaned + "示意图";
        }
    }

    /**
     * 从章节名称中提取视频搜索关键词
     */
    public String extractVideoKeyword(String sectionName) {
        // 去除编号，保留核心技术词
        return sectionName.replaceAll("^[\\d.]+\\s*", "").trim();
    }

    /**
     * 从视频搜索结果中提取URL
     */
    public String extractVideoUrl(String videoResult) {
        if (videoResult == null || videoResult.isEmpty()) {
            return "";
        }
        // 如果返回的是完整URL，直接返回
        if (videoResult.startsWith("http")) {
            return videoResult;
        }
        // 如果包含🔗符号，提取后面的URL
        if (videoResult.contains("🔗")) {
            int index = videoResult.indexOf("🔗") + 2;
            return videoResult.substring(index).trim();
        }
        // 否则返回原始结果
        return videoResult;
    }

    /**
     * 保存思维导图到文件（mindmap目录）
     */
    public String saveMindMapToFile(String content) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String fileName = "mindmap_" + timestamp + "_" + uniqueId + ".md";

        // 使用配置的 mindmap 路径
        File directory = new File(mindmapPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String filePath = Paths.get(mindmapPath, fileName).toString();

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
        }

        log.info("思维导图已保存到文件：{}", filePath);

        // 确保 baseUrl 以 / 结尾，fileName 不以 / 开头
        String baseUrl = mindmapBaseUrl.endsWith("/") ? mindmapBaseUrl : mindmapBaseUrl + "/";
        return baseUrl + fileName;
    }


    /**
     * 更新Markdown内容中的图片URL
     * @param content 原始Markdown内容
     * @param oldImageUrl 旧的图片URL（可能是外部链接）
     * @param newImageUrl 新的图片URL（本地路径）
     * @return 更新后的Markdown内容
     */
    public String updateImageInMarkdown(String content, String oldImageUrl, String newImageUrl) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        if (oldImageUrl == null || oldImageUrl.isEmpty()) {
            return content;
        }
        if (newImageUrl == null || newImageUrl.isEmpty()) {
            return content;
        }

        // 替换Markdown格式的图片链接 ![alt](oldUrl) -> ![alt](newUrl)
        // 需要转义URL中的特殊字符用于正则表达式
        String escapedOldUrl = oldImageUrl.replace(".", "\\.")
                .replace("?", "\\?")
                .replace("&", "\\&")
                .replace("=", "\\=")
                .replace("+", "\\+")
                .replace("*", "\\*")
                .replace("/", "\\/")
                .replace("-", "-")
                .replace(":", "\\:");

        // 匹配 ![任意文本](oldImageUrl) 格式
        String regex = "!\\[([^\\]]*)\\]\\(" + escapedOldUrl + "\\)";
        return content.replaceAll(regex, "![$1](" + newImageUrl + ")");
    }

    /**
     * 更新Markdown内容中的所有百度图片URL
     * @param content 原始Markdown内容
     * @param newImageUrl 新的图片URL（本地路径）
     * @return 更新后的Markdown内容
     */
    public String updateBaiduImagesInMarkdown(String content, String newImageUrl) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        if (newImageUrl == null || newImageUrl.isEmpty()) {
            return content;
        }

        String regex = "!\\[([^\\]]*)\\]\\((https?://(?:cdn\\.)?pixabay\\.com/[^)\\s]+)\\)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String altText = matcher.group(1);
            String replacement = "![" + altText + "](" + Matcher.quoteReplacement(newImageUrl) + ")";
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    public String saveImageUrl(String imageUrl) throws IOException {

        // 1. 创建图片保存目录
        File directory = new File(iamgePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 2. 获取图片后缀
        String suffix = getFileSuffix(imageUrl);
        if (suffix == null || suffix.isEmpty()) {
            suffix = ".png";
        }

        // 3. 生成文件名
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String fileName = timestamp + uniqueId + suffix;

        // 4. 本地文件路径
        String filePath = Paths.get(iamgePath, fileName).toString();

        // 5. 下载图片
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(20_000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        try (InputStream inputStream = connection.getInputStream()) {
            Files.copy(inputStream, Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            connection.disconnect();
        }

        // 6. 返回本地路径
        return iamgeBaseUrl + fileName;
    }

    private static String getFileSuffix(String imageUrl) {
        try {
            String path = new URL(imageUrl).getPath();

            int dotIndex = path.lastIndexOf(".");
            if (dotIndex != -1) {
                return path.substring(dotIndex);
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }


    /**
     * 创建课堂考试的测试汇总记录
     */
    public SectionTestSummary createCourseExamSummary(Integer userId, Integer courseId,
                                                      String testBatchNo, List<QuestionTestRecord> records) {
        // 统计数据
        int totalQuestions = records.size();
        long answeredQuestions = records.stream().filter(r -> r.getUserAnswer() != null && !r.getUserAnswer().trim().isEmpty()).count();
        long correctQuestions = records.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
        long wrongQuestions = records.stream().filter(r -> Boolean.FALSE.equals(r.getIsCorrect())).count();
        long unansweredQuestions = totalQuestions - answeredQuestions;

        BigDecimal totalScore = records.stream()
                .map(r -> r.getScore() != null ? r.getScore() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal accuracyRate = answeredQuestions > 0
                ? BigDecimal.valueOf(correctQuestions).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(answeredQuestions), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        int totalTimeSpent = records.get(0).getTimeSpent();
        double avgSeconds = totalTimeSpent / records.size();
        BigDecimal avgTimeSpent = BigDecimal.valueOf(avgSeconds).setScale(2, RoundingMode.HALF_UP);
        LocalDateTime startTime = records.stream()
                .map(QuestionTestRecord::getCreateTime)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        LocalDateTime endTime = LocalDateTime.now();

        // 创建课堂考试汇总记录
        SectionTestSummary summary = new SectionTestSummary();
        summary.setUserId(userId);
        summary.setCourseId(courseId);  // 设置课程ID
        summary.setSectionId(0);        // 课堂考试的sectionId固定为0
        summary.setTestName(records.get(0).getKnowledgePoint() + "课程考试");
        summary.setTestBatchNo(testBatchNo);
        summary.setTotalQuestions(totalQuestions);
        summary.setAnsweredQuestions((int) answeredQuestions);
        summary.setCorrectQuestions((int) correctQuestions);
        summary.setWrongQuestions((int) wrongQuestions);
        summary.setUnansweredQuestions((int) unansweredQuestions);
        summary.setTotalScore(totalScore);
        summary.setAccuracyRate(accuracyRate);
        summary.setAvgTimeSpent(avgTimeSpent);
        summary.setTotalTimeSpent(totalTimeSpent);
        summary.setStartTime(startTime);
        summary.setEndTime(endTime);
        summary.setCreateTime(LocalDateTime.now());
        summary.setUpdateTime(LocalDateTime.now());
        summary.setStatus("COMPLETED");

        // 幂等处理：已存在同批次则更新
        SectionTestSummary existing = sectionTestSummaryMapper.selectByBatchNo(testBatchNo);
        if (existing != null) {
            summary.setId(existing.getId());
            sectionTestSummaryMapper.update(summary);
            log.info("=== 随堂练习汇总记录更新成功（幂等），批次号：{}", testBatchNo);
        } else {
            sectionTestSummaryMapper.insert(summary);
            log.info("=== 随堂练习汇总记录创建成功，批次号：{}", testBatchNo);
        }

        return summary;
    }

    /**
     * 创建课堂考试的测试汇总记录
     */
    public SectionTestSummary createSectionExamSummary(Integer userId, Integer courseId,Integer sectionId,
                                                      String testBatchNo, List<QuestionTestRecord> records) {
        // 统计数据
        int totalQuestions = records.size();
        long answeredQuestions = records.stream().filter(r -> r.getUserAnswer() != null && !r.getUserAnswer().trim().isEmpty()).count();
        long correctQuestions = records.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
        long wrongQuestions = records.stream().filter(r -> Boolean.FALSE.equals(r.getIsCorrect())).count();
        long unansweredQuestions = totalQuestions - answeredQuestions;

        BigDecimal totalScore = records.stream()
                .map(r -> r.getScore() != null ? r.getScore() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal accuracyRate = answeredQuestions > 0
                ? BigDecimal.valueOf(correctQuestions).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(answeredQuestions), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        int totalTimeSpent = records.get(0).getTimeSpent();
        double avgSeconds = totalTimeSpent / records.size();
        BigDecimal avgTimeSpent = BigDecimal.valueOf(avgSeconds).setScale(2, RoundingMode.HALF_UP);
        LocalDateTime startTime = records.stream()
                .map(QuestionTestRecord::getCreateTime)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        LocalDateTime endTime = LocalDateTime.now();

        // 创建课堂考试汇总记录
        SectionTestSummary summary = new SectionTestSummary();
        summary.setUserId(userId);
        summary.setCourseId(courseId);  // 设置课程ID
        summary.setSectionId(sectionId);
        summary.setTestName(records.get(0).getKnowledgePoint() + "随堂练习");
        summary.setTestBatchNo(testBatchNo);
        summary.setTotalQuestions(totalQuestions);
        summary.setAnsweredQuestions((int) answeredQuestions);
        summary.setCorrectQuestions((int) correctQuestions);
        summary.setWrongQuestions((int) wrongQuestions);
        summary.setUnansweredQuestions((int) unansweredQuestions);
        summary.setTotalScore(totalScore);
        summary.setAccuracyRate(accuracyRate);
        summary.setAvgTimeSpent(avgTimeSpent);
        summary.setTotalTimeSpent(totalTimeSpent);
        summary.setStartTime(startTime);
        summary.setEndTime(endTime);
        summary.setCreateTime(LocalDateTime.now());
        summary.setUpdateTime(LocalDateTime.now());
        summary.setStatus("COMPLETED");

        // 幂等处理：已存在同批次则更新，否则插入（防止重复提交报唯一键冲突）
        SectionTestSummary existing = sectionTestSummaryMapper.selectByBatchNo(testBatchNo);
        if (existing != null) {
            summary.setId(existing.getId());
            sectionTestSummaryMapper.update(summary);
            log.info("=== 随堂练习汇总记录更新成功（幂等），批次号：{}", testBatchNo);
        } else {
            sectionTestSummaryMapper.insert(summary);
            log.info("=== 随堂练习汇总记录创建成功，批次号：{}", testBatchNo);
        }

        return summary;
    }
}
